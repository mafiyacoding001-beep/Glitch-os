package com.example.data.local

import androidx.room.*

@Dao
interface AuthDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUser(user: UserEntity)

    @Query("SELECT * FROM user_auth LIMIT 1")
    suspend fun getUser(): UserEntity?
}

@Dao
interface SchedulerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: SchedulerEntity)

    @Query("SELECT * FROM scheduler_settings WHERE id = 1")
    suspend fun getSettings(): SchedulerEntity?
}
