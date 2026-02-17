package com.example.tenniscounter.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val POINT_LABELS = listOf("0", "15", "30", "40", "AD")

data class PlayerScore(
    val points: Int = 0,
    val games: Int = 0,
    val sets: Int = 0
)

data class MatchState(
    val playerA: PlayerScore = PlayerScore(),
    val playerB: PlayerScore = PlayerScore(),
    val elapsedSeconds: Int = 0
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

class TennisViewModel : ViewModel() {
    private val _matchState = MutableStateFlow(MatchState())
    val matchState: StateFlow<MatchState> = _matchState.asStateFlow()

    fun addPointToPlayerA() = updatePoint(isPlayerA = true, delta = 1)
    fun addPointToPlayerB() = updatePoint(isPlayerA = false, delta = 1)
    fun removePointFromPlayerA() = updatePoint(isPlayerA = true, delta = -1)
    fun removePointFromPlayerB() = updatePoint(isPlayerA = false, delta = -1)

    fun tickClock() {
        _matchState.value = _matchState.value.copy(
            elapsedSeconds = _matchState.value.elapsedSeconds + 1
        )
    }

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

        val winnerTakesGame =
            winnerPoints >= 4 && winnerPoints - loserPoints >= 2

        if (!winnerTakesGame) {
            return winner.copy(points = winnerPoints) to loser
        }

        val winnerGames = winner.games + 1
        val loserGames = loser.games

        val winnerTakesSet =
            winnerGames >= 6 && winnerGames - loserGames >= 2

        return if (winnerTakesSet) {
            winner.copy(points = 0, games = 0, sets = winner.sets + 1) to
                loser.copy(points = 0, games = 0)
        } else {
            winner.copy(points = 0, games = winnerGames) to
                loser.copy(points = 0)
        }
    }
}
