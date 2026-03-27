package com.example.tenniscounter.mobile.data

import com.example.tenniscounter.mobile.data.local.MatchDao
import com.example.tenniscounter.mobile.data.local.MatchEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchRepositoryTest {

    @Test
    fun insertIfNotExists_returnsFalseForDuplicateIdempotencyKey() = runBlocking {
        val dao = FakeMatchDao()
        val repository = MatchRepository(dao)

        val firstInsert = repository.insertIfNotExists(
            createdAt = 1_000L,
            durationSeconds = 3_600L,
            finalScoreText = "2-1",
            idempotencyKey = "same-key",
            setScoresText = "6-4 3-6 6-2"
        )
        val duplicateInsert = repository.insertIfNotExists(
            createdAt = 1_000L,
            durationSeconds = 3_600L,
            finalScoreText = "2-1",
            idempotencyKey = "same-key",
            setScoresText = "6-4 3-6 6-2"
        )

        assertTrue(firstInsert)
        assertFalse(duplicateInsert)
        assertEquals(1, dao.snapshot().size)
    }

    @Test
    fun insertIfNotExists_preservesOptionalFields() = runBlocking {
        val dao = FakeMatchDao()
        val repository = MatchRepository(dao)

        repository.insertIfNotExists(
            createdAt = 2_000L,
            durationSeconds = 5_400L,
            finalScoreText = "2-0",
            idempotencyKey = "unique-key",
            setScoresText = "6-3 6-4",
            photoUri = "content://share/card"
        )

        val stored = dao.snapshot().single()
        assertEquals("6-3 6-4", stored.setScoresText)
        assertEquals("content://share/card", stored.photoUri)
        assertEquals("unique-key", stored.idempotencyKey)
    }
}

private class FakeMatchDao : MatchDao {
    private val matches = mutableListOf<MatchEntity>()
    private val allMatchesFlow = MutableStateFlow<List<MatchEntity>>(emptyList())
    private var nextId = 1L

    override fun getAllMatches(): Flow<List<MatchEntity>> = allMatchesFlow

    override suspend fun getMatchById(id: Long): MatchEntity? = matches.firstOrNull { it.id == id }

    override fun observeMatchById(id: Long): Flow<MatchEntity?> {
        return MutableStateFlow(matches.firstOrNull { it.id == id })
    }

    override suspend fun insert(match: MatchEntity): Long {
        val assigned = match.copy(id = nextId++)
        matches += assigned
        allMatchesFlow.value = matches.toList()
        return assigned.id
    }

    override suspend fun insertOrIgnore(match: MatchEntity): Long {
        if (matches.any { it.idempotencyKey == match.idempotencyKey }) {
            return -1L
        }
        return insert(match)
    }

    override suspend fun update(match: MatchEntity) {
        val index = matches.indexOfFirst { it.id == match.id }
        if (index >= 0) {
            matches[index] = match
            allMatchesFlow.value = matches.toList()
        }
    }

    fun snapshot(): List<MatchEntity> = matches.toList()
}
