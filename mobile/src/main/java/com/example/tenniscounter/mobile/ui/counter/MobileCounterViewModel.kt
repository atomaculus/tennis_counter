package com.example.tenniscounter.mobile.ui.counter

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val POINT_LABELS = listOf("0", "15", "30", "40", "AD")

data class CounterSetScore(
    val a: Int,
    val b: Int
)

data class CounterPlayerScore(
    val points: Int = 0,
    val games: Int = 0,
    val sets: Int = 0
)

data class MobileCounterState(
    val playerA: CounterPlayerScore = CounterPlayerScore(),
    val playerB: CounterPlayerScore = CounterPlayerScore(),
    val completedSets: List<CounterSetScore> = emptyList(),
    val elapsedSeconds: Int = 0,
    val isTimerRunning: Boolean = true
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

private data class BaselineScore(
    val playerA: CounterPlayerScore,
    val playerB: CounterPlayerScore,
    val completedSets: List<CounterSetScore>
)

private data class ResolveResult(
    val winner: CounterPlayerScore,
    val loser: CounterPlayerScore,
    val completedSet: Pair<Int, Int>? = null
)

class MobileCounterViewModel : ViewModel() {
    private val _state = MutableStateFlow(MobileCounterState())
    val state: StateFlow<MobileCounterState> = _state.asStateFlow()

    private var tickerJob: Job? = null
    private var timerStartElapsedRealtime: Long = SystemClock.elapsedRealtime()
    private var accumulatedSeconds = 0

    private var scoreBaseline = BaselineScore(CounterPlayerScore(), CounterPlayerScore(), emptyList())
    private val pointHistory = mutableListOf<Boolean>()

    init {
        startTicker()
    }

    fun addPointToPlayerA() {
        pointHistory.add(true)
        applyPointWon(isPlayerA = true)
    }

    fun addPointToPlayerB() {
        pointHistory.add(false)
        applyPointWon(isPlayerA = false)
    }

    fun undoLastPointForPlayerA(): Boolean = undoLastPointForPlayer(true)

    fun undoLastPointForPlayerB(): Boolean = undoLastPointForPlayer(false)

    fun resetGame() {
        pointHistory.clear()
        val current = _state.value
        scoreBaseline = BaselineScore(
            playerA = current.playerA.copy(points = 0),
            playerB = current.playerB.copy(points = 0),
            completedSets = current.completedSets
        )
        _state.value = current.copy(
            playerA = current.playerA.copy(points = 0),
            playerB = current.playerB.copy(points = 0)
        )
    }

    fun resetMatch() {
        pointHistory.clear()
        scoreBaseline = BaselineScore(CounterPlayerScore(), CounterPlayerScore(), emptyList())
        accumulatedSeconds = 0
        timerStartElapsedRealtime = SystemClock.elapsedRealtime()
        _state.value = MobileCounterState()
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

    private fun applyPointWon(isPlayerA: Boolean) {
        val current = _state.value
        val currentA = current.playerA
        val currentB = current.playerB
        val sets = current.completedSets.toMutableList()

        val (newA, newB) = if (isPlayerA) {
            val resolved = resolvePointWon(currentA, currentB)
            resolved.completedSet?.let { sets.add(CounterSetScore(it.first, it.second)) }
            resolved.winner to resolved.loser
        } else {
            val resolved = resolvePointWon(currentB, currentA)
            resolved.completedSet?.let { sets.add(CounterSetScore(it.second, it.first)) }
            resolved.loser to resolved.winner
        }

        _state.value = current.copy(playerA = newA, playerB = newB, completedSets = sets)
    }

    private fun undoLastPointForPlayer(isPlayerA: Boolean): Boolean {
        val index = pointHistory.indexOfLast { it == isPlayerA }
        if (index < 0) return false
        pointHistory.removeAt(index)
        rebuildScoreFromHistory()
        return true
    }

    private fun rebuildScoreFromHistory() {
        var scoreA = scoreBaseline.playerA
        var scoreB = scoreBaseline.playerB
        val sets = scoreBaseline.completedSets.toMutableList()

        pointHistory.forEach { winnerIsA ->
            if (winnerIsA) {
                val resolved = resolvePointWon(scoreA, scoreB)
                resolved.completedSet?.let { sets.add(CounterSetScore(it.first, it.second)) }
                scoreA = resolved.winner
                scoreB = resolved.loser
            } else {
                val resolved = resolvePointWon(scoreB, scoreA)
                resolved.completedSet?.let { sets.add(CounterSetScore(it.second, it.first)) }
                scoreA = resolved.loser
                scoreB = resolved.winner
            }
        }

        _state.value = _state.value.copy(playerA = scoreA, playerB = scoreB, completedSets = sets)
    }

    private fun resolvePointWon(winner: CounterPlayerScore, loser: CounterPlayerScore): ResolveResult {
        val winnerPoints = winner.points + 1
        val loserPoints = loser.points
        val winnerTakesGame = winnerPoints >= 4 && winnerPoints - loserPoints >= 2

        if (!winnerTakesGame) {
            return ResolveResult(
                winner = winner.copy(points = winnerPoints),
                loser = loser
            )
        }

        val winnerGames = winner.games + 1
        val loserGames = loser.games
        val winnerTakesSet = winnerGames >= 6 && winnerGames - loserGames >= 2

        return if (winnerTakesSet) {
            ResolveResult(
                winner = winner.copy(points = 0, games = 0, sets = winner.sets + 1),
                loser = loser.copy(points = 0, games = 0),
                completedSet = winnerGames to loserGames
            )
        } else {
            ResolveResult(
                winner = winner.copy(points = 0, games = winnerGames),
                loser = loser.copy(points = 0)
            )
        }
    }
}
