package com.example.tenniscounter.ui

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "timer_prefs")

private val POINT_LABELS = listOf("0", "15", "30", "40", "AD")

data class PlayerScore(
    val points: Int = 0,
    val games: Int = 0,
    val sets: Int = 0
)

data class MatchState(
    val playerA: PlayerScore = PlayerScore(),
    val playerB: PlayerScore = PlayerScore(),
    val elapsedSeconds: Int = 0,
    val isRunning: Boolean = true
) {
    fun pointLabelForA(): String = toPointLabel(playerA.points, playerB.points)
    fun pointLabelForB(): String = toPointLabel(playerB.points, playerA.points)

    private fun toPointLabel(playerPoints: Int, rivalPoints: Int): String {
        if (playerPoints >= 3 && rivalPoints >= 3) {
            return when {
                playerPoints == rivalPoints -> "40"
                playerPoints == rivalPoints + 1 -> "AD"
                else -> "40"
            }
        }
        return POINT_LABELS.getOrElse(playerPoints.coerceIn(0, 4)) { "0" }
    }
}

class TennisViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = application.dataStore

    private val START_TIME_KEY = longPreferencesKey("start_time")
    private val PAUSED_ACCUMULATED_KEY = longPreferencesKey("paused_accumulated")
    private val IS_RUNNING_KEY = booleanPreferencesKey("is_running")
    private val LAST_PAUSE_TIME_KEY = longPreferencesKey("last_pause_time")

    private val _matchState = MutableStateFlow(MatchState())
    val matchState: StateFlow<MatchState> = _matchState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            // Initialize start time if not present
            val prefs = dataStore.data.first()
            if (prefs[START_TIME_KEY] == null) {
                dataStore.edit { it[START_TIME_KEY] = SystemClock.elapsedRealtime() }
            }
            startTicker()
        }
    }

    private fun startTicker() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                updateTimerValue()
                delay(1000)
            }
        }
    }

    private suspend fun updateTimerValue() {
        val prefs = dataStore.data.first()
        val startTime = prefs[START_TIME_KEY] ?: SystemClock.elapsedRealtime()
        val isRunning = prefs[IS_RUNNING_KEY] ?: true
        val pausedAccumulated = prefs[PAUSED_ACCUMULATED_KEY] ?: 0L
        val lastPauseTime = prefs[LAST_PAUSE_TIME_KEY] ?: 0L

        val now = SystemClock.elapsedRealtime()
        val totalElapsedMillis = if (isRunning) {
            (now - startTime) - pausedAccumulated
        } else {
            (lastPauseTime - startTime) - pausedAccumulated
        }

        _matchState.value = _matchState.value.copy(
            elapsedSeconds = (totalElapsedMillis / 1000).coerceAtLeast(0).toInt(),
            isRunning = isRunning
        )
    }

    fun toggleTimer() {
        viewModelScope.launch {
            val now = SystemClock.elapsedRealtime()
            dataStore.edit { p ->
                val currentlyRunning = p[IS_RUNNING_KEY] ?: true
                if (currentlyRunning) {
                    p[IS_RUNNING_KEY] = false
                    p[LAST_PAUSE_TIME_KEY] = now
                } else {
                    val lastPauseTime = p[LAST_PAUSE_TIME_KEY] ?: now
                    val currentPausedAcc = p[PAUSED_ACCUMULATED_KEY] ?: 0L
                    p[PAUSED_ACCUMULATED_KEY] = currentPausedAcc + (now - lastPauseTime)
                    p[IS_RUNNING_KEY] = true
                }
            }
            updateTimerValue()
        }
    }

    fun resetTimer() {
        viewModelScope.launch {
            dataStore.edit { p ->
                p[START_TIME_KEY] = SystemClock.elapsedRealtime()
                p[PAUSED_ACCUMULATED_KEY] = 0L
                p[IS_RUNNING_KEY] = true
            }
            updateTimerValue()
        }
    }

    // Tick clock remains for compatibility if needed, but now it's internal
    fun tickClock() {
        // Now handled by internal ticker
    }

    // Score Logic
    fun addPointToPlayerA() = updatePoint(isPlayerA = true, delta = 1)
    fun addPointToPlayerB() = updatePoint(isPlayerA = false, delta = 1)
    fun removePointFromPlayerA() = updatePoint(isPlayerA = true, delta = -1)
    fun removePointFromPlayerB() = updatePoint(isPlayerA = false, delta = -1)

    private fun updatePoint(isPlayerA: Boolean, delta: Int) {
        val state = _matchState.value
        val currentA = state.playerA
        val currentB = state.playerB

        if (delta < 0) {
            val updatedA = if (isPlayerA) currentA.copy(points = (currentA.points - 1).coerceAtLeast(0)) else currentA
            val updatedB = if (!isPlayerA) currentB.copy(points = (currentB.points - 1).coerceAtLeast(0)) else currentB
            _matchState.value = state.copy(playerA = updatedA, playerB = updatedB)
            return
        }

        val (newA, newB) = if (isPlayerA) {
            resolvePointWon(currentA, currentB)
        } else {
            val (b, a) = resolvePointWon(currentB, currentA)
            a to b
        }
        _matchState.value = state.copy(playerA = newA, playerB = newB)
    }

    private fun resolvePointWon(winner: PlayerScore, loser: PlayerScore): Pair<PlayerScore, PlayerScore> {
        val winnerPoints = winner.points + 1
        val loserPoints = loser.points

        val winnerTakesGame = winnerPoints >= 4 && winnerPoints - loserPoints >= 2

        if (!winnerTakesGame) {
            return winner.copy(points = winnerPoints) to loser
        }

        val winnerGames = winner.games + 1
        val loserGames = loser.games
        val winnerTakesSet = winnerGames >= 6 && winnerGames - loserGames >= 2

        return if (winnerTakesSet) {
            winner.copy(points = 0, games = 0, sets = winner.sets + 1) to
                loser.copy(points = 0, games = 0)
        } else {
            winner.copy(points = 0, games = winnerGames) to
                loser.copy(points = 0)
        }
    }
}
