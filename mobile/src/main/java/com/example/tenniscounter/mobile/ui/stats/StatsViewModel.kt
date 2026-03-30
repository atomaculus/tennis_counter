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
    val isLoading: Boolean = true
)

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

            _stats.value = MatchStats(
                totalMatches = count,
                totalPlayTimeSeconds = matchDao.getTotalPlayTime() ?: 0,
                avgDurationSeconds = matchDao.getAverageDuration()?.toLong() ?: 0,
                longestMatchSeconds = matchDao.getLongestMatchDuration() ?: 0,
                shortestMatchSeconds = matchDao.getShortestMatchDuration() ?: 0,
                isLoading = false
            )
        }
    }
}
