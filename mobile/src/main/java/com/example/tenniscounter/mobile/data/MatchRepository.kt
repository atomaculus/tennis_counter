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
        idempotencyKey: String,
        setScoresText: String? = null,
        photoUri: String? = null
    ): Boolean {
        val rowId = matchDao.insertOrIgnore(
            MatchEntity(
                createdAt = createdAt,
                durationSeconds = durationSeconds,
                finalScoreText = finalScoreText,
                setScoresText = setScoresText,
                photoUri = photoUri,
                idempotencyKey = idempotencyKey
            )
        )
        return rowId != -1L
    }

    suspend fun update(match: MatchEntity) = matchDao.update(match)
}
