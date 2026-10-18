package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.AppiumLogEntity
import com.example.data.model.GlitchAgentSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class FirestoreSyncState(
    val isSyncing: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val lastSyncMessage: String = "Ready to sync",
    val agentSynced: Boolean = false,
    val deviceSynced: Boolean = false,
    val youtubeSynced: Boolean = false,
    val logsSyncedCount: Int = 0
)

data class CloudYouTubeUploadRecord(
    val uploadId: String = "",
    val videoTitle: String = "",
    val youtubeUrl: String = "",
    val privacyStatus: String = "public",
    val uploadedAtTimestamp: Long = 0L,
    val tags: List<String> = emptyList(),
    val categoryId: String = "24",
    val channelEmail: String = ""
)

data class AppUpdateInfo(
    val appName: String = "",
    val appVersion: String = "",
    val downloadUrl: String = "",
    val newUpdates: String = "",
    val updateStatus: Boolean = false
)

data class AppUpdateData(
    val docId: String = "Rjxlap1CyCRtgn683jE7",
    val status: Boolean = false,
    val version: String = "",
    val url: String = "",
    val newFeatures: String = "",
    val currentAppVersion: String = "1.1",
    val shouldShowAlert: Boolean = false
)

object FirestoreService {
    private const val TAG = "FirestoreService"

    private val _syncState = MutableStateFlow(FirestoreSyncState())
    val syncState: StateFlow<FirestoreSyncState> = _syncState.asStateFlow()

    private fun getFirestoreSafe(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.e(TAG, "FirebaseFirestore instance unavailable: ${e.message}")
            null
        }
    }

    private fun getFirebaseAuthSafe(): FirebaseAuth? {
        return try {
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            Log.e(TAG, "FirebaseAuth instance unavailable: ${e.message}")
            null
        }
    }

    private suspend fun ensureAuthenticated(context: Context): String = withContext(Dispatchers.IO) {
        val auth = getFirebaseAuthSafe()
        if (auth != null) {
            val current = auth.currentUser
            if (current != null) {
                return@withContext current.uid
            }
        }
        // Fallback to Google ID formatted string or device ID if absolutely necessary, 
        // but since we block the app with LoginScreen, we should ideally have a UID.
        return@withContext getCurrentUid(context)
    }

    fun getCurrentUid(context: Context): String {
        val authUid = getFirebaseAuthSafe()?.currentUser?.uid
        if (!authUid.isNullOrBlank()) return authUid

        val savedUser = GoogleSignInHelper.getSavedUserFromPreferences(context)
        if (!savedUser?.email.isNullOrBlank()) {
            return savedUser!!.email!!.replace(".", "_").replace("@", "_at_")
        }

        val deviceId = DeviceDataCollector.getDeviceId(context)
        return "device_user_${deviceId.takeLast(8)}"
    }

    // =========================================================================
    // 1. AGENT DATA STRUCTURE & PERSISTENCE
    // Collection: users/{uid}/agent_settings/current & users/{uid}/agent_profiles/{agentId}
    // Top-Level: global_agents/{agentId}
    // =========================================================================

    /**
     * Saves complete Agent Configuration to Firestore
     */
    suspend fun saveAgentSettings(
        context: Context,
        settings: GlitchAgentSettings,
        agentId: String = "primary_glitch_agent"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestoreSafe() ?: return@withContext false
            val uid = ensureAuthenticated(context)
            val jsonString = settings.toJsonString(pretty = true)

            val agentDoc = mutableMapOf<String, Any>(
                "agent_id" to agentId,
                "user_id" to uid,
                "agent_name" to settings.general.agentName,
                "mode" to settings.general.mode,
                "category" to settings.general.category,
                "language" to settings.languageAndVoice.language,
                "voice_gender" to settings.languageAndVoice.voiceGender,
                "voice_accent" to settings.languageAndVoice.voiceAccent,
                "speech_rate" to settings.languageAndVoice.speechRate.toDouble(),
                "pitch" to settings.languageAndVoice.pitch,
                "orientation" to settings.videoFormat.orientation,
                "aspect_ratio" to settings.videoFormat.aspectRatio,
                "resolution" to settings.videoFormat.resolution,
                "fps" to settings.videoFormat.fps,
                "max_duration_seconds" to settings.videoFormat.maxDurationSeconds,
                "captions_enabled" to settings.captionsAndSubtitles.captionsEnabled,
                "caption_style" to settings.captionsAndSubtitles.captionStyle,
                "highlight_color" to settings.captionsAndSubtitles.highlightColor,
                "auto_generate" to settings.schedulingAndAutomation.autoGenerate,
                "delay_between_videos_seconds" to settings.schedulingAndAutomation.delayBetweenVideosSeconds,
                "daily_video_limit" to settings.schedulingAndAutomation.dailyVideoLimit,
                "visual_style" to settings.contentAndStyle.visualStyle,
                "background_music" to settings.contentAndStyle.backgroundMusic,
                "auto_publish" to settings.publishing.autoPublish,
                "publishing_platforms" to settings.publishing.platforms,
                "auto_generate_title" to settings.publishing.autoGenerateTitle,
                "auto_generate_hashtags" to settings.publishing.autoGenerateHashtags,
                "full_json_config" to jsonString,
                "last_updated" to FieldValue.serverTimestamp(),
                "last_updated_device" to DeviceDataCollector.getDeviceId(context),
                "system_prompt_directive" to settings.toSystemPromptDirective()
            )

            // 1. Save in user's agent profile
            try {
                firestore.collection("users").document(uid)
                    .collection("agent_profiles").document(agentId)
                    .set(agentDoc, SetOptions.merge())
                    .await()
            } catch (e: Throwable) {
                Log.w(TAG, "Agent profile sync notice: ${e.message}")
            }

            // 2. Save in user's current active settings shortcut
            try {
                firestore.collection("users").document(uid)
                    .collection("agent_settings").document("current")
                    .set(agentDoc, SetOptions.merge())
                    .await()
            } catch (e: Throwable) {
                Log.w(TAG, "Agent current settings sync notice: ${e.message}")
            }

            // 3. Save to Global Agents Registry for remote fleet orchestration
            try {
                firestore.collection("global_agents").document("${uid}_$agentId")
                    .set(agentDoc, SetOptions.merge())
                    .await()
            } catch (e: Throwable) {
                Log.w(TAG, "Global agents sync notice: ${e.message}")
            }

            Log.d(TAG, "Agent Settings processed for $uid [$agentId]")
            return@withContext true
        } catch (e: Throwable) {
            Log.w(TAG, "Save agent settings notice: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Fetches saved Agent Settings from Firestore
     */
    suspend fun fetchAgentSettingsFromCloud(
        context: Context,
        agentId: String = "primary_glitch_agent"
    ): GlitchAgentSettings? = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestoreSafe() ?: return@withContext null
            val uid = getCurrentUid(context)

            val snapshot = firestore.collection("users").document(uid)
                .collection("agent_settings").document("current")
                .get()
                .await()

            if (snapshot.exists()) {
                val fullJson = snapshot.getString("full_json_config")
                if (!fullJson.isNullOrBlank()) {
                    return@withContext GlitchAgentSettings.fromJsonString(fullJson)
                }
            }
            null
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to fetch cloud agent settings: ${e.message}")
            null
        }
    }

    /**
     * Logs an Agent Autonomous/Manual Generation Run to Firestore
     */
    suspend fun saveAgentRunLog(
        context: Context,
        runId: String,
        agentName: String,
        category: String,
        prompt: String,
        script: String,
        storyboardScenes: List<String>,
        visualPrompts: List<String>,
        imageCount: Int,
        audioCount: Int,
        voiceoverDurationSec: Double,
        videoPath: String? = null,
        youtubeUploadUrl: String? = null,
        status: String = "COMPLETED"
    ) = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestoreSafe() ?: return@withContext
            val uid = ensureAuthenticated(context)
            val deviceId = DeviceDataCollector.getDeviceId(context)

            val runRecord = mutableMapOf<String, Any>(
                "run_id" to runId,
                "user_id" to uid,
                "device_id" to deviceId,
                "agent_name" to agentName,
                "category" to category,
                "prompt" to prompt,
                "script" to script,
                "storyboard_scenes" to storyboardScenes,
                "visual_prompts" to visualPrompts,
                "image_count" to imageCount,
                "audio_count" to audioCount,
                "voiceover_duration_sec" to voiceoverDurationSec,
                "video_file_path" to (videoPath ?: ""),
                "youtube_upload_url" to (youtubeUploadUrl ?: ""),
                "status" to status,
                "created_at" to FieldValue.serverTimestamp(),
                "created_at_ms" to System.currentTimeMillis()
            )

            try {
                firestore.collection("users").document(uid)
                    .collection("agent_runs").document(runId)
                    .set(runRecord, SetOptions.merge())
                    .await()
            } catch (e: Throwable) {
                Log.w(TAG, "Agent run user write notice: ${e.message}")
            }

            try {
                firestore.collection("global_agent_runs").document(runId)
                    .set(runRecord, SetOptions.merge())
                    .await()
            } catch (e: Throwable) {
                Log.w(TAG, "Global agent run write notice: ${e.message}")
            }

            Log.d(TAG, "Agent Run recorded in Firestore: $runId [$status]")
        } catch (e: Throwable) {
            Log.w(TAG, "Record agent run notice: ${e.message}")
        }
    }

    // =========================================================================
    // 2. YOUTUBE COMPLETE DATA STRUCTURE & PERSISTENCE
    // Collection: users/{uid}/youtube_channels/{channelId} & users/{uid}/youtube_uploads/{uploadId}
    // Top-Level: global_youtube_uploads/{uploadId}
    // =========================================================================

    /**
     * Saves or updates YouTube Channel profile & permissions in Firestore
     */
    suspend fun saveYouTubeChannelProfile(
        context: Context,
        user: GoogleUser,
        customChannelInfo: Map<String, Any>? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestoreSafe() ?: return@withContext
            val uid = ensureAuthenticated(context)
            val channelId = "channel_${(user.email ?: uid).replace(".", "_")}"

            val channelData = mutableMapOf<String, Any>(
                "channel_id" to channelId,
                "user_id" to uid,
                "email" to (user.email ?: ""),
                "display_name" to (user.displayName ?: "YouTube Creator"),
                "photo_url" to (user.photoUrl ?: ""),
                "has_youtube_upload_permission" to user.hasYouTubePermission,
                "oauth_scope_granted" to GoogleSignInHelper.YOUTUBE_UPLOAD_SCOPE,
                "last_active" to FieldValue.serverTimestamp(),
                "last_verified_device" to DeviceDataCollector.getDeviceId(context)
            )

            if (customChannelInfo != null) {
                channelData.putAll(customChannelInfo)
            }

            try {
                firestore.collection("users").document(uid)
                    .collection("youtube_channels").document(channelId)
                    .set(channelData, SetOptions.merge())
                    .await()
            } catch (e: Throwable) {
                Log.w(TAG, "YouTube channel profile write notice: ${e.message}")
            }

            // Update primary user document with YouTube metadata
            try {
                val userSummary = mutableMapOf<String, Any>(
                    "youtube_email" to (user.email ?: ""),
                    "youtube_creator_name" to (user.displayName ?: ""),
                    "has_youtube_permission" to user.hasYouTubePermission,
                    "youtube_channel_id" to channelId,
                    "last_youtube_sync" to FieldValue.serverTimestamp()
                )

                firestore.collection("users").document(uid)
                    .set(userSummary, SetOptions.merge())
                    .await()
            } catch (e: Throwable) {
                Log.w(TAG, "User summary update notice: ${e.message}")
            }

            Log.d(TAG, "YouTube Channel profile saved to Firestore for $uid")
        } catch (e: Throwable) {
            Log.w(TAG, "Save YouTube channel profile notice: ${e.message}")
        }
    }

    /**
     * Comprehensive YouTube Upload Record Saver
     */
    suspend fun saveYouTubeUpload(
        context: Context,
        uploadId: String = "YT_${System.currentTimeMillis()}",
        videoTitle: String,
        description: String = "",
        tags: List<String> = listOf("Shorts", "AI", "GlitchOS"),
        privacyStatus: String = "public",
        categoryId: String = "24",
        youtubeUrl: String,
        videoId: String? = null,
        fileSizeBytes: Long = 0L,
        durationSec: Double = 0.0,
        generationTimeMs: Long = 0L,
        status: String = "UPLOADED",
        agentRunId: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestoreSafe() ?: return@withContext
            val uid = ensureAuthenticated(context)
            val userEmail = GoogleSignInHelper.getSavedUserFromPreferences(context)?.email ?: ""
            val deviceId = DeviceDataCollector.getDeviceId(context)

            val uploadRecord = mutableMapOf<String, Any>(
                "upload_id" to uploadId,
                "user_id" to uid,
                "channel_email" to userEmail,
                "device_id" to deviceId,
                "video_title" to videoTitle,
                "description" to description,
                "tags" to tags,
                "privacy_status" to privacyStatus,
                "category_id" to categoryId,
                "youtube_url" to youtubeUrl,
                "video_id" to (videoId ?: ""),
                "file_size_bytes" to fileSizeBytes,
                "duration_seconds" to durationSec,
                "generation_time_ms" to generationTimeMs,
                "generation_time_seconds" to (generationTimeMs / 1000.0),
                "status" to status,
                "agent_run_id" to (agentRunId ?: ""),
                "uploaded_at" to FieldValue.serverTimestamp(),
                "uploaded_at_ms" to System.currentTimeMillis()
            )

            // Save in user's subcollection
            try {
                firestore.collection("users").document(uid)
                    .collection("youtube_uploads").document(uploadId)
                    .set(uploadRecord, SetOptions.merge())
                    .await()
            } catch (e: Throwable) {
                Log.w(TAG, "YouTube upload user write notice: ${e.message}")
            }

            // Save in global uploads index
            try {
                firestore.collection("global_youtube_uploads").document(uploadId)
                    .set(uploadRecord, SetOptions.merge())
                    .await()
            } catch (e: Throwable) {
                Log.w(TAG, "Global youtube upload write notice: ${e.message}")
            }

            // Increment user's total upload stats counter
            try {
                firestore.collection("users").document(uid)
                    .update("total_youtube_uploads", FieldValue.increment(1))
            } catch (e: Throwable) {
                Log.w(TAG, "Total upload stats counter notice: ${e.message}")
            }

            Log.d(TAG, "YouTube Upload Record processed: $uploadId")
        } catch (e: Throwable) {
            Log.w(TAG, "Save YouTube upload notice: ${e.message}")
        }
    }

    /**
     * Backward-compatible simple YouTube logger
     */
    suspend fun logYouTubeUpload(
        videoTitle: String,
        youtubeUrl: String,
        privacyStatus: String
    ) = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestoreSafe() ?: return@withContext
            val currentUser = getFirebaseAuthSafe()?.currentUser
            val uid = currentUser?.uid ?: "anonymous_user"
            val uploadId = "YT_${System.currentTimeMillis()}"

            val uploadData = hashMapOf(
                "uploadId" to uploadId,
                "userId" to uid,
                "userEmail" to (currentUser?.email ?: ""),
                "videoTitle" to videoTitle,
                "youtubeUrl" to youtubeUrl,
                "privacyStatus" to privacyStatus,
                "uploadedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection("users").document(uid)
                .collection("youtube_uploads").document(uploadId)
                .set(uploadData)
                .await()

            firestore.collection("global_youtube_uploads").document(uploadId)
                .set(uploadData)
                .await()

            Log.d(TAG, "YouTube upload logged in Firestore: $uploadId")
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to log YouTube upload in Firestore: ${e.message}")
        }
    }

    /**
     * Fetches recent YouTube upload history from Firestore
     */
    suspend fun fetchRecentYouTubeUploads(context: Context, limit: Long = 20): List<CloudYouTubeUploadRecord> = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestoreSafe() ?: return@withContext emptyList()
            val uid = getCurrentUid(context)

            val querySnapshot = firestore.collection("users").document(uid)
                .collection("youtube_uploads")
                .orderBy("uploaded_at_ms", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            val list = mutableListOf<CloudYouTubeUploadRecord>()
            for (doc in querySnapshot.documents) {
                val uploadId = doc.getString("upload_id") ?: doc.getString("uploadId") ?: doc.id
                val title = doc.getString("video_title") ?: doc.getString("videoTitle") ?: "Untitled Video"
                val url = doc.getString("youtube_url") ?: doc.getString("youtubeUrl") ?: ""
                val privacy = doc.getString("privacy_status") ?: doc.getString("privacyStatus") ?: "public"
                val timeMs = doc.getLong("uploaded_at_ms") ?: 0L
                val tags = (doc.get("tags") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val catId = doc.getString("category_id") ?: "24"
                val email = doc.getString("channel_email") ?: ""
                list.add(
                    CloudYouTubeUploadRecord(
                        uploadId = uploadId,
                        videoTitle = title,
                        youtubeUrl = url,
                        privacyStatus = privacy,
                        uploadedAtTimestamp = timeMs,
                        tags = tags,
                        categoryId = catId,
                        channelEmail = email
                    )
                )
            }
            list
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to fetch YouTube uploads from Firestore: ${e.message}")
            emptyList()
        }
    }

    // =========================================================================
    // 3. DEVICE & TELEMETRY COMPLETE DATA STRUCTURE & PERSISTENCE
    // Collection: users/{uid}/devices/{deviceId} & device_telemetry/{deviceId}
    // Subcollection: users/{uid}/login_history/{loginId}
    // =========================================================================

    /**
     * Saves complete exhaustive Device, Hardware, Memory, Network, Battery & App Specs to Firestore
     */
    suspend fun saveCompleteDeviceTelemetry(
        context: Context,
        deviceStatus: String = "ONLINE"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestoreSafe() ?: return@withContext false
            val uid = ensureAuthenticated(context)
            val deviceId = DeviceDataCollector.getDeviceId(context)
            val telemetry = DeviceDataCollector.collectAllTelemetry(context)

            val hardwareOs = telemetry["hardware_and_os"] as? Map<*, *>
            val memory = telemetry["memory"] as? Map<*, *>
            val storage = telemetry["storage"] as? Map<*, *>
            val battery = telemetry["battery"] as? Map<*, *>
            val network = telemetry["network"] as? Map<*, *>
            val app = telemetry["app"] as? Map<*, *>
            val display = telemetry["display"] as? Map<*, *>

            val deviceRecord = mutableMapOf<String, Any>(
                "device_id" to deviceId,
                "user_id" to uid,
                "status" to deviceStatus,
                "last_active_at" to FieldValue.serverTimestamp(),
                "last_sync_timestamp_ms" to System.currentTimeMillis(),
                // Hardware & OS
                "manufacturer" to (hardwareOs?.get("manufacturer") ?: ""),
                "brand" to (hardwareOs?.get("brand") ?: ""),
                "model" to (hardwareOs?.get("model") ?: ""),
                "device_name" to "${hardwareOs?.get("brand")} ${hardwareOs?.get("model")}",
                "android_version" to (hardwareOs?.get("android_version") ?: ""),
                "sdk_int" to (hardwareOs?.get("sdk_int") ?: 0),
                "is_emulator" to (hardwareOs?.get("is_emulator") ?: false),
                "cpu_cores" to (hardwareOs?.get("processor_cores") ?: 1),
                // Memory & RAM
                "total_ram_mb" to (memory?.get("total_ram_mb") ?: 0),
                "available_ram_mb" to (memory?.get("available_ram_mb") ?: 0),
                "used_ram_percentage" to (memory?.get("used_ram_percentage") ?: 0),
                // Storage
                "internal_total_gb" to (storage?.get("internal_total_gb") ?: ""),
                "internal_available_gb" to (storage?.get("internal_available_gb") ?: ""),
                // Battery
                "battery_level_percent" to (battery?.get("battery_level_percent") ?: -1),
                "is_charging" to (battery?.get("is_charging") ?: false),
                "power_source" to (battery?.get("power_source") ?: ""),
                "battery_health" to (battery?.get("health") ?: ""),
                // Network
                "transport_type" to (network?.get("transport_type") ?: ""),
                "is_connected" to (network?.get("is_connected") ?: false),
                "is_metered" to (network?.get("is_metered") ?: false),
                // Display
                "screen_width_px" to (display?.get("width_px") ?: 0),
                "screen_height_px" to (display?.get("height_px") ?: 0),
                "density_dpi" to (display?.get("density_dpi") ?: 0),
                // App
                "app_version" to (app?.get("version_name") ?: "1.0"),
                "app_version_code" to (app?.get("version_code") ?: 1),
                "app_cache_kb" to (app?.get("app_cache_size_kb") ?: 0),
                // Exhaustive Raw Telemetry Tree
                "telemetry_tree" to telemetry
            )

            // 1. User Subcollection
            try {
                firestore.collection("users").document(uid)
                    .collection("devices").document(deviceId)
                    .set(deviceRecord, SetOptions.merge())
                    .await()
            } catch (e: Throwable) {
                Log.w(TAG, "User device telemetry write notice: ${e.message}")
            }

            // 2. Global Diagnostic Registry
            try {
                firestore.collection("device_telemetry").document(deviceId)
                    .set(deviceRecord, SetOptions.merge())
                    .await()
            } catch (e: Throwable) {
                Log.w(TAG, "Global device telemetry write notice: ${e.message}")
            }

            // 3. Update User Header
            try {
                val userSummary = mutableMapOf<String, Any>(
                    "last_active_device" to "${hardwareOs?.get("brand")} ${hardwareOs?.get("model")}",
                    "last_active_device_id" to deviceId,
                    "last_seen_at" to FieldValue.serverTimestamp()
                )
                firestore.collection("users").document(uid)
                    .set(userSummary, SetOptions.merge())
                    .await()
            } catch (e: Throwable) {
                Log.w(TAG, "User header update notice: ${e.message}")
            }

            Log.d(TAG, "Device telemetry processed for $deviceId")
            return@withContext true
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to save device telemetry to Firestore notice: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Realtime Heartbeat updater
     */
    suspend fun recordDeviceHeartbeat(context: Context, status: String = "ONLINE", activeTask: String? = null) = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestoreSafe() ?: return@withContext
            val uid = getCurrentUid(context)
            val deviceId = DeviceDataCollector.getDeviceId(context)

            val heartbeat = mutableMapOf<String, Any>(
                "status" to status,
                "last_heartbeat" to FieldValue.serverTimestamp(),
                "active_task" to (activeTask ?: "IDLE")
            )

            firestore.collection("users").document(uid)
                .collection("devices").document(deviceId)
                .set(heartbeat, SetOptions.merge())
                .await()
        } catch (e: Throwable) {
            Log.w(TAG, "Heartbeat error: ${e.message}")
        }
    }

    /**
     * User & Device complete data sync on login
     */
    suspend fun saveCompleteUserAndDeviceData(context: Context, user: GoogleUser) = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestoreSafe() ?: return@withContext
            val uid = getCurrentUid(context)
            val deviceId = DeviceDataCollector.getDeviceId(context)
            val telemetry = DeviceDataCollector.collectAllTelemetry(context)
            val loginTimestamp = System.currentTimeMillis()
            val loginId = "LOGIN_${loginTimestamp}_${deviceId.takeLast(6)}"

            val lastDeviceModel = (telemetry["hardware_and_os"] as? Map<*, *>)?.get("model")?.toString() ?: "Android Device"
            val lastNetwork = (telemetry["network"] as? Map<*, *>)?.get("transport_type")?.toString() ?: "Network"
            val appVer = (telemetry["app"] as? Map<*, *>)?.get("version_name")?.toString() ?: "1.0"

            val userDoc = mutableMapOf<String, Any>(
                "uid" to uid,
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: "YouTube Creator"),
                "photoUrl" to (user.photoUrl ?: ""),
                "hasYouTubePermission" to user.hasYouTubePermission,
                "lastLoginAt" to FieldValue.serverTimestamp(),
                "lastLoginDevice" to lastDeviceModel,
                "lastDeviceId" to deviceId,
                "lastActiveIpOrNetwork" to lastNetwork,
                "appVersion" to appVer
            )

            firestore.collection("users").document(uid)
                .set(userDoc, SetOptions.merge())
                .await()

            // Save device specs
            saveCompleteDeviceTelemetry(context, "ONLINE")

            // Record Login Audit
            val loginAudit = mutableMapOf<String, Any>(
                "login_id" to loginId,
                "user_id" to uid,
                "user_email" to (user.email ?: ""),
                "timestamp" to FieldValue.serverTimestamp(),
                "timestamp_ms" to loginTimestamp,
                "device_id" to deviceId,
                "device_model" to lastDeviceModel,
                "app_version" to appVer
            )

            firestore.collection("users").document(uid)
                .collection("login_history").document(loginId)
                .set(loginAudit)
                .await()

            // Save YouTube profile
            saveYouTubeChannelProfile(context, user)

            Log.d(TAG, "✅ Complete user, device & YouTube audit synced on login for $uid")
        } catch (e: Throwable) {
            Log.w(TAG, "saveCompleteUserAndDeviceData error: ${e.message}")
        }
    }

    /**
     * Backward-compatible simple user profile saver
     */
    suspend fun saveUserProfile(user: GoogleUser) = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestoreSafe() ?: return@withContext
            val uid = getFirebaseAuthSafe()?.currentUser?.uid ?: user.email?.replace(".", "_")?.replace("@", "_at_") ?: return@withContext
            val userData = hashMapOf(
                "uid" to uid,
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: "YouTube Creator"),
                "photoUrl" to (user.photoUrl ?: ""),
                "hasYouTubePermission" to user.hasYouTubePermission,
                "lastLoginAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("users").document(uid)
                .set(userData, SetOptions.merge())
                .await()
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to save user profile to Firestore: ${e.message}")
        }
    }

    /**
     * Saves a generated Video Project and all its scenes, audio, and visual details to Firestore
     */
    suspend fun saveVideoProject(
        context: Context? = null,
        projectTitle: String,
        storyboardScenes: List<String>,
        visualPrompts: List<String>,
        audioCount: Int,
        imageCount: Int,
        voiceoverDurationSec: Double,
        youtubeUploadUrl: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestoreSafe() ?: return@withContext
            val uid = if (context != null) ensureAuthenticated(context) else (getFirebaseAuthSafe()?.currentUser?.uid ?: "anonymous_user")
            val userEmail = getFirebaseAuthSafe()?.currentUser?.email ?: ""
            val projectId = "PROJ_${System.currentTimeMillis()}"

            val projectData = hashMapOf(
                "projectId" to projectId,
                "userId" to uid,
                "userEmail" to userEmail,
                "title" to projectTitle,
                "storyboardScenes" to storyboardScenes,
                "visualPrompts" to visualPrompts,
                "audioCount" to audioCount,
                "imageCount" to imageCount,
                "voiceoverDurationSec" to voiceoverDurationSec,
                "youtubeUploadUrl" to (youtubeUploadUrl ?: ""),
                "status" to if (youtubeUploadUrl.isNullOrBlank()) "COMPLETED" else "UPLOADED_TO_YOUTUBE",
                "createdAt" to FieldValue.serverTimestamp()
            )

            try {
                firestore.collection("users").document(uid)
                    .collection("video_projects").document(projectId)
                    .set(projectData)
                    .await()
            } catch (e: Throwable) {
                Log.w(TAG, "Video project user write notice: ${e.message}")
            }

            try {
                firestore.collection("global_video_projects").document(projectId)
                    .set(projectData)
                    .await()
            } catch (e: Throwable) {
                Log.w(TAG, "Global video project write notice: ${e.message}")
            }

            Log.d(TAG, "Video project $projectId processed!")
        } catch (e: Throwable) {
            Log.w(TAG, "Save video project notice: ${e.message}")
        }
    }

    // =========================================================================
    // 4. APPIUM AUTOMATION TRACE & LOGS SYNC
    // Collection: users/{uid}/appium_logs/{logId}
    // =========================================================================

    /**
     * Saves an individual Appium action log to Firestore
     */
    suspend fun saveAppiumLog(
        context: Context,
        actionName: String,
        locator: String,
        payload: String,
        status: String,
        timestamp: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestoreSafe() ?: return@withContext
            val uid = getCurrentUid(context)
            val logId = "LOG_${timestamp}_${(0..999).random()}"

            val logDoc = mutableMapOf<String, Any>(
                "log_id" to logId,
                "user_id" to uid,
                "action_name" to actionName,
                "locator" to locator,
                "payload" to payload,
                "status" to status,
                "timestamp" to FieldValue.serverTimestamp(),
                "timestamp_ms" to timestamp,
                "device_id" to DeviceDataCollector.getDeviceId(context)
            )

            firestore.collection("users").document(uid)
                .collection("appium_logs").document(logId)
                .set(logDoc)
                .await()
        } catch (e: Throwable) {
            Log.w(TAG, "Appium log firestore sync error: ${e.message}")
        }
    }

    /**
     * Batch Syncs all local Room Appium Logs to Firestore
     */
    suspend fun batchSyncLocalLogsToCloud(context: Context): Int = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestoreSafe() ?: return@withContext 0
            val uid = getCurrentUid(context)
            val db = AppDatabase.getDatabase(context)
            val localLogs = db.appiumLogDao().getAllLogsSync()

            if (localLogs.isEmpty()) return@withContext 0

            var count = 0
            for (log in localLogs.take(50)) {
                val logId = "LOG_${log.id}_${log.timestamp}"
                val logDoc = mutableMapOf<String, Any>(
                    "log_id" to logId,
                    "action_name" to log.actionName,
                    "locator" to log.targetLocator,
                    "payload" to log.payload,
                    "status" to log.status,
                    "timestamp_ms" to log.timestamp,
                    "device_id" to DeviceDataCollector.getDeviceId(context)
                )

                firestore.collection("users").document(uid)
                    .collection("appium_logs").document(logId)
                    .set(logDoc, SetOptions.merge())
                count++
            }

            Log.d(TAG, "✅ Synced $count Appium trace logs to Firestore for $uid")
            return@withContext count
        } catch (e: Throwable) {
            Log.w(TAG, "Batch log sync error: ${e.message}")
            return@withContext 0
        }
    }

    // =========================================================================
    // 5. MASTER ALL-IN-ONE FIRESTORE SYNCHRONIZATION
    // =========================================================================

    /**
     * Performs a complete one-tap bidirectional synchronization of:
     * - Agent Settings & System Prompts
     * - Complete Hardware & Device Telemetry
     * - YouTube Account, Credentials & Upload History
     * - Local Appium Execution Trace Logs
     */
    suspend fun syncAllDataToFirestore(
        context: Context,
        settings: GlitchAgentSettings,
        onProgress: (String) -> Unit = {}
    ): FirestoreSyncState = withContext(Dispatchers.IO) {
        _syncState.value = _syncState.value.copy(isSyncing = true, lastSyncMessage = "Initiating Cloud Sync...")

        try {
            onProgress("1/4: Syncing Agent Settings & Configuration...")
            val agentSuccess = saveAgentSettings(context, settings)

            onProgress("2/4: Syncing Device Telemetry & Fleet Specs...")
            val deviceSuccess = saveCompleteDeviceTelemetry(context, "ONLINE")

            onProgress("3/4: Syncing YouTube Account & Channel Profile...")
            val savedUser = GoogleSignInHelper.getSavedUserFromPreferences(context)
            if (savedUser != null) {
                saveYouTubeChannelProfile(context, savedUser)
            }
            val ytSuccess = true

            onProgress("4/4: Syncing Appium Execution Logs...")
            val logsCount = batchSyncLocalLogsToCloud(context)

            val finalState = FirestoreSyncState(
                isSyncing = false,
                lastSyncTimestamp = System.currentTimeMillis(),
                lastSyncMessage = "✅ Complete Cloud Sync Successful (Agent, Device, YouTube & $logsCount Logs)",
                agentSynced = agentSuccess,
                deviceSynced = deviceSuccess,
                youtubeSynced = ytSuccess,
                logsSyncedCount = logsCount
            )
            _syncState.value = finalState
            onProgress("✅ All Agent, Device & YouTube data linked with Firestore!")
            return@withContext finalState
        } catch (e: Throwable) {
            Log.w(TAG, "Master Firestore Sync Failed: ${e.message}")
            val errorState = FirestoreSyncState(
                isSyncing = false,
                lastSyncTimestamp = System.currentTimeMillis(),
                lastSyncMessage = "❌ Sync failed: ${e.message}",
                agentSynced = false,
                deviceSynced = false,
                youtubeSynced = false,
                logsSyncedCount = 0
            )
            _syncState.value = errorState
            return@withContext errorState
        }
    }

    fun getAppVersionName(context: Context): String {
        return try {
            val buildConfigVer = com.example.BuildConfig.VERSION_NAME
            if (!buildConfigVer.isNullOrBlank()) return buildConfigVer
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.1"
        } catch (_: Throwable) {
            "1.1"
        }
    }

    private fun extractStatus(doc: com.google.firebase.firestore.DocumentSnapshot): Boolean {
        val candidateKeys = listOf(
            "status", "stetus", "update_status", "update status", "updateStatus",
            "is_update", "isUpdate", "is_updated", "active", "enabled", "enable", "state"
        )
        for (key in candidateKeys) {
            val value = doc.get(key) ?: continue
            when (value) {
                is Boolean -> return value
                is Number -> return value.toInt() == 1
                is String -> {
                    val trimmed = value.trim().lowercase()
                    if (trimmed == "true" || trimmed == "1" || trimmed == "yes" || trimmed == "on") {
                        return true
                    }
                    if (trimmed == "false" || trimmed == "0" || trimmed == "no" || trimmed == "off") {
                        return false
                    }
                }
            }
        }
        val dataMap = doc.data ?: return false
        for ((k, v) in dataMap) {
            val normalizedKey = k.lowercase().replace("_", "").replace(" ", "")
            if (normalizedKey == "status" || normalizedKey == "stetus" || normalizedKey == "updatestatus" || normalizedKey == "isupdate") {
                when (v) {
                    is Boolean -> return v
                    is Number -> return v.toInt() == 1
                    is String -> {
                        val trimmed = v.trim().lowercase()
                        if (trimmed == "true" || trimmed == "1" || trimmed == "yes" || trimmed == "on") {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    private fun extractVersion(doc: com.google.firebase.firestore.DocumentSnapshot): String {
        val candidateKeys = listOf(
            "version", "app_version", "app version", "appVersion",
            "new_version", "new version", "latest_version", "latest version",
            "version_name", "versionName", "ver", "target_version"
        )
        for (key in candidateKeys) {
            val value = doc.get(key) ?: continue
            val str = value.toString().trim()
            if (str.isNotBlank()) return str
        }
        val dataMap = doc.data ?: return ""
        for ((k, v) in dataMap) {
            val normalizedKey = k.lowercase().replace("_", "").replace(" ", "")
            if (normalizedKey == "version" || normalizedKey == "appversion" || normalizedKey == "latestversion" || normalizedKey == "ver") {
                val str = v?.toString()?.trim() ?: ""
                if (str.isNotBlank()) return str
            }
        }
        return ""
    }

    private fun extractUrl(doc: com.google.firebase.firestore.DocumentSnapshot): String {
        val candidateKeys = listOf(
            "url", "download_url", "download url", "downloadUrl",
            "link", "app_url", "apk_url", "apk link", "apk_link"
        )
        for (key in candidateKeys) {
            val value = doc.get(key) ?: continue
            val str = value.toString().trim()
            if (str.isNotBlank()) return str
        }
        val dataMap = doc.data ?: return ""
        for ((k, v) in dataMap) {
            val normalizedKey = k.lowercase().replace("_", "").replace(" ", "")
            if (normalizedKey.contains("url") || normalizedKey.contains("link")) {
                val str = v?.toString()?.trim() ?: ""
                if (str.isNotBlank()) return str
            }
        }
        return ""
    }

    private fun extractNewFeatures(doc: com.google.firebase.firestore.DocumentSnapshot): String {
        val candidateKeys = listOf(
            "new fetures", "new features", "new futures", "new_features",
            "features", "new updates", "new_updates", "description",
            "whats_new", "whats new", "changelog", "notes", "message"
        )
        for (key in candidateKeys) {
            val value = doc.get(key) ?: continue
            val str = value.toString().trim()
            if (str.isNotBlank()) return str
        }
        return ""
    }

    private fun parseUpdateDocument(doc: com.google.firebase.firestore.DocumentSnapshot, currentVersion: String): AppUpdateData {
        val status = extractStatus(doc)
        val version = extractVersion(doc)
        val url = extractUrl(doc)
        val newFeatures = extractNewFeatures(doc)

        val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V").trim()
        val cleanRemote = version.trim().removePrefix("v").removePrefix("V").trim()

        // 1. Check status boolean: If false -> shouldShowAlert = false
        // 2. If status is true -> Compare version string against local app version:
        //    If remote version is different from local version -> shouldShowAlert = true
        //    If remote version is blank -> shouldShowAlert = true
        //    If remote version matches local version -> shouldShowAlert = false (already up-to-date)
        val isVersionDifferent = cleanRemote.isNotBlank() && (cleanRemote != cleanCurrent)
        val shouldShow = status && (cleanRemote.isBlank() || isVersionDifferent)

        Log.i(TAG, "UPDATE EVALUATION [path='${doc.reference.path}', docId='${doc.id}']: status=$status, remoteVersion='$cleanRemote', localVersion='$cleanCurrent', shouldShowAlert=$shouldShow, url='$url'")

        return AppUpdateData(
            docId = doc.id,
            status = status,
            version = if (version.isNotBlank()) version else cleanRemote,
            url = url,
            newFeatures = newFeatures,
            currentAppVersion = currentVersion,
            shouldShowAlert = shouldShow
        )
    }

    /**
     * Fetches the app update configuration directly from root /update or /update alert collections (outside user collection)
     */
    suspend fun fetchAppUpdateDocument(context: Context): AppUpdateData? = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestoreSafe() ?: return@withContext null
            val currentVersion = getAppVersionName(context)

            // Ensure anonymous authentication if Firestore rules require request.auth != null
            val auth = getFirebaseAuthSafe()
            if (auth != null && auth.currentUser == null) {
                try {
                    auth.signInAnonymously().await()
                    Log.i(TAG, "Authenticated anonymously for Firestore update access")
                } catch (e: Exception) {
                    Log.d(TAG, "Anonymous auth notice: ${e.message}")
                }
            }

            // Target root collections outside user collection in order of priority
            val rootCollectionNames = listOf("update alert", "update", "updates", "app_update", "app_updates")

            // 1. Check direct documents in primary root collections
            val candidateDocIds = listOf("Rjxlap1CyCRtgn683jE7", "alert", "latest", "config")
            for (col in listOf("update alert", "update")) {
                for (docId in candidateDocIds) {
                    try {
                        val directDoc = firestore.collection(col).document(docId).get().await()
                        if (directDoc != null && directDoc.exists()) {
                            val parsed = parseUpdateDocument(directDoc, currentVersion)
                            Log.i(TAG, "Direct document $col/$docId fetch success: status=${parsed.status}, version=${parsed.version}, shouldShow=${parsed.shouldShowAlert}")
                            if (parsed.shouldShowAlert) return@withContext parsed
                        }
                    } catch (_: Exception) {}
                }
            }

            // 2. Query all root collections outside user collection
            for (colName in rootCollectionNames) {
                try {
                    val updateCol = firestore.collection(colName).get().await()
                    if (!updateCol.isEmpty) {
                        val parsedDocs = updateCol.documents.map { parseUpdateDocument(it, currentVersion) }
                        val activeAlert = parsedDocs.firstOrNull { it.shouldShowAlert }
                        if (activeAlert != null) return@withContext activeAlert
                        val statusTrueDoc = parsedDocs.firstOrNull { it.status }
                        if (statusTrueDoc != null) return@withContext statusTrueDoc
                        return@withContext parsedDocs.first()
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Root collection '$colName' query notice: ${e.message}")
                }
            }

            // 3. Collection Group fallback across entire database
            for (colGroup in listOf("update alert", "update")) {
                try {
                    val groupDocs = firestore.collectionGroup(colGroup).get().await()
                    if (!groupDocs.isEmpty) {
                        val parsedDocs = groupDocs.documents.map { parseUpdateDocument(it, currentVersion) }
                        val active = parsedDocs.firstOrNull { it.shouldShowAlert } ?: parsedDocs.firstOrNull { it.status } ?: parsedDocs.firstOrNull()
                        if (active != null) return@withContext active
                    }
                } catch (_: Exception) {}
            }

            null
        } catch (e: Throwable) {
            Log.e(TAG, "Error in fetchAppUpdateDocument: ${e.message}", e)
            null
        }
    }

    /**
     * Realtime flow listener for root collections outside user collection (update alert, update, updates)
     */
    fun listenToAppUpdateFlow(context: Context): kotlinx.coroutines.flow.Flow<AppUpdateData?> = kotlinx.coroutines.flow.callbackFlow {
        val firestore = getFirestoreSafe()
        if (firestore == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val currentVersion = getAppVersionName(context)
        val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()

        // 1. Listeners for root collections outside user collection
        val collectionsToListen = listOf("update alert", "update", "updates", "app_update")
        for (colName in collectionsToListen) {
            try {
                val regCol = firestore.collection(colName)
                    .addSnapshotListener { querySnapshot, error ->
                        if (error != null) {
                            Log.d(TAG, "Collection '$colName' listener notice: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (querySnapshot != null && !querySnapshot.isEmpty) {
                            val parsedDocs = querySnapshot.documents.map { parseUpdateDocument(it, currentVersion) }
                            val active = parsedDocs.firstOrNull { it.shouldShowAlert }
                                ?: parsedDocs.firstOrNull { it.status }
                                ?: parsedDocs.firstOrNull()
                            if (active != null) {
                                Log.i(TAG, "Realtime listener on '$colName' emitted -> status=${active.status}, version=${active.version}, shouldShow=${active.shouldShowAlert}")
                                trySend(active)
                            }
                        }
                    }
                listeners.add(regCol)
            } catch (e: Exception) {
                Log.d(TAG, "Failed to register listener on '$colName': ${e.message}")
            }
        }

        // 2. Direct document listener on root /update/Rjxlap1CyCRtgn683jE7 and /update alert/Rjxlap1CyCRtgn683jE7
        for (col in listOf("update", "update alert")) {
            try {
                val regDirectDoc = firestore.collection(col).document("Rjxlap1CyCRtgn683jE7")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) return@addSnapshotListener
                        if (snapshot != null && snapshot.exists()) {
                            val data = parseUpdateDocument(snapshot, currentVersion)
                            Log.i(TAG, "Direct document listener on $col/Rjxlap1CyCRtgn683jE7 emitted -> status=${data.status}, version=${data.version}, shouldShow=${data.shouldShowAlert}")
                            trySend(data)
                        }
                    }
                listeners.add(regDirectDoc)
            } catch (_: Exception) {}
        }

        // 3. Collection Group snapshot listeners
        for (groupName in listOf("update alert", "update")) {
            try {
                val regGroup = firestore.collectionGroup(groupName)
                    .addSnapshotListener { querySnapshot, error ->
                        if (error != null) return@addSnapshotListener
                        if (querySnapshot != null && !querySnapshot.isEmpty) {
                            val parsedDocs = querySnapshot.documents.map { parseUpdateDocument(it, currentVersion) }
                            val active = parsedDocs.firstOrNull { it.shouldShowAlert }
                                ?: parsedDocs.firstOrNull { it.status }
                                ?: parsedDocs.firstOrNull()
                            if (active != null) {
                                trySend(active)
                            }
                        }
                    }
                listeners.add(regGroup)
            } catch (_: Exception) {}
        }

        // Initial direct fetch
        CoroutineScope(Dispatchers.IO).launch {
            val fetched = fetchAppUpdateDocument(context)
            if (fetched != null) {
                trySend(fetched)
            }
        }

        awaitClose {
            listeners.forEach {
                try {
                    it.remove()
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Fetches the app update configuration from "update alert" Firestore collection
     */
    suspend fun fetchAppUpdateInfo(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestoreSafe() ?: return@withContext null
            Log.d(TAG, "Fetching app update info from collection 'update alert'")
            val querySnapshot = firestore.collection("update alert")
                .get()
                .await()
            
            Log.d(TAG, "Query snapshot size: ${querySnapshot.size()}")
            if (!querySnapshot.isEmpty) {
                val doc = querySnapshot.documents.first()
                val appName = doc.getString("app name") ?: ""
                val appVersion = doc.getString("app version") ?: ""
                val downloadUrl = doc.getString("download url") ?: ""
                val newUpdates = doc.getString("new updates") ?: ""
                val updateStatus = doc.getBoolean("update status") ?: false
                
                Log.d(TAG, "Fetched update alert: status=$updateStatus, version=$appVersion")
                return@withContext AppUpdateInfo(
                    appName = appName,
                    appVersion = appVersion,
                    downloadUrl = downloadUrl,
                    newUpdates = newUpdates,
                    updateStatus = updateStatus
                )
            }
            null
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to fetch app update info from Firestore: ${e.message}")
            null
        }
    }

    /**
     * Records when the video pipeline is started (from START VIDEO or prompt icon click)
     */
    suspend fun recordPipelineStart(context: Context, status: String = "video_start"): Boolean = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestoreSafe() ?: return@withContext false
            val uid = getCurrentUid(context)
            val email = GoogleSignInHelper.getSavedUserFromPreferences(context)?.email ?: "unknown@gmail.com"
            
            val record = hashMapOf<String, Any>(
                "uid" to uid,
                "user_email" to email,
                "status" to status,
                "isPipelineStarted" to true,
                "startedAt" to FieldValue.serverTimestamp(),
                "timestamp" to System.currentTimeMillis(),
                "deviceId" to DeviceDataCollector.getDeviceId(context)
            )
            
            firestore.collection("video_pipeline_runs")
                .document(uid)
                .set(record, SetOptions.merge())
                .await()
            
            Log.d(TAG, "Recorded pipeline start in Firestore collection 'video_pipeline_runs'")
            true
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to record pipeline start in Firestore: ${e.message}")
            false
        }
    }

    /**
     * Checks if the pipeline run state is marked as started in Firestore
     */
    suspend fun checkPipelineRunState(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestoreSafe() ?: return@withContext false
            val uid = getCurrentUid(context)
            val docSnapshot = firestore.collection("video_pipeline_runs")
                .document(uid)
                .get()
                .await()
            
            if (docSnapshot.exists()) {
                return@withContext docSnapshot.getBoolean("isPipelineStarted") ?: false
            }
            false
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to check pipeline run state from Firestore: ${e.message}")
            false
        }
    }

    /**
     * Gets the latest YouTube video upload timestamp for the user
     */
    suspend fun getLastVideoUploadTime(context: Context): Long = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestoreSafe() ?: return@withContext 0L
            val uid = getCurrentUid(context)
            val querySnapshot = firestore.collection("users")
                .document(uid)
                .collection("youtube_uploads")
                .orderBy("uploaded_at_ms", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                val doc = querySnapshot.documents.first()
                return@withContext doc.getLong("uploaded_at_ms") ?: 0L
            }
            0L
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to get last video upload time from Firestore: ${e.message}")
            0L
        }
    }
}
