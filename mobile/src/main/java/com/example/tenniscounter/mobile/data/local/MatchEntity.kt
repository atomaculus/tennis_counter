package com.example.tenniscounter.mobile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val durationSeconds: Long,
    val finalScoreText: String,
    val photoUri: String? = null
)
