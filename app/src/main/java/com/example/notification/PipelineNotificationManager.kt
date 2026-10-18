package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.receiver.VpnActionReceiver
import com.example.vpn.VpnGateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class LivePipelineProgress(
    val isActive: Boolean = false,
    val jobId: String = "",
    val title: String = "",
    val statusMessage: String = "",
    val progressPercent: Int = 0,
    val category: String? = null,
    val categoryId: String? = null,
    val language: String? = null,
    val durationSec: Int? = null,
    val stage: String = "",
    val lastUpdatedMs: Long = 0L,
    val isCompleted: Boolean = false,
    val youtubeUrl: String? = null,
    val error: String? = null
)

data class NotificationHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "PROGRESS", "COMPLETION", "ERROR"
    val title: String,
    val message: String,
    val category: String? = null,
    val categoryId: String? = null,
    val language: String? = null,
    val durationSec: Int? = null,
    val youtubeUrl: String? = null,
    val playlistName: String? = null
)

object PipelineNotificationManager {
    private const val CHANNEL_ID = "glitch_video_pipeline_channel"
    private const val CHANNEL_NAME = "GLITCH AI Video Pipeline"
    private const val CHANNEL_DESC = "Live progress and alerts for AI Video Generation & YouTube Auto-Publish"

    private const val PREFS_NAME = "glitch_notification_history_prefs"
    private const val KEY_HISTORY = "key_notification_history_json"

    private val _liveProgressFlow = kotlinx.coroutines.flow.MutableStateFlow(LivePipelineProgress())
    val liveProgressFlow: kotlinx.coroutines.flow.StateFlow<LivePipelineProgress> = _liveProgressFlow

    const val PROGRESS_NOTIFICATION_ID = 2001
    const val COMPLETION_NOTIFICATION_ID = 2002

    fun saveHistoryItem(context: Context, item: NotificationHistoryItem) {
        try {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val existingJson = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
            val array = JSONArray(existingJson)

            val obj = JSONObject().apply {
                put("id", item.id)
                put("timestamp", item.timestamp)
                put("type", item.type)
                put("title", item.title)
                put("message", item.message)
                put("category", item.category ?: "")
                put("categoryId", item.categoryId ?: "")
                put("language", item.language ?: "")
                put("durationSec", item.durationSec ?: 0)
                put("youtubeUrl", item.youtubeUrl ?: "")
                put("playlistName", item.playlistName ?: "")
            }

            // Insert at top, keep max 50 items
            val newArray = JSONArray()
            newArray.put(obj)
            for (i in 0 until minOf(array.length(), 49)) {
                newArray.put(array.get(i))
            }

            prefs.edit().putString(KEY_HISTORY, newArray.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Failed to save history item: ${e.message}")
        }
    }

    fun getNotificationHistory(context: Context): List<NotificationHistoryItem> {
        val list = mutableListOf<NotificationHistoryItem>()
        try {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
            val array = JSONArray(jsonStr)

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    NotificationHistoryItem(
                        id = obj.optString("id"),
                        timestamp = obj.optLong("timestamp"),
                        type = obj.optString("type"),
                        title = obj.optString("title"),
                        message = obj.optString("message"),
                        category = obj.optString("category").ifBlank { null },
                        categoryId = obj.optString("categoryId").ifBlank { null },
                        language = obj.optString("language").ifBlank { null },
                        durationSec = if (obj.optInt("durationSec") > 0) obj.optInt("durationSec") else null,
                        youtubeUrl = obj.optString("youtubeUrl").ifBlank { null },
                        playlistName = obj.optString("playlistName").ifBlank { null }
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Failed to load notification history: ${e.message}")
        }
        return list
    }

    fun clearNotificationHistory(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(false)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun createForegroundInfo(
        context: Context,
        title: String,
        statusMessage: String
    ): androidx.work.ForegroundInfo {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚡ $title")
            .setContentText(statusMessage)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)

        val notification = builder.build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            androidx.work.ForegroundInfo(
                PROGRESS_NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            androidx.work.ForegroundInfo(PROGRESS_NOTIFICATION_ID, notification)
        }
    }

    fun showProgressNotification(
        context: Context,
        title: String,
        statusMessage: String,
        progressPercent: Int,
        category: String? = null,
        categoryId: String? = null,
        language: String? = null,
        durationSec: Int? = null
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Change Network / Rotate VPN PendingIntent
        val rotateIntent = Intent(context, VpnActionReceiver::class.java).apply {
            action = VpnActionReceiver.ACTION_ROTATE_VPN
        }
        val rotatePendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            rotateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val expandedText = buildString {
            append("⚡ Glitch Engine: $statusMessage\n")
            if (!category.isNullOrBlank()) {
                append("📁 Niche: $category")
                if (!categoryId.isNullOrBlank()) append(" (ID: $categoryId)")
                append("\n")
            }
            if (!language.isNullOrBlank() || durationSec != null) {
                val langStr = language ?: "Standard"
                val durStr = if (durationSec != null) "${durationSec}s" else "Shorts"
                append("🎙 Voice: $langStr | ⏱ Duration: $durStr\n")
            }
            append("🚀 GLITCH OS Autonomous Studio Active [$progressPercent%]")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚡ GLITCH OS [$progressPercent%]: $title")
            .setContentText(statusMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(progressPercent in 1..99)
            .setOnlyAlertOnce(true)
            .setProgress(100, progressPercent, progressPercent <= 0)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "🔄 CHANGE NETWORK", rotatePendingIntent)
            .setAutoCancel(false)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(PROGRESS_NOTIFICATION_ID, builder.build())
        } catch (e: Throwable) {
            android.util.Log.e("NotificationManager", "Progress notification error: ${e.message}")
        }

        _liveProgressFlow.value = LivePipelineProgress(
            isActive = progressPercent in 1..99,
            jobId = com.example.pipeline.PipelineExecutionCoordinator.getActiveJobId() ?: "active_${System.currentTimeMillis()}",
            title = title,
            statusMessage = statusMessage,
            progressPercent = progressPercent,
            category = category,
            categoryId = categoryId,
            language = language,
            durationSec = durationSec,
            stage = statusMessage,
            lastUpdatedMs = System.currentTimeMillis(),
            isCompleted = false
        )
    }

    fun showCompletionNotification(
        context: Context,
        title: String,
        youtubeUrl: String? = null,
        nextRunTimeText: String? = null,
        category: String? = null,
        categoryId: String? = null,
        language: String? = null,
        durationSec: Int? = null,
        playlistName: String? = null,
        hashtags: List<String>? = null
    ) {
        createNotificationChannel(context)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.cancel(PROGRESS_NOTIFICATION_ID)
        } catch (e: Exception) {}

        val summaryText = if (!youtubeUrl.isNullOrBlank()) {
            "🚀 Video Live On YouTube! Tap to watch."
        } else {
            "⚡ Video Synthesized & Saved by Glitch OS!"
        }

        val expandedText = buildString {
            append("🎬 Title: $title\n")
            if (!category.isNullOrBlank()) {
                append("📁 Category: $category")
                if (!categoryId.isNullOrBlank()) append(" (ID: $categoryId)")
                append("\n")
            }
            if (!language.isNullOrBlank() || durationSec != null) {
                val langStr = language ?: "Standard"
                val durStr = if (durationSec != null) "${durationSec}s" else "60s"
                append("🎙 Language: $langStr | ⏱ Length: $durStr\n")
            }
            if (!playlistName.isNullOrBlank()) {
                append("📂 Playlist: $playlistName\n")
            }
            if (!youtubeUrl.isNullOrBlank()) {
                append("📺 YouTube: $youtubeUrl\n")
            }
            if (!hashtags.isNullOrEmpty()) {
                append("🏷 Tags: ${hashtags.take(5).joinToString(" ")}\n")
            }
            if (!nextRunTimeText.isNullOrBlank()) {
                append("⏰ Next Autopilot Run: $nextRunTimeText\n")
            }
            append("⚡ Powered by GLITCH OS Autonomous Studio")
        }.trimEnd()

        val mainIntent = if (!youtubeUrl.isNullOrBlank()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl))
        } else {
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🚀 GLITCH OS: $title Live!")
            .setContentText(summaryText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (!youtubeUrl.isNullOrBlank()) {
            builder.addAction(
                R.drawable.ic_launcher_foreground,
                "Watch Video",
                pendingIntent
            )

            // Direct Share action button
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Check out my new video created with Glitch OS: $youtubeUrl #Shorts")
            }
            val sharePendingIntent = PendingIntent.getActivity(
                context,
                3,
                Intent.createChooser(shareIntent, "Share Video Link"),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.ic_launcher_foreground,
                "Share Link",
                sharePendingIntent
            )
        }

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(COMPLETION_NOTIFICATION_ID, builder.build())

            saveHistoryItem(
                context,
                NotificationHistoryItem(
                    type = "COMPLETION",
                    title = title,
                    message = summaryText,
                    category = category,
                    categoryId = categoryId,
                    language = language,
                    durationSec = durationSec,
                    youtubeUrl = youtubeUrl,
                    playlistName = playlistName
                )
            )
        } catch (e: Throwable) {
            android.util.Log.e("NotificationManager", "Completion notification error: ${e.message}")
        }

        _liveProgressFlow.value = LivePipelineProgress(
            isActive = false,
            jobId = "",
            title = title,
            statusMessage = summaryText,
            progressPercent = 100,
            category = category,
            categoryId = categoryId,
            language = language,
            durationSec = durationSec,
            stage = "Completed",
            lastUpdatedMs = System.currentTimeMillis(),
            isCompleted = true,
            youtubeUrl = youtubeUrl
        )
    }

    fun showErrorNotification(
        context: Context,
        title: String,
        errorMessage: String
    ) {
        createNotificationChannel(context)

        // 🛡️ Trigger automatic VPN network failover on error
        CoroutineScope(Dispatchers.Default).launch {
            try {
                VpnGateManager.switchToNextServer(context)
            } catch (e: Exception) {
                android.util.Log.w("NotificationManager", "Auto VPN switch on error failed: ${e.message}")
            }
        }

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.cancel(PROGRESS_NOTIFICATION_ID)
        } catch (e: Exception) {}

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Change Network / Rotate VPN PendingIntent
        val rotateIntent = Intent(context, VpnActionReceiver::class.java).apply {
            action = VpnActionReceiver.ACTION_ROTATE_VPN
        }
        val rotatePendingIntent = PendingIntent.getBroadcast(
            context,
            102,
            rotateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val vpnState = VpnGateManager.status.value
        val serverInfo = if (vpnState.currentServer != null) {
            "${vpnState.currentServer.countryFlag} ${vpnState.currentServer.countryLong} (${vpnState.maskedIp})"
        } else {
            "Auto-Failover Gateway Active"
        }

        val expandedText = buildString {
            append("⚠️ GLITCH OS Alert: $errorMessage\n\n")
            append("🛡️ GLITCH Auto-Shield: Network failover triggered ($serverInfo).\n")
            append("Tap 'CHANGE NETWORK' to rotate to another high-speed VPN IP.")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🛡️ GLITCH AUTO-SHIELD: $title")
            .setContentText("Network failover active ($serverInfo). Tap to rotate IP.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "🔄 CHANGE NETWORK", rotatePendingIntent)
            .setAutoCancel(true)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(COMPLETION_NOTIFICATION_ID, builder.build())

            saveHistoryItem(
                context,
                NotificationHistoryItem(
                    type = "ERROR",
                    title = title,
                    message = errorMessage
                )
            )
        } catch (e: Throwable) {
            android.util.Log.e("NotificationManager", "Error notification error: ${e.message}")
        }

        _liveProgressFlow.value = LivePipelineProgress(
            isActive = false,
            jobId = "",
            title = title,
            statusMessage = errorMessage,
            progressPercent = 0,
            stage = "Error",
            lastUpdatedMs = System.currentTimeMillis(),
            isCompleted = false,
            error = errorMessage
        )
    }

    fun clearLiveProgress() {
        _liveProgressFlow.value = LivePipelineProgress()
    }
}
