package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_settings")
data class AgentSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val jsonContent: String,
    val updatedAt: Long = System.currentTimeMillis()
)
