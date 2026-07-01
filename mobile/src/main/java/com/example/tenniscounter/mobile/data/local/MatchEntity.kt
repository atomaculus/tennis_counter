package com.example.tenniscounter.mobile.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "matches",
    indices = [Index(value = ["idempotencyKey"], unique = true)]
)
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val durationSeconds: Long,
    val finalScoreText: String,
    val setScoresText: String? = null,
    val photoUri: String? = null,
    val idempotencyKey: String,
    val playerAName: String? = null,
    val playerBName: String? = null,
    val caloriesKcal: Double? = null,
    val avgHeartRateBpm: Int? = null,
    val maxHeartRateBpm: Int? = null
)
