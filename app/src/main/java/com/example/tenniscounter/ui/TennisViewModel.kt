package com.example.tenniscounter.ui

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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

data class SetScore(
    val a: Int,
    val b: Int
)

data class PlayerScore(
    val points: Int = 0,
    val games: Int = 0,
    val sets: Int = 0
)

data class MatchState(
    val playerA: PlayerScore = PlayerScore(),
    val playerB: PlayerScore = PlayerScore(),
    val completedSets: List<SetScore> = emptyList(),
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

data class FinishedMatchSummary(
    val createdAt: Long,
    val durationSeconds: Int,
    val setsScore: String,
    val setsDetail: String
)

private data class BaselineScore(
    val playerA: PlayerScore,
    val playerB: PlayerScore,
    val completedSets: List<SetScore>
)

private data class ResolveResult(
    val winner: PlayerScore,
    val loser: PlayerScore,
    val completedSet: Pair<Int, Int>? = null
)

class TennisViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = application.dataStore

    private val START_TIME_KEY = longPreferencesKey("start_time")
    private val PAUSED_ACCUMULATED_KEY = longPreferencesKey("paused_accumulated")
    private val IS_RUNNING_KEY = booleanPreferencesKey("is_running")
    private val LAST_PAUSE_TIME_KEY = longPreferencesKey("last_pause_time")
    private val SAVED_MATCHES_KEY = stringSetPreferencesKey("saved_matches_v1")

    private val _matchState = MutableStateFlow(MatchState())
    val matchState: StateFlow<MatchState> = _matchState.asStateFlow()

    private val _finishedMatch = MutableStateFlow<FinishedMatchSummary?>(null)
    val finishedMatch: StateFlow<FinishedMatchSummary?> = _finishedMatch.asStateFlow()

    private val _isFinishedMatchSaved = MutableStateFlow(false)
    val isFinishedMatchSaved: StateFlow<Boolean> = _isFinishedMatchSaved.asStateFlow()

    private var timerJob: Job? = null
    private var scoreBaseline = BaselineScore(PlayerScore(), PlayerScore(), emptyList())
    private val pointHistory = mutableListOf<Boolean>()

    init {
        viewModelScope.launch {
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

    fun resetTimer() {
        viewModelScope.launch {
            dataStore.edit { p ->
                p[START_TIME_KEY] = SystemClock.elapsedRealtime()
                p[PAUSED_ACCUMULATED_KEY] = 0L
                p[IS_RUNNING_KEY] = true
                p[LAST_PAUSE_TIME_KEY] = 0L
            }
            updateTimerValue()
        }
    }

    fun tickClock() {
        // Kept for compatibility.
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
        val state = _matchState.value
        scoreBaseline = BaselineScore(
            playerA = state.playerA.copy(points = 0),
            playerB = state.playerB.copy(points = 0),
            completedSets = state.completedSets
        )
        _matchState.value = state.copy(
            playerA = state.playerA.copy(points = 0),
            playerB = state.playerB.copy(points = 0)
        )
    }

    fun resetMatch() {
        pointHistory.clear()
        scoreBaseline = BaselineScore(PlayerScore(), PlayerScore(), emptyList())
        viewModelScope.launch {
            dataStore.edit { p ->
                p[START_TIME_KEY] = SystemClock.elapsedRealtime()
                p[PAUSED_ACCUMULATED_KEY] = 0L
                p[IS_RUNNING_KEY] = true
                p[LAST_PAUSE_TIME_KEY] = 0L
            }
            _matchState.value = MatchState()
            updateTimerValue()
        }
    }

    // Called from UI after end-match confirmation; this is the manual navigation trigger source.
    fun finishMatch() {
        _finishedMatch.value = buildFinishedSummary(_matchState.value)
        _isFinishedMatchSaved.value = false
    }

    // Persists a minimal final-match record in DataStore. Duplicate saves are blocked by state + createdAt key.
    fun saveFinishedMatch(): Boolean {
        val summary = _finishedMatch.value ?: return false
        if (_isFinishedMatchSaved.value) return false

        _isFinishedMatchSaved.value = true
        viewModelScope.launch {
            dataStore.edit { prefs ->
                val current = prefs[SAVED_MATCHES_KEY] ?: emptySet()
                val keyPrefix = "${summary.createdAt}|"
                if (current.any { it.startsWith(keyPrefix) }) {
                    return@edit
                }
                prefs[SAVED_MATCHES_KEY] = current + encodeSavedMatch(summary)
            }
        }
        return true
    }

    fun buildShareStubText(): String {
        val summary = _finishedMatch.value ?: return "Match summary unavailable"
        return "Match finished: ${summary.setsScore} | ${summary.setsDetail} | ${formatDuration(summary.durationSeconds)}"
    }

    fun startNewMatch() {
        _finishedMatch.value = null
        _isFinishedMatchSaved.value = false
        resetMatch()
    }

    private fun buildFinishedSummary(state: MatchState): FinishedMatchSummary {
        val setsScore = "${state.playerA.sets}-${state.playerB.sets}"
        val completed = state.completedSets.joinToString(" ") { "${it.a}-${it.b}" }
        val liveSegment = "G ${state.playerA.games}-${state.playerB.games} P ${state.pointLabelForA()}-${state.pointLabelForB()}"
        val detail = if (completed.isBlank()) liveSegment else "$completed | $liveSegment"

        return FinishedMatchSummary(
            createdAt = System.currentTimeMillis(),
            durationSeconds = state.elapsedSeconds,
            setsScore = setsScore,
            setsDetail = detail
        )
    }

    private fun encodeSavedMatch(summary: FinishedMatchSummary): String {
        return listOf(
            summary.createdAt.toString(),
            summary.durationSeconds.toString(),
            summary.setsScore.replace("|", "/"),
            summary.setsDetail.replace("|", "/")
        ).joinToString("|")
    }

    private fun applyPointWon(isPlayerA: Boolean) {
        val state = _matchState.value
        val currentA = state.playerA
        val currentB = state.playerB
        val sets = state.completedSets.toMutableList()

        val (newA, newB) = if (isPlayerA) {
            val resolved = resolvePointWon(currentA, currentB)
            resolved.completedSet?.let { sets.add(SetScore(it.first, it.second)) }
            resolved.winner to resolved.loser
        } else {
            val resolved = resolvePointWon(currentB, currentA)
            resolved.completedSet?.let { sets.add(SetScore(it.second, it.first)) }
            resolved.loser to resolved.winner
        }

        _matchState.value = state.copy(playerA = newA, playerB = newB, completedSets = sets)
    }

    private fun undoLastPointForPlayer(isPlayerA: Boolean): Boolean {
        val index = pointHistory.indexOfLast { winnerIsA -> winnerIsA == isPlayerA }
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
                resolved.completedSet?.let { sets.add(SetScore(it.first, it.second)) }
                scoreA = resolved.winner
                scoreB = resolved.loser
            } else {
                val resolved = resolvePointWon(scoreB, scoreA)
                resolved.completedSet?.let { sets.add(SetScore(it.second, it.first)) }
                scoreA = resolved.loser
                scoreB = resolved.winner
            }
        }

        _matchState.value = _matchState.value.copy(
            playerA = scoreA,
            playerB = scoreB,
            completedSets = sets
        )
    }

    private fun resolvePointWon(winner: PlayerScore, loser: PlayerScore): ResolveResult {
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

    private fun formatDuration(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}
