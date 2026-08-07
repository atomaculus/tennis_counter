package com.example.tenniscounter.mobile.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tenniscounter.mobile.data.local.MatchDao
import com.example.tenniscounter.mobile.data.local.MatchEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MatchStats(
    val totalMatches: Int = 0,
    val totalPlayTimeSeconds: Long = 0,
    val avgDurationSeconds: Long = 0,
    val longestMatchSeconds: Long = 0,
    val shortestMatchSeconds: Long = 0,
    val playerAWins: Int = 0,
    val playerBWins: Int = 0,
    val isLoading: Boolean = true
)

private enum class MatchWinner { PLAYER_A, PLAYER_B }

class StatsViewModel(matchDao: MatchDao) : ViewModel() {
    // Derived from the Room flow so stats stay current while the screen is
    // open (deletes from Detail, matches arriving from the watch, etc.).
    val stats: StateFlow<MatchStats> = matchDao.getAllMatches()
        .map { matches -> buildStats(matches) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MatchStats())

    companion object {
        fun factory(matchDao: MatchDao) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return StatsViewModel(matchDao) as T
            }
        }
    }

    private fun buildStats(matches: List<MatchEntity>): MatchStats {
        if (matches.isEmpty()) return MatchStats(isLoading = false)

        val durations = matches.map { it.durationSeconds }
        val winners = matches.map { winnerSide(it.finalScoreText) }
        return MatchStats(
            totalMatches = matches.size,
            totalPlayTimeSeconds = durations.sum(),
            avgDurationSeconds = durations.average().toLong(),
            longestMatchSeconds = durations.max(),
            shortestMatchSeconds = durations.min(),
            playerAWins = winners.count { it == MatchWinner.PLAYER_A },
            playerBWins = winners.count { it == MatchWinner.PLAYER_B },
            isLoading = false
        )
    }

    /**
     * Parses a "X-Y" final score (sets won, or games won when the match had no full sets)
     * into a winner side. Matches with an unparseable or tied score are excluded ("unknown"),
     * mirroring the iOS MatchStats.winnerSide calculation.
     */
    private fun winnerSide(finalScoreText: String): MatchWinner? {
        val parts = finalScoreText.split("-").mapNotNull { it.trim().toIntOrNull() }
        if (parts.size != 2 || parts[0] == parts[1]) return null
        return if (parts[0] > parts[1]) MatchWinner.PLAYER_A else MatchWinner.PLAYER_B
    }
}
