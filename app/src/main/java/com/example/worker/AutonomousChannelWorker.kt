package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.pipeline.NotrackWebViewClient
import com.example.util.YouTubeAutonomousManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class AutonomousChannelWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "AutonomousChannelWorker"
        const val WORK_NAME = "glitch_autonomous_channel_worker"

        fun schedulePeriodic(context: Context, intervalMinutes: Long = 60) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<AutonomousChannelWorker>(
                intervalMinutes.coerceAtLeast(15),
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.i(TAG, "Scheduled periodic autonomous YouTube channel manager every $intervalMinutes mins")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "Cancelled periodic autonomous channel worker")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "AutonomousChannelWorker triggered: Starting background comment moderation & conversion...")
        try {
            // Initialize NoTrack WebView client on Main dispatcher
            val notrack = try {
                NotrackWebViewClient.create(appContext)
            } catch (e: Exception) {
                Log.w(TAG, "Could not initialize AI reply provider; replies will be skipped: ${e.message}")
                null
            }

            val summary = YouTubeAutonomousManager.runAutonomousCommentScanAndReply(appContext, notrack)
            Log.i(TAG, "Autonomous run finished: ${summary.details} (Quota: ${summary.currentQuotaUsed}/10000)")

            if (summary.retryable) Result.retry() else Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "AutonomousChannelWorker error: ${e.message}", e)
            Result.retry()
        }
    }
}
