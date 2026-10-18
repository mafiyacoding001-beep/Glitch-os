package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        UserEntity::class,
        SchedulerEntity::class,
        OfflineQueueEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class VideoDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun authDao(): AuthDao
    abstract fun schedulerDao(): SchedulerDao
    abstract fun offlineQueueDao(): OfflineQueueDao

    companion object {
        @Volatile
        private var INSTANCE: VideoDatabase? = null

        fun getDatabase(context: Context): VideoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VideoDatabase::class.java,
                    "glitch_video_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
