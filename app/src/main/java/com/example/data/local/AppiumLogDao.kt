package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppiumLogDao {
    @Query("SELECT * FROM appium_automation_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<AppiumLogEntity>>

    @Query("SELECT * FROM appium_automation_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AppiumLogEntity>>

    @Query("SELECT * FROM appium_automation_logs ORDER BY timestamp DESC")
    suspend fun getAllLogsSync(): List<AppiumLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AppiumLogEntity)

    @Query("DELETE FROM appium_automation_logs")
    suspend fun clearLogs()
}
