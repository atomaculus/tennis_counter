package com.example.tenniscounter.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(match: MatchEntity): Long

    @Update
    suspend fun update(match: MatchEntity)

    @Query("SELECT COUNT(*) FROM matches")
    suspend fun getMatchCount(): Int

    @Query("SELECT AVG(durationSeconds) FROM matches")
    suspend fun getAverageDuration(): Double?

    @Query("SELECT MAX(durationSeconds) FROM matches")
    suspend fun getLongestMatchDuration(): Long?

    @Query("SELECT MIN(durationSeconds) FROM matches")
    suspend fun getShortestMatchDuration(): Long?

    @Query("SELECT SUM(durationSeconds) FROM matches")
    suspend fun getTotalPlayTime(): Long?

    @Query("SELECT * FROM matches ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestMatch(): MatchEntity?

    @Query("SELECT * FROM matches ORDER BY createdAt DESC")
    suspend fun getAllMatchesOnce(): List<MatchEntity>

    @Query(
        """
        SELECT * FROM matches
        WHERE caloriesKcal IS NOT NULL
           OR avgHeartRateBpm IS NOT NULL
           OR maxHeartRateBpm IS NOT NULL
        ORDER BY createdAt DESC
        LIMIT :limit
        """
    )
    suspend fun getRecentMatchesWithHealthMetrics(limit: Int): List<MatchEntity>
}
