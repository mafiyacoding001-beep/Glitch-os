package com.example.data.local

import androidx.room.*

@Entity(tableName = "offline_queue")
data class OfflineQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val taskType: String, // "GENERATION" or "UPLOAD"
    val title: String,
    val topic: String,
    val category: String,
    val language: String,
    val duration: Int,
    val videoPath: String? = null,
    val payloadJson: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val status: String = "PENDING" // PENDING, SUCCESS, FAILED
)

@Dao
interface OfflineQueueDao {
    @Query("SELECT * FROM offline_queue WHERE status = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingTasks(): List<OfflineQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: OfflineQueueEntity): Long

    @Update
    suspend fun updateTask(task: OfflineQueueEntity)

    @Delete
    suspend fun deleteTask(task: OfflineQueueEntity)

    @Query("DELETE FROM offline_queue WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)
}
