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

data class MobileFinishedSummary(
    val createdAt: Long,
    val durationSeconds: Int,
    val setsScore: String,
    val setsDetail: String,
    val playerAName: String,
    val playerBName: String
)

data class MobileCounterState(
    val score: MatchScore = MatchScore(),
    val elapsedSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val hasTimerStarted: Boolean = false,
    val playerAName: String = "Player A",
    val playerBName: String = "Player B",
    val finishedSummary: MobileFinishedSummary? = null,
    val declinedDecidingTiebreak: Boolean = false
) {
    // Convenience accessors for backward compatibility with UI
    val playerA: CounterPlayerScore get() = score.playerA
    val playerB: CounterPlayerScore get() = score.playerB
    val completedSets: List<CounterSetScore> get() = score.completedSets
    val isTiebreak: Boolean get() = score.isTiebreak
    val isMatchOver: Boolean get() = score.isMatchOver

    /** True right after tied sets, offering the player a chance to decide the match by tiebreak. */
    val offerDecidingTiebreak: Boolean
        get() = ScoringEngine.canOfferDecidingTiebreak(score) && !declinedDecidingTiebreak

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

    private val matchEvents = mutableListOf<ScoringEngine.MatchEvent>()
    private var initialServerIsPlayerA = true

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
        if (matchEvents.isEmpty()) {
            _state.value = _state.value.copy(
                score = _state.value.score.copy(format = format)
            )
        }
    }

    fun setInitialServerIsPlayerA(value: Boolean) {
        initialServerIsPlayerA = value
        if (matchEvents.isEmpty()) {
            _state.value = _state.value.copy(
                score = _state.value.score.copy(initialServerIsPlayerA = value)
            )
        }
    }

    fun addPointToPlayerA() {
        matchEvents.add(ScoringEngine.MatchEvent.Point(true))
        applyPoint(isPlayerA = true)
    }

    fun addPointToPlayerB() {
        matchEvents.add(ScoringEngine.MatchEvent.Point(false))
        applyPoint(isPlayerA = false)
    }

    /** User declined the deciding-tiebreak offer; play a normal set instead. */
    fun declineDecidingTiebreak() {
        _state.value = _state.value.copy(declinedDecidingTiebreak = true)
    }

    /** User chose to decide the match by a standalone tiebreak (7 or 10 points). */
    fun startDecidingTiebreak(targetPoints: Int) {
        val current = _state.value
        if (!ScoringEngine.canOfferDecidingTiebreak(current.score)) return
        matchEvents.add(ScoringEngine.MatchEvent.DecidingTiebreakStarted(targetPoints))
        val newScore = ScoringEngine.startDecidingTiebreak(current.score, targetPoints)
        _state.value = current.copy(score = newScore, declinedDecidingTiebreak = false)
    }

    fun undoLastPointForPlayerA(): Boolean = undoLastPointForPlayer(true)

    fun undoLastPointForPlayerB(): Boolean = undoLastPointForPlayer(false)

    fun resetGame() {
        matchEvents.clear()
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
        matchEvents.clear()
        accumulatedSeconds = 0
        timerStartElapsedRealtime = SystemClock.elapsedRealtime()
        _state.value = MobileCounterState(
            score = MatchScore(format = _matchFormat.value, initialServerIsPlayerA = initialServerIsPlayerA)
        )
    }

    fun finishMatch() {
        val current = _state.value
        // Stop timer
        if (current.isTimerRunning) {
            accumulatedSeconds = current.elapsedSeconds
        }

        val hasSets = current.playerA.sets > 0 || current.playerB.sets > 0
        val setsScore = if (hasSets) {
            "${current.playerA.sets}-${current.playerB.sets}"
        } else {
            "${current.playerA.games}-${current.playerB.games}"
        }
        val completed = current.completedSets.joinToString(" ") { "${it.a}-${it.b}" }
        val detail = when {
            completed.isBlank() && !hasSets -> "Games: ${current.playerA.games}-${current.playerB.games}"
            completed.isBlank() -> "G ${current.playerA.games}-${current.playerB.games}"
            else -> "$completed | G ${current.playerA.games}-${current.playerB.games}"
        }

        val summary = MobileFinishedSummary(
            createdAt = System.currentTimeMillis(),
            durationSeconds = current.elapsedSeconds,
            setsScore = setsScore,
            setsDetail = detail,
            playerAName = current.playerAName,
            playerBName = current.playerBName
        )
        _state.value = current.copy(
            isTimerRunning = false,
            finishedSummary = summary
        )
    }

    fun startNewMatch() {
        matchEvents.clear()
        accumulatedSeconds = 0
        timerStartElapsedRealtime = SystemClock.elapsedRealtime()
        _state.value = MobileCounterState(
            score = MatchScore(format = _matchFormat.value, initialServerIsPlayerA = initialServerIsPlayerA)
        )
    }

    fun toggleTimer() {
        val current = _state.value
        if (current.isTimerRunning) {
            accumulatedSeconds = current.elapsedSeconds
            _state.value = current.copy(isTimerRunning = false)
        } else {
            timerStartElapsedRealtime = SystemClock.elapsedRealtime()
            _state.value = current.copy(isTimerRunning = true, hasTimerStarted = true)
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
        _state.value = current.copy(score = newScore, declinedDecidingTiebreak = false)
    }

    private fun undoLastPointForPlayer(isPlayerA: Boolean): Boolean {
        val index = matchEvents.indexOfLast { it is ScoringEngine.MatchEvent.Point && it.isPlayerA == isPlayerA }
        if (index < 0) return false
        matchEvents.removeAt(index)
        rebuildScoreFromHistory()
        return true
    }

    private fun rebuildScoreFromHistory() {
        val newScore = ScoringEngine.replayEvents(matchEvents, _matchFormat.value, initialServerIsPlayerA)
        _state.value = _state.value.copy(score = newScore, declinedDecidingTiebreak = false)
    }
}
