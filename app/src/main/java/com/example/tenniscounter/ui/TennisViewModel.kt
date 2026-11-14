package com.example.tenniscounter.ui

import android.app.Application
import android.app.BackgroundServiceStartNotAllowedException
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tenniscounter.health.HealthMetricsSnapshot
import com.example.tenniscounter.timer.MatchTimerService
import com.playce.shared.scoring.MatchEventCodec
import com.playce.shared.scoring.MatchFormat
import com.playce.shared.scoring.ScoringEngine
import com.playce.shared.scoring.ScoringEngine.MatchScore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Re-export shared types with aliases for backward compat with UI code
typealias SetScore = ScoringEngine.SetScore
typealias PlayerScore = ScoringEngine.PlayerScore

data class MatchState(
    val score: MatchScore = MatchScore(),
    val elapsedSeconds: Int = 0,
    val isRunning: Boolean = false,
    val hasTimerStarted: Boolean = false,
    val playerAName: String = "Player A",
    val playerBName: String = "Player B"
) {
    // Convenience accessors for backward compatibility with UI
    val playerA: PlayerScore get() = score.playerA
    val playerB: PlayerScore get() = score.playerB
    val completedSets: List<SetScore> get() = score.completedSets
    val initialServerIsPlayerA: Boolean get() = score.initialServerIsPlayerA
    val isTiebreak: Boolean get() = score.isTiebreak
    val isMatchOver: Boolean get() = score.isMatchOver

    fun pointLabelForA(): String = score.pointLabelForA()
    fun pointLabelForB(): String = score.pointLabelForB()
    fun currentServerIsPlayerA(): Boolean = score.currentServerIsPlayerA()
    fun serveStartsOnLeftSide(): Boolean = score.serveStartsOnLeftSide()
}

data class FinishedMatchSummary(
    val createdAt: Long,
    val durationSeconds: Int,
    val setsScore: String,
    val setsDetail: String,
    val playerAName: String = "Player A",
    val playerBName: String = "Player B",
    val caloriesKcal: Double? = null,
    val avgHeartRateBpm: Int? = null,
    val maxHeartRateBpm: Int? = null
)

class TennisViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val dataStore = appContext.dataStore

    private val SAVED_MATCHES_KEY = stringSetPreferencesKey("saved_matches_v1")

    private val _matchState = MutableStateFlow(MatchState())
    val matchState: StateFlow<MatchState> = _matchState.asStateFlow()

    private val _finishedMatch = MutableStateFlow<FinishedMatchSummary?>(null)
    val finishedMatch: StateFlow<FinishedMatchSummary?> = _finishedMatch.asStateFlow()

    private val _isFinishedMatchSaved = MutableStateFlow(false)
    val isFinishedMatchSaved: StateFlow<Boolean> = _isFinishedMatchSaved.asStateFlow()

    private val _matchFormat = MutableStateFlow(MatchFormat.STANDARD)
    val matchFormat: StateFlow<MatchFormat> = _matchFormat.asStateFlow()

    private var timerJob: Job? = null
    private val eventHistory = mutableListOf<ScoringEngine.MatchEvent>()

    /** True once the initial server has been fixed (phone config or user choice) or the prompt was dismissed. */
    private val _initialServerResolved = MutableStateFlow(false)

    /** Show "Who serves first?" only on a fresh scoreboard when nothing has fixed the server yet. */
    val showInitialServerPrompt: StateFlow<Boolean> =
        combine(_matchState, _initialServerResolved) { state, resolved ->
            !resolved && state.isFreshScoreboard()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Set to true when the user answers "Continue normal set"; reset on any score change or new match. */
    private val _decidingTiebreakDeclined = MutableStateFlow(false)

    val offerDecidingTiebreak: StateFlow<Boolean> =
        combine(_matchState, _decidingTiebreakDeclined) { state, declined ->
            ScoringEngine.canOfferDecidingTiebreak(state.score) && !declined
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** True once a match with at least one scored event has been restored from [ActiveSessionStore] on launch. */
    private val _restoredActiveMatch = MutableStateFlow(false)
    val restoredActiveMatch: StateFlow<Boolean> = _restoredActiveMatch.asStateFlow()

    /** True while the scoreboard has any progress (points/games/sets), whether restored or scored this session. */
    val hasActiveMatch: StateFlow<Boolean> =
        _matchState.map { !it.isFreshScoreboard() }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            TimerStateStore.ensureInitialized(appContext, SystemClock.elapsedRealtime())
            val timerSnapshot = TimerStateStore.read(appContext)
            if (timerSnapshot.isRunning) {
                startMatchTimerService()
            }
            startTicker()
            restoreActiveSessionIfPresent()
        }
    }

    private suspend fun restoreActiveSessionIfPresent() {
        val snapshot = ActiveSessionStore.readActive(appContext) ?: return
        val restoredEvents = MatchEventCodec.fromJson(snapshot.eventsJson)
        if (restoredEvents.isEmpty()) return

        val format = matchFormatFromName(snapshot.matchFormat)
        val initialServerIsPlayerA = snapshot.initialServerIsPlayerA ?: true

        eventHistory.clear()
        eventHistory.addAll(restoredEvents)
        _matchFormat.value = format
        _initialServerResolved.value = true
        _decidingTiebreakDeclined.value = false

        val restoredScore = ScoringEngine.replayEvents(restoredEvents, format, initialServerIsPlayerA)
        _matchState.value = _matchState.value.copy(
            score = restoredScore,
            playerAName = snapshot.playerAName.ifBlank { _matchState.value.playerAName },
            playerBName = snapshot.playerBName.ifBlank { _matchState.value.playerBName }
        )
        _restoredActiveMatch.value = true
    }

    private fun persistActiveSessionSnapshot() {
        viewModelScope.launch {
            val state = _matchState.value
            ActiveSessionStore.saveMatchSnapshot(
                context = appContext,
                eventsJson = MatchEventCodec.toJson(eventHistory),
                playerAName = state.playerAName,
                playerBName = state.playerBName,
                matchFormat = matchFormatToName(_matchFormat.value),
                initialServerIsPlayerA = if (_initialServerResolved.value) state.score.initialServerIsPlayerA else null
            )
        }
    }

    fun setPlayerNames(nameA: String, nameB: String) {
        _matchState.value = _matchState.value.copy(
            playerAName = nameA.ifBlank { "Player A" },
            playerBName = nameB.ifBlank { "Player B" }
        )
        persistActiveSessionSnapshot()
    }

    fun setMatchFormat(format: MatchFormat) {
        _matchFormat.value = format
        // Apply to current score if match hasn't started
        val state = _matchState.value
        if (eventHistory.isEmpty()) {
            _matchState.value = state.copy(
                score = state.score.copy(format = format)
            )
        }
        persistActiveSessionSnapshot()
    }

    /** Fix who serves first. Only valid before the first point is played. */
    fun setInitialServer(isPlayerA: Boolean) {
        _initialServerResolved.value = true
        val state = _matchState.value
        if (eventHistory.isEmpty()) {
            _matchState.value = state.copy(
                score = state.score.copy(initialServerIsPlayerA = isPlayerA)
            )
        }
        persistActiveSessionSnapshot()
    }

    /** User ignored the "Who serves first?" prompt; keep default (Player A) and stop asking. */
    fun dismissInitialServerPrompt() {
        _initialServerResolved.value = true
    }

    /** User chose to play a normal set instead of a deciding tiebreak. */
    fun declineDecidingTiebreak() {
        _decidingTiebreakDeclined.value = true
    }

    /** Replace the upcoming set with a standalone tiebreak to [targetPoints] (7 or 10). */
    fun startDecidingTiebreak(targetPoints: Int) {
        val state = _matchState.value
        if (!ScoringEngine.canOfferDecidingTiebreak(state.score)) return
        eventHistory.add(ScoringEngine.MatchEvent.DecidingTiebreakStarted(targetPoints, timestampMillis = System.currentTimeMillis()))
        _matchState.value = state.copy(
            score = ScoringEngine.startDecidingTiebreak(state.score, targetPoints)
        )
        persistActiveSessionSnapshot()
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
        val now = SystemClock.elapsedRealtime()
        val timerSnapshot = TimerStateStore.read(appContext)

        _matchState.value = _matchState.value.copy(
            elapsedSeconds = timerSnapshot.elapsedSeconds(now),
            isRunning = timerSnapshot.isRunning
        )
    }

    fun resetTimer() {
        viewModelScope.launch {
            TimerStateStore.start(appContext, SystemClock.elapsedRealtime())
            startMatchTimerService()
            updateTimerValue()
        }
    }

    fun tickClock() {
        // Kept for compatibility.
    }

    fun onCounterScreenVisible() {
        viewModelScope.launch {
            val now = SystemClock.elapsedRealtime()
            val state = _matchState.value
            val timerSnapshot = TimerStateStore.read(appContext)
            val elapsed = timerSnapshot.elapsedSeconds(now)
            val isFresh = state.isFreshScoreboard()

            // Only reset if the scoreboard is fresh AND the user never tapped Start
            // (i.e. leftover time from a previous app session, not a manual pause)
            if (isFresh && !state.hasTimerStarted && !timerSnapshot.isRunning && elapsed > 0) {
                Log.i(TIMER_TAG, "reset timer on fresh scoreboard after task removed")
                TimerStateStore.resetStopped(appContext)
                _matchState.value = state.copy(elapsedSeconds = 0, isRunning = false, hasTimerStarted = false)
            }
            // Timer no longer auto-starts — user must tap Start manually
        }
    }

    fun startTimer() {
        viewModelScope.launch {
            val now = SystemClock.elapsedRealtime()
            val state = _matchState.value
            if (state.hasTimerStarted) {
                // Resume from where we left off
                TimerStateStore.resume(appContext, now)
            } else {
                // Fresh start
                TimerStateStore.start(appContext, now)
            }
            startMatchTimerService()
            _matchState.value = state.copy(isRunning = true, hasTimerStarted = true)
            updateTimerValue()
        }
    }

    fun pauseTimer() {
        viewModelScope.launch {
            val now = SystemClock.elapsedRealtime()
            TimerStateStore.consolidateAndStop(appContext, now)
            _matchState.value = _matchState.value.copy(isRunning = false)
            updateTimerValue()
        }
    }

    fun addPointToPlayerA() {
        eventHistory.add(ScoringEngine.MatchEvent.Point(isPlayerA = true, timestampMillis = System.currentTimeMillis()))
        applyPoint(isPlayerA = true)
        persistActiveSessionSnapshot()
    }

    fun addPointToPlayerB() {
        eventHistory.add(ScoringEngine.MatchEvent.Point(isPlayerA = false, timestampMillis = System.currentTimeMillis()))
        applyPoint(isPlayerA = false)
        persistActiveSessionSnapshot()
    }

    fun undoLastPointForPlayerA(): Boolean = undoLastPointForPlayer(true)

    fun undoLastPointForPlayerB(): Boolean = undoLastPointForPlayer(false)

    fun resetGame() {
        eventHistory.clear()
        _decidingTiebreakDeclined.value = false
        val state = _matchState.value
        _matchState.value = state.copy(
            score = state.score.copy(
                playerA = state.playerA.copy(points = 0),
                playerB = state.playerB.copy(points = 0),
                isTiebreak = false
            )
        )
    }

    fun resetMatch() {
        eventHistory.clear()
        _initialServerResolved.value = false
        _decidingTiebreakDeclined.value = false
        viewModelScope.launch {
            TimerStateStore.resetStopped(appContext)
            ActiveSessionStore.clear(appContext)
            _matchState.value = MatchState(
                score = MatchScore(format = _matchFormat.value)
            )
            updateTimerValue()
        }
    }

    fun finishMatch(healthMetrics: HealthMetricsSnapshot = HealthMetricsSnapshot()) {
        _finishedMatch.value = buildFinishedSummary(_matchState.value, healthMetrics)
        _isFinishedMatchSaved.value = false
    }

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
            ActiveSessionStore.clear(appContext)
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

    /** JSON-encoded point/event history for the match currently in progress (or just finished). */
    fun pointEventsJson(): String = MatchEventCodec.toJson(eventHistory)

    private fun buildFinishedSummary(
        state: MatchState,
        healthMetrics: HealthMetricsSnapshot
    ): FinishedMatchSummary {
        val hasSets = state.playerA.sets > 0 || state.playerB.sets > 0
        // If no complete sets, show games as the headline score
        val setsScore = if (hasSets) {
            "${state.playerA.sets}-${state.playerB.sets}"
        } else {
            "${state.playerA.games}-${state.playerB.games}"
        }
        val completed = state.completedSets.joinToString(" ") { "${it.a}-${it.b}" }
        val liveSegment = "G ${state.playerA.games}-${state.playerB.games} P ${state.pointLabelForA()}-${state.pointLabelForB()}"
        val detail = when {
            completed.isBlank() && !hasSets -> "Games: ${state.playerA.games}-${state.playerB.games}"
            completed.isBlank() -> liveSegment
            // Match decided on the last completed set: the residual 0-0 game adds nothing.
            state.isMatchOver -> completed
            else -> "$completed | $liveSegment"
        }

        return FinishedMatchSummary(
            createdAt = System.currentTimeMillis(),
            durationSeconds = state.elapsedSeconds,
            setsScore = setsScore,
            setsDetail = detail,
            playerAName = state.playerAName,
            playerBName = state.playerBName,
            caloriesKcal = healthMetrics.caloriesKcal,
            avgHeartRateBpm = healthMetrics.avgHeartRateBpm,
            maxHeartRateBpm = healthMetrics.maxHeartRateBpm
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

    private fun applyPoint(isPlayerA: Boolean) {
        val state = _matchState.value
        val newScore = ScoringEngine.scorePoint(state.score, isPlayerA)
        _decidingTiebreakDeclined.value = false
        _matchState.value = state.copy(score = newScore)
    }

    private fun undoLastPointForPlayer(isPlayerA: Boolean): Boolean {
        val index = eventHistory.indexOfLast { event ->
            event is ScoringEngine.MatchEvent.Point && event.isPlayerA == isPlayerA
        }
        if (index < 0) return false

        eventHistory.removeAt(index)
        rebuildScoreFromHistory()
        persistActiveSessionSnapshot()
        return true
    }

    private fun rebuildScoreFromHistory() {
        val newScore = ScoringEngine.replayEvents(
            eventHistory,
            _matchFormat.value,
            _matchState.value.score.initialServerIsPlayerA
        )
        _decidingTiebreakDeclined.value = false
        _matchState.value = _matchState.value.copy(score = newScore)
    }

    /**
     * [MatchFormat] is a plain data class (not a real Kotlin enum), but the
     * common formats used across the app are the three named companion
     * constants. Encode those as their names for a compact, human-readable
     * snapshot; encode anything else (e.g. a custom config pushed from the
     * phone) as its raw fields so a restore never silently drops a custom
     * format. Falls back to STANDARD on any parse failure.
     */
    private fun matchFormatToName(format: MatchFormat): String = when (format) {
        MatchFormat.STANDARD -> "STANDARD"
        MatchFormat.GRAND_SLAM -> "GRAND_SLAM"
        MatchFormat.FAST4 -> "FAST4"
        else -> "CUSTOM:${format.setsToWin},${format.tiebreakAtSixAll}," +
            "${format.tiebreakPoints},${format.superTiebreakInFinalSet},${format.noAdScoring}"
    }

    private fun matchFormatFromName(name: String): MatchFormat = when {
        name == "STANDARD" -> MatchFormat.STANDARD
        name == "GRAND_SLAM" -> MatchFormat.GRAND_SLAM
        name == "FAST4" -> MatchFormat.FAST4
        name.startsWith("CUSTOM:") -> parseCustomMatchFormat(name.removePrefix("CUSTOM:"))
        else -> MatchFormat.STANDARD
    }

    private fun parseCustomMatchFormat(payload: String): MatchFormat {
        return try {
            val parts = payload.split(",")
            MatchFormat(
                setsToWin = parts[0].toInt(),
                tiebreakAtSixAll = parts[1].toBoolean(),
                tiebreakPoints = parts[2].toInt(),
                superTiebreakInFinalSet = parts[3].toBoolean(),
                noAdScoring = parts[4].toBoolean()
            )
        } catch (e: Exception) {
            MatchFormat.STANDARD
        }
    }

    private fun formatDuration(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    private fun startMatchTimerService() {
        val serviceIntent = Intent(appContext, MatchTimerService::class.java)
        try {
            appContext.startService(serviceIntent)
        } catch (e: BackgroundServiceStartNotAllowedException) {
            Log.w(TIMER_TAG, "MatchTimerService start blocked by system", e)
        } catch (e: IllegalStateException) {
            Log.w(TIMER_TAG, "MatchTimerService start failed", e)
        }
    }

    private companion object {
        const val TIMER_TAG = "MatchTimer"
    }
}

private fun MatchState.isFreshScoreboard(): Boolean {
    return playerA.points == 0 &&
        playerA.games == 0 &&
        playerA.sets == 0 &&
        playerB.points == 0 &&
        playerB.games == 0 &&
        playerB.sets == 0 &&
        completedSets.isEmpty()
}
