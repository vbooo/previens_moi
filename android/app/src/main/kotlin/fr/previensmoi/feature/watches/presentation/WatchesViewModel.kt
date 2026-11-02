package fr.previensmoi.feature.watches.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.*
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WatchesUiState(
    val watches: List<Watch> = emptyList(),
    val findings: List<Finding> = emptyList(),
    val refreshing: Boolean = false,
    val refreshFailed: Boolean = false,
    val offline: Boolean = false,
    val commandPending: Boolean = false,
    val commandError: Failure? = null,
    val commandWatchId: String? = null,
    val commandOperation: Operation? = null,
    val commandUnconfirmed: Boolean = false,
    val deletedId: String? = null,
    val findingError: Failure? = null,
    val findingRequestId: String? = null,
)

@HiltViewModel
class WatchesViewModel
@Inject
constructor(
    private val repository: WatchRepository,
    private val findings: FindingRepository,
    private val ids: IdGenerator,
) : ViewModel() {
    private val mutableState = MutableStateFlow(WatchesUiState())
    val state = mutableState.asStateFlow()
    private var pending: Command? = null
    private var findingEpoch = 0L
    private val refreshMutex = Mutex()

    init {
        viewModelScope.launch {
            repository.observeList().collect { watches ->
                mutableState.update { it.copy(watches = watches) }
            }
        }
        viewModelScope.launch {
            findings.observeFindings().collect { results ->
                mutableState.update { it.copy(findings = results) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshFromService(showLoading = true) }
    }

    /** Called only inside the signed-in screen's STARTED lifecycle. No provider work is triggered. */
    suspend fun synchronizeWhileVisible() {
        while (currentCoroutineContext().isActive) {
            val result = refreshFromService(showLoading = false)
            val preparing = state.value.commandUnconfirmed || state.value.watches.any {
                it.status == WatchStatus.Pending ||
                    it.remoteState in setOf(RemoteState.Starting, RemoteState.Stopping)
            }
            // A slow/offline read backs off. Settled watches still receive later checks/results.
            withTimeoutOrNull(if (result != RefreshResult.Success || !preparing) 30_000 else 3_000) {
                // A newly submitted preparation wakes an otherwise idle reader immediately.
                state.first { !preparing && (it.commandUnconfirmed || it.watches.any { watch ->
                    watch.status == WatchStatus.Pending ||
                        watch.remoteState in setOf(RemoteState.Starting, RemoteState.Stopping)
                }) }
            }
        }
    }

    private suspend fun refreshFromService(showLoading: Boolean): RefreshResult? {
        // Manual and lifecycle refreshes share one reader; keep existing content visible.
        if (!refreshMutex.tryLock()) return null
        if (showLoading) mutableState.update { it.copy(refreshing = true) }
        return try {
            val result = try {
                repository.refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                RefreshResult.TemporaryFailure
            }
            mutableState.update {
                it.copy(
                    refreshFailed = result != RefreshResult.Success,
                    offline = result == RefreshResult.Offline,
                )
            }
            if (result == RefreshResult.Success && state.value.commandUnconfirmed && !state.value.commandPending) {
                retryCommand()
            }
            result
        } finally {
            mutableState.update { it.copy(refreshing = false) }
            refreshMutex.unlock()
        }
    }

    fun command(watch: Watch, operation: Operation) {
        if (state.value.commandPending || state.value.commandUnconfirmed) return
        val previous = pending
        val command =
            if (
                previous != null && previous.operation == operation && previous.targetId == watch.id
            )
                previous
            else Command(ids.next(), operation, watch.id, watch.version)
        pending = command
        submit(command)
    }

    /** Reconcile the original identifier. A retry never creates a second mutation. */
    fun retryCommand() {
        if (state.value.commandPending) return
        pending?.let(::submit)
    }

    private fun submit(command: Command) {
        mutableState.update {
            it.copy(
                commandPending = true,
                commandError = null,
                commandWatchId = command.targetId,
                commandOperation = command.operation,
                commandUnconfirmed = true,
            )
        }
        viewModelScope.launch {
            try {
                var result = repository.execute(command)
                var polls = 0
                // A server-accepted operation can finish asynchronously. Poll only that receipt,
                // with a bounded wait; uncertain network failures require explicit verification.
                while (result == Outcome.Error(Failure.OperationPending) && polls < 30) {
                    delay(2_000)
                    polls++
                    result = repository.execute(command)
                }
                when (val settled = result) {
                    is Outcome.Success -> {
                        val refreshed = repository.refresh()
                        val confirmed = refreshed == RefreshResult.Success
                        if (confirmed) pending = null
                        mutableState.update {
                            it.copy(
                                deletedId =
                                    if (settled.value.deleted) settled.value.watchId else null,
                                commandError =
                                    if (confirmed) null
                                    else if (refreshed == RefreshResult.Offline) Failure.Offline
                                    else Failure.TemporaryFailure,
                                commandWatchId = if (confirmed) null else command.targetId,
                                commandOperation = if (confirmed) null else command.operation,
                                commandUnconfirmed = !confirmed,
                                refreshFailed = !confirmed,
                                offline = refreshed == RefreshResult.Offline,
                            )
                        }
                    }
                    is Outcome.Error -> {
                        val uncertain =
                            settled.reason in
                                setOf(
                                    Failure.Offline,
                                    Failure.TemporaryFailure,
                                    Failure.OperationPending,
                                )
                        if (!uncertain) pending = null
                        mutableState.update {
                            it.copy(
                                commandError = settled.reason,
                                commandUnconfirmed = uncertain,
                                commandWatchId = if (uncertain) command.targetId else null,
                                commandOperation = if (uncertain) command.operation else null,
                            )
                        }
                        if (!uncertain) refresh()
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(commandError = Failure.TemporaryFailure, commandUnconfirmed = true)
                }
            } finally {
                mutableState.update { it.copy(commandPending = false) }
            }
        }
    }

    fun loadFinding(id: String) {
        val epoch = ++findingEpoch
        mutableState.update { it.copy(findingError = null, findingRequestId = id) }
        viewModelScope.launch {
            val result = findings.getFinding(id)
            if (epoch != findingEpoch) return@launch
            when (result) {
                is Outcome.Success ->
                    mutableState.update {
                        it.copy(
                            findings = it.findings.filterNot { old -> old.id == id } + result.value,
                        )
                    }
                is Outcome.Error ->
                    mutableState.update {
                        it.copy(
                            findingError = result.reason,
                            findings = it.findings.filterNot { old -> old.id == id },
                        )
                    }
            }
        }
    }

    fun consumeDeletion() {
        mutableState.update { it.copy(deletedId = null) }
    }

    fun clearCommandError() {
        mutableState.update { it.copy(commandError = null) }
    }
}
