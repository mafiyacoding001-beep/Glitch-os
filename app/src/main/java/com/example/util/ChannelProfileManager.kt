package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.ChannelProfileEntity
import com.example.data.local.YouTubeChannelAccountEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

object ChannelProfileManager {
    private const val TAG = "ChannelProfileManager"

    fun getAllChannelsFlow(context: Context): Flow<List<YouTubeChannelAccountEntity>> {
        return AppDatabase.getInstance(context).youTubeChannelDao().getAllChannelsFlow()
    }

    fun getAllProfilesFlow(context: Context): Flow<List<ChannelProfileEntity>> {
        return AppDatabase.getInstance(context).autonomousChannelDao().getAllProfilesFlow()
    }

    fun getActiveChannelFlow(context: Context): Flow<YouTubeChannelAccountEntity?> {
        return AppDatabase.getInstance(context).youTubeChannelDao().getActiveChannelFlow()
    }

    suspend fun getActiveChannel(context: Context): YouTubeChannelAccountEntity? = withContext(Dispatchers.IO) {
        AppDatabase.getInstance(context).youTubeChannelDao().getActiveChannel()
    }

    suspend fun saveOrSyncCurrentChannelProfile(
        context: Context,
        channelInfo: YouTubeChannelInfo?,
        defaultCategory: String,
        autoPilotEnabled: Boolean,
        oauthToken: String? = null,
        authEmail: String = ""
    ): YouTubeChannelAccountEntity? = withContext(Dispatchers.IO) {
        if (channelInfo == null) return@withContext null
        val chId = channelInfo.channelId ?: return@withContext null
        if (chId.isBlank()) return@withContext null

        try {
            val db = AppDatabase.getInstance(context)
            val channelDao = db.youTubeChannelDao()
            val autonomousDao = db.autonomousChannelDao()

            val existing = channelDao.getChannelById(chId)
            val channelAccount = YouTubeChannelAccountEntity(
                channelId = chId,
                channelTitle = channelInfo.channelName,
                channelHandle = channelInfo.channelHandle,
                avatarUrl = channelInfo.avatarUrl ?: "",
                subscriberCount = channelInfo.subscriberCount,
                videoCount = channelInfo.videoCount,
                viewCount = channelInfo.viewCount,
                defaultCategory = if (existing != null && existing.defaultCategory.isNotBlank()) existing.defaultCategory else defaultCategory,
                defaultLanguage = existing?.defaultLanguage ?: "Hindi",
                autoPilotEnabled = autoPilotEnabled,
                uploadIntervalHours = existing?.uploadIntervalHours ?: 12,
                isActiveProfile = true,
                authEmail = authEmail.ifBlank { existing?.authEmail ?: "" },
                oauthToken = oauthToken ?: existing?.oauthToken,
                refreshToken = existing?.refreshToken,
                lastSyncEpochMs = System.currentTimeMillis()
            )

            channelDao.insertOrUpdateChannel(channelAccount)
            channelDao.setActiveChannel(channelAccount.channelId)

            // Also sync legacy ChannelProfileEntity for backward compatibility
            val profile = ChannelProfileEntity(
                channelId = chId,
                channelTitle = channelInfo.channelName,
                channelHandle = channelInfo.channelHandle,
                avatarUrl = channelInfo.avatarUrl ?: "",
                subscriberCount = channelInfo.subscriberCount,
                videoCount = channelInfo.videoCount,
                defaultCategory = channelAccount.defaultCategory,
                autoPilotEnabled = autoPilotEnabled,
                targetDailyUploads = 1,
                isActiveProfile = true,
                lastSyncEpochMs = System.currentTimeMillis()
            )
            autonomousDao.insertOrUpdateProfile(profile)
            autonomousDao.setActiveProfile(profile.channelId)

            return@withContext channelAccount
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save channel profile: ${e.message}", e)
            return@withContext null
        }
    }

    suspend fun addNewChannelManually(
        context: Context,
        channelId: String,
        channelTitle: String,
        channelHandle: String,
        category: String,
        language: String = "Hindi",
        avatarUrl: String = "",
        autoPilotEnabled: Boolean = false,
        intervalHours: Int = 12
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            val cleanId = channelId.trim()
            if (cleanId.isBlank()) return@withContext false

            val newChannel = YouTubeChannelAccountEntity(
                channelId = cleanId,
                channelTitle = channelTitle.trim().ifBlank { "YouTube Channel ($cleanId)" },
                channelHandle = if (channelHandle.isNotBlank() && !channelHandle.startsWith("@")) "@$channelHandle" else channelHandle.trim(),
                avatarUrl = avatarUrl.trim(),
                defaultCategory = category,
                defaultLanguage = language,
                autoPilotEnabled = autoPilotEnabled,
                uploadIntervalHours = intervalHours,
                isActiveProfile = true,
                createdAtEpochMs = System.currentTimeMillis(),
                lastSyncEpochMs = System.currentTimeMillis()
            )

            db.youTubeChannelDao().insertOrUpdateChannel(newChannel)
            db.youTubeChannelDao().setActiveChannel(newChannel.channelId)

            val profile = ChannelProfileEntity(
                channelId = cleanId,
                channelTitle = newChannel.channelTitle,
                channelHandle = newChannel.channelHandle,
                avatarUrl = newChannel.avatarUrl,
                subscriberCount = "0",
                videoCount = "0",
                defaultCategory = category,
                autoPilotEnabled = autoPilotEnabled,
                targetDailyUploads = 1,
                isActiveProfile = true,
                lastSyncEpochMs = System.currentTimeMillis()
            )
            db.autonomousChannelDao().insertOrUpdateProfile(profile)
            db.autonomousChannelDao().setActiveProfile(cleanId)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding manual channel: ${e.message}", e)
            false
        }
    }

    suspend fun switchActiveProfile(context: Context, channelId: String) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            db.youTubeChannelDao().setActiveChannel(channelId)
            db.autonomousChannelDao().setActiveProfile(channelId)
            
            // Sync settings with channel's chosen category
            val activeChannel = db.youTubeChannelDao().getChannelById(channelId)
            if (activeChannel != null) {
                val settingsManager = com.example.data.local.SettingsManager(context)
                val current = settingsManager.settingsFlow.value
                settingsManager.updateSettings(
                    current.copy(
                        defaultCategory = activeChannel.defaultCategory,
                        autoCategory = activeChannel.defaultCategory,
                        defaultLanguage = activeChannel.defaultLanguage,
                        scheduleIntervalHours = activeChannel.uploadIntervalHours,
                        autoPublishEnabled = activeChannel.autoPilotEnabled
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch active profile to $channelId: ${e.message}")
        }
    }

    suspend fun updateChannelCategory(context: Context, channelId: String, category: String) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            db.youTubeChannelDao().updateChannelCategory(channelId, category)
            val profile = db.autonomousChannelDao().getActiveProfile()
            if (profile?.channelId == channelId) {
                db.autonomousChannelDao().insertOrUpdateProfile(profile.copy(defaultCategory = category))
            }
            val settingsManager = com.example.data.local.SettingsManager(context)
            val current = settingsManager.settingsFlow.value
            settingsManager.updateSettings(current.copy(defaultCategory = category, autoCategory = category))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update channel category: ${e.message}")
        }
    }

    suspend fun updateActiveChannelCategory(context: Context, category: String) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            val active = db.youTubeChannelDao().getActiveChannel()
            if (active != null) {
                updateChannelCategory(context, active.channelId, category)
            }
        } catch (_: Exception) {}
    }

    suspend fun deleteProfile(context: Context, channelId: String) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            db.youTubeChannelDao().deleteChannel(channelId)
            db.autonomousChannelDao().deleteProfile(channelId)
            
            // If deleted active channel, activate another available one
            val remaining = db.youTubeChannelDao().getAllChannels()
            if (remaining.isNotEmpty()) {
                switchActiveProfile(context, remaining.first().channelId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete profile $channelId: ${e.message}")
        }
    }

    suspend fun saveChannelAccount(
        context: Context,
        channel: YouTubeChannelAccountEntity
    ) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            db.youTubeChannelDao().insertOrUpdateChannel(channel)
            if (channel.isActiveProfile) {
                db.youTubeChannelDao().setActiveChannel(channel.channelId)
            }
            val profile = ChannelProfileEntity(
                channelId = channel.channelId,
                channelTitle = channel.channelTitle,
                channelHandle = channel.channelHandle,
                avatarUrl = channel.avatarUrl,
                subscriberCount = channel.subscriberCount,
                videoCount = channel.videoCount,
                defaultCategory = channel.defaultCategory,
                autoPilotEnabled = channel.autoPilotEnabled,
                targetDailyUploads = 1,
                isActiveProfile = channel.isActiveProfile,
                lastSyncEpochMs = System.currentTimeMillis()
            )
            db.autonomousChannelDao().insertOrUpdateProfile(profile)
            if (channel.isActiveProfile) {
                db.autonomousChannelDao().setActiveProfile(channel.channelId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save channel account: ${e.message}", e)
        }
    }

    suspend fun toggleAutoPilotForProfile(
        context: Context,
        channelId: String,
        isEnabled: Boolean,
        intervalHours: Int = 12
    ) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            db.youTubeChannelDao().updateChannelAutoPilot(channelId, isEnabled, intervalHours)
            val profile = db.autonomousChannelDao().getActiveProfile()
            if (profile?.channelId == channelId) {
                db.autonomousChannelDao().insertOrUpdateProfile(profile.copy(autoPilotEnabled = isEnabled))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle autopilot for $channelId: ${e.message}")
        }
    }
}
