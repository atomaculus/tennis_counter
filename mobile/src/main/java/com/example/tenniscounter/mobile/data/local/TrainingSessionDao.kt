package com.example.tenniscounter.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingSessionDao {
    @Query("SELECT * FROM training_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<TrainingSessionEntity>>

    @Query("SELECT * FROM training_sessions ORDER BY createdAt DESC")
    suspend fun getAllSessionsOnce(): List<TrainingSessionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(session: TrainingSessionEntity): Long

    @Query("SELECT COUNT(*) FROM training_sessions")
    suspend fun getSessionCount(): Int
}
