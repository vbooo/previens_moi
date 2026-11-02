package fr.previensmoi.feature.create.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.*
import javax.inject.Inject
import fr.previensmoi.diagnostics.Diagnostics
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CreationStep {
    Editing,
    Evaluating,
    Clarifying,
    Ready,
    Unsupported,
    Login,
    Confirming,
}

data class CreateUiState(
    val draft: RequestDraft = RequestDraft(),
    val loaded: Boolean = false,
    val step: CreationStep = CreationStep.Editing,
    val proposal: Proposal? = null,
    val error: Failure? = null,
    val resetNotice: Boolean = false,
    val storageFailed: Boolean = false,
    val partialAccepted: Boolean = false,
    val session: Session? = null,
    val questions: List<ClarificationQuestion> = emptyList(),
    val rejection: String? = null,
    val editingId: String? = null,
    val activatedId: String? = null,
    val invalidSite: Boolean = false,
    val optionsUpdating: Boolean = false,
)

@HiltViewModel
class CreateViewModel
@Inject
constructor(
    private val proposals: ProposalRepository,
    private val watches: WatchRepository,
    private val sessions: SessionRepository,
    private val drafts: DraftRepository,
    private val ids: IdGenerator,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val mutable = MutableStateFlow(CreateUiState())
    val state = mutable.asStateFlow()
    private var evaluationJob: Job? = null
    private var evaluationEpoch = 0L
    private var saveJob: Job? = null
    private var expectedWatchVersion: Long = saved["editVersion"] ?: 0
    private var pending: Command? = null
    private var sessionEpoch = 0L

    init {
        viewModelScope.launch {
            sessions.observeSession().collect { session ->
                val previous = state.value.session
                if (previous?.id != session?.id) {
                    sessionEpoch++
                    if (previous != null) {
                        resetEditing(state.value.draft)
                    }
                }
                mutable.update { it.copy(session = session) }
            }
        }
        val restoringEpoch = evaluationEpoch
        viewModelScope.launch {
            try {
                val loaded = drafts.load()
                if (evaluationEpoch != restoringEpoch) return@launch
                when (loaded) {
                    is Outcome.Success -> mutable.update { it.copy(draft = loaded.value) }
                    is Outcome.Error -> mutable.update { it.copy(storageFailed = true) }
                }
                val editId = saved.get<String>("editId")
                mutable.update { it.copy(editingId = editId) }
                val id = saved.get<String>("proposalId")
                if (id != null) {
                    val result = proposals.get(id)
                    if (evaluationEpoch != restoringEpoch) return@launch
                    when (result) {
                        is Outcome.Success -> mutable.update { it.copy(proposal = result.value, step = CreationStep.Ready, partialAccepted = result.value.backgroundPreparation) }
                        is Outcome.Error -> {
                            mutable.update { it.copy(resetNotice = result.reason == Failure.NotFound,
                                error = if (result.reason == Failure.NotFound) null else result.reason, editingId = null) }
                            clearSavedProposal(); saved["editId"] = null; saved["editVersion"] = null
                        }
                    }
                }
                // An empty local watch cache is not proof that a saved edit target was deleted.
                // The authenticated, versioned command validates that target when confirmed.
            } finally {
                mutable.update { it.copy(loaded = true) }
            }
        }
    }

    fun updateSite(value: String) =
        updateDraft(
            state.value.draft.copy(
                answers = state.value.draft.answers + ("preferred_site" to value),
            ),
            keepClarification = false,
        )

    fun updateRegion(value: String) =
        updateDraft(
            state.value.draft.copy(answers = state.value.draft.answers + ("region" to value)),
            keepClarification = false,
        )

    fun continueClarification(language: String) {
        if (
            state.value.questions.any {
                it.required && state.value.draft.answers[it.id].isNullOrBlank()
            }
        ) {
            mutable.update {
                it.copy(
                    questions =
                        it.questions.sortedBy { q -> !it.draft.answers[q.id].isNullOrBlank() },
                )
            }
            return
        }
        evaluate(language)
    }

    fun chooseMode(mode: FollowMode) {
        val before = state.value
        val proposal = before.proposal ?: return
        if (
            before.step != CreationStep.Ready ||
                before.optionsUpdating ||
                proposal.draft.mode == mode
        )
            return
        val epoch = ++evaluationEpoch
        mutable.update { it.copy(optionsUpdating = true, error = null) }
        viewModelScope.launch {
            val r = proposals.configure(proposal, mode)
            if (epoch != evaluationEpoch) return@launch
            when (r) {
                is Outcome.Success -> {
                    mutable.update {
                        it.copy(proposal = r.value, draft = r.value.draft, optionsUpdating = false)
                    }
                    persist(r.value.draft)
                }
                is Outcome.Error ->
                    mutable.update { it.copy(error = r.reason, optionsUpdating = false) }
            }
        }
    }

    fun updateText(text: String) =
        updateDraft(state.value.draft.copy(text = text), keepClarification = false)

    fun updateAnswer(answer: String) =
        updateDraft(state.value.draft.copy(answer = answer), keepClarification = true)

    fun updateAnswerFor(id: String, answer: String) =
        updateDraft(
            state.value.draft.copy(answers = state.value.draft.answers + (id to answer)),
            keepClarification = true,
        )

    fun updateMode(mode: FollowMode) =
        updateDraft(state.value.draft.copy(mode = mode), keepClarification = false)

    private fun updateDraft(draft: RequestDraft, keepClarification: Boolean) {
        if (
            !state.value.loaded ||
                state.value.step == CreationStep.Confirming ||
                draft == state.value.draft
        )
            return
        evaluationEpoch++
        evaluationJob?.cancel()
        pending = null
        clearSavedProposal()
        val next =
            draft.copy(
                revision = state.value.draft.revision + 1,
                answers =
                    if (keepClarification) draft.answers
                    else draft.answers.filterKeys { it in setOf("region", "preferred_site") },
            )
        mutable.update {
            it.copy(
                draft = next,
                proposal = null,
                partialAccepted = false,
                error = null,
                invalidSite = false,
                optionsUpdating = false,
                step =
                    if (keepClarification && it.step == CreationStep.Clarifying)
                        CreationStep.Clarifying
                    else CreationStep.Editing,
            )
        }
        persist(next)
    }

    private fun persist(draft: RequestDraft) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            val failed = drafts.save(draft) is Outcome.Error
            mutable.update { it.copy(storageFailed = failed) }
        }
    }

    private fun resetEditing(draft: RequestDraft, editingId: String? = null, version: Long = 0) {
        Diagnostics.record(Diagnostics.Event.DraftReset, Diagnostics.Result.Started)
        evaluationEpoch++
        evaluationJob?.cancel()
        pending = null
        clearSavedProposal()
        saved["editId"] = editingId
        saved["editVersion"] = if (editingId == null) null else version
        expectedWatchVersion = version
        mutable.update { it.copy(draft = draft, editingId = editingId, activatedId = null,
            proposal = null, step = CreationStep.Editing, partialAccepted = false, error = null,
            questions = emptyList(), rejection = null, invalidSite = false, optionsUpdating = false, resetNotice = false) }
    }

    fun startNew() {
        if (state.value.step == CreationStep.Confirming || pending != null) return
        val draft = RequestDraft(revision = state.value.draft.revision + 1, answers = mapOf("region" to "France"))
        resetEditing(draft)
        persist(draft)
    }

    fun edit(watch: Watch) {
        if (state.value.step == CreationStep.Confirming || pending != null) return
        val draft = watch.plan.draft.copy(
            revision = maxOf(state.value.draft.revision, watch.plan.draft.revision) + 1,
            answers = if (watch.plan.draft.region.isBlank()) watch.plan.draft.answers + ("region" to "France") else watch.plan.draft.answers,
        )
        resetEditing(draft, watch.id, watch.version)
        persist(draft)
    }

    fun acceptPartial(value: Boolean) {
        mutable.update { it.copy(partialAccepted = value) }
    }

    fun evaluate(language: String) {
        if (
            !state.value.loaded ||
                state.value.step == CreationStep.Evaluating ||
                state.value.step == CreationStep.Confirming
        )
            return
        if (state.value.draft.text.isBlank()) {
            mutable.update { it.copy(error = Failure.InvalidInput) }
            return
        }
        if (
            state.value.step == CreationStep.Clarifying &&
                state.value.questions.any {
                    it.required && state.value.draft.answers[it.id].isNullOrBlank()
                }
        ) {
            mutable.update { it.copy(error = Failure.InvalidInput) }
            return
        }
        val site = state.value.draft.preferredSite.trim()
        if (site.isNotEmpty() && !validSite(site)) {
            mutable.update { it.copy(invalidSite = true) }
            return
        }
        val draft = state.value.draft.copy(language = language)
        mutable.update {
            it.copy(
                draft = draft,
                step = CreationStep.Evaluating,
                proposal = null,
                error = null,
                partialAccepted = false,
                rejection = null,
            )
        }
        persist(draft)
        clearSavedProposal()
        pending = null
        val epoch = ++evaluationEpoch
        evaluationJob = viewModelScope.launch {
            val outcome = proposals.evaluate(draft)
            if (evaluationEpoch != epoch || state.value.draft.revision != draft.revision)
                return@launch
            when (outcome) {
                is Outcome.Error ->
                    mutable.update { it.copy(step = CreationStep.Editing, error = outcome.reason) }
                is Outcome.Success ->
                    when (val result = outcome.value) {
                        is Evaluation.Ready -> {
                            saved["proposalId"] = result.proposal.id
                            mutable.update {
                                it.copy(
                                    proposal = result.proposal,
                                    step = CreationStep.Ready,
                                    partialAccepted = result.proposal.backgroundPreparation,
                                )
                            }
                        }
                        is Evaluation.Questions ->
                            mutable.update {
                                it.copy(step = CreationStep.Clarifying, questions = result.items)
                            }
                        Evaluation.Clarify ->
                            mutable.update { it.copy(step = CreationStep.Clarifying) }
                        is Evaluation.Rejected ->
                            mutable.update {
                                it.copy(
                                    step = CreationStep.Unsupported,
                                    rejection = result.explanation,
                                )
                            }
                        Evaluation.Unsupported ->
                            mutable.update { it.copy(step = CreationStep.Unsupported) }
                    }
            }
        }
    }

    fun revise() {
        if (state.value.step == CreationStep.Confirming) return
        evaluationEpoch++
        evaluationJob?.cancel()
        pending = null
        clearSavedProposal()
        mutable.update {
            it.copy(
                step = CreationStep.Editing,
                proposal = null,
                error = null,
                partialAccepted = false,
                optionsUpdating = false,
            )
        }
    }

    fun signIn() {
        if (state.value.step != CreationStep.Login) return
        mutable.update { it.copy(step = CreationStep.Confirming, error = null) }
        viewModelScope.launch {
            when (val result = sessions.signIn()) {
                is Outcome.Error ->
                    mutable.update { it.copy(step = CreationStep.Login, error = result.reason) }
                is Outcome.Success ->
                    mutable.update { it.copy(step = CreationStep.Ready, session = result.value) }
            }
        }
    }

    fun confirm() {
        if (state.value.step != CreationStep.Ready || state.value.optionsUpdating) return
        val proposal = state.value.proposal ?: return
        if (proposal.partial && !state.value.partialAccepted) return
        if (state.value.session == null) {
            mutable.update { it.copy(step = CreationStep.Login, error = null) }
            return
        }
        val ownerEpoch = sessionEpoch
        mutable.update { it.copy(step = CreationStep.Confirming, error = null) }
        viewModelScope.launch {
            // A retry of an uncertain command must reach the receipt lookup, even after expiry.
            if (pending == null) {
                val current = proposals.get(proposal.id)
                if (ownerEpoch != sessionEpoch) return@launch
                when (current) {
                    is Outcome.Error -> {
                        mutable.update {
                            it.copy(step = CreationStep.Ready, error = current.reason)
                        }
                        return@launch
                    }
                    is Outcome.Success ->
                        if (current.value != proposal) {
                            mutable.update {
                                it.copy(
                                    step = CreationStep.Ready,
                                    proposal = current.value,
                                    partialAccepted = false,
                                    error = Failure.VersionConflict,
                                )
                            }
                            return@launch
                        }
                }
                val editing = state.value.editingId
                pending =
                    if (editing == null)
                        Command(
                            ids.next(),
                            Operation.Activate,
                            proposal.id,
                            proposal.version,
                            acceptPartial = state.value.partialAccepted,
                        )
                    else
                        Command(
                            ids.next(),
                            Operation.Replace,
                            editing,
                            expectedWatchVersion,
                            proposal.id,
                            proposal.version,
                            state.value.partialAccepted,
                        )
            }
            val result = watches.execute(checkNotNull(pending))
            if (ownerEpoch != sessionEpoch) return@launch
            when (result) {
                is Outcome.Success -> {
                    watches.refresh()
                    if (ownerEpoch != sessionEpoch) return@launch
                    saveJob?.cancelAndJoin()
                    val storageFailed = drafts.clear() is Outcome.Error
                    pending = null
                    clearSavedProposal()
                    saved["editId"] = null
                    saved["editVersion"] = null
                    mutable.update {
                        it.copy(
                            draft = RequestDraft(),
                            proposal = null,
                            step = CreationStep.Editing,
                            editingId = null,
                            activatedId = result.value.watchId,
                            storageFailed = storageFailed,
                        )
                    }
                }
                is Outcome.Error -> {
                    if (
                        result.reason !in
                            setOf(
                                Failure.Offline,
                                Failure.TemporaryFailure,
                                Failure.OperationPending,
                            )
                    )
                        pending = null
                    if (result.reason == Failure.VersionConflict && state.value.editingId != null) {
                        val watch =
                            watches.observeList().first().find { it.id == state.value.editingId }
                        if (watch != null) {
                            expectedWatchVersion = watch.version
                            saved["editVersion"] = watch.version
                        }
                    }
                    mutable.update { it.copy(step = CreationStep.Ready, error = result.reason) }
                }
            }
        }
    }

    fun resumeAfterSignIn() {
        if (state.value.session != null && state.value.step == CreationStep.Login) {
            mutable.update { it.copy(step = CreationStep.Ready, error = null) }
            confirm()
        }
    }

    fun consumeActivation() {
        mutable.update { it.copy(activatedId = null) }
    }

    fun discard() {
        if (state.value.step == CreationStep.Confirming || pending != null) return
        resetEditing(RequestDraft())
        val previousSave = saveJob
        previousSave?.cancel()
        saveJob = viewModelScope.launch {
            previousSave?.join()
            val failed = drafts.clear() is Outcome.Error
            mutable.update { it.copy(storageFailed = failed) }
        }
    }

    private fun clearSavedProposal() {
        saved["proposalId"] = null
    }
}

internal fun validSite(value: String): Boolean = runCatching {
    val uri = java.net.URI(value)
    uri.scheme == "https" &&
        uri.host != null &&
        uri.host.contains('.') &&
        uri.userInfo == null &&
        uri.port == -1 &&
        uri.fragment == null &&
        !uri.host.matches(Regex("[0-9.]+")) &&
        !uri.host.endsWith(".local")
}
    .getOrDefault(false)
