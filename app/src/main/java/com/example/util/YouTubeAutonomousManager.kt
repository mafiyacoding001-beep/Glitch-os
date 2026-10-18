package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.ProcessedCommentEntity
import com.example.data.local.QuotaLedgerEntity
import com.example.pipeline.NotrackWebViewClient
import com.example.data.youtube.VideoItemData
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class RealYouTubeComment(
    val threadId: String,
    val commentId: String,
    val videoId: String,
    val videoTitle: String,
    val authorName: String,
    val authorProfileImageUrl: String?,
    val text: String,
    val publishedAt: String,
    val likeCount: Int,
    val isReplied: Boolean,
    val smartReplies: List<SmartReplyOption> = emptyList()
)

data class AutonomousRunSummary(
    val commentsScanned: Int,
    val newRepliesPosted: Int,
    val currentQuotaUsed: Int,
    val details: String,
    val success: Boolean,
    val retryable: Boolean = false
)

object YouTubeAutonomousManager {
    private const val TAG = "YouTubeAutonomousMgr"
    private const val MAX_DAILY_QUOTA_UNITS = 9500 // Hard safety cap under 10k daily limit

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    /**
     * Gets or creates today's Quota Ledger
     */
    suspend fun getOrCreateTodayQuota(context: Context): QuotaLedgerEntity = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getInstance(context).autonomousChannelDao()
        val today = getTodayDateString()
        var quota = dao.getQuotaForDate(today)
        if (quota == null) {
            quota = QuotaLedgerEntity(date = today)
            dao.insertOrUpdateQuota(quota)
        }
        quota
    }

    /**
     * Increments quota ledger units safely
     */
    suspend fun consumeQuota(context: Context, units: Int, isUpload: Boolean = false, isComment: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            val dao = AppDatabase.getInstance(context).autonomousChannelDao()
            val today = getTodayDateString()
            val current = dao.getQuotaForDate(today) ?: QuotaLedgerEntity(date = today)
            val updated = current.copy(
                usedUnits = current.usedUnits + units,
                uploadCountToday = if (isUpload) current.uploadCountToday + 1 else current.uploadCountToday,
                commentsRepliedToday = if (isComment) current.commentsRepliedToday + 1 else current.commentsRepliedToday
            )
            dao.insertOrUpdateQuota(updated)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating quota ledger: ${e.message}")
        }
    }

    /**
     * Fetches real comments from the authenticated channel or its recent videos
     */
    suspend fun fetchRealChannelComments(
        context: Context,
        channelId: String? = null,
        videoList: List<VideoItemData> = emptyList(),
        maxResults: Int = 15
    ): Result<List<RealYouTubeComment>> = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.failure(Exception("Google Sign-In required to fetch YouTube comments"))

            val token = GoogleSignInHelper.getOAuthAccessToken(context, account)
                ?: return@withContext Result.failure(Exception("Active YouTube OAuth token missing"))

            val dao = AppDatabase.getInstance(context).autonomousChannelDao()
            val commentsList = mutableListOf<RealYouTubeComment>()

            // 1. Try channel-level commentThreads first
            val effectiveChannelId = channelId ?: GoogleSignInHelper.fetchYouTubeChannelInfo(context).getOrNull()?.channelId

            var fetchedViaChannel = false
            var successfulRequests = 0
            val requestFailures = mutableListOf<String>()
            if (!effectiveChannelId.isNullOrBlank()) {
                val url = "https://www.googleapis.com/youtube/v3/commentThreads?part=snippet&allThreadsRelatedToChannelId=$effectiveChannelId&maxResults=$maxResults&order=time"
                val req = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()

                try {
                    val resp = okHttpClient.newCall(req).execute()
                    val body = resp.body?.string().orEmpty()
                    if (resp.isSuccessful) {
                        successfulRequests++
                        consumeQuota(context, 1) // 1 quota unit for list
                        val json = JSONObject(body)
                        val items = json.optJSONArray("items")
                        if (items != null) {
                            for (i in 0 until items.length()) {
                                val item = items.getJSONObject(i)
                                val threadId = item.optString("id", "")
                                val snippet = item.optJSONObject("snippet")
                                val topComment = snippet?.optJSONObject("topLevelComment")
                                val topSnippet = topComment?.optJSONObject("snippet")

                                if (topSnippet != null) {
                                    val commentId = topComment.optString("id", threadId)
                                    val vidId = topSnippet.optString("videoId", "")
                                    val author = topSnippet.optString("authorDisplayName", "Viewer")
                                    val authorImg = topSnippet.optString("authorProfileImageUrl", null)
                                    val text = topSnippet.optString("textDisplay", "").ifBlank {
                                        topSnippet.optString("textOriginal", "")
                                    }
                                    val pubAt = topSnippet.optString("publishedAt", "").take(10)
                                    val likes = topSnippet.optInt("likeCount", 0)

                                    val isReplied = dao.isCommentProcessed(threadId)

                                    // Find matching video title
                                    val matchingTitle = videoList.firstOrNull { it.id == vidId }?.title ?: "Shorts Video"

                                    commentsList.add(
                                        RealYouTubeComment(
                                            threadId = threadId,
                                            commentId = commentId,
                                            videoId = vidId,
                                            videoTitle = matchingTitle,
                                            authorName = author,
                                            authorProfileImageUrl = authorImg,
                                            text = text,
                                            publishedAt = pubAt,
                                            likeCount = likes,
                                            isReplied = isReplied
                                        )
                                    )
                                }
                            }
                            fetchedViaChannel = true
                        }
                    } else {
                        requestFailures.add("Channel comments request failed (HTTP ${resp.code}).")
                    }
                } catch (e: Exception) {
                    requestFailures.add("Channel comments request failed: ${e.localizedMessage ?: "Unknown error"}.")
                    Log.w(TAG, "Channel commentThreads query failed, will fallback to per-video scan: ${e.message}")
                }
            }

            // 2. Fallback: if channel query returned no comments or failed, query the top 3 recent videos
            if (!fetchedViaChannel || commentsList.isEmpty()) {
                val videosToScan = videoList.take(3)
                for (v in videosToScan) {
                    if (v.id.isNotBlank()) {
                        val vidUrl = "https://www.googleapis.com/youtube/v3/commentThreads?part=snippet&videoId=${v.id}&maxResults=5&order=time"
                        val vidReq = Request.Builder()
                            .url(vidUrl)
                            .addHeader("Authorization", "Bearer $token")
                            .get()
                            .build()

                        try {
                            val vidResp = okHttpClient.newCall(vidReq).execute()
                            val vidBody = vidResp.body?.string().orEmpty()
                            if (vidResp.isSuccessful) {
                                successfulRequests++
                                consumeQuota(context, 1)
                                val vidJson = JSONObject(vidBody)
                                val items = vidJson.optJSONArray("items")
                                if (items != null) {
                                    for (j in 0 until items.length()) {
                                        val item = items.getJSONObject(j)
                                        val threadId = item.optString("id", "")
                                        val snippet = item.optJSONObject("snippet")
                                        val topComment = snippet?.optJSONObject("topLevelComment")
                                        val topSnippet = topComment?.optJSONObject("snippet")

                                        if (topSnippet != null) {
                                            val commentId = topComment.optString("id", threadId)
                                            val author = topSnippet.optString("authorDisplayName", "Viewer")
                                            val authorImg = topSnippet.optString("authorProfileImageUrl", null)
                                            val text = topSnippet.optString("textDisplay", "").ifBlank {
                                                topSnippet.optString("textOriginal", "")
                                            }
                                            val pubAt = topSnippet.optString("publishedAt", "").take(10)
                                            val likes = topSnippet.optInt("likeCount", 0)
                                            val isReplied = dao.isCommentProcessed(threadId)

                                            commentsList.add(
                                                RealYouTubeComment(
                                                    threadId = threadId,
                                                    commentId = commentId,
                                                    videoId = v.id,
                                                    videoTitle = v.title,
                                                    authorName = author,
                                                    authorProfileImageUrl = authorImg,
                                                    text = text,
                                                    publishedAt = pubAt,
                                                    likeCount = likes,
                                                    isReplied = isReplied
                                                )
                                            )
                                        }
                                    }
                                }
                            } else {
                                requestFailures.add("Comments request for video ${v.id} failed (HTTP ${vidResp.code}).")
                            }
                        } catch (e: Exception) {
                            requestFailures.add("Comments request for video ${v.id} failed: ${e.localizedMessage ?: "Unknown error"}.")
                            Log.w(TAG, "Per-video comment query failed for ${v.id}: ${e.message}")
                        }
                    }
                }
            }

            if (successfulRequests == 0) {
                val message = requestFailures.distinct().joinToString(" ").ifBlank {
                    "No channel or recent videos were available to load YouTube comments."
                }
                return@withContext Result.failure(IllegalStateException(message))
            }
            Result.success(commentsList)
        } catch (e: Exception) {
            Log.e(TAG, "fetchRealChannelComments exception", e)
            Result.failure(e)
        }
    }

    /**
     * Posts a comment reply directly to YouTube Data API (50 quota units)
     */
    suspend fun postCommentReply(
        context: Context,
        threadId: String,
        replyText: String,
        videoId: String = "",
        authorName: String = "",
        originalText: String = ""
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val quota = getOrCreateTodayQuota(context)
            if (quota.usedUnits + 50 > MAX_DAILY_QUOTA_UNITS) {
                return@withContext Result.failure(Exception("Daily YouTube API Quota limit safety cap reached (${quota.usedUnits}/10,000 units). Reply skipped to preserve upload quota."))
            }

            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.failure(Exception("Google Sign-In required to post YouTube comment reply"))

            val token = GoogleSignInHelper.getOAuthAccessToken(context, account)
                ?: return@withContext Result.failure(Exception("OAuth token missing. Please sign in with YouTube permissions."))

            val url = "https://www.googleapis.com/youtube/v3/comments?part=snippet"

            val jsonBody = JSONObject().apply {
                put("snippet", JSONObject().apply {
                    put("parentId", threadId)
                    put("textOriginal", replyText)
                })
            }

            val requestBody = jsonBody.toString().toMediaType().let { mediaType ->
                jsonBody.toString().toRequestBody(mediaType)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                consumeQuota(context, 50, isComment = true)
                // Record in Room DB
                val dao = AppDatabase.getInstance(context).autonomousChannelDao()
                dao.insertProcessedComment(
                    ProcessedCommentEntity(
                        commentId = threadId,
                        videoId = videoId,
                        authorDisplayName = authorName,
                        originalText = originalText,
                        replyText = replyText,
                        repliedAtEpochMs = System.currentTimeMillis()
                    )
                )
                Log.i(TAG, "Successfully posted reply to YouTube comment $threadId: $replyText")
                Result.success(true)
            } else {
                val errorMsg = "YouTube API error HTTP ${response.code}: $body"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "postCommentReply exception", e)
            Result.failure(e)
        }
    }

    /**
     * Executes fully autonomous comment moderation and subscriber conversion run via NoTrack AI
     */
    suspend fun runAutonomousCommentScanAndReply(
        context: Context,
        notrackClient: NotrackWebViewClient? = null
    ): AutonomousRunSummary = withContext(Dispatchers.IO) {
        val quota = getOrCreateTodayQuota(context)
        if (quota.usedUnits + 150 > MAX_DAILY_QUOTA_UNITS) {
            return@withContext AutonomousRunSummary(
                commentsScanned = 0,
                newRepliesPosted = 0,
                currentQuotaUsed = quota.usedUnits,
                details = "Daily YouTube quota safety threshold reached (${quota.usedUnits}/10,000 units). Autonomous run postponed.",
                success = false
            )
        }

        // 1. Fetch channel videos to get context
        val infoRes = GoogleSignInHelper.fetchYouTubeChannelInfo(context)
        val uploadsPlaylistId = infoRes.getOrNull()?.uploadsPlaylistId.orEmpty()
        val videos = if (uploadsPlaylistId.isNotBlank()) {
            GoogleSignInHelper.fetchYouTubeChannelVideos(context, uploadsPlaylistId).getOrDefault(emptyList())
        } else {
            emptyList()
        }

        // 2. Fetch comments
        val commentRes = fetchRealChannelComments(context, videoList = videos, maxResults = 10)
        val comments = commentRes.getOrElse { error ->
            return@withContext AutonomousRunSummary(
                commentsScanned = 0,
                newRepliesPosted = 0,
                currentQuotaUsed = quota.usedUnits,
                details = "Could not fetch live YouTube comments: ${error.localizedMessage ?: "Unknown provider error"}",
                success = false,
                retryable = true
            )
        }

        val unreplied = comments.filter { !it.isReplied }
        if (unreplied.isEmpty()) {
            return@withContext AutonomousRunSummary(
                commentsScanned = comments.size,
                newRepliesPosted = 0,
                currentQuotaUsed = quota.usedUnits,
                details = "All ${comments.size} latest comments are already replied or moderated.",
                success = true
            )
        }

        // Process up to 3 unreplied comments to conserve quota and ensure organic pacing
        val targetComments = unreplied.take(3)
        var repliedCount = 0
        val replyFailures = mutableListOf<String>()

        for (comment in targetComments) {
            try {
                // Generate high-converting response through NoTrack AI
                val client = notrackClient
                    ?: throw IllegalStateException("AI provider unavailable; skipping automatic comment reply.")
                val smartReplies = client.askNotrackForSmartReplies(
                    commentText = comment.text,
                    videoTitle = comment.videoTitle,
                    category = "Entertainment"
                )

                // Pick primary high-conversion reply
                val chosenReply = smartReplies.firstOrNull { it.category.contains("Subscriber", ignoreCase = true) }?.replyText
                    ?: smartReplies.firstOrNull()?.replyText
                    ?: throw IllegalStateException("AI provider returned no usable comment replies.")

                // Post reply directly to YouTube
                val postRes = postCommentReply(
                    context = context,
                    threadId = comment.threadId,
                    replyText = chosenReply,
                    videoId = comment.videoId,
                    authorName = comment.authorName,
                    originalText = comment.text
                )

                if (postRes.isSuccess) {
                    repliedCount++
                    delay(1500) // 1.5s human-like pacing
                } else {
                    val error = postRes.exceptionOrNull()
                    replyFailures.add(error?.localizedMessage ?: "YouTube rejected the reply.")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed auto-replying to comment ${comment.threadId}: ${e.message}")
                replyFailures.add(e.localizedMessage ?: "Reply generation or posting failed.")
            }
        }

        val updatedQuota = getOrCreateTodayQuota(context)
        val success = replyFailures.isEmpty()
        val details = buildString {
            append("Scanned ${comments.size} comments. Posted $repliedCount replies via NoTrack AI.")
            if (replyFailures.isNotEmpty()) {
                append(" ${replyFailures.size} reply attempt(s) failed; the run can retry.")
            }
        }
        AutonomousRunSummary(
            commentsScanned = comments.size,
            newRepliesPosted = repliedCount,
            currentQuotaUsed = updatedQuota.usedUnits,
            details = details,
            success = success,
            retryable = replyFailures.isNotEmpty()
        )
    }
}
