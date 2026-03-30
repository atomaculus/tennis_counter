package com.example.tenniscounter.mobile.ui.counter

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playce.shared.scoring.MatchFormat
import com.playce.shared.scoring.ScoringEngine
import com.playce.shared.scoring.ScoringEngine.MatchScore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Re-export shared types for UI backward compat
typealias CounterSetScore = ScoringEngine.SetScore
typealias CounterPlayerScore = ScoringEngine.PlayerScore

data class MobileCounterState(
    val score: MatchScore = MatchScore(),
    val elapsedSeconds: Int = 0,
    val isTimerRunning: Boolean = true,
    val playerAName: String = "Player A",
    val playerBName: String = "Player B"
) {
    // Convenience accessors for backward compatibility with UI
    val playerA: CounterPlayerScore get() = score.playerA
    val playerB: CounterPlayerScore get() = score.playerB
    val completedSets: List<CounterSetScore> get() = score.completedSets
    val isTiebreak: Boolean get() = score.isTiebreak
    val isMatchOver: Boolean get() = score.isMatchOver

    fun pointLabelForA(): String = score.pointLabelForA()
    fun pointLabelForB(): String = score.pointLabelForB()
}

class MobileCounterViewModel : ViewModel() {
    private val _state = MutableStateFlow(MobileCounterState())
    val state: StateFlow<MobileCounterState> = _state.asStateFlow()

    private val _matchFormat = MutableStateFlow(MatchFormat.STANDARD)
    val matchFormat: StateFlow<MatchFormat> = _matchFormat.asStateFlow()

    private var tickerJob: Job? = null
    private var timerStartElapsedRealtime: Long = SystemClock.elapsedRealtime()
    private var accumulatedSeconds = 0

    private val pointHistory = mutableListOf<Boolean>()

    init {
        startTicker()
    }

    fun setPlayerNames(nameA: String, nameB: String) {
        _state.value = _state.value.copy(
            playerAName = nameA.ifBlank { "Player A" },
            playerBName = nameB.ifBlank { "Player B" }
        )
    }

    fun setMatchFormat(format: MatchFormat) {
        _matchFormat.value = format
        if (pointHistory.isEmpty()) {
            _state.value = _state.value.copy(
                score = _state.value.score.copy(format = format)
            )
        }
    }

    fun addPointToPlayerA() {
        pointHistory.add(true)
        applyPoint(isPlayerA = true)
    }

    fun addPointToPlayerB() {
        pointHistory.add(false)
        applyPoint(isPlayerA = false)
    }

    fun undoLastPointForPlayerA(): Boolean = undoLastPointForPlayer(true)

    fun undoLastPointForPlayerB(): Boolean = undoLastPointForPlayer(false)

    fun resetGame() {
        pointHistory.clear()
        val current = _state.value
        _state.value = current.copy(
            score = current.score.copy(
                playerA = current.playerA.copy(points = 0),
                playerB = current.playerB.copy(points = 0),
                isTiebreak = false
            )
        )
    }

    fun resetMatch() {
        pointHistory.clear()
        accumulatedSeconds = 0
        timerStartElapsedRealtime = SystemClock.elapsedRealtime()
        _state.value = MobileCounterState(
            score = MatchScore(format = _matchFormat.value)
        )
    }

    fun toggleTimer() {
        val current = _state.value
        if (current.isTimerRunning) {
            accumulatedSeconds = current.elapsedSeconds
            _state.value = current.copy(isTimerRunning = false)
        } else {
            timerStartElapsedRealtime = SystemClock.elapsedRealtime()
            _state.value = current.copy(isTimerRunning = true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                val current = _state.value
                if (current.isTimerRunning) {
                    val elapsed = accumulatedSeconds +
                        ((SystemClock.elapsedRealtime() - timerStartElapsedRealtime) / 1000L).toInt()
                    if (elapsed != current.elapsedSeconds) {
                        _state.value = current.copy(elapsedSeconds = elapsed)
                    }
                }
                delay(250)
            }
        }
    }

    private fun applyPoint(isPlayerA: Boolean) {
        val current = _state.value
        val newScore = ScoringEngine.scorePoint(current.score, isPlayerA)
        _state.value = current.copy(score = newScore)
    }

    private fun undoLastPointForPlayer(isPlayerA: Boolean): Boolean {
        val index = pointHistory.indexOfLast { it == isPlayerA }
        if (index < 0) return false
        pointHistory.removeAt(index)
        rebuildScoreFromHistory()
        return true
    }

    private fun rebuildScoreFromHistory() {
        val newScore = ScoringEngine.replay(pointHistory, _matchFormat.value)
        _state.value = _state.value.copy(score = newScore)
    }
}
