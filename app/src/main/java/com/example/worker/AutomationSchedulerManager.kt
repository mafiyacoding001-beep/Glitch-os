package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.local.SettingsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object AutomationSchedulerManager {
    private const val PERIODIC_WORK_TAG = "glitch_periodic_video_generation"
    private const val IMMEDIATE_WORK_TAG = "glitch_immediate_video_generation"

    fun parseTimestamp(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return 0L
        val formats = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd",
            "dd MMM, hh:mm a",
            "dd MMM yyyy, hh:mm a"
        )
        for (format in formats) {
            try {
                val date = SimpleDateFormat(format, Locale.getDefault()).parse(dateStr)
                if (date != null) return date.time
            } catch (_: Exception) {}
        }
        return dateStr.toLongOrNull() ?: 0L
    }

    /**
     * Accurately calculates the next scheduled automation timestamp based on the LAST video's
     * creation timestamp and the chosen interval in hours or delay in seconds.
     */
    fun getEffectiveNextPublishTimestamp(
        lastVideoTimestamp: Long,
        lastAutoPublishTimestamp: Long,
        intervalHours: Int = 2,
        delaySeconds: Long = 0L
    ): Long {
        val effectiveSeconds = if (delaySeconds > 0L) {
            delaySeconds
        } else {
            (if (intervalHours <= 0) 1 else intervalHours) * 3600L
        }
        val intervalMillis = effectiveSeconds.coerceAtLeast(15L) * 1000L
        val effectiveLast = maxOf(lastVideoTimestamp, lastAutoPublishTimestamp)
        val now = System.currentTimeMillis()

        if (effectiveLast <= 0L) {
            return now + intervalMillis
        }

        val targetNext = effectiveLast + intervalMillis
        return if (targetNext > now) {
            targetNext
        } else {
            val elapsed = now - effectiveLast
            val cycles = (elapsed / intervalMillis) + 1
            effectiveLast + (cycles * intervalMillis)
        }
    }

    fun getEffectiveLastPublishTimestamp(
        lastVideoTimestamp: Long,
        lastAutoPublishTimestamp: Long
    ): Long {
        return maxOf(lastVideoTimestamp, lastAutoPublishTimestamp)
    }

    fun scheduleRecurringAutomation(
        context: Context,
        intervalHours: Int = 2,
        delaySeconds: Long = 0L,
        category: String = "Technology",
        language: String = "Hindi",
        autoUpload: Boolean = true,
        lastVideoTimestamp: Long = 0L
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val settingsManager = SettingsManager(context)
        val currentSettings = settingsManager.settingsFlow.value

        // Phase 3: Smart Auto-Pivot detection
        val effectiveCategory = if (currentSettings.smartAutoPivotEnabled && currentSettings.topPerformingCategory.isNotBlank()) {
            currentSettings.topPerformingCategory
        } else {
            category
        }

        val inputData = workDataOf(
            VideoAutomationWorker.KEY_CATEGORY to effectiveCategory,
            VideoAutomationWorker.KEY_LANGUAGE to language,
            VideoAutomationWorker.KEY_TOPIC to "",
            VideoAutomationWorker.KEY_DURATION to 60,
            VideoAutomationWorker.KEY_AUTO_UPLOAD to autoUpload
        )

        val effectiveDelaySeconds = if (delaySeconds > 0L) {
            delaySeconds
        } else if (currentSettings.scheduleDelaySeconds > 0L) {
            currentSettings.scheduleDelaySeconds
        } else {
            (intervalHours.coerceAtLeast(1) * 3600L)
        }

        val now = System.currentTimeMillis()
        // Phase 3: Smart Peak Timing vs. User Chosen Interval Scheduling
        val nextRun = if (currentSettings.smartTimingMode == "SMART_PEAK") {
            com.example.util.SelfLearningEngine.calculateNextPeakPublishTimestamp(currentSettings.smartPeakHour)
        } else {
            now + (effectiveDelaySeconds * 1000L)
        }

        val initialDelayMillis = (nextRun - now).coerceAtLeast(15_000L) // At least 15s to let current action finish cleanly

        // Save calculated timestamps & user delay to settings
        settingsManager.updateSettings(
            currentSettings.copy(
                autoPublishEnabled = true,
                scheduleIntervalHours = intervalHours,
                scheduleDelaySeconds = effectiveDelaySeconds,
                autoCategory = effectiveCategory,
                nextAutoPublishTimestamp = nextRun
            )
        )

        // Cancel any stale scheduled workers first
        WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_WORK_TAG)

        // 1. Enqueue OneTimeWorkRequest for EXACT user selected delay execution (supports 30s, 1m, 5m, 1h, 2h, etc.)
        val oneTimeRequest = OneTimeWorkRequestBuilder<VideoAutomationWorker>()
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(IMMEDIATE_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_TAG,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )

        // 2. If interval is 15 minutes or longer, also register periodic work fail-safe
        if (effectiveDelaySeconds >= 900L) {
            val periodicRequest = PeriodicWorkRequestBuilder<VideoAutomationWorker>(
                effectiveDelaySeconds,
                TimeUnit.SECONDS
            )
                .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setInputData(inputData)
                .addTag(PERIODIC_WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_TAG,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )
        }
    }

    /**
     * Re-schedules the next single run after a video finishes successfully,
     * maintaining the user-chosen delay precisely.
     */
    fun scheduleNextOneTimeRun(context: Context, delaySeconds: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val settings = SettingsManager(context).settingsFlow.value
        val effectiveDelay = delaySeconds.coerceAtLeast(30L)
        val initialDelayMs = effectiveDelay * 1000L

        val inputData = workDataOf(
            VideoAutomationWorker.KEY_CATEGORY to settings.autoCategory,
            VideoAutomationWorker.KEY_LANGUAGE to settings.defaultLanguage,
            VideoAutomationWorker.KEY_TOPIC to "",
            VideoAutomationWorker.KEY_DURATION to 60,
            VideoAutomationWorker.KEY_AUTO_UPLOAD to settings.autoPublishEnabled
        )

        val request = OneTimeWorkRequestBuilder<VideoAutomationWorker>()
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(IMMEDIATE_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_TAG,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun triggerImmediateAutomation(
        context: Context,
        category: String = "Technology",
        language: String = "Hindi",
        autoUpload: Boolean = true
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = workDataOf(
            VideoAutomationWorker.KEY_CATEGORY to category,
            VideoAutomationWorker.KEY_LANGUAGE to language,
            VideoAutomationWorker.KEY_TOPIC to "",
            VideoAutomationWorker.KEY_DURATION to 60,
            VideoAutomationWorker.KEY_AUTO_UPLOAD to autoUpload
        )

        val immediateRequest = OneTimeWorkRequestBuilder<VideoAutomationWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(IMMEDIATE_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_TAG,
            ExistingWorkPolicy.REPLACE,
            immediateRequest
        )
    }

    fun cancelRecurringAutomation(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_TAG)
        WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_WORK_TAG)
    }
}
