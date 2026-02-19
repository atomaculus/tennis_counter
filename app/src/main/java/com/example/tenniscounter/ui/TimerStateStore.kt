package com.example.tenniscounter.ui

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlin.math.max
import kotlinx.coroutines.flow.first

val Context.dataStore by preferencesDataStore(name = "timer_prefs")

data class TimerSnapshot(
    val isRunning: Boolean,
    val startElapsedRealtime: Long,
    val accumulatedSeconds: Long
) {
    fun elapsedSeconds(nowElapsedRealtime: Long): Int {
        val runningDelta = if (isRunning) {
            max(0L, nowElapsedRealtime - startElapsedRealtime) / 1000L
        } else {
            0L
        }
        return (accumulatedSeconds + runningDelta).coerceAtLeast(0L).toInt()
    }
}

object TimerStateStore {
    private val START_ELAPSED_REALTIME_KEY = longPreferencesKey("start_elapsed_realtime")
    private val ACCUMULATED_SECONDS_KEY = longPreferencesKey("accumulated_seconds")
    private val IS_RUNNING_KEY = booleanPreferencesKey("is_running")

    // Legacy keys from previous timer model.
    private val LEGACY_START_TIME_KEY = longPreferencesKey("start_time")
    private val LEGACY_PAUSED_ACCUMULATED_KEY = longPreferencesKey("paused_accumulated")
    private val LEGACY_LAST_PAUSE_TIME_KEY = longPreferencesKey("last_pause_time")

    suspend fun ensureInitialized(context: Context, nowElapsedRealtime: Long) {
        context.dataStore.edit { prefs ->
            val hasNewModel = prefs[START_ELAPSED_REALTIME_KEY] != null &&
                prefs[ACCUMULATED_SECONDS_KEY] != null &&
                prefs[IS_RUNNING_KEY] != null
            if (hasNewModel) return@edit

            val legacyStart = prefs[LEGACY_START_TIME_KEY]
            val legacyPausedAccumulated = prefs[LEGACY_PAUSED_ACCUMULATED_KEY] ?: 0L
            val legacyLastPauseTime = prefs[LEGACY_LAST_PAUSE_TIME_KEY] ?: legacyStart ?: nowElapsedRealtime
            val legacyIsRunning = prefs[IS_RUNNING_KEY] ?: true

            val migratedAccumulatedSeconds = if (legacyStart != null) {
                val legacyElapsedMillis = if (legacyIsRunning) {
                    (nowElapsedRealtime - legacyStart) - legacyPausedAccumulated
                } else {
                    (legacyLastPauseTime - legacyStart) - legacyPausedAccumulated
                }
                max(0L, legacyElapsedMillis) / 1000L
            } else {
                0L
            }

            prefs[IS_RUNNING_KEY] = legacyIsRunning
            prefs[START_ELAPSED_REALTIME_KEY] = nowElapsedRealtime
            prefs[ACCUMULATED_SECONDS_KEY] = migratedAccumulatedSeconds

            prefs.remove(LEGACY_START_TIME_KEY)
            prefs.remove(LEGACY_PAUSED_ACCUMULATED_KEY)
            prefs.remove(LEGACY_LAST_PAUSE_TIME_KEY)
        }
    }

    suspend fun read(context: Context): TimerSnapshot {
        val prefs = context.dataStore.data.first()
        return TimerSnapshot(
            isRunning = prefs[IS_RUNNING_KEY] ?: true,
            startElapsedRealtime = prefs[START_ELAPSED_REALTIME_KEY] ?: 0L,
            accumulatedSeconds = prefs[ACCUMULATED_SECONDS_KEY] ?: 0L
        )
    }

    suspend fun start(context: Context, nowElapsedRealtime: Long) {
        context.dataStore.edit { prefs ->
            prefs[IS_RUNNING_KEY] = true
            prefs[START_ELAPSED_REALTIME_KEY] = nowElapsedRealtime
            prefs[ACCUMULATED_SECONDS_KEY] = 0L
        }
    }

    suspend fun resetStopped(context: Context) {
        context.dataStore.edit { prefs ->
            prefs[IS_RUNNING_KEY] = false
            prefs[START_ELAPSED_REALTIME_KEY] = 0L
            prefs[ACCUMULATED_SECONDS_KEY] = 0L
        }
    }

    suspend fun consolidateAndStop(context: Context, nowElapsedRealtime: Long) {
        context.dataStore.edit { prefs ->
            val isRunning = prefs[IS_RUNNING_KEY] ?: false
            val startElapsedRealtime = prefs[START_ELAPSED_REALTIME_KEY] ?: nowElapsedRealtime
            val accumulatedSeconds = prefs[ACCUMULATED_SECONDS_KEY] ?: 0L

            val consolidatedSeconds = if (isRunning) {
                accumulatedSeconds + (max(0L, nowElapsedRealtime - startElapsedRealtime) / 1000L)
            } else {
                accumulatedSeconds
            }

            prefs[ACCUMULATED_SECONDS_KEY] = consolidatedSeconds
            prefs[IS_RUNNING_KEY] = false
            prefs[START_ELAPSED_REALTIME_KEY] = nowElapsedRealtime
        }
    }
}
