package com.example.util

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class YouTubePlaylistItem(
    val id: String,
    val title: String,
    val description: String,
    val itemCount: Int,
    val privacyStatus: String = "public",
    val thumbnailUrl: String? = null
)

data class SmartPlaylistSuggestion(
    val title: String,
    val description: String,
    val targetCategory: String,
    val recommendedVideoTitles: List<String>,
    val bingeWatchHook: String,
    val estimatedRetentionMultiplier: String
)

/**
 * Phase 1 & Step 6: Automatic Category Playlist Manager for YouTube with NoTrack AI.
 * Automatically creates and organizes videos into niche-specific playlists.
 */
object AutoPlaylistManager {
    private const val TAG = "AutoPlaylistManager"

    suspend fun generateAutonomousPlaylistsWithNotrack(
        context: Context,
        category: String,
        channelName: String,
        recentTitles: List<String>,
        notrackClient: com.example.pipeline.NotrackWebViewClient?
    ): List<SmartPlaylistSuggestion> {
        if (notrackClient != null) {
            try {
                val results = notrackClient.askNotrackForSmartPlaylists(
                    category = category,
                    channelName = channelName,
                    recentTitles = recentTitles
                )
                if (results.isNotEmpty()) return results
            } catch (e: Exception) {
                Log.w(TAG, "NoTrack smart playlist generation error: ${e.message}")
            }
        }
        val def = getCategoryConfig(category)
        return listOf(
            SmartPlaylistSuggestion(
                title = def.first,
                description = def.second,
                targetCategory = category,
                recommendedVideoTitles = recentTitles.take(3),
                bingeWatchHook = "Sequential cliffhanger pacing keeps viewers hooked till the final video.",
                estimatedRetentionMultiplier = "+3.5x Session Duration"
            )
        )
    }
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val categoryPlaylistDefaults = mapOf(
        "Technology" to Pair(
            "Tech Breakthroughs & Futuristic Gadgets 🚀💻",
            "Explore cutting-edge technology, innovative gadgets, and digital revolutions shaping our future! #Technology #TechShorts #Innovation"
        ),
        "AI & Futuristic" to Pair(
            "Future AI & Cyber Tech Chronicles 🤖⚡",
            "Deep dive into Artificial Intelligence, AGI, neural breakthroughs, and future world transformations! #AI #ArtificialIntelligence #Tech"
        ),
        "Horror & Thriller" to Pair(
            "Chilling Horror & Thriller Shorts 😱👻",
            "Unexplained paranormal encounters, bone-chilling horror stories, and dark mysteries of the night! #Horror #ScaryStories #Thriller"
        ),
        "Mystery & Crime" to Pair(
            "Unsolved True Crime & Dark Mysteries 🔍🕵️",
            "Investigating mysterious vanishings, cold cases, unsolved secrets, and shocking true crime stories. #TrueCrime #Mystery #Unsolved"
        ),
        "History & Culture" to Pair(
            "Untold Ancient History & Epic Legends 🏛️⚔️",
            "Forgotten empires, legendary warriors, ancient ruins, and captivating history secrets revealed! #History #AncientWorld #Legends"
        ),
        "Motivation & Facts" to Pair(
            "Daily Mindset, Motivation & Shocking Facts 🧠🔥",
            "Transform your mindset with powerful daily motivation, stoic wisdom, and mind-blowing life facts! #Motivation #Mindset #Facts"
        ),
        "Science" to Pair(
            "Mind-Blowing Science & Cosmos Secrets 🌌🔬",
            "Fascinating science experiments, quantum paradoxes, cosmic wonders, and astronomy discoveries! #Science #Cosmos #Astronomy"
        ),
        "Business & Finance" to Pair(
            "Money, Wealth & Startup Success 💰📈",
            "Master wealth creation, entrepreneurial insights, financial freedom tips, and startup growth! #Business #Finance #Wealth"
        ),
        "Gaming" to Pair(
            "Epic Gaming Lore & Next-Gen Secrets 🎮👾",
            "Legendary video game lore, hidden easter eggs, speedrun records, and next-gen gaming highlights! #Gaming #GamingShorts #Gamer"
        ),
        "Entertainment" to Pair(
            "Viral Entertainment, Pop Culture & Cinema 🎬🍿",
            "Trending entertainment news, hidden movie details, celebrity pop culture, and cinematic breakdowns! #Entertainment #Cinema #Viral"
        )
    )

    fun getCategoryConfig(category: String): Pair<String, String> {
        return categoryPlaylistDefaults[category]
            ?: Pair(
                "$category Shorts & Trends 🎬",
                "Curated high-quality viral shorts and stories in the $category niche! #$category #Shorts"
            )
    }

    /**
     * Fetches all user playlists from YouTube Data API v3
     */
    suspend fun fetchUserPlaylists(context: Context, customToken: String? = null): Result<List<YouTubePlaylistItem>> = withContext(Dispatchers.IO) {
        try {
            val token = customToken ?: run {
                val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext Result.failure(Exception("Not signed in"))
                GoogleSignInHelper.getOAuthAccessToken(context, account)
            } ?: return@withContext Result.failure(Exception("Missing YouTube OAuth token"))

            val request = Request.Builder()
                .url("https://www.googleapis.com/youtube/v3/playlists?part=snippet,contentDetails,status&mine=true&maxResults=50")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to fetch playlists: $body")
                return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
            }

            val json = JSONObject(body)
            val items = json.optJSONArray("items") ?: JSONArray()
            val playlistList = mutableListOf<YouTubePlaylistItem>()

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val id = item.optString("id", "")
                val snippet = item.optJSONObject("snippet")
                val contentDetails = item.optJSONObject("contentDetails")
                val status = item.optJSONObject("status")

                val title = snippet?.optString("title", "Untitled Playlist") ?: "Untitled Playlist"
                val description = snippet?.optString("description", "") ?: ""
                val itemCount = contentDetails?.optInt("itemCount", 0) ?: 0
                val privacy = status?.optString("privacyStatus", "public") ?: "public"
                val thumb = snippet?.optJSONObject("thumbnails")?.optJSONObject("medium")?.optString("url")
                    ?: snippet?.optJSONObject("thumbnails")?.optJSONObject("default")?.optString("url")

                playlistList.add(
                    YouTubePlaylistItem(
                        id = id,
                        title = title,
                        description = description,
                        itemCount = itemCount,
                        privacyStatus = privacy,
                        thumbnailUrl = thumb
                    )
                )
            }

            Result.success(playlistList)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching playlists", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a new playlist on YouTube
     */
    suspend fun createPlaylist(
        context: Context,
        title: String,
        description: String,
        privacyStatus: String = "public",
        customToken: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val token = customToken ?: run {
                val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext Result.failure(Exception("Not signed in"))
                GoogleSignInHelper.getOAuthAccessToken(context, account)
            } ?: return@withContext Result.failure(Exception("Missing YouTube OAuth token"))

            val jsonPayload = JSONObject().apply {
                put("snippet", JSONObject().apply {
                    put("title", title)
                    put("description", description)
                })
                put("status", JSONObject().apply {
                    put("privacyStatus", privacyStatus)
                })
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://www.googleapis.com/youtube/v3/playlists?part=snippet,status")
                .addHeader("Authorization", "Bearer $token")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Error creating playlist: $body")
                return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
            }

            val resJson = JSONObject(body)
            val playlistId = resJson.optString("id", "")
            if (playlistId.isNotBlank()) {
                Log.d(TAG, "Successfully created playlist '$title' ($playlistId)")
                Result.success(playlistId)
            } else {
                Result.failure(Exception("Failed to get playlist ID from YouTube response"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating playlist", e)
            Result.failure(e)
        }
    }

    /**
     * Inserts a video into a specified playlist
     */
    suspend fun addVideoToPlaylist(
        context: Context,
        playlistId: String,
        videoId: String,
        customToken: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val token = customToken ?: run {
                val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext Result.failure(Exception("Not signed in"))
                GoogleSignInHelper.getOAuthAccessToken(context, account)
            } ?: return@withContext Result.failure(Exception("Missing YouTube OAuth token"))

            val jsonPayload = JSONObject().apply {
                put("snippet", JSONObject().apply {
                    put("playlistId", playlistId)
                    put("resourceId", JSONObject().apply {
                        put("kind", "youtube#video")
                        put("videoId", videoId)
                    })
                })
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://www.googleapis.com/youtube/v3/playlistItems?part=snippet")
                .addHeader("Authorization", "Bearer $token")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                Log.d(TAG, "Added video $videoId to playlist $playlistId successfully")
                Result.success(true)
            } else {
                Log.e(TAG, "Failed to add video to playlist: $body")
                Result.failure(Exception("HTTP ${response.code}: $body"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception adding video to playlist", e)
            Result.failure(e)
        }
    }

    /**
     * Auto-checks or creates the category playlist, then adds the newly uploaded video into it.
     */
    suspend fun autoAddVideoToCategoryPlaylist(
        context: Context,
        category: String,
        videoId: String,
        customToken: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val (defaultTitle, defaultDesc) = getCategoryConfig(category)
            val playlistsRes = fetchUserPlaylists(context, customToken)

            var targetPlaylistId: String? = null

            if (playlistsRes.isSuccess) {
                val existing = playlistsRes.getOrNull() ?: emptyList()
                // Find matching playlist by category name in title
                val match = existing.firstOrNull { pl ->
                    pl.title.contains(category, ignoreCase = true) ||
                    category.split("&", " ").any { part -> part.trim().length > 3 && pl.title.contains(part.trim(), ignoreCase = true) }
                }
                targetPlaylistId = match?.id
            }

            // If playlist does not exist, create it automatically
            if (targetPlaylistId.isNullOrBlank()) {
                Log.d(TAG, "Category playlist for '$category' not found. Creating new playlist...")
                val createRes = createPlaylist(context, defaultTitle, defaultDesc, "public", customToken)
                if (createRes.isSuccess) {
                    targetPlaylistId = createRes.getOrNull()
                } else {
                    Log.w(TAG, "Could not auto-create playlist: ${createRes.exceptionOrNull()?.message}")
                }
            }

            if (!targetPlaylistId.isNullOrBlank()) {
                val addRes = addVideoToPlaylist(context, targetPlaylistId, videoId, customToken)
                if (addRes.isSuccess) {
                    Log.d(TAG, "Auto-organized video $videoId into category playlist $targetPlaylistId ($category)")
                    return@withContext Result.success(targetPlaylistId)
                }
            }

            Result.failure(Exception("Failed to add to category playlist"))
        } catch (e: Exception) {
            Log.e(TAG, "Error in autoAddVideoToCategoryPlaylist", e)
            Result.failure(e)
        }
    }
}
