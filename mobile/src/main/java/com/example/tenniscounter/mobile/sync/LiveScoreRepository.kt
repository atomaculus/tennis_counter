package com.example.tenniscounter.mobile.sync

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the latest live match state received from the scorer watch.
 * null means no active live match is being broadcast.
 */
data class LiveMatchState(
    val playerAPoints: Int,
    val playerAGames: Int,
    val playerASets: Int,
    val playerBPoints: Int,
    val playerBGames: Int,
    val playerBSets: Int,
    val completedSets: String,      // "6-4,3-6" comma-separated
    val pointLabelA: String,
    val pointLabelB: String,
    val elapsedSeconds: Int,
    val isMatchActive: Boolean,
    val lastScoredPlayer: String,   // "A", "B", or ""
    val scorerNodeId: String,
    val timestamp: Long
)

object LiveScoreRepository {
    private val _state = MutableStateFlow<LiveMatchState?>(null)
    val state: StateFlow<LiveMatchState?> = _state.asStateFlow()

    /**
     * Anchor timestamp (SystemClock.elapsedRealtime()) captured at the moment the currently
     * held [state] value was stored. The UI uses this to extrapolate elapsed match time
     * locally between payload updates, rather than relying on the watch to transmit ticks.
     * Only meaningful while [state] is non-null.
     */
    var receivedAtElapsedRealtime: Long = SystemClock.elapsedRealtime()
        private set

    fun update(newState: LiveMatchState) {
        if (newState.isMatchActive) {
            receivedAtElapsedRealtime = SystemClock.elapsedRealtime()
            _state.value = newState
        } else {
            _state.value = null
        }
    }

    fun clear() {
        _state.value = null
    }
}
