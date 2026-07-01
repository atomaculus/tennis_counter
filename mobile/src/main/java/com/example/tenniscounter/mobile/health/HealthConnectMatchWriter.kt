package com.example.tenniscounter.mobile.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import com.example.tenniscounter.mobile.data.local.MatchEntity
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class HealthConnectMatchWriter(private val context: Context) {
    private val sourceDevice = Device(
        type = Device.TYPE_WATCH,
        manufacturer = "PLAYCE",
        model = "Wear OS"
    )

    suspend fun writeTennisSessionIfPermitted(match: MatchEntity) {
        runCatching {
            if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) {
                Log.i(TAG, "Health Connect unavailable; skipping session write")
                return
            }

            val client = HealthConnectClient.getOrCreate(context)
            val requiredPermissions = requiredPermissions(match)
            val grantedPermissions = client.permissionController.getGrantedPermissions()
            if (!grantedPermissions.containsAll(requiredPermissions)) {
                Log.i(TAG, "Health Connect permissions missing; skipping session write")
                return
            }

            val end = Instant.ofEpochMilli(match.createdAt)
            val start = end.minusSeconds(match.durationSeconds.coerceAtLeast(0L))
            if (!start.isBefore(end)) {
                Log.w(TAG, "Invalid match time range for Health Connect; skipping session write")
                return
            }

            val zoneOffset = ZoneId.systemDefault().rules.getOffset(end)
            deleteExistingRecords(client, match)
            client.insertRecords(buildRecords(match, start, end, zoneOffset))
            Log.i(TAG, "Health Connect tennis session written idempotencyKey=${match.idempotencyKey}")
        }.onFailure {
            Log.w(TAG, "Health Connect session write failed", it)
        }
    }

    private fun requiredPermissions(match: MatchEntity): Set<String> {
        return buildSet {
            add(HealthPermission.getWritePermission(ExerciseSessionRecord::class))
            if (match.caloriesKcal != null) {
                add(HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class))
            }
        }
    }

    private suspend fun deleteExistingRecords(client: HealthConnectClient, match: MatchEntity) {
        val sessionClientRecordId = sessionClientRecordId(match)
        val caloriesClientRecordId = caloriesClientRecordId(match)
        runCatching {
            client.deleteRecords(
                ExerciseSessionRecord::class,
                recordIdsList = emptyList(),
                clientRecordIdsList = listOf(sessionClientRecordId)
            )
            client.deleteRecords(
                TotalCaloriesBurnedRecord::class,
                recordIdsList = emptyList(),
                clientRecordIdsList = listOf(caloriesClientRecordId)
            )
        }.onSuccess {
            Log.i(TAG, "Health Connect previous records cleared idempotencyKey=${match.idempotencyKey}")
        }.onFailure {
            Log.w(TAG, "Health Connect previous record cleanup failed", it)
        }
    }

    private fun buildRecords(
        match: MatchEntity,
        start: Instant,
        end: Instant,
        zoneOffset: ZoneOffset
    ): List<Record> {
        return buildList {
            add(
                ExerciseSessionRecord(
                    startTime = start,
                    startZoneOffset = zoneOffset,
                    endTime = end,
                    endZoneOffset = zoneOffset,
                    metadata = Metadata.activelyRecorded(sourceDevice, sessionClientRecordId(match)),
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_TENNIS,
                    title = buildTitle(match),
                    notes = buildNotes(match)
                )
            )

            match.caloriesKcal?.let { calories ->
                add(
                    TotalCaloriesBurnedRecord(
                        startTime = start,
                        startZoneOffset = zoneOffset,
                        endTime = end,
                        endZoneOffset = zoneOffset,
                        energy = Energy.kilocalories(calories),
                        metadata = Metadata.activelyRecorded(sourceDevice, caloriesClientRecordId(match))
                    )
                )
            }
        }
    }

    private fun sessionClientRecordId(match: MatchEntity): String = "playce-${match.idempotencyKey}"

    private fun caloriesClientRecordId(match: MatchEntity): String = "playce-calories-${match.idempotencyKey}"

    private fun buildTitle(match: MatchEntity): String {
        val playerA = match.playerAName?.trim().orEmpty()
        val playerB = match.playerBName?.trim().orEmpty()
        return if (playerA.isNotBlank() || playerB.isNotBlank()) {
            "PLAYCE: ${playerA.ifBlank { "Player A" }} vs ${playerB.ifBlank { "Player B" }}"
        } else {
            "PLAYCE tennis match"
        }
    }

    private fun buildNotes(match: MatchEntity): String {
        return buildString {
            append("Result ${match.finalScoreText}")
            match.setScoresText?.takeIf { it.isNotBlank() }?.let { setScores ->
                append(" | Sets $setScores")
            }
            append(" | Duration ${formatDuration(match.durationSeconds)}")
        }
    }

    private fun formatDuration(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    private companion object {
        const val TAG = "HealthConnectWriter"
    }
}
