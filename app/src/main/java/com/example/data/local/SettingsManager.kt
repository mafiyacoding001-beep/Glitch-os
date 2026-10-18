package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val serverUrl: String = "http://10.0.2.2:5000",
    val apiKey: String = "",
    val engineMode: String = "Smart Auto", // "Smart Auto", "On-Device Studio Engine", "Remote Server Only"
    val defaultLanguage: String = "Hindi",
    val defaultCategory: String = "Technology",
    val defaultCategoryId: String = "28", // 28 = Science & Technology, 24 = Entertainment, 20 = Gaming
    val defaultDuration: Int = 60,
    val defaultStyle: String = "Cinematic",
    val defaultAspect: String = "9:16",
    val visualRenderMode: String = "AI_IMAGES", // "AI_IMAGES", "HTML_CAPTION_ANIMATION"
    val htmlTheme: String = "Cyberpunk Dark",
    val ttsVoice: String = "",
    val whisperModel: String = "small",
    val maxRetries: Int = 3,
    val imageDelayMs: Int = 500,
    val imageDurationSec: Int = 3, // 3s or 6s
    val targetAudience: String = "General",
    val showCaptions: Boolean = true,
    val debugLogging: Boolean = false,
    val autoPublishEnabled: Boolean = true,
    val scheduleIntervalHours: Int = 2,
    val scheduleDelaySeconds: Long = 7200L, // User-selected exact delay in seconds (e.g. 30s, 60s, 300s, 7200s)
    val autoCategory: String = "Technology",
    val lastAutoPublishTimestamp: Long = 0L,
    val nextAutoPublishTimestamp: Long = 0L,
    val pollinationsEnabled: Boolean = true,
    val keyboardName: String = "GLITCH KEYBOARD",
    val keyboardPackage: String = "com.aistudio.glitchv3.trdxk",
    val autoSwitchKeyboard: Boolean = true,
    // Phase 3: Self-Learning Loop & Smart Auto-Pivot
    val smartAutoPivotEnabled: Boolean = true,
    val smartTimingMode: String = "FIXED_INTERVAL", // "FIXED_INTERVAL" (strictly honors user delay), "SMART_PEAK"
    val smartPeakHour: Int = 18, // 18:00 (6 PM) peak hour default
    val topPerformingCategory: String = "Technology",
    // Global Fallback & Fail-Safe Engine (Master Toggle)
    val globalFallbackEnabled: Boolean = false,
    // VPN Autopilot Shield Settings & Full Device Internet Cutoff Mode
    val fullDeviceInternetCutoffEnabled: Boolean = false,
    val vpnAutoConnectOnAutopilot: Boolean = false,
    val vpnAutoSwitchOnFailure: Boolean = true,
    // Free Trial First Video Experience
    val firstVideoTrialUsed: Boolean = false,
    val trialVideosCount: Int = 0
)

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ai_video_settings", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            serverUrl = prefs.getString("server_url", "http://10.0.2.2:5000") ?: "http://10.0.2.2:5000",
            apiKey = prefs.getString("api_key", "") ?: "",
            engineMode = prefs.getString("engine_mode", "Smart Auto") ?: "Smart Auto",
            defaultLanguage = prefs.getString("default_language", "Hindi") ?: "Hindi",
            defaultCategory = prefs.getString("default_category", "Technology") ?: "Technology",
            defaultCategoryId = prefs.getString("default_category_id", "28") ?: "28",
            defaultDuration = prefs.getInt("default_duration", 60),
            defaultStyle = prefs.getString("default_style", "Cinematic") ?: "Cinematic",
            defaultAspect = prefs.getString("default_aspect", "9:16") ?: "9:16",
            visualRenderMode = prefs.getString("visual_render_mode", "AI_IMAGES") ?: "AI_IMAGES",
            htmlTheme = prefs.getString("html_theme", "Cyberpunk Dark") ?: "Cyberpunk Dark",
            ttsVoice = prefs.getString("tts_voice", "") ?: "",
            whisperModel = prefs.getString("whisper_model", "small") ?: "small",
            maxRetries = prefs.getInt("max_retries", 3),
            imageDelayMs = prefs.getInt("image_delay", 500),
            imageDurationSec = prefs.getInt("image_duration_sec", 3),
            targetAudience = prefs.getString("target_audience", "General") ?: "General",
            showCaptions = prefs.getBoolean("show_captions", true),
            debugLogging = prefs.getBoolean("debug_logging", false),
            autoPublishEnabled = prefs.getBoolean("auto_publish_enabled", true),
            scheduleIntervalHours = prefs.getInt("schedule_interval_hours", 2),
            scheduleDelaySeconds = prefs.getLong("schedule_delay_seconds", prefs.getInt("schedule_interval_hours", 2) * 3600L),
            autoCategory = prefs.getString("auto_category", "Technology") ?: "Technology",
            lastAutoPublishTimestamp = prefs.getLong("last_auto_publish_timestamp", 0L),
            nextAutoPublishTimestamp = prefs.getLong("next_auto_publish_timestamp", 0L),
            pollinationsEnabled = prefs.getBoolean("pollinations_enabled", true),
            keyboardName = prefs.getString("keyboard_name", "GLITCH KEYBOARD") ?: "GLITCH KEYBOARD",
            keyboardPackage = prefs.getString("keyboard_package", "com.aistudio.glitchv3.trdxk") ?: "com.aistudio.glitchv3.trdxk",
            autoSwitchKeyboard = prefs.getBoolean("auto_switch_keyboard", true),
            smartAutoPivotEnabled = prefs.getBoolean("smart_auto_pivot_enabled", true),
            smartTimingMode = prefs.getString("smart_timing_mode", "FIXED_INTERVAL") ?: "FIXED_INTERVAL",
            smartPeakHour = prefs.getInt("smart_peak_hour", 18),
            topPerformingCategory = prefs.getString("top_performing_category", "Technology") ?: "Technology",
            globalFallbackEnabled = prefs.getBoolean("global_fallback_enabled", false),
            fullDeviceInternetCutoffEnabled = prefs.getBoolean("full_device_internet_cutoff_enabled", false),
            vpnAutoConnectOnAutopilot = prefs.getBoolean("vpn_auto_connect_on_autopilot", false),
            vpnAutoSwitchOnFailure = prefs.getBoolean("vpn_auto_switch_on_failure", true),
            firstVideoTrialUsed = prefs.getBoolean("first_video_trial_used", false),
            trialVideosCount = prefs.getInt("trial_videos_count", 0)
        )
    }

    fun updateSettings(newSettings: AppSettings) {
        prefs.edit().apply {
            putString("server_url", newSettings.serverUrl.trim().removeSuffix("/"))
            putString("api_key", newSettings.apiKey.trim())
            putString("engine_mode", newSettings.engineMode)
            putString("default_language", newSettings.defaultLanguage)
            putString("default_category", newSettings.defaultCategory)
            putString("default_category_id", newSettings.defaultCategoryId)
            putInt("default_duration", newSettings.defaultDuration)
            putString("default_style", newSettings.defaultStyle)
            putString("default_aspect", newSettings.defaultAspect)
            putString("visual_render_mode", newSettings.visualRenderMode)
            putString("html_theme", newSettings.htmlTheme)
            putString("tts_voice", newSettings.ttsVoice)
            putString("whisper_model", newSettings.whisperModel)
            putInt("max_retries", newSettings.maxRetries)
            putInt("image_delay", newSettings.imageDelayMs)
            putInt("image_duration_sec", newSettings.imageDurationSec)
            putString("target_audience", newSettings.targetAudience)
            putBoolean("show_captions", newSettings.showCaptions)
            putBoolean("debug_logging", newSettings.debugLogging)
            putBoolean("auto_publish_enabled", newSettings.autoPublishEnabled)
            putInt("schedule_interval_hours", newSettings.scheduleIntervalHours)
            putLong("schedule_delay_seconds", newSettings.scheduleDelaySeconds)
            putString("auto_category", newSettings.autoCategory)
            putLong("last_auto_publish_timestamp", newSettings.lastAutoPublishTimestamp)
            putLong("next_auto_publish_timestamp", newSettings.nextAutoPublishTimestamp)
            putBoolean("pollinations_enabled", newSettings.pollinationsEnabled)
            putString("keyboard_name", newSettings.keyboardName)
            putString("keyboard_package", newSettings.keyboardPackage)
            putBoolean("auto_switch_keyboard", newSettings.autoSwitchKeyboard)
            putBoolean("smart_auto_pivot_enabled", newSettings.smartAutoPivotEnabled)
            putString("smart_timing_mode", newSettings.smartTimingMode)
            putInt("smart_peak_hour", newSettings.smartPeakHour)
            putString("top_performing_category", newSettings.topPerformingCategory)
            putBoolean("global_fallback_enabled", newSettings.globalFallbackEnabled)
            putBoolean("full_device_internet_cutoff_enabled", newSettings.fullDeviceInternetCutoffEnabled)
            putBoolean("vpn_auto_connect_on_autopilot", newSettings.vpnAutoConnectOnAutopilot)
            putBoolean("vpn_auto_switch_on_failure", newSettings.vpnAutoSwitchOnFailure)
            putBoolean("first_video_trial_used", newSettings.firstVideoTrialUsed)
            putInt("trial_videos_count", newSettings.trialVideosCount)
            apply()
        }
        _settingsFlow.value = newSettings
    }

    fun getServerUrl(): String {
        return _settingsFlow.value.serverUrl
    }
}
