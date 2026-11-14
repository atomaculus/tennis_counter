package com.example.tenniscounter.mobile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Room-persisted training session (paredón, 21) played on the phone. */
@Entity(tableName = "training_sessions")
data class TrainingSessionEntity(
    @PrimaryKey val id: String,
    val modality: String,
    val createdAt: Long,
    val durationSeconds: Long,
    val targetPoints: Int,
    val finalCount: Int,
    val bestStreak: Int,
    val attempts: Int
)
