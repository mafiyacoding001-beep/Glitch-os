package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Entity for storing multiple connected YouTube Channel accounts.
 * Supports individual category configurations, OAuth credentials, and AutoPilot rules per channel.
 */
@Entity(tableName = "youtube_channels")
data class YouTubeChannelAccountEntity(
    @PrimaryKey val channelId: String,
    val channelTitle: String,
    val channelHandle: String = "",
    val avatarUrl: String = "",
    val subscriberCount: String = "0",
    val videoCount: String = "0",
    val viewCount: String = "0",
    val defaultCategory: String = "AI & Futuristic",
    val defaultLanguage: String = "Hindi",
    val autoPilotEnabled: Boolean = false,
    val uploadIntervalHours: Int = 12,
    val isActiveProfile: Boolean = false,
    val authEmail: String = "",
    val oauthToken: String? = null,
    val refreshToken: String? = null,
    val customApiKey: String? = null,
    val customClientId: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val lastSyncEpochMs: Long = System.currentTimeMillis()
)

@Dao
interface YouTubeChannelDao {

    @Query("SELECT * FROM youtube_channels ORDER BY isActiveProfile DESC, createdAtEpochMs DESC")
    fun getAllChannelsFlow(): Flow<List<YouTubeChannelAccountEntity>>

    @Query("SELECT * FROM youtube_channels ORDER BY isActiveProfile DESC, createdAtEpochMs DESC")
    suspend fun getAllChannels(): List<YouTubeChannelAccountEntity>

    @Query("SELECT * FROM youtube_channels WHERE isActiveProfile = 1 LIMIT 1")
    suspend fun getActiveChannel(): YouTubeChannelAccountEntity?

    @Query("SELECT * FROM youtube_channels WHERE isActiveProfile = 1 LIMIT 1")
    fun getActiveChannelFlow(): Flow<YouTubeChannelAccountEntity?>

    @Query("SELECT * FROM youtube_channels WHERE channelId = :channelId LIMIT 1")
    suspend fun getChannelById(channelId: String): YouTubeChannelAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateChannel(channel: YouTubeChannelAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<YouTubeChannelAccountEntity>)

    @Update
    suspend fun updateChannel(channel: YouTubeChannelAccountEntity)

    @Query("UPDATE youtube_channels SET isActiveProfile = (channelId = :channelId)")
    suspend fun setActiveChannel(channelId: String)

    @Query("UPDATE youtube_channels SET defaultCategory = :category WHERE channelId = :channelId")
    suspend fun updateChannelCategory(channelId: String, category: String)

    @Query("UPDATE youtube_channels SET autoPilotEnabled = :enabled, uploadIntervalHours = :intervalHours WHERE channelId = :channelId")
    suspend fun updateChannelAutoPilot(channelId: String, enabled: Boolean, intervalHours: Int)

    @Query("UPDATE youtube_channels SET oauthToken = :token, lastSyncEpochMs = :syncTime WHERE channelId = :channelId")
    suspend fun updateChannelOAuthToken(channelId: String, token: String?, syncTime: Long = System.currentTimeMillis())

    @Query("UPDATE youtube_channels SET subscriberCount = :subs, videoCount = :videos, viewCount = :views, lastSyncEpochMs = :syncTime WHERE channelId = :channelId")
    suspend fun updateChannelStats(channelId: String, subs: String, videos: String, views: String, syncTime: Long = System.currentTimeMillis())

    @Query("DELETE FROM youtube_channels WHERE channelId = :channelId")
    suspend fun deleteChannel(channelId: String)

    @Query("SELECT COUNT(*) FROM youtube_channels")
    suspend fun getChannelCount(): Int
}
