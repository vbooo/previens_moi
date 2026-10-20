package fr.previensmoi.feature.watches.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.previensmoi.domain.model.RefreshResult
import fr.previensmoi.domain.model.Watch
import fr.previensmoi.domain.repository.WatchRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WatchesUiState(
    val watches: List<Watch> = emptyList(),
    val refreshing: Boolean = false,
    val refreshFailed: Boolean = false,
)

@HiltViewModel
class WatchesViewModel @Inject constructor(private val repository: WatchRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(WatchesUiState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeList().collect { watches -> mutableState.update { it.copy(watches = watches) } }
        }
    }

    fun refresh() {
        if (mutableState.value.refreshing) return
        mutableState.update { it.copy(refreshing = true, refreshFailed = false) }
        viewModelScope.launch {
            try {
                val failed = repository.refresh() == RefreshResult.TemporaryFailure
                mutableState.update { it.copy(refreshFailed = failed) }
            } finally {
                // Cancellation propagates; it must not become a false server error.
                mutableState.update { it.copy(refreshing = false) }
            }
        }
    }
}
