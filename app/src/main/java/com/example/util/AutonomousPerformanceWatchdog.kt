package com.example.util

import android.content.Context
import android.util.Log
import com.example.pipeline.NotrackWebViewClient
import com.example.data.youtube.VideoItemData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class VideoAuditReport(
    val videoId: String,
    val title: String,
    val views: Long,
    val likes: Long,
    val comments: Long,
    val performanceGrade: String,
    val diagnosisNote: String,
    val optimizedTitle: String,
    val thumbnailHookSuggestion: String,
    val pinnedCommentBooster: String,
    val retentionRepairAdvice: String
)

data class ChannelWatchdogSummary(
    val totalVideosAudited: Int,
    val topOpportunityVideo: VideoAuditReport?,
    val overallRetentionStrategy: String,
    val videoAudits: List<VideoAuditReport>
)

object AutonomousPerformanceWatchdog {
    private const val TAG = "PerformanceWatchdog"

    suspend fun auditChannelPerformance(
        context: Context,
        videos: List<VideoItemData>,
        category: String,
        notrackClient: NotrackWebViewClient?
    ): ChannelWatchdogSummary = withContext(Dispatchers.IO) {
        if (videos.isEmpty()) {
            return@withContext ChannelWatchdogSummary(
                totalVideosAudited = 0,
                topOpportunityVideo = null,
                overallRetentionStrategy = "No YouTube video data is available. Load channel analytics to run this review.",
                videoAudits = emptyList()
            )
        }

        val totalViews = videos.sumOf { it.views.toLongOrNull() ?: 0L }
        val avgViews = if (videos.isNotEmpty()) totalViews / videos.size else 0L

        val audits = mutableListOf<VideoAuditReport>()

        for (video in videos.take(6)) {
            val vViews = video.views.toLongOrNull() ?: 0L
            val vLikes = video.likes.toLongOrNull() ?: 0L
            val vComments = video.comments.toLongOrNull() ?: 0L

            val grade = when {
                avgViews <= 0L -> "Insufficient view data"
                vViews >= avgViews * 1.5 -> "Well above channel average"
                vViews >= avgViews * 0.8 -> "Near channel average"
                else -> "Below channel average"
            }

            audits.add(
                VideoAuditReport(
                    videoId = video.id,
                    title = video.title,
                    views = vViews,
                    likes = vLikes,
                    comments = vComments,
                    performanceGrade = grade,
                    diagnosisNote = when (grade) {
                        "Well above channel average" -> "Views are well above the average for the loaded videos."
                        "Near channel average" -> "Views are near the average for the loaded videos."
                        "Below channel average" -> "Views are below the loaded average; retention and click-through metrics were not loaded."
                        else -> "A view-count comparison is unavailable for this video."
                    },
                    optimizedTitle = generateHighCtrTitle(video.title, category),
                    thumbnailHookSuggestion = "Add vibrant neon overlay with 3-word uppercase curiosity text",
                    pinnedCommentBooster = "Pinned Question: 'Which part surprised you the most? Drop your answer below! 👇'",
                    retentionRepairAdvice = "Re-target audience with 15-second teaser clip in Community Tab."
                )
            )
        }

        val underperforming = audits.firstOrNull { it.performanceGrade == "Below channel average" }

        val defaultStrategy = "This review compares loaded view counts only; audience retention and click-through analytics are unavailable."

        if (notrackClient != null && underperforming != null) {
            try {
                val prompt = """
                    You are a YouTube channel content advisor for niche '$category'.
                    Analyze this video:
                    Title: "${underperforming.title}"
                    Views: ${underperforming.views} (Channel Average: $avgViews)
                    
                    Provide a JSON object with:
                    - "strategy": "2 sentences of advice based only on the provided view-count comparison"
                    - "optimizedTitle": "1 alternative title under 60 chars"
                    - "thumbnailHook": "specific thumbnail composition tip"
                    Do not claim to know retention, click-through rate, or causes of performance; those metrics are not provided.
                """.trimIndent()

                val resJson = notrackClient.askNotrackForJsonObject(prompt, timeout = 35_000)
                val json = JSONObject(resJson)
                val strategy = json.optString("strategy", defaultStrategy)
                val optTitle = json.optString("optimizedTitle", underperforming.optimizedTitle)
                val thumbHook = json.optString("thumbnailHook", underperforming.thumbnailHookSuggestion)

                val updatedTop = underperforming.copy(
                    optimizedTitle = optTitle,
                    thumbnailHookSuggestion = thumbHook
                )

                val updatedAudits = audits.map { if (it.videoId == underperforming.videoId) updatedTop else it }

                return@withContext ChannelWatchdogSummary(
                    totalVideosAudited = audits.size,
                    topOpportunityVideo = updatedTop,
                    overallRetentionStrategy = strategy,
                    videoAudits = updatedAudits
                )
            } catch (e: Exception) {
                Log.w(TAG, "NoTrack watchdog deep diagnosis fallback: ${e.message}")
            }
        }

        ChannelWatchdogSummary(
            totalVideosAudited = audits.size,
            topOpportunityVideo = underperforming,
            overallRetentionStrategy = defaultStrategy,
            videoAudits = audits
        )
    }

    private fun generateHighCtrTitle(originalTitle: String, category: String): String {
        val clean = originalTitle.trim()
        return if (clean.length > 8) {
            "The Shocking Truth About $clean (Revealed)"
        } else {
            "Why 99% Of People Fail At $category (Secret Fix)"
        }
    }
}
