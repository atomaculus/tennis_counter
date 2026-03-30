package com.example.tenniscounter.sync

import com.playce.shared.scoring.MatchFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the latest match configuration received from the phone.
 * The watch UI observes [pendingConfig] and applies it to the ViewModel
 * when the user starts a new match.
 */
data class MatchConfig(
    val playerAName: String,
    val playerBName: String,
    val format: MatchFormat,
    val timestamp: Long
)

object MatchConfigRepository {

    private val _pendingConfig = MutableStateFlow<MatchConfig?>(null)
    val pendingConfig: StateFlow<MatchConfig?> = _pendingConfig.asStateFlow()

    fun update(config: MatchConfig) {
        _pendingConfig.value = config
    }

    /** Called after the watch applies the config, so it doesn't re-apply. */
    fun consume() {
        _pendingConfig.value = null
    }
}
