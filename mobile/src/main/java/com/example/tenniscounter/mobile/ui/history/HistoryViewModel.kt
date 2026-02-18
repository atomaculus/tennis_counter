package com.example.tenniscounter.mobile.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tenniscounter.mobile.data.MatchRepository
import com.example.tenniscounter.mobile.data.local.MatchEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: MatchRepository
) : ViewModel() {

    val matches: StateFlow<List<MatchEntity>> = repository.getAllMatches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createDefaultMatch(onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.insert(
                MatchEntity(
                    createdAt = System.currentTimeMillis(),
                    durationSeconds = 0L,
                    finalScoreText = "0-0",
                    photoUri = null
                )
            )
            onCreated(id)
        }
    }

    companion object {
        fun factory(repository: MatchRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
                        return HistoryViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
