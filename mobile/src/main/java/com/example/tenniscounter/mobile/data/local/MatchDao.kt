package com.example.tenniscounter.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY createdAt DESC")
    fun getAllMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :id LIMIT 1")
    suspend fun getMatchById(id: Long): MatchEntity?

    @Query("SELECT * FROM matches WHERE id = :id LIMIT 1")
    fun observeMatchById(id: Long): Flow<MatchEntity?>

    @Insert
    suspend fun insert(match: MatchEntity): Long

    @Query(
        "SELECT EXISTS(" +
            "SELECT 1 FROM matches " +
            "WHERE createdAt = :createdAt " +
            "AND durationSeconds = :durationSeconds " +
            "AND finalScoreText = :finalScoreText LIMIT 1)"
    )
    suspend fun existsMatch(
        createdAt: Long,
        durationSeconds: Long,
        finalScoreText: String
    ): Boolean

    @Update
    suspend fun update(match: MatchEntity)
}
