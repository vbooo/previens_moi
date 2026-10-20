package fr.previensmoi.feature.watches.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.*
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WatchesUiState(
    val watches: List<Watch> = emptyList(), val findings: List<Finding> = emptyList(),
    val refreshing: Boolean = false, val refreshFailed: Boolean = false, val offline: Boolean = false,
    val commandPending: Boolean = false, val commandError: Failure? = null,
    val deletedId: String? = null,
)

@HiltViewModel
class WatchesViewModel @Inject constructor(
    private val repository: WatchRepository, private val findings: FindingRepository, private val ids: IdGenerator,
) : ViewModel() {
    private val mutableState = MutableStateFlow(WatchesUiState())
    val state = mutableState.asStateFlow()
    private var pending: Command? = null
    init {
        viewModelScope.launch { repository.observeList().collect { watches -> mutableState.update { it.copy(watches = watches) } } }
        viewModelScope.launch { findings.observeFindings().collect { results -> mutableState.update { it.copy(findings = results) } } }
    }
    fun refresh() {
        if (state.value.refreshing) return
        mutableState.update { it.copy(refreshing = true, refreshFailed = false, offline = false) }
        viewModelScope.launch {
            try {
                val result = repository.refresh()
                mutableState.update { it.copy(refreshFailed = result != RefreshResult.Success, offline = result == RefreshResult.Offline) }
            } finally { mutableState.update { it.copy(refreshing = false) } }
        }
    }
    fun command(watch: Watch, operation: Operation) {
        if (state.value.commandPending) return
        val previous = pending
        val command = if (previous != null && previous.operation == operation && previous.targetId == watch.id) previous
            else Command(ids.next(), operation, watch.id, watch.version)
        pending = command
        mutableState.update { it.copy(commandPending = true, commandError = null) }
        viewModelScope.launch {
            try {
                when (val result = repository.execute(command)) {
                    is Outcome.Success -> {
                        pending = null
                        mutableState.update { it.copy(deletedId = if (result.value.deleted) result.value.watchId else null) }
                    }
                    is Outcome.Error -> {
                        if (result.reason !in setOf(Failure.Offline, Failure.TemporaryFailure)) pending = null
                        mutableState.update { it.copy(commandError = result.reason) }
                        if (result.reason == Failure.VersionConflict) refresh()
                    }
                }
            } finally { mutableState.update { it.copy(commandPending = false) } }
        }
    }
    fun consumeDeletion() { mutableState.update { it.copy(deletedId = null) } }
    fun clearCommandError() { mutableState.update { it.copy(commandError = null) } }
}
