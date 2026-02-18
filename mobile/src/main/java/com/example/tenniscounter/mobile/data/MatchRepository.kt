package com.example.tenniscounter.mobile.data

import com.example.tenniscounter.mobile.data.local.MatchDao
import com.example.tenniscounter.mobile.data.local.MatchEntity
import kotlinx.coroutines.flow.Flow

class MatchRepository(
    private val matchDao: MatchDao
) {
    fun getAllMatches(): Flow<List<MatchEntity>> = matchDao.getAllMatches()

    suspend fun getMatchById(id: Long): MatchEntity? = matchDao.getMatchById(id)

    fun observeMatchById(id: Long): Flow<MatchEntity?> = matchDao.observeMatchById(id)

    suspend fun insert(match: MatchEntity): Long = matchDao.insert(match)

    suspend fun insertIfNotExists(
        createdAt: Long,
        durationSeconds: Long,
        finalScoreText: String,
        setScoresText: String? = null,
        photoUri: String? = null
    ): Boolean {
        if (matchDao.existsMatch(createdAt, durationSeconds, finalScoreText)) return false
        matchDao.insert(
            MatchEntity(
                createdAt = createdAt,
                durationSeconds = durationSeconds,
                finalScoreText = finalScoreText,
                setScoresText = setScoresText,
                photoUri = photoUri
            )
        )
        return true
    }

    suspend fun update(match: MatchEntity) = matchDao.update(match)
}
