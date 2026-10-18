package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.local.OfflineQueueEntity
import com.example.data.local.SettingsManager
import com.example.data.local.VideoDatabase
import com.example.data.network.ApiClient
import com.example.data.repository.VideoRepository
import com.example.data.youtube.YouTubeUploader
import com.example.notification.PipelineNotificationManager
import com.example.pipeline.VideoRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoAutomationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "VideoAutomationWorker"
        const val KEY_TOPIC = "key_topic"
        const val KEY_CATEGORY = "key_category"
        const val KEY_LANGUAGE = "key_language"
        const val KEY_DURATION = "key_duration"
        const val KEY_AUTO_UPLOAD = "key_auto_upload"
        const val KEY_VOICE_NAME = "key_voice_name"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val topic = inputData.getString(KEY_TOPIC)?.trim().orEmpty()
        val category = inputData.getString(KEY_CATEGORY) ?: "Technology"
        val language = inputData.getString(KEY_LANGUAGE) ?: "Hindi"
        val duration = inputData.getInt(KEY_DURATION, 60)
        val autoUpload = inputData.getBoolean(KEY_AUTO_UPLOAD, true)
        val voiceName = inputData.getString(KEY_VOICE_NAME) ?: "hi-IN-MadhurNeural"

        val workerJobId = "worker_" + System.currentTimeMillis()

        // 0. Guard against pending mandatory app update
        try {
            val updateInfo = com.example.util.FirestoreService.fetchAppUpdateDocument(appContext)
            if (updateInfo != null && updateInfo.shouldShowAlert) {
                Log.w(TAG, "Mandatory update required (v${updateInfo.version}). Pausing Auto-pilot and aborting worker execution.")
                val settingsManager = SettingsManager(appContext)
                val currentSettings = settingsManager.settingsFlow.value
                settingsManager.updateSettings(currentSettings.copy(autoPublishEnabled = false))
                AutomationSchedulerManager.cancelRecurringAutomation(appContext)
                return@withContext Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking update document in worker: ${e.message}")
        }

        // 1. Guard against parallel generation collisions
        val lockAcquired = com.example.pipeline.PipelineExecutionCoordinator.tryAcquireExecution(
            jobId = workerJobId,
            title = topic,
            isWorkerTask = true
        )

        if (!lockAcquired) {
            Log.w(
                TAG,
                "Another video generation is already active (${com.example.pipeline.PipelineExecutionCoordinator.getActiveJobId()}). Skipping worker execution to avoid duplicate video creation."
            )
            return@withContext Result.success()
        }

        Log.d(TAG, "Starting Automated Video Pipeline for: $topic ($category)")
        try {
            setForeground(
                PipelineNotificationManager.createForegroundInfo(
                    context = appContext,
                    title = "Automated Video Pipeline",
                    statusMessage = "Starting pipeline for $category..."
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not set foreground service info: ${e.message}")
        }

        PipelineNotificationManager.showProgressNotification(
            context = appContext,
            title = "Automated Video Pipeline Running",
            statusMessage = "Starting pipeline for $category...",
            progressPercent = 5
        )

        try {
            val db = VideoDatabase.getDatabase(appContext)
            val settingsManager = SettingsManager(appContext)
            val currentSettings = settingsManager.settingsFlow.value

            // Feature 1: Auto-activate VPN Shield when Autopilot runs
            if (currentSettings.vpnAutoConnectOnAutopilot) {
                try {
                    Log.d(TAG, "Autopilot active: Auto-activating VPN Shield...")
                    com.example.vpn.VpnGateManager.ensureVpnConnectedForAutopilot(appContext)
                } catch (vpnErr: Exception) {
                    Log.w(TAG, "VPN Auto-connect failed, proceeding with native connection: ${vpnErr.message}")
                }
            }

            val apiClient = ApiClient(settingsManager)
            val repository = VideoRepository(appContext, apiClient, db.projectDao(), settingsManager)

            // Autonomous Content Queue Check: Pick next pending due item if available
            val queueDao = com.example.data.local.AppDatabase.getInstance(appContext).autonomousChannelDao()
            val dueQueueItem = queueDao.getNextPendingDueItem(System.currentTimeMillis())
            val effectiveTopic = dueQueueItem?.topicTitle?.takeIf { it.isNotBlank() }
                ?: topic.takeIf { it.isNotBlank() }
                ?: com.example.util.GoogleSignInHelper.fetchTrendingYouTubeVideos(appContext)
                    .getOrElse { error ->
                        throw IllegalStateException(
                            "No due queue item or explicit topic is available, and live YouTube trend discovery failed.",
                            error
                        )
                    }
                    .firstOrNull()
                    ?.title
                    ?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException(
                    "No due queue item, explicit topic, or live YouTube popular-chart video is available."
                )
            val effectiveCategory = dueQueueItem?.category ?: category
            val customNarration = dueQueueItem?.voiceNarration?.takeIf { it.isNotBlank() }
            val customHook = dueQueueItem?.hookSentence?.takeIf { it.isNotBlank() }

            // Autonomous Script & Visual Prompt Generation
            val viralScript = com.example.util.ViralScriptGenerator.generateViralScript(
                topic = effectiveTopic,
                category = effectiveCategory,
                language = language,
                targetDurationSec = duration
            )
            val visualPrompts = com.example.util.ThumbnailBannerPromptGenerator.generateVisualPrompts(
                topic = viralScript.topic,
                category = effectiveCategory
            )

            Log.d(TAG, "Autopilot generated 8K Thumbnail Prompt: ${visualPrompts.mainVisualPrompt}")
            Log.d(TAG, "Autopilot generated Viral Script Hook: ${customHook ?: viralScript.viralHook}")

            val request = VideoRequest(
                topic = viralScript.topic,
                language = language,
                category = effectiveCategory,
                targetDurationSec = duration,
                voiceName = voiceName,
                imageDurationSec = settingsManager.settingsFlow.value.imageDurationSec,
                showCaptions = settingsManager.settingsFlow.value.showCaptions,
                bgmEnabled = true,
                bgmTrackId = com.example.util.FreeMusicManager.getCategoryDefaultTrackId(effectiveCategory),
                bgmVolume = 0.18f,
                autoDucking = true,
                visualRenderMode = settingsManager.settingsFlow.value.visualRenderMode,
                htmlTheme = settingsManager.settingsFlow.value.htmlTheme,
                customNarration = customNarration,
                customHook = customHook
            )

            val result = repository.runNativePipeline(request) { progress, message ->
                Log.d(TAG, "[$progress%] $message")
                PipelineNotificationManager.showProgressNotification(
                    context = appContext,
                    title = "GLITCH Autopilot: Generating Video",
                    statusMessage = message,
                    progressPercent = progress,
                    category = category,
                    categoryId = settingsManager.settingsFlow.value.defaultCategoryId,
                    language = language,
                    durationSec = duration
                )
                setProgressAsync(workDataOf("progress" to progress, "message" to message))
            }

            if (dueQueueItem != null) {
                queueDao.updateQueueItemStatus(
                    queueId = dueQueueItem.queueId,
                    newStatus = "GENERATED"
                )
            }

            var youtubeUrl: String? = null
            if (autoUpload) {
                val finalUploadTitle = result.metadata.title

                val user = db.authDao().getUser()
                val activeChannel = com.example.data.local.AppDatabase.getInstance(appContext).youTubeChannelDao().getActiveChannel()
                val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(appContext)
                val token = activeChannel?.oauthToken?.ifBlank { null }
                    ?: if (account != null) {
                        com.example.util.GoogleSignInHelper.getOAuthAccessToken(appContext, account) ?: user?.oauthToken
                    } else {
                        user?.oauthToken
                    }
                if (token.isNullOrBlank()) {
                    throw IllegalStateException("YouTube sign-in is required before automatic upload.")
                }
                PipelineNotificationManager.showProgressNotification(
                    context = appContext,
                    title = "Uploading to YouTube",
                    statusMessage = "Uploading $finalUploadTitle...",
                    progressPercent = 95
                )
                val uploader = YouTubeUploader(appContext)
                val uploadedUrl = try {
                    uploader.uploadVideoResumable(
                        videoFile = result.video,
                        title = finalUploadTitle,
                        description = "${result.metadata.description}\n\n#Shorts ${result.metadata.hashtags.joinToString(" ")}",
                        tags = result.metadata.tags,
                        accessToken = token,
                        privacyStatus = "public",
                        categoryId = settingsManager.settingsFlow.value.defaultCategoryId.ifBlank { "28" },
                        onProgress = { p, msg ->
                            Log.d(TAG, "[Upload $p%] $msg")
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "YouTube Upload failed in background worker", e)
                    throw e
                }
                youtubeUrl = uploadedUrl
                val videoId = uploadedUrl.substringAfterLast("/")
                if (dueQueueItem != null) {
                    queueDao.updateQueueItemStatus(
                        queueId = dueQueueItem.queueId,
                        newStatus = "PUBLISHED",
                        videoId = videoId
                    )
                }
                com.example.util.FirestoreService.saveYouTubeUpload(
                    context = appContext,
                    videoTitle = result.metadata.title,
                    description = result.metadata.description,
                    youtubeUrl = uploadedUrl,
                    videoId = videoId,
                    durationSec = result.audioDuration
                )

                try {
                    com.example.util.AutoPlaylistManager.autoAddVideoToCategoryPlaylist(
                        context = appContext,
                        category = category,
                        videoId = videoId,
                        customToken = token
                    )
                } catch (plErr: Exception) {
                    Log.w(TAG, "Auto playlist insert error: ${plErr.message}")
                }
            }
            // Update scheduler timestamps accurately with user chosen delay
            val latestSettings = settingsManager.settingsFlow.value
            val now = System.currentTimeMillis()
            val effectiveDelaySec = if (latestSettings.scheduleDelaySeconds > 0L) {
                latestSettings.scheduleDelaySeconds
            } else {
                (latestSettings.scheduleIntervalHours * 3600L).coerceAtLeast(30L)
            }

            val nextRun = if (latestSettings.smartTimingMode == "SMART_PEAK") {
                com.example.util.SelfLearningEngine.calculateNextPeakPublishTimestamp(latestSettings.smartPeakHour)
            } else {
                now + (effectiveDelaySec * 1000L)
            }
            settingsManager.updateSettings(
                latestSettings.copy(
                    lastAutoPublishTimestamp = now,
                    nextAutoPublishTimestamp = nextRun
                )
            )

            // Re-arm next run with exact user delay
            if (latestSettings.autoPublishEnabled) {
                AutomationSchedulerManager.scheduleNextOneTimeRun(appContext, effectiveDelaySec)
            }

            val nextRunFormatted = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(nextRun))
            PipelineNotificationManager.showCompletionNotification(
                context = appContext,
                title = result.metadata.title,
                youtubeUrl = youtubeUrl,
                nextRunTimeText = nextRunFormatted,
                category = category,
                categoryId = settingsManager.settingsFlow.value.defaultCategoryId,
                language = language,
                durationSec = duration,
                playlistName = category,
                hashtags = result.metadata.hashtags
            )

            Result.success(
                workDataOf(
                    "video_path" to result.video.absolutePath,
                    "youtube_url" to (youtubeUrl ?: "")
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Automated Pipeline execution encountered an error", e)

            // Feature 2: Auto-switch VPN IP on video generation or API failure
            val settingsManager = SettingsManager(appContext)
            if (settingsManager.settingsFlow.value.vpnAutoSwitchOnFailure) {
                try {
                    Log.i(TAG, "Video generation error detected. Auto-rotating VPN IP to next server...")
                    com.example.vpn.VpnGateManager.switchToNextServer(appContext)
                } catch (vpnSwitchErr: Exception) {
                    Log.w(TAG, "Failed to auto-switch VPN IP: ${vpnSwitchErr.message}")
                }
            }

            PipelineNotificationManager.showErrorNotification(
                context = appContext,
                title = topic,
                errorMessage = e.localizedMessage ?: "Pipeline error occurred"
            )
            Result.failure()
        } finally {
            com.example.pipeline.PipelineExecutionCoordinator.releaseExecution(workerJobId)
        }
    }
}
