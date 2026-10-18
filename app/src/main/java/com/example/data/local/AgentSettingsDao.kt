package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentSettingsDao {
    @Query("SELECT * FROM agent_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<AgentSettingsEntity?>

    @Query("SELECT * FROM agent_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): AgentSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(entity: AgentSettingsEntity)
}
