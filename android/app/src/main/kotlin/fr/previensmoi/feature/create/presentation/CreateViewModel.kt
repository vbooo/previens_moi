package fr.previensmoi.feature.create.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CreationStep { Editing, Evaluating, Clarifying, Ready, Unsupported, Login, Confirming }
data class CreateUiState(
    val draft: RequestDraft = RequestDraft(), val loaded: Boolean = false,
    val step: CreationStep = CreationStep.Editing, val proposal: Proposal? = null,
    val error: Failure? = null, val resetNotice: Boolean = false, val storageFailed: Boolean = false,
    val partialAccepted: Boolean = false, val session: Session? = null,
    val editingId: String? = null, val activatedId: String? = null,
)

@HiltViewModel
class CreateViewModel @Inject constructor(
    private val proposals: ProposalRepository, private val watches: WatchRepository,
    private val sessions: SessionRepository, private val drafts: DraftRepository,
    private val ids: IdGenerator, private val saved: SavedStateHandle,
) : ViewModel() {
    private val mutable = MutableStateFlow(CreateUiState())
    val state = mutable.asStateFlow()
    private var evaluationJob: Job? = null
    private var evaluationEpoch = 0L
    private var saveJob: Job? = null
    private var expectedWatchVersion: Long = saved["editVersion"] ?: 0
    private var pending: Command? = null

    init {
        viewModelScope.launch { sessions.observeSession().collect { session -> mutable.update { it.copy(session = session) } } }
        viewModelScope.launch {
            when (val loaded = drafts.load()) {
                is Outcome.Success -> mutable.update { it.copy(draft = loaded.value) }
                is Outcome.Error -> mutable.update { it.copy(storageFailed = true) }
            }
            val editId = saved.get<String>("editId")
            mutable.update { it.copy(editingId = editId) }
            val id = saved.get<String>("proposalId")
            if (id != null) {
                when (val result = proposals.get(id)) {
                    is Outcome.Success -> mutable.update { it.copy(proposal = result.value, step = CreationStep.Ready) }
                    is Outcome.Error -> {
                        mutable.update { it.copy(resetNotice = result.reason == Failure.NotFound, error = if (result.reason == Failure.NotFound) null else result.reason, editingId = null) }
                        clearSavedProposal(); saved["editId"] = null
                    }
                }
            } else if (editId != null && watches.observeList().first().none { it.id == editId }) {
                mutable.update { it.copy(resetNotice = true, editingId = null) }; saved["editId"] = null
            }
            mutable.update { it.copy(loaded = true) }
        }
    }

    fun updateText(text: String) = updateDraft(state.value.draft.copy(text = text), keepClarification = false)
    fun updateAnswer(answer: String) = updateDraft(state.value.draft.copy(answer = answer), keepClarification = true)
    fun updateMode(mode: FollowMode) = updateDraft(state.value.draft.copy(mode = mode), keepClarification = false)
    private fun updateDraft(draft: RequestDraft, keepClarification: Boolean) {
        if (!state.value.loaded || state.value.step == CreationStep.Confirming || draft == state.value.draft) return
        evaluationEpoch++; evaluationJob?.cancel(); pending = null; clearSavedProposal()
        val next = draft.copy(revision = state.value.draft.revision + 1)
        mutable.update { it.copy(draft = next, proposal = null, partialAccepted = false, error = null,
            step = if (keepClarification && it.step == CreationStep.Clarifying) CreationStep.Clarifying else CreationStep.Editing) }
        persist(next)
    }
    private fun persist(draft: RequestDraft) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            val failed = drafts.save(draft) is Outcome.Error
            mutable.update { it.copy(storageFailed = failed) }
        }
    }
    fun startNew() {
        if (state.value.step == CreationStep.Confirming) return
        saved["editId"] = null; saved["editVersion"] = null
        mutable.update { it.copy(editingId = null, activatedId = null) }
    }
    fun edit(watch: Watch) {
        if (state.value.step == CreationStep.Confirming) return
        evaluationEpoch++; evaluationJob?.cancel(); pending = null; clearSavedProposal()
        saved["editId"] = watch.id; saved["editVersion"] = watch.version; expectedWatchVersion = watch.version
        val draft = watch.plan.draft.copy(revision = state.value.draft.revision + 1)
        mutable.update { it.copy(draft = draft, editingId = watch.id, proposal = null, step = CreationStep.Editing,
            partialAccepted = false, error = null, activatedId = null) }
        persist(draft)
    }
    fun acceptPartial(value: Boolean) { mutable.update { it.copy(partialAccepted = value) } }
    fun evaluate(language: String) {
        if (!state.value.loaded || state.value.step == CreationStep.Evaluating || state.value.step == CreationStep.Confirming) return
        if (state.value.draft.text.isBlank()) { mutable.update { it.copy(error = Failure.InvalidInput) }; return }
        val draft = state.value.draft.copy(language = language)
        mutable.update { it.copy(draft = draft, step = CreationStep.Evaluating, proposal = null, error = null, partialAccepted = false) }
        persist(draft); clearSavedProposal(); pending = null
        val epoch = ++evaluationEpoch
        evaluationJob = viewModelScope.launch {
            val outcome = proposals.evaluate(draft)
            if (evaluationEpoch != epoch || state.value.draft.revision != draft.revision) return@launch
            when (outcome) {
                is Outcome.Error -> mutable.update { it.copy(step = CreationStep.Editing, error = outcome.reason) }
                is Outcome.Success -> when (val result = outcome.value) {
                    is Evaluation.Ready -> {
                        saved["proposalId"] = result.proposal.id
                        mutable.update { it.copy(proposal = result.proposal, step = CreationStep.Ready) }
                    }
                    Evaluation.Clarify -> mutable.update { it.copy(step = CreationStep.Clarifying) }
                    Evaluation.Unsupported -> mutable.update { it.copy(step = CreationStep.Unsupported) }
                }
            }
        }
    }
    fun revise() {
        if (state.value.step == CreationStep.Confirming) return
        evaluationEpoch++; evaluationJob?.cancel(); pending = null; clearSavedProposal()
        mutable.update { it.copy(step = CreationStep.Editing, proposal = null, error = null, partialAccepted = false) }
    }
    fun signIn() {
        if (state.value.step != CreationStep.Login) return
        mutable.update { it.copy(step = CreationStep.Confirming, error = null) }
        viewModelScope.launch {
            when (val result = sessions.signIn()) {
                is Outcome.Error -> mutable.update { it.copy(step = CreationStep.Login, error = result.reason) }
                is Outcome.Success -> mutable.update { it.copy(step = CreationStep.Ready, session = result.value) }
            }
        }
    }
    fun confirm() {
        if (state.value.step != CreationStep.Ready) return
        val proposal = state.value.proposal ?: return
        if (proposal.partial && !state.value.partialAccepted) return
        if (state.value.session == null) { mutable.update { it.copy(step = CreationStep.Login, error = null) }; return }
        mutable.update { it.copy(step = CreationStep.Confirming, error = null) }
        viewModelScope.launch {
            // A retry of an uncertain command must reach the receipt lookup, even after expiry.
            if (pending == null) {
                when (val current = proposals.get(proposal.id)) {
                    is Outcome.Error -> { mutable.update { it.copy(step = CreationStep.Ready, error = current.reason) }; return@launch }
                    is Outcome.Success -> if (current.value != proposal) {
                        mutable.update { it.copy(step = CreationStep.Ready, proposal = current.value, partialAccepted = false, error = Failure.VersionConflict) }
                        return@launch
                    }
                }
                val editing = state.value.editingId
                pending = if (editing == null) Command(ids.next(), Operation.Activate, proposal.id, proposal.version, acceptPartial = state.value.partialAccepted)
                    else Command(ids.next(), Operation.Replace, editing, expectedWatchVersion, proposal.id, proposal.version, state.value.partialAccepted)
            }
            when (val result = watches.execute(checkNotNull(pending))) {
                is Outcome.Success -> {
                    saveJob?.cancelAndJoin()
                    val storageFailed = drafts.clear() is Outcome.Error
                    pending = null; clearSavedProposal(); saved["editId"] = null; saved["editVersion"] = null
                    mutable.update { it.copy(draft = RequestDraft(), proposal = null, step = CreationStep.Editing,
                        editingId = null, activatedId = result.value.watchId, storageFailed = storageFailed) }
                }
                is Outcome.Error -> {
                    if (result.reason !in setOf(Failure.Offline, Failure.TemporaryFailure, Failure.OperationPending)) pending = null
                    if (result.reason == Failure.VersionConflict && state.value.editingId != null) {
                        val watch = watches.observeList().first().find { it.id == state.value.editingId }
                        if (watch != null) { expectedWatchVersion = watch.version; saved["editVersion"] = watch.version }
                    }
                    mutable.update { it.copy(step = CreationStep.Ready, error = result.reason) }
                }
            }
        }
    }
    fun resumeAfterSignIn() { if (state.value.session != null && state.value.step == CreationStep.Login) mutable.update { it.copy(step = CreationStep.Ready, error = null) } }
    fun consumeActivation() { mutable.update { it.copy(activatedId = null) } }
    fun discard() {
        if (state.value.step == CreationStep.Confirming) return
        evaluationEpoch++; evaluationJob?.cancel(); pending = null; clearSavedProposal(); saved["editId"] = null; saved["editVersion"] = null
        mutable.update { it.copy(draft = RequestDraft(), step = CreationStep.Editing, proposal = null, editingId = null, error = null, partialAccepted = false) }
        val previousSave = saveJob
        previousSave?.cancel()
        saveJob = viewModelScope.launch { previousSave?.join(); val failed = drafts.clear() is Outcome.Error; mutable.update { it.copy(storageFailed = failed) } }
    }
    private fun clearSavedProposal() { saved["proposalId"] = null }
}
