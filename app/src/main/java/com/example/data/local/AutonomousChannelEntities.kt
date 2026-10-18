package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "youtube_quota_ledger")
data class QuotaLedgerEntity(
    @PrimaryKey val date: String,            // Format: "YYYY-MM-DD"
    val usedUnits: Int = 0,                 // Daily quota limit: 10,000 units (9,500 hard safety cap)
    val uploadCountToday: Int = 0,          // 1,600 units each
    val commentsRepliedToday: Int = 0,      // 50 units each
    val lastResetEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "processed_comments_log")
data class ProcessedCommentEntity(
    @PrimaryKey val commentId: String,       // YouTube Comment ID or Thread ID
    val videoId: String,
    val authorDisplayName: String,
    val originalText: String,
    val replyText: String,
    val isPinned: Boolean = false,
    val repliedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "autonomous_content_queue")
data class AutonomousQueueEntity(
    @PrimaryKey val queueId: String = java.util.UUID.randomUUID().toString(),
    val topicTitle: String,
    val category: String,
    val viralScore: Int,                 // 1-100 predicted viral potential
    val hookFormula: String,             // e.g., "The Hidden Truth..."
    val hookSentence: String,            // First 3-second retention line
    val scriptOutline: String,           // Key bullet points / structure
    val voiceNarration: String,          // Full narration script ready for TTS
    val targetKeywords: String,          // Comma-separated high CTR tags
    val scheduledPublishEpochMs: Long,   // Target autonomous publish time
    val status: String = "QUEUED",       // QUEUED, GENERATED, PUBLISHED, or FAILED
    val publishedVideoId: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "channel_profiles")
data class ChannelProfileEntity(
    @PrimaryKey val channelId: String,          // YouTube Channel ID
    val channelTitle: String,
    val channelHandle: String,
    val avatarUrl: String,
    val subscriberCount: String,
    val videoCount: String,
    val defaultCategory: String = "AI & Futuristic",
    val autoPilotEnabled: Boolean = false,
    val targetDailyUploads: Int = 1,
    val isActiveProfile: Boolean = false,
    val lastSyncEpochMs: Long = System.currentTimeMillis()
)

@Dao
interface AutonomousChannelDao {

    @Query("SELECT * FROM youtube_quota_ledger WHERE date = :date LIMIT 1")
    suspend fun getQuotaForDate(date: String): QuotaLedgerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateQuota(quota: QuotaLedgerEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM processed_comments_log WHERE commentId = :commentId)")
    suspend fun isCommentProcessed(commentId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProcessedComment(comment: ProcessedCommentEntity)

    @Query("SELECT * FROM processed_comments_log ORDER BY repliedAtEpochMs DESC LIMIT :limit")
    fun getRecentProcessedComments(limit: Int = 20): Flow<List<ProcessedCommentEntity>>

    @Query("SELECT COUNT(*) FROM processed_comments_log WHERE videoId = :videoId")
    suspend fun getProcessedCountForVideo(videoId: String): Int

    @Query("SELECT COUNT(*) FROM processed_comments_log")
    suspend fun getTotalRepliedCount(): Int

    // ── Content Queue Operations (Step 2) ───────────────────────────
    @Query("SELECT * FROM autonomous_content_queue ORDER BY scheduledPublishEpochMs ASC")
    fun getAllQueuedContentFlow(): Flow<List<AutonomousQueueEntity>>

    @Query("SELECT * FROM autonomous_content_queue ORDER BY scheduledPublishEpochMs ASC")
    suspend fun getAllQueuedContent(): List<AutonomousQueueEntity>

    @Query("SELECT * FROM autonomous_content_queue WHERE status = :status ORDER BY scheduledPublishEpochMs ASC")
    suspend fun getItemsByStatus(status: String): List<AutonomousQueueEntity>

    @Query("SELECT * FROM autonomous_content_queue WHERE scheduledPublishEpochMs <= :currentTimeEpochMs AND status = 'QUEUED' ORDER BY scheduledPublishEpochMs ASC LIMIT 1")
    suspend fun getNextPendingDueItem(currentTimeEpochMs: Long): AutonomousQueueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItem(item: AutonomousQueueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItems(items: List<AutonomousQueueEntity>)

    @Query("UPDATE autonomous_content_queue SET status = :newStatus, publishedVideoId = :videoId WHERE queueId = :queueId")
    suspend fun updateQueueItemStatus(queueId: String, newStatus: String, videoId: String? = null)

    @Query("DELETE FROM autonomous_content_queue WHERE queueId = :queueId")
    suspend fun deleteQueueItem(queueId: String)

    @Query("DELETE FROM autonomous_content_queue WHERE status = 'PUBLISHED'")
    suspend fun clearPublishedQueue()

    // ── Multi-Channel Profile Operations (Step 5) ───────────────────
    @Query("SELECT * FROM channel_profiles ORDER BY isActiveProfile DESC, channelTitle ASC")
    fun getAllProfilesFlow(): Flow<List<ChannelProfileEntity>>

    @Query("SELECT * FROM channel_profiles WHERE isActiveProfile = 1 LIMIT 1")
    suspend fun getActiveProfile(): ChannelProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: ChannelProfileEntity)

    @Query("UPDATE channel_profiles SET isActiveProfile = (channelId = :activeChannelId)")
    suspend fun setActiveProfile(activeChannelId: String)

    @Query("DELETE FROM channel_profiles WHERE channelId = :channelId")
    suspend fun deleteProfile(channelId: String)
}
