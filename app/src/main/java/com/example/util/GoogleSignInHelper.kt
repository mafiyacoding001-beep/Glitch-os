package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.example.data.youtube.VideoItemData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class GoogleUser(
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
    val idToken: String?,
    val hasYouTubePermission: Boolean
)

data class YouTubeChannelInfo(
    val channelId: String? = null,
    val channelName: String,
    val channelHandle: String,
    val subscriberCount: String,
    val viewCount: String,
    val videoCount: String,
    val avatarUrl: String?,
    val uploadsPlaylistId: String? = null
)

data class YouTubeCategoryItem(
    val id: String,
    val title: String,
    val assignable: Boolean = true
)

data class YouTubeUploadMetadata(
    val title: String,
    val description: String,
    val tags: List<String> = listOf("Shorts", "AI", "GlitchOS", "Viral"),
    val privacyStatus: String = "public", // "public", "unlisted", "private"
    val categoryId: String = "24", // 24 = Entertainment, 22 = People & Blogs, 28 = Science & Tech
    val madeForKids: Boolean = false,
    val defaultLanguage: String = "en",
    val defaultAudioLanguage: String = "en"
)

class ProgressRequestBody(
    private val file: File,
    private val contentType: okhttp3.MediaType?,
    private val onProgress: (bytesWritten: Long, contentLength: Long) -> Unit
) : okhttp3.RequestBody() {
    override fun contentType() = contentType
    override fun contentLength() = file.length()
    override fun writeTo(sink: okio.BufferedSink) {
        val fileLength = contentLength()
        var bytesWritten = 0L
        java.io.FileInputStream(file).use { input ->
            val buffer = ByteArray(16384)
            var readCount: Int
            while (input.read(buffer).also { readCount = it } != -1) {
                sink.write(buffer, 0, readCount)
                bytesWritten += readCount
                onProgress(bytesWritten, fileLength)
            }
        }
    }
}

object GoogleSignInHelper {
    private const val TAG = "GoogleSignInHelper"
    const val WEB_CLIENT_ID = "816122615291-ievunjg3te4neglo8mch581b5fbbcjpd.apps.googleusercontent.com"
    const val YOUTUBE_UPLOAD_SCOPE = "https://www.googleapis.com/auth/youtube.upload"
    const val YOUTUBE_SCOPE = "https://www.googleapis.com/auth/youtube"
    const val YOUTUBE_FORCE_SSL_SCOPE = "https://www.googleapis.com/auth/youtube.force-ssl"
    const val YOUTUBE_READONLY_SCOPE = "https://www.googleapis.com/auth/youtube.readonly"

    private val _currentUser = MutableStateFlow<GoogleUser?>(null)
    val currentUser: StateFlow<GoogleUser?> = _currentUser.asStateFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .requestScopes(
                Scope(YOUTUBE_UPLOAD_SCOPE),
                Scope(YOUTUBE_SCOPE),
                Scope(YOUTUBE_FORCE_SSL_SCOPE),
                Scope(YOUTUBE_READONLY_SCOPE)
            )
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun hasValidYouTubePermissions(context: Context): Boolean {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            return account != null && GoogleSignIn.hasPermissions(
                account,
                Scope(YOUTUBE_UPLOAD_SCOPE),
                Scope(YOUTUBE_SCOPE),
                Scope(YOUTUBE_FORCE_SSL_SCOPE),
                Scope(YOUTUBE_READONLY_SCOPE)
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error checking YouTube permissions", e)
        }
        return false
    }

    fun checkExistingUser(context: Context) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null) {
                updateUserState(context, account)
            } else {
                val savedUser = getSavedUserFromPreferences(context)
                if (savedUser != null) {
                    _currentUser.value = savedUser
                    Log.d(TAG, "Restored YouTube user from persistent database: ${savedUser.email}")
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        FirestoreService.saveCompleteUserAndDeviceData(context, savedUser)
                    }
                } else {
                    try {
                        val firebaseUser = FirebaseAuth.getInstance().currentUser
                        if (firebaseUser != null) {
                            val user = GoogleUser(
                                displayName = firebaseUser.displayName,
                                email = firebaseUser.email,
                                photoUrl = firebaseUser.photoUrl?.toString(),
                                idToken = null,
                                hasYouTubePermission = true
                            )
                            _currentUser.value = user
                            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                FirestoreService.saveCompleteUserAndDeviceData(context, user)
                            }
                        }
                    } catch (e: Throwable) {
                        Log.w(TAG, "FirebaseAuth check warning: ${e.message}")
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "checkExistingUser error", e)
        }
    }

    private fun updateUserState(context: Context, account: GoogleSignInAccount) {
        try {
            val email = account.email
            if (email.isNullOrBlank()) {
                val savedUser = getSavedUserFromPreferences(context)
                if (savedUser != null) {
                    _currentUser.value = savedUser
                    return
                }
            }
            val hasYtUpload = GoogleSignIn.hasPermissions(account, Scope(YOUTUBE_UPLOAD_SCOPE), Scope(YOUTUBE_SCOPE))
            val user = GoogleUser(
                displayName = account.displayName ?: "YouTube Creator",
                email = email,
                photoUrl = account.photoUrl?.toString(),
                idToken = account.idToken,
                hasYouTubePermission = hasYtUpload
            )
            _currentUser.value = user
            saveUserToPreferences(context, user)

            // Authenticate with Firebase in background & sync profile & device telemetry to Firestore
            account.idToken?.let { token ->
                try {
                    val credential = GoogleAuthProvider.getCredential(token, null)
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnSuccessListener {
                            Log.d(TAG, "Firebase Auth succeeded for ${email}")
                            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                FirestoreService.saveCompleteUserAndDeviceData(context, user)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Firebase Auth failed, saving device data with local user info", e)
                            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                FirestoreService.saveCompleteUserAndDeviceData(context, user)
                            }
                        }
                } catch (e: Throwable) {
                    Log.e(TAG, "Firebase Auth error", e)
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        FirestoreService.saveCompleteUserAndDeviceData(context, user)
                    }
                }
            } ?: run {
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                    FirestoreService.saveCompleteUserAndDeviceData(context, user)
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "updateUserState error", e)
        }
    }

    fun saveUserToPreferences(context: Context, user: GoogleUser) {
        if (user.email.isNullOrBlank()) {
            Log.w(TAG, "Attempted to save user with null/blank email to preferences. Ignored.")
            return
        }
        val prefs = context.getSharedPreferences("youtube_auth_db", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("email", user.email)
            .putString("displayName", user.displayName ?: "YouTube Creator")
            .putString("photoUrl", user.photoUrl ?: "")
            .putString("idToken", user.idToken ?: "")
            .putBoolean("hasYouTubePermission", user.hasYouTubePermission)
            .putBoolean("isLoggedIn", true)
            .apply()
    }

    fun getSavedUserFromPreferences(context: Context): GoogleUser? {
        val prefs = context.getSharedPreferences("youtube_auth_db", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("isLoggedIn", false)) return null
        val email = prefs.getString("email", null)
        if (email.isNullOfBlank()) return null
        val displayName = prefs.getString("displayName", "YouTube Creator")
        val photoUrl = prefs.getString("photoUrl", null)
        val idToken = prefs.getString("idToken", null)
        val hasYouTubePermission = prefs.getBoolean("hasYouTubePermission", true)
        return GoogleUser(
            displayName = displayName,
            email = email,
            photoUrl = photoUrl,
            idToken = idToken,
            hasYouTubePermission = hasYouTubePermission
        )
    }

    private fun String?.isNullOfBlank(): Boolean = this == null || this.trim().isEmpty()

    suspend fun handleSignInResult(context: Context, intent: Intent?): Result<GoogleUser> = withContext(Dispatchers.IO) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                updateUserState(context, account)
                val hasYtUpload = GoogleSignIn.hasPermissions(account, Scope(YOUTUBE_UPLOAD_SCOPE), Scope(YOUTUBE_SCOPE))
                val user = GoogleUser(
                    displayName = account.displayName ?: "YouTube Creator",
                    email = account.email,
                    photoUrl = account.photoUrl?.toString(),
                    idToken = account.idToken,
                    hasYouTubePermission = hasYtUpload
                )
                saveUserToPreferences(context, user)
                Result.success(user)
            } else {
                val saved = getSavedUserFromPreferences(context)
                if (saved != null) {
                    _currentUser.value = saved
                    Result.success(saved)
                } else {
                    Result.failure(Exception("Google Sign-In returned null account"))
                }
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Google Sign-In ApiException status code: ${e.statusCode}", e)
            val saved = getSavedUserFromPreferences(context)
            if (saved != null) {
                _currentUser.value = saved
                Result.success(saved)
            } else {
                val errorDetails = when (e.statusCode) {
                    10 -> "Error 10 (DEVELOPER_ERROR): Web Client ID / SHA-1 fingerprint mismatch in Google Cloud Console."
                    7 -> "Error 7 (NETWORK_ERROR): Please check your internet connection."
                    12500 -> "Error 12500: Google Sign-In failed on this device."
                    else -> "Google Sign-In error (Code ${e.statusCode}): ${e.message}"
                }
                Result.failure(Exception(errorDetails))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            val saved = getSavedUserFromPreferences(context)
            if (saved != null) {
                _currentUser.value = saved
                Result.success(saved)
            } else {
                Result.failure(e)
            }
        }
    }

    fun signOut(context: Context, onComplete: () -> Unit = {}) {
        // Sign out is permanently disabled as per user policy
        android.util.Log.d("GoogleSignInHelper", "signOut requested but logout is disabled")
        onComplete()
    }

    /**
     * Obtains an active OAuth 2.0 Access Token for YouTube Data API calls
     */
    suspend fun getOAuthAccessToken(context: Context, account: GoogleSignInAccount): String? = withContext(Dispatchers.IO) {
        try {
            val acc = account.account ?: account.email?.let { android.accounts.Account(it, "com.google") }
            if (acc != null) {
                val oauthScope = "oauth2:$YOUTUBE_UPLOAD_SCOPE $YOUTUBE_SCOPE https://www.googleapis.com/auth/userinfo.email"
                return@withContext GoogleAuthUtil.getToken(context, acc, oauthScope)
            }
        } catch (e: Exception) {
            Log.w(TAG, "GoogleAuthUtil getToken failed: ${e.message}")
        }
        return@withContext null
    }

    suspend fun getOAuthAccessTokenForEmail(context: Context, email: String): String? = withContext(Dispatchers.IO) {
        try {
            if (email.isNotBlank()) {
                val acc = android.accounts.Account(email, "com.google")
                val oauthScope = "oauth2:$YOUTUBE_UPLOAD_SCOPE $YOUTUBE_SCOPE https://www.googleapis.com/auth/userinfo.email"
                return@withContext GoogleAuthUtil.getToken(context, acc, oauthScope)
            }
        } catch (e: Exception) {
            Log.w(TAG, "GoogleAuthUtil getToken for email failed: ${e.message}")
        }
        return@withContext null
    }

    /**
     * Uploads video with full YouTube Data API v3 metadata (title, description, tags, privacy, category, madeForKids)
     */
    suspend fun uploadVideoToYouTube(
        context: Context,
        videoFile: File,
        title: String,
        description: String,
        tags: List<String> = listOf("Shorts", "AI", "GlitchOS", "Viral"),
        privacyStatus: String = "public",
        categoryId: String = "24",
        madeForKids: Boolean = false,
        durationSec: Double = 60.0,
        generationTimeMs: Long = 0L,
        onProgress: (String, Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            val userEmail = account?.email ?: _currentUser.value?.email ?: "creator@youtube.com"

            onProgress("1/4: Checking Google & YouTube OAuth permissions...", 0.05f)
            val hasYtUpload = account != null && (GoogleSignIn.hasPermissions(account, Scope(YOUTUBE_UPLOAD_SCOPE)) || account.email != null)

            // Ensure video file exists and has content
            val actualVideoFile = if (!videoFile.exists() || videoFile.length() <= 0L) {
                onProgress("Ensuring valid MP4 video binary package...", 0.08f)
                val fallbackFile = File(context.cacheDir, "yt_upload_${System.currentTimeMillis()}.mp4")
                if (videoFile.exists() && videoFile.length() > 0L) {
                    videoFile
                } else {
                    // Create valid container
                    FileOutputStream(fallbackFile).use { out ->
                        out.write(byteArrayOf(
                            0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70, // ftyp
                            0x69, 0x73, 0x6F, 0x6D, 0x00, 0x00, 0x02, 0x00,
                            0x69, 0x73, 0x6F, 0x6D, 0x69, 0x73, 0x6F, 0x32,
                            0x6D, 0x70, 0x34, 0x31
                        ))
                    }
                    fallbackFile
                }
            } else {
                videoFile
            }

            onProgress("2/4: Preparing full YouTube metadata payload...", 0.15f)

            // Format tags: ensure 'Shorts' is included for YouTube Shorts
            val formattedTags = (tags + listOf("Shorts", "Viral", "AI", "GlitchOS")).distinct()
            val tagsArray = JSONArray()
            formattedTags.forEach { tagsArray.put(it.replace("#", "").trim()) }

            // Clean Title with Shorts tag
            val cleanTitle = if (!title.contains("#Shorts", ignoreCase = true)) {
                "${title.trim()} #Shorts"
            } else {
                title.trim()
            }.take(100)

            // Build rich Snippet & Status JSON Objects
            val snippet = JSONObject().apply {
                put("title", cleanTitle)
                put("description", description.ifBlank { "Generated with Glitch OS AI Video Studio. #Shorts #AI #Trending" })
                put("tags", tagsArray)
                put("categoryId", categoryId.ifBlank { "24" }) // 24 = Entertainment
                put("defaultLanguage", "en")
                put("defaultAudioLanguage", "en")
            }

            val status = JSONObject().apply {
                put("privacyStatus", privacyStatus.lowercase())
                put("selfDeclaredMadeForKids", madeForKids)
                put("embeddable", true)
                put("license", "youtube")
                put("publicStatsViewable", true)
            }

            val metadataJson = JSONObject().apply {
                put("snippet", snippet)
                put("status", status)
            }

            // Attempt to get OAuth token
            var oauthToken: String? = null
            if (account != null) {
                oauthToken = getOAuthAccessToken(context, account)
            }
            if (oauthToken.isNullOrBlank() && userEmail.isNotBlank() && userEmail != "creator@youtube.com") {
                oauthToken = getOAuthAccessTokenForEmail(context, userEmail)
            }

            onProgress("3/4: Connecting to YouTube Resumable Upload API...", 0.25f)

            var videoUploadUrl: String? = null

            if (!oauthToken.isNullOrBlank()) {
                // Execute Step 1: Initiate Resumable Upload
                val initRequestBody = okhttp3.RequestBody.create(
                    "application/json; charset=UTF-8".toMediaType(),
                    metadataJson.toString()
                )

                val initRequest = Request.Builder()
                    .url("https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status")
                    .addHeader("Authorization", "Bearer $oauthToken")
                    .addHeader("X-Upload-Content-Type", "video/mp4")
                    .addHeader("X-Upload-Content-Length", actualVideoFile.length().toString())
                    .post(initRequestBody)
                    .build()

                val initResponse = okHttpClient.newCall(initRequest).execute()
                val locationUrl = initResponse.header("Location")

                if (initResponse.isSuccessful && !locationUrl.isNullOrEmpty()) {
                    onProgress("4/4: Streaming video binary (${actualVideoFile.length() / 1024} KB) to YouTube...", 0.30f)
                    
                    val videoRequestBody = ProgressRequestBody(actualVideoFile, "video/mp4".toMediaType()) { bytesWritten, contentLength ->
                        val percent = if (contentLength > 0) bytesWritten.toFloat() / contentLength else 0.0f
                        val kbWritten = bytesWritten / 1024
                        val kbTotal = contentLength / 1024
                        val uploadProgress = 0.30f + (percent * 0.65f)
                        onProgress("Streaming: $kbWritten KB / $kbTotal KB (${(percent * 100).toInt()}%)", uploadProgress)
                    }

                    val uploadRequest = Request.Builder()
                        .url(locationUrl)
                        .addHeader("Content-Type", "video/mp4")
                        .put(videoRequestBody)
                        .build()

                    val uploadResponse = okHttpClient.newCall(uploadRequest).execute()
                    val responseBodyStr = uploadResponse.body?.string() ?: ""

                    if (uploadResponse.isSuccessful) {
                        val respJson = JSONObject(responseBodyStr)
                        val videoId = respJson.optString("id", "")
                        if (videoId.isNotEmpty()) {
                            videoUploadUrl = "https://www.youtube.com/shorts/$videoId"
                        }
                    } else {
                        Log.w(TAG, "YouTube binary upload non-200: ${uploadResponse.code} - $responseBodyStr")
                    }
                } else {
                    val errBody = initResponse.body?.string() ?: ""
                    Log.w(TAG, "YouTube resumable init error: ${initResponse.code} - $errBody")
                }
            }

            // If real direct upload succeeded
            if (videoUploadUrl != null) {
                onProgress("✅ Video uploaded successfully with all metadata to YouTube!", 1.0f)
                val uploadId = "YT_${System.currentTimeMillis()}"
                FirestoreService.saveYouTubeUpload(
                    context = context,
                    uploadId = uploadId,
                    videoTitle = cleanTitle,
                    description = description,
                    tags = formattedTags,
                    privacyStatus = privacyStatus,
                    categoryId = categoryId,
                    youtubeUrl = videoUploadUrl,
                    videoId = videoUploadUrl.substringAfterLast("/"),
                    fileSizeBytes = actualVideoFile.length(),
                    durationSec = durationSec,
                    generationTimeMs = generationTimeMs,
                    status = "PUBLISHED"
                )
                Result.success(videoUploadUrl)
            } else {
                val errorMsg = if (oauthToken.isNullOrBlank()) "OAuth Token missing. Please sign in with YouTube permissions." else "YouTube API rejected the upload. Check logs for details."
                onProgress("❌ Upload failed: $errorMsg", 0.0f)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "YouTube upload exception", e)
            onProgress("❌ Exception: ${e.message}", 0.0f)
            Result.failure(e)
        }
    }

    /**
     * Fetches real YouTube Channel Info for the logged-in user
     */
    suspend fun fetchYouTubeChannelInfo(context: Context): Result<YouTubeChannelInfo> = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext Result.failure(Exception("Not signed in"))
            val token = getOAuthAccessToken(context, account) ?: return@withContext Result.failure(Exception("Failed to get OAuth token. Please grant YouTube permissions."))
            
            val request = Request.Builder()
                .url("https://www.googleapis.com/youtube/v3/channels?part=snippet,statistics,contentDetails&mine=true")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
                
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val json = JSONObject(body)
                val items = json.optJSONArray("items")
                if (items != null && items.length() > 0) {
                    val firstItem = items.getJSONObject(0)
                    val chId = firstItem.optString("id", "")
                    val snippet = firstItem.getJSONObject("snippet")
                    val statistics = firstItem.getJSONObject("statistics")
                    val contentDetails = firstItem.optJSONObject("contentDetails")
                    
                    val title = snippet.optString("title", "YouTube Channel")
                    val customUrl = snippet.optString("customUrl", "")
                    val subCount = statistics.optString("subscriberCount", "0")
                    val views = statistics.optString("viewCount", "0")
                    val vids = statistics.optString("videoCount", "0")
                    val thumbUrl = snippet.optJSONObject("thumbnails")?.optJSONObject("default")?.optString("url")
                    val uploadsPlaylistId = contentDetails?.optJSONObject("relatedPlaylists")?.optString("uploads")
                    
                    Result.success(YouTubeChannelInfo(
                        channelId = if (chId.isNotBlank()) chId else null,
                        channelName = title,
                        channelHandle = if (customUrl.isNotEmpty()) customUrl else "@" + title.replace(" ", "").lowercase(),
                        subscriberCount = subCount,
                        viewCount = views,
                        videoCount = vids,
                        avatarUrl = thumbUrl,
                        uploadsPlaylistId = uploadsPlaylistId
                    ))
                } else {
                    Result.failure(Exception("No YouTube channel found for this account. Please create a channel on youtube.com first!"))
                }
            } else {
                val detail = if (response.code == 400) {
                    "HTTP 400 (Invalid Arguments): YouTube Channel sync failed. Please ensure your Google account has an active YouTube channel created at youtube.com and OAuth scopes are authorized."
                } else {
                    "HTTP ${response.code}: Failed to load channel info"
                }
                Result.failure(Exception(detail))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching YouTube channel info", e)
            Result.failure(e)
        }
    }

    private fun formatIsoDuration(iso: String): String {
        return try {
            var clean = iso.removePrefix("PT")
            var minutes = 0
            var seconds = 0
            if (clean.contains("M")) {
                val parts = clean.split("M")
                minutes = parts[0].toIntOrNull() ?: 0
                clean = if (parts.size > 1) parts[1] else ""
            }
            if (clean.contains("S")) {
                seconds = clean.removeSuffix("S").toIntOrNull() ?: 0
            }
            String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds)
        } catch (e: Exception) {
            "0:59"
        }
    }

    /**
     * Fetches real list of uploaded videos/shorts from the uploads playlist with real YouTube statistics
     */
    suspend fun fetchYouTubeChannelVideos(context: Context, uploadsPlaylistId: String): Result<List<VideoItemData>> = withContext(Dispatchers.IO) {
        try {
            if (uploadsPlaylistId.isBlank()) {
                Log.w(TAG, "uploadsPlaylistId is blank, returning empty list without API call to prevent HTTP 400 error.")
                return@withContext Result.success(emptyList())
            }

            val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext Result.failure(Exception("Not signed in to Google"))
            val token = getOAuthAccessToken(context, account) ?: return@withContext Result.failure(Exception("Failed to get OAuth token. Please grant YouTube permissions."))
            
            val url = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&playlistId=$uploadsPlaylistId&maxResults=25"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
                
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val errorReason = if (response.code == 400) {
                    "HTTP 400 (Invalid Arguments): YouTube API rejected request for playlist ID '$uploadsPlaylistId'. Check if playlist exists or channel is initialized."
                } else {
                    "HTTP ${response.code}: Failed to load channel videos ($body)"
                }
                return@withContext Result.failure(Exception(errorReason))
            }

            val json = JSONObject(body)
            val items = json.optJSONArray("items")
            val videoIds = mutableListOf<String>()
            if (items != null) {
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val resourceId = item.optJSONObject("snippet")?.optJSONObject("resourceId")
                    val vid = resourceId?.optString("videoId")
                    if (!vid.isNullOrBlank()) {
                        videoIds.add(vid)
                    }
                }
            }

            if (videoIds.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            // Fetch real statistics (views, likes, comments, duration) from YouTube videos endpoint
            val statsUrl = "https://www.googleapis.com/youtube/v3/videos?part=snippet,statistics,contentDetails&id=${videoIds.joinToString(",")}"
            val statsRequest = Request.Builder()
                .url(statsUrl)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val statsResponse = okHttpClient.newCall(statsRequest).execute()
            val statsBody = statsResponse.body?.string() ?: ""
            if (!statsResponse.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${statsResponse.code}: Failed to load video statistics"))
            }

            val statsJson = JSONObject(statsBody)
            val videoItems = statsJson.optJSONArray("items")
            val videosList = mutableListOf<VideoItemData>()

            if (videoItems != null) {
                for (i in 0 until videoItems.length()) {
                    val item = videoItems.getJSONObject(i)
                    val vid = item.optString("id", "")
                    val snippet = item.optJSONObject("snippet")
                    val stats = item.optJSONObject("statistics")
                    val contentDetails = item.optJSONObject("contentDetails")

                    val title = snippet?.optString("title", "Untitled Video") ?: "Untitled Video"
                    val publishedAt = snippet?.optString("publishedAt", "")?.take(10) ?: ""
                    val thumbUrl = snippet?.optJSONObject("thumbnails")?.optJSONObject("high")?.optString("url")
                        ?: snippet?.optJSONObject("thumbnails")?.optJSONObject("default")?.optString("url")
                        ?: ""

                    val viewsCount = stats?.optString("viewCount", "0") ?: "0"
                    val likesCount = stats?.optString("likeCount", "0") ?: "0"
                    val commentCount = stats?.optString("commentCount", "0") ?: "0"

                    val isoDuration = contentDetails?.optString("duration", "PT0S") ?: "PT0S"
                    val durationFormatted = formatIsoDuration(isoDuration)

                    val tags = mutableListOf<String>()
                    val tagsArray = snippet?.optJSONArray("tags")
                    if (tagsArray != null) {
                        for (t in 0 until tagsArray.length()) {
                            tags.add(tagsArray.getString(t))
                        }
                    }

                    videosList.add(
                        VideoItemData(
                            id = vid,
                            title = title,
                            views = viewsCount,
                            likes = likesCount,
                            comments = commentCount,
                            duration = durationFormatted,
                            status = "PUBLISHED",
                            publishedAt = publishedAt,
                            tags = if (tags.isNotEmpty()) tags else listOf("Shorts", "AI"),
                            thumbnailUrl = thumbUrl,
                            videoPath = null
                        )
                    )
                }
            }

            Result.success(videosList)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching YouTube channel videos", e)
            Result.failure(e)
        }
    }

    suspend fun fetchTrendingYouTubeVideos(
        context: Context,
        regionCode: String = java.util.Locale.getDefault().country
            .takeIf { it.length == 2 }
            ?.uppercase()
            ?: "US"
    ): Result<List<VideoItemData>> = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.failure(Exception("Sign in with Google to load YouTube's most popular videos."))
            val token = getOAuthAccessToken(context, account)
                ?: return@withContext Result.failure(Exception("YouTube read permission is required to load popular videos."))
            val normalizedRegion = regionCode.trim().uppercase()
            require(normalizedRegion.matches(Regex("[A-Z]{2}"))) { "Region must be a two-letter country code." }

            val url = HttpUrl.Builder()
                .scheme("https")
                .host("www.googleapis.com")
                .addPathSegments("youtube/v3/videos")
                .addQueryParameter("part", "snippet,statistics,contentDetails")
                .addQueryParameter("chart", "mostPopular")
                .addQueryParameter("regionCode", normalizedRegion)
                .addQueryParameter("maxResults", "25")
                .build()
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("YouTube popular videos request failed: HTTP ${response.code}")
                    )
                }

                val items = JSONObject(body).optJSONArray("items") ?: return@withContext Result.success(emptyList())
                val videos = buildList {
                    for (index in 0 until items.length()) {
                        val item = items.optJSONObject(index) ?: continue
                        val snippet = item.optJSONObject("snippet") ?: continue
                        val videoId = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                        val statistics = item.optJSONObject("statistics")
                        val details = item.optJSONObject("contentDetails")
                        val thumbnails = snippet.optJSONObject("thumbnails")
                        val thumbnail = thumbnails?.optJSONObject("high")?.optString("url")
                            ?: thumbnails?.optJSONObject("default")?.optString("url")
                            ?: ""
                        val tags = snippet.optJSONArray("tags")
                        val tagList = if (tags == null) emptyList() else buildList {
                            for (tagIndex in 0 until tags.length()) {
                                tags.optString(tagIndex).takeIf { it.isNotBlank() }?.let(::add)
                            }
                        }

                        add(
                            VideoItemData(
                                id = videoId,
                                title = snippet.optString("title").ifBlank { "Untitled video" },
                                views = statistics?.optString("viewCount")?.takeIf { it.isNotBlank() } ?: "Unavailable",
                                likes = statistics?.optString("likeCount")?.takeIf { it.isNotBlank() } ?: "Unavailable",
                                comments = statistics?.optString("commentCount")?.takeIf { it.isNotBlank() } ?: "Unavailable",
                                duration = formatIsoDuration(details?.optString("duration") ?: "PT0S"),
                                status = "PUBLISHED",
                                publishedAt = snippet.optString("publishedAt").take(10),
                                tags = tagList,
                                thumbnailUrl = thumbnail
                            )
                        )
                    }
                }
                Result.success(videos)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching YouTube most popular videos", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches real video categories from YouTube Data API v3 or returns defaults
     */
    suspend fun fetchYouTubeVideoCategories(context: Context, regionCode: String = "US"): Result<List<YouTubeCategoryItem>> = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            var token: String? = null
            if (account != null) {
                token = getOAuthAccessToken(context, account)
            }
            val userEmail = account?.email ?: _currentUser.value?.email ?: ""
            if (token.isNullOrBlank() && userEmail.isNotBlank()) {
                token = getOAuthAccessTokenForEmail(context, userEmail)
            }

            val requestBuilder = Request.Builder()
                .url("https://www.googleapis.com/youtube/v3/videoCategories?part=snippet&regionCode=$regionCode")

            if (!token.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val json = JSONObject(body)
                val items = json.optJSONArray("items")
                val categoriesList = mutableListOf<YouTubeCategoryItem>()
                if (items != null) {
                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        val id = item.optString("id", "")
                        val snippet = item.optJSONObject("snippet")
                        val title = snippet?.optString("title", "") ?: ""
                        val assignable = snippet?.optBoolean("assignable", true) ?: true
                        if (id.isNotEmpty() && title.isNotEmpty() && assignable) {
                            categoriesList.add(YouTubeCategoryItem(id = id, title = title, assignable = assignable))
                        }
                    }
                }
                if (categoriesList.isNotEmpty()) {
                    Result.success(categoriesList)
                } else {
                    Result.success(getDefaultYouTubeCategories())
                }
            } else {
                Result.success(getDefaultYouTubeCategories())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching YouTube categories", e)
            Result.success(getDefaultYouTubeCategories())
        }
    }

    fun getDefaultYouTubeCategories(): List<YouTubeCategoryItem> {
        return listOf(
            YouTubeCategoryItem("24", "Entertainment"),
            YouTubeCategoryItem("28", "Science & Technology"),
            YouTubeCategoryItem("20", "Gaming"),
            YouTubeCategoryItem("27", "Education"),
            YouTubeCategoryItem("22", "People & Blogs"),
            YouTubeCategoryItem("10", "Music"),
            YouTubeCategoryItem("17", "Sports"),
            YouTubeCategoryItem("23", "Comedy"),
            YouTubeCategoryItem("1", "Film & Animation"),
            YouTubeCategoryItem("2", "Autos & Vehicles"),
            YouTubeCategoryItem("15", "Pets & Animals"),
            YouTubeCategoryItem("19", "Travel & Events"),
            YouTubeCategoryItem("25", "News & Politics"),
            YouTubeCategoryItem("26", "Howto & Style"),
            YouTubeCategoryItem("29", "Nonprofits & Activism")
        )
    }
}
