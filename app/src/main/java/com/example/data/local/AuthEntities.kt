package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_auth")
data class UserEntity(
    @PrimaryKey val email: String,
    val name: String,
    val avatarUrl: String?,
    val oauthToken: String?,
    val lastLogin: Long
)

@Entity(tableName = "scheduler_settings")
data class SchedulerEntity(
    @PrimaryKey val id: Int = 1,
    val enabled: Boolean,
    val intervalHours: Int,
    val category: String,
    val lastRunTime: Long,
    val nextRunTime: Long
)
