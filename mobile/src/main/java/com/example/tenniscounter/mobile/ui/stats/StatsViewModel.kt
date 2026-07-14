package com.example.tenniscounter.mobile.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tenniscounter.mobile.data.local.MatchDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

class StatsViewModel(private val matchDao: MatchDao) : ViewModel() {
    private val _stats = MutableStateFlow(MatchStats())
    val stats: StateFlow<MatchStats> = _stats.asStateFlow()

    init {
        loadStats()
    }

    companion object {
        fun factory(matchDao: MatchDao) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return StatsViewModel(matchDao) as T
            }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            val count = matchDao.getMatchCount()
            if (count == 0) {
                _stats.value = MatchStats(isLoading = false)
                return@launch
            }

            val winners = matchDao.getAllMatchesOnce().map { winnerSide(it.finalScoreText) }

            _stats.value = MatchStats(
                totalMatches = count,
                totalPlayTimeSeconds = matchDao.getTotalPlayTime() ?: 0,
                avgDurationSeconds = matchDao.getAverageDuration()?.toLong() ?: 0,
                longestMatchSeconds = matchDao.getLongestMatchDuration() ?: 0,
                shortestMatchSeconds = matchDao.getShortestMatchDuration() ?: 0,
                playerAWins = winners.count { it == MatchWinner.PLAYER_A },
                playerBWins = winners.count { it == MatchWinner.PLAYER_B },
                isLoading = false
            )
        }
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
