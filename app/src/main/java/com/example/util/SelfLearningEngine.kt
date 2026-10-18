package com.example.util

import com.example.data.local.ProjectEntity
import com.example.data.youtube.VideoItemData
import java.util.Calendar
import java.util.Locale

data class CategoryPerformance(
    val category: String,
    val videoCount: Int,
    val totalViews: Long,
    val avgViews: Long,
    val totalLikes: Long,
    val totalComments: Long,
    val engagementRate: Double,
    val weightPercentage: Int,
    val viralScore: Int
)

data class PeakTimingOption(
    val hour: Int,
    val label: String,
    val timeDisplay: String,
    val description: String,
    val evidenceLabel: String,
    val isRecommended: Boolean = false
)

data class SelfLearningReport(
    val topCategory: String,
    val winningGenre: String,
    val totalAnalyzedVideos: Int,
    val avgEngagementRate: Double,
    val recommendedNextTopic: String,
    val recommendedCategory: String,
    val bestPublishHour: Int,
    val bestPublishLabel: String,
    val categoryWeights: List<CategoryPerformance>,
    val insights: List<String>,
    val autoPivotActive: Boolean = true
)

object SelfLearningEngine {

    val STANDARD_CATEGORIES = listOf(
        "Technology",
        "Mystery & Secrets",
        "Science & Universe",
        "Psychology & Mind",
        "History & Ancient",
        "Finance & Wealth",
        "Motivation & Stoicism"
    )

    fun getPeakTimingOptions(): List<PeakTimingOption> {
        return listOf(
            PeakTimingOption(
                hour = 18,
                label = "Evening preset",
                timeDisplay = "06:00 PM (18:00)",
                description = "Optional schedule time. Performance depends on your audience.",
                evidenceLabel = "Preset; not based on channel analytics",
                isRecommended = false
            ),
            PeakTimingOption(
                hour = 9,
                label = "Morning preset",
                timeDisplay = "09:00 AM (09:00)",
                description = "Optional schedule time. Performance depends on your audience.",
                evidenceLabel = "Preset; not based on channel analytics",
                isRecommended = false
            ),
            PeakTimingOption(
                hour = 13,
                label = "Midday preset",
                timeDisplay = "01:00 PM (13:00)",
                description = "Optional schedule time. Performance depends on your audience.",
                evidenceLabel = "Preset; not based on channel analytics",
                isRecommended = false
            ),
            PeakTimingOption(
                hour = 21,
                label = "Night preset",
                timeDisplay = "09:00 PM (21:00)",
                description = "Optional schedule time. Performance depends on your audience.",
                evidenceLabel = "Preset; not based on channel analytics",
                isRecommended = false
            )
        )
    }

    /**
     * Calculates the exact next epoch timestamp for a given peak hour.
     * If the hour has already passed today, schedules for tomorrow at the same peak hour.
     */
    fun calculateNextPeakPublishTimestamp(peakHour: Int = 18): Long {
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)

        if (currentHour > peakHour || (currentHour == peakHour && currentMinute >= 30)) {
            // Already past today's window, schedule for tomorrow
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        cal.set(Calendar.HOUR_OF_DAY, peakHour)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val nextTime = cal.timeInMillis
        val now = System.currentTimeMillis()
        return if (nextTime > now) nextTime else now + (2 * 3600 * 1000L)
    }

    /**
     * Live Self-Learning Engine:
     * Ingests YouTube channel videos (views, likes, comments, titles, tags)
     * and local projects to determine the highest performing genre and auto-pivot future content.
     */
    fun analyzeChannelPerformance(
        videos: List<VideoItemData>,
        localProjects: List<ProjectEntity>? = null,
        fallbackCategory: String = "Technology"
    ): SelfLearningReport {
        val categoryDataMap = mutableMapOf<String, MutableList<VideoItemData>>()

        // Initialize with standard categories
        STANDARD_CATEGORIES.forEach { cat ->
            categoryDataMap[cat] = mutableListOf()
        }

        // Categorize each video based on its tags, title, or matched keywords
        videos.forEach { video ->
            val detectedCat = detectCategory(video.title, video.tags, fallbackCategory)
            categoryDataMap.getOrPut(detectedCat) { mutableListOf() }.add(video)
        }

        val performanceList = mutableListOf<CategoryPerformance>()
        var totalAllViews = 0L
        var totalAllLikes = 0L
        var totalAllComments = 0L

        categoryDataMap.forEach { (category, videoList) ->
            val count = videoList.size
            val views = videoList.sumOf { it.views.toLongOrNull() ?: 0L }
            val likes = videoList.sumOf { it.likes.toLongOrNull() ?: 0L }
            val comments = videoList.sumOf { it.comments.toLongOrNull() ?: 0L }

            totalAllViews += views
            totalAllLikes += likes
            totalAllComments += comments

            val avgViews = if (count > 0) views / count else 0L
            val engagement = if (views > 0) {
                ((likes + comments).toDouble() / views) * 100.0
            } else {
                0.0
            }

            // Viral Score calculation: (avgViews * 1.0) + (likes * 5) + (comments * 10)
            val viralScore = ((avgViews * 0.5) + (likes * 3) + (comments * 6)).toInt()

            performanceList.add(
                CategoryPerformance(
                    category = category,
                    videoCount = count,
                    totalViews = views,
                    avgViews = avgViews,
                    totalLikes = likes,
                    totalComments = comments,
                    engagementRate = engagement,
                    weightPercentage = 0, // Calculated below
                    viralScore = viralScore
                )
            )
        }

        // Distribute dynamic self-learning weights
        val maxScore = performanceList.maxOfOrNull { it.viralScore } ?: 0
        val sumScores = performanceList.sumOf { it.viralScore.toLong() }.coerceAtLeast(1L)

        val weightedList = performanceList.map { perf ->
            val dynamicWeight = if (sumScores > 0 && maxScore > 0) {
                val rawShare = ((perf.viralScore.toDouble() / sumScores) * 100).toInt()
                rawShare.coerceIn(5, 70) // Ensure balanced minimum 5% and max 70%
            } else {
                0
            }
            perf.copy(weightPercentage = dynamicWeight)
        }.sortedByDescending { it.avgViews + it.viralScore }

        // Determine the winning category
        val topCategory = if (weightedList.any { it.videoCount > 0 }) {
            weightedList.firstOrNull { it.videoCount > 0 }?.category ?: fallbackCategory
        } else "No data"

        val totalAnalyzed = videos.size
        val avgEngagementRate = if (totalAllViews > 0) {
            ((totalAllLikes + totalAllComments).toDouble() / totalAllViews) * 100.0
        } else 0.0

        val recommendedNextTopic = videos
            .maxByOrNull { it.views.toLongOrNull() ?: 0L }
            ?.title
            ?.takeIf { it.isNotBlank() }
            ?.let { "Explore a follow-up to: $it" }
            .orEmpty()

        val insights = mutableListOf<String>()
        if (totalAnalyzed > 0) {
            val topPerf = weightedList.first()
            insights.add("🎯 AI Self-Learning Pivot: '${topPerf.category}' generates the highest engagement (${String.format(Locale.getDefault(), "%.1f", topPerf.engagementRate)}%) and view velocity.")
            insights.add("📈 Dynamic Weights adjusted: Giving ${topPerf.weightPercentage}% allocation to '${topPerf.category}' for next generation.")
            insights.add("⏰ Upload-time performance is not available from the loaded video data.")
        } else {
            insights.add("No connected channel videos have been analyzed.")
            insights.add("Connect a YouTube channel and load its analytics to generate data-based recommendations.")
        }

        return SelfLearningReport(
            topCategory = topCategory,
            winningGenre = topCategory,
            totalAnalyzedVideos = totalAnalyzed,
            avgEngagementRate = avgEngagementRate,
            recommendedNextTopic = recommendedNextTopic,
            recommendedCategory = topCategory,
            bestPublishHour = -1,
            bestPublishLabel = "Unavailable: upload-time analytics not loaded",
            categoryWeights = weightedList,
            insights = insights,
            autoPivotActive = true
        )
    }

    private fun detectCategory(title: String, tags: List<String>, fallback: String): String {
        val lower = (title + " " + tags.joinToString(" ")).lowercase(Locale.ROOT)
        return when {
            lower.contains("secret") || lower.contains("mystery") || lower.contains("curse") || lower.contains("unexplained") || lower.contains("bermuda") -> "Mystery & Secrets"
            lower.contains("ai") || lower.contains("tech") || lower.contains("robot") || lower.contains("software") || lower.contains("code") -> "Technology"
            lower.contains("psychology") || lower.contains("brain") || lower.contains("dark") || lower.contains("manipulat") || lower.contains("mind") -> "Psychology & Mind"
            lower.contains("space") || lower.contains("universe") || lower.contains("planet") || lower.contains("quantum") || lower.contains("black hole") -> "Science & Universe"
            lower.contains("ancient") || lower.contains("history") || lower.contains("rome") || lower.contains("pyramid") || lower.contains("empire") -> "History & Ancient"
            lower.contains("money") || lower.contains("rich") || lower.contains("wealth") || lower.contains("invest") || lower.contains("crypto") -> "Finance & Wealth"
            lower.contains("stoic") || lower.contains("discipline") || lower.contains("motivat") || lower.contains("focus") || lower.contains("rule") -> "Motivation & Stoicism"
            else -> fallback
        }
    }

}
