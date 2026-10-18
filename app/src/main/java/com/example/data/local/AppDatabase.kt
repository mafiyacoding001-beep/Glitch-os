package com.example.data.local

import android.content.Context
import androidx.room.*

@Entity(tableName = "installed_apps")
data class InstalledAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String = "",
    val isSystemApp: Boolean = false,
    val installTimeEpochMs: Long = System.currentTimeMillis()
)

@Dao
interface InstalledAppDao {
    @Query("SELECT * FROM installed_apps ORDER BY appName ASC")
    suspend fun getAllInstalledApps(): List<InstalledAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<InstalledAppEntity>)
}

@Entity(tableName = "youtube_chat_messages")
data class YouTubeChatMessageEntity(
    @PrimaryKey val messageId: String,
    val authorName: String = "",
    val messageText: String = "",
    val timestampEpochMs: Long = System.currentTimeMillis()
)

@Dao
interface YouTubeChatDao {
    @Query("SELECT * FROM youtube_chat_messages ORDER BY timestampEpochMs ASC")
    suspend fun getAllMessages(): List<YouTubeChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: YouTubeChatMessageEntity)
}

@Database(
    entities = [
        AgentSettingsEntity::class,
        AppiumLogEntity::class,
        VideoProjectEntity::class,
        QuotaLedgerEntity::class,
        ProcessedCommentEntity::class,
        AutonomousQueueEntity::class,
        ChannelProfileEntity::class,
        YouTubeChannelAccountEntity::class,
        YouTubeChatMessageEntity::class,
        InstalledAppEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun agentSettingsDao(): AgentSettingsDao
    abstract fun appiumLogDao(): AppiumLogDao
    abstract fun videoProjectDao(): VideoProjectDao
    abstract fun autonomousChannelDao(): AutonomousChannelDao
    abstract fun youTubeChannelDao(): YouTubeChannelDao
    abstract fun youTubeChatDao(): YouTubeChatDao
    abstract fun installedAppDao(): InstalledAppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "glitchos_app_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun getDatabase(context: Context): AppDatabase = getInstance(context)
    }
}
