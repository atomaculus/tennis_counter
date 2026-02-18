package com.example.tenniscounter.mobile.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tenniscounter.mobile.data.MatchRepository
import com.example.tenniscounter.mobile.data.local.MatchEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MatchDetailViewModel(
    private val matchId: Long,
    private val repository: MatchRepository
) : ViewModel() {

    val match: StateFlow<MatchEntity?> = repository.observeMatchById(matchId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun updatePhotoUri(photoUri: String?) {
        val current = match.value ?: return
        viewModelScope.launch {
            repository.update(current.copy(photoUri = photoUri))
        }
    }

    companion object {
        fun factory(matchId: Long, repository: MatchRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MatchDetailViewModel::class.java)) {
                        return MatchDetailViewModel(matchId, repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
