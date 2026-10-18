package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.AutonomousQueueEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ViralTrendIdea(
    val videoId: String,
    val title: String,
    val category: String,
    val chartRank: Int,
    val views: String,
    val likes: String,
    val comments: String,
    val publishedAt: String,
    val targetKeywords: List<String>
)

object AutonomousContentQueueManager {
    private const val TAG = "AutonomousContentQueue"

    suspend fun discoverViralTrends(
        context: Context,
        category: String
    ): List<ViralTrendIdea> = withContext(Dispatchers.IO) {
        val videos = GoogleSignInHelper.fetchTrendingYouTubeVideos(context).getOrElse { error ->
            throw IllegalStateException("Could not load YouTube's live popular-video chart.", error)
        }

        videos.distinctBy { it.id }.mapIndexed { index, video ->
            ViralTrendIdea(
                videoId = video.id,
                title = video.title,
                category = category,
                chartRank = index + 1,
                views = video.views,
                likes = video.likes,
                comments = video.comments,
                publishedAt = video.publishedAt,
                targetKeywords = video.tags
            )
        }
    }

    suspend fun generate7DayAutonomousQueue(
        context: Context,
        category: String,
        delaySeconds: Long = 0L
    ): Result<List<AutonomousQueueEntity>> = withContext(Dispatchers.IO) {
        try {
            val trends = discoverViralTrends(context, category)
                .distinctBy { it.title.trim().lowercase(Locale.ROOT) }
            require(trends.size >= 7) {
                "YouTube returned only ${trends.size} chart items; at least 7 are needed to build a non-duplicated weekly queue."
            }

            val dao = AppDatabase.getInstance(context).autonomousChannelDao()
            val now = System.currentTimeMillis()
            val settings = com.example.data.local.SettingsManager(context).settingsFlow.value
            val effectiveDelaySeconds = when {
                delaySeconds > 0L -> delaySeconds
                settings.scheduleDelaySeconds > 0L -> settings.scheduleDelaySeconds
                else -> (settings.scheduleIntervalHours * 3600L).coerceAtLeast(3600L)
            }
            val intervalMs = effectiveDelaySeconds.coerceAtLeast(30L) * 1000L
            val queueItems = trends.take(7).mapIndexed { index, trend ->
                AutonomousQueueEntity(
                    queueId = UUID.randomUUID().toString(),
                    topicTitle = trend.title,
                    category = trend.category,
                    viralScore = 0,
                    hookFormula = "Topic sourced from YouTube most-popular chart rank ${trend.chartRank}",
                    hookSentence = "",
                    scriptOutline = "Create an original script and verify factual claims against primary sources.",
                    voiceNarration = "",
                    targetKeywords = trend.targetKeywords.joinToString(", "),
                    scheduledPublishEpochMs = now + ((index + 1) * intervalMs),
                    status = "QUEUED",
                    createdAtEpochMs = now
                )
            }

            dao.insertQueueItems(queueItems)
            Log.i(TAG, "Queued ${queueItems.size} distinct titles from YouTube's live popular-video chart.")
            Result.success(queueItems)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate live-data content queue.", e)
            Result.failure(e)
        }
    }

    suspend fun deleteQueueItem(context: Context, queueId: String) = withContext(Dispatchers.IO) {
        try {
            AppDatabase.getInstance(context).autonomousChannelDao().deleteQueueItem(queueId)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting queue item $queueId.", e)
            throw e
        }
    }

    suspend fun clearPublishedItems(context: Context) = withContext(Dispatchers.IO) {
        try {
            AppDatabase.getInstance(context).autonomousChannelDao().clearPublishedQueue()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing published queue items.", e)
            throw e
        }
    }

    fun formatScheduleDate(epochMs: Long): String =
        SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.getDefault()).format(Date(epochMs))
}
