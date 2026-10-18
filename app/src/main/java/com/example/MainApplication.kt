package com.example

import android.app.Application
import android.util.Log
import com.example.data.local.SettingsManager
import com.example.data.local.VideoDatabase
import com.example.data.network.ApiClient
import com.example.data.repository.VideoRepository
import com.example.notification.PipelineNotificationManager
import com.example.worker.AutomationSchedulerManager
import com.google.firebase.FirebaseApp

class MainApplication : Application() {

    lateinit var database: VideoDatabase
        private set

    lateinit var settingsManager: SettingsManager
        private set

    lateinit var apiClient: ApiClient
        private set

    lateinit var videoRepository: VideoRepository
        private set

    override fun onCreate() {
        super.onCreate()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            try {
                val processName = getProcessName()
                if (packageName != processName) {
                    android.webkit.WebView.setDataDirectorySuffix(processName)
                }
            } catch (e: Exception) {
                Log.w("MainApplication", "WebView setDataDirectorySuffix notice: ${e.message}")
            }
        }

        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("MainApplication", "FATAL EXCEPTION on thread ${thread.name}: ${throwable.message}", throwable)
            originalHandler?.uncaughtException(thread, throwable)
        }

        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.e("MainApplication", "FirebaseApp init failed", e)
        }

        try {
            val wasmCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
            if (!wasmCacheDir.exists()) {
                wasmCacheDir.mkdirs()
            }
        } catch (e: Exception) {
            Log.w("MainApplication", "Could not pre-create wasm cache dir", e)
        }

        database = VideoDatabase.getDatabase(this)
        settingsManager = SettingsManager(this)
        apiClient = ApiClient(settingsManager)
        videoRepository = VideoRepository(this, apiClient, database.projectDao(), settingsManager)

        PipelineNotificationManager.createNotificationChannel(this)

        try {
            com.example.util.GoogleSignInHelper.checkExistingUser(this)
        } catch (e: Exception) {
            Log.w("MainApplication", "Check existing user notice: ${e.message}")
        }

        val settings = settingsManager.settingsFlow.value
        if (settings.autoPublishEnabled) {
            try {
                AutomationSchedulerManager.scheduleRecurringAutomation(
                    context = this,
                    intervalHours = settings.scheduleIntervalHours,
                    delaySeconds = settings.scheduleDelaySeconds,
                    category = settings.autoCategory,
                    language = settings.defaultLanguage,
                    autoUpload = true
                )
            } catch (e: Exception) {
                Log.w("MainApplication", "WorkManager scheduling on app start skipped: ${e.message}")
            }
        }
    }
}
