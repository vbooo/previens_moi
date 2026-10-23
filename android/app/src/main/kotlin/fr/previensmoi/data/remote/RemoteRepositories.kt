package fr.previensmoi.data.remote

import fr.previensmoi.data.auth.RemoteSessionRepository
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.serialization.Serializable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface PendingCommands {
    fun read(owner: String): String?
    fun write(owner: String, id: String)
    fun clear(owner: String)
}
@Serializable
class EvaluationCache(val token: String, val request: EvaluationRequestDto?, val previousId: String?, val expiresAt: Long = Long.MAX_VALUE, val workflowVersion: Int = 1) {
    override fun toString() = "EvaluationCache([redacted])"
}
interface EvaluationJournal {
    fun read(): EvaluationCache?
    fun write(value: EvaluationCache)
}
private class MemoryEvaluationJournal : EvaluationJournal {
    private var value: EvaluationCache? = null
    override fun read() = value
    override fun write(value: EvaluationCache) { this.value = value }
}
class RemoteRepositories(
    private val api: ApplicationApi, private val sessions: RemoteSessionRepository,
    private val journal: PendingCommands, private val ids: IdGenerator,
    private val evaluations: EvaluationJournal = MemoryEvaluationJournal(),
    private val evaluationPolls: Int = 61,
    private val evaluationPollDelayMillis: Long = 5_000,
) : WatchRepository, ProposalRepository, FindingRepository {
    init { require(evaluationPolls in 1..61 && evaluationPollDelayMillis in 0..5_000) }
    private val mutex = Mutex()
    private val watches = MutableStateFlow<List<Watch>>(emptyList())
    private val findings = MutableStateFlow<List<Finding>>(emptyList())
    private var preview: String? = null
    private var previewExpires: Long = Long.MAX_VALUE
    private var evaluationRequest: EvaluationRequestDto? = null
    private var previousEvaluation: String? = null
    private var evaluationLoaded = false
    private fun restoreEvaluation(): Boolean = try {
        if (!evaluationLoaded) {
            evaluations.read()?.let { preview = it.token.takeIf(String::isNotEmpty); previewExpires = it.expiresAt; evaluationRequest = it.request.takeIf { _ -> it.workflowVersion >= 2 }; previousEvaluation = it.previousId.takeIf { _ -> it.workflowVersion >= 2 } }
            evaluationLoaded = true
        }; true
    } catch (_: Exception) { false }
    private fun saveEvaluation(): Boolean = try {
        evaluations.write(EvaluationCache(preview.orEmpty(),evaluationRequest,previousEvaluation,previewExpires,workflowVersion = 2));true
    } catch (_: Exception) { false }
    private var owner: String? = null
    private var commandInMemory: CommandDto? = null
    private fun expireEvaluation(): Boolean {
        if (preview == null || System.currentTimeMillis() < previewExpires) return false
        preview = null; previewExpires = 0; evaluationRequest = null; previousEvaluation = null
        return true
    }
    private fun evaluationFailure(result: ApiResult<*>): Failure {
        if (result is ApiResult.Rejected && result.httpStatus == 401) {
            preview = null; previewExpires = 0; evaluationRequest = null; previousEvaluation = null
            return if (saveEvaluation()) Failure.ProposalExpired else Failure.StorageFailure
        }
        return result.failure()
    }
    override fun observeList() = combine(watches, sessions.observeSession()) { value, session -> if (session?.id == owner) value else emptyList() }
    override fun observeFindings() = combine(findings, sessions.observeSession()) { value, session -> if (session?.id == owner) value else emptyList() }
    fun clearPrivateState() { watches.value = emptyList(); findings.value = emptyList(); owner = null; commandInMemory = null }
    private suspend fun bearer(): String? {
        val current = sessions.observeSession().first()?.id
        if (current != owner) { watches.value = emptyList(); findings.value = emptyList(); commandInMemory = null; owner = current }
        return sessions.bearer()
    }
    override suspend fun evaluate(draft: RequestDraft): Outcome<Evaluation> {
        if (!restoreEvaluation()) return Outcome.Error(Failure.StorageFailure)
        if (expireEvaluation()) return Outcome.Error(if(saveEvaluation()) Failure.ProposalExpired else Failure.StorageFailure)
        val token = preview ?: when (val r = apiCall { api.previewSession() }) {
            is ApiResult.Value -> r.body.token.also { previewExpires = instant(r.body.expiresAt); preview = it }
            else -> return Outcome.Error(r.failure())
        }
        val answers = draft.answers.entries.sortedBy { it.key }.map { AnswerDto(it.key, it.value) }
        val wire = DraftDto(draft.text, draft.language, if (draft.mode == FollowMode.Once) "once" else "continuous", draft.revision, answers)
        val request = evaluationRequest?.takeIf { it.draft == wire }
            ?: EvaluationRequestDto(ids.next(), wire, previousEvaluation.takeIf { evaluationRequest?.draft?.text == wire.text && evaluationRequest?.draft?.mode == wire.mode }).also { evaluationRequest = it }
        if (!saveEvaluation()) return Outcome.Error(Failure.StorageFailure)
        var response = apiCall { api.evaluate(token, request) }
        repeat(evaluationPolls) { attempt ->
            when (val r = response) {
                is ApiResult.Value -> {
                    val value = r.body
                    if (value.id != request.id || value.draftRevision != draft.revision) return Outcome.Error(Failure.VersionConflict)
                    when (value) {
                        is EvaluationDto.Pending -> {
                            if (attempt == evaluationPolls - 1) return Outcome.Error(Failure.AnalysisPending)
                            delay(evaluationPollDelayMillis)
                            response = apiCall { api.evaluation(token, request.id) }
                        }
                        is EvaluationDto.Ready -> return mapped { Evaluation.Ready(value.proposal.domain()) }
                        is EvaluationDto.Clarification -> {
                            previousEvaluation = value.id
                            if (!saveEvaluation()) return Outcome.Error(Failure.StorageFailure)
                            return Outcome.Success(Evaluation.Questions(value.questions.map { ClarificationQuestion(it.id, it.label, it.options, it.required) }))
                        }
                        is EvaluationDto.Unsupported -> return Outcome.Success(Evaluation.Rejected(value.reason, value.explanation))
                        is EvaluationDto.Failed -> return Outcome.Error(errorFailure(value.error.code))
                    }
                }
                else -> return Outcome.Error(evaluationFailure(r))
            }
        }
        return Outcome.Error(Failure.AnalysisPending)
    }
    override suspend fun get(id: String): Outcome<Proposal> {
        if (!restoreEvaluation()) return Outcome.Error(Failure.StorageFailure)
        if (expireEvaluation()) return Outcome.Error(if(saveEvaluation()) Failure.ProposalExpired else Failure.StorageFailure)
        val token = preview ?: return Outcome.Error(Failure.NotFound)
        return when (val r = apiCall { api.proposal(token, id) }) {
            is ApiResult.Value -> mapped { r.body.domain() }
            else -> Outcome.Error(evaluationFailure(r))
        }
    }
    override suspend fun refresh(): RefreshResult = mutex.withLock {
        val bearer = bearer() ?: return@withLock RefreshResult.TemporaryFailure
        val snapshotOwner = owner
        val all = mutableListOf<WatchDto>(); var cursor: String? = null
        do {
            when (val r = authorized(bearer) { api.watches(it, cursor) }) {
                is ApiResult.Value -> { all += r.body.items; val next = r.body.nextCursor
                    if (next != null && next == cursor) return@withLock RefreshResult.TemporaryFailure
                    cursor = next }
                else -> return@withLock RefreshResult.TemporaryFailure
            }
        } while (cursor != null && all.size <= 1000)
        if (cursor != null) return@withLock RefreshResult.TemporaryFailure
        // Resume the existing identifier only; a refresh never posts a second activation.
        val currentOwner = snapshotOwner ?: return@withLock RefreshResult.TemporaryFailure
        journal.read(currentOwner)?.let { recover(bearer, currentOwner, it) }
        val mapped = try { all.map { it.domain() } } catch (_: IllegalArgumentException) { return@withLock RefreshResult.TemporaryFailure }
        val results = mutableListOf<Finding>()
        for (watch in all) {
            var page: String? = null; var count = 0
            do {
                when (val r = authorized(bearer) { api.findings(it, watch.id, page) }) {
                    is ApiResult.Value -> {
                        try { results += r.body.items.map { it.domain() } } catch (_: IllegalArgumentException) { return@withLock RefreshResult.TemporaryFailure }
                        val next = r.body.nextCursor
                        if (next != null && next == page) return@withLock RefreshResult.TemporaryFailure
                        count += r.body.items.size; page = next
                    }
                    else -> return@withLock RefreshResult.TemporaryFailure
                }
            } while (page != null && count <= 1000)
            if (page != null) return@withLock RefreshResult.TemporaryFailure
        }
        if (sessions.observeSession().first()?.id != snapshotOwner) { clearPrivateState(); return@withLock RefreshResult.TemporaryFailure }
        watches.value = mapped; findings.value = results
        RefreshResult.Success
    }
    override suspend fun execute(command: Command): Outcome<CommandReceipt> = mutex.withLock {
        val bearer = bearer() ?: return@withLock Outcome.Error(Failure.SessionRequired)
        val user = owner ?: return@withLock Outcome.Error(Failure.SessionRequired)
        val pending = journal.read(user)
        if (pending != null) {
            // Once the server identifies the pending watch, cancelling that same watch
            // is a new fenced command, even after process death. Never infer its identity.
            val cancelling = pending != command.id && command.operation in setOf(Operation.Pause, Operation.Delete)
            val receipt = if (cancelling) authorized(bearer) { api.commandReceipt(it, pending) } else null
            val known = (receipt as? ApiResult.Value)?.body as? CommandReceiptDto.Pending
            if (known?.watchId == command.targetId && known.remoteState == "starting" && sessions.observeSession().first()?.id == user) {
                journal.clear(user); commandInMemory = null
            } else {
                val previous = if (receipt != null) finish(user,pending,receipt) else recover(bearer, user, pending)
                // A recovered old success must never confirm the new action that triggered lookup.
                return@withLock if (pending != command.id && previous is Outcome.Success) Outcome.Error(Failure.VersionConflict) else previous
            }
        }
        val token = preview
        val dto = when (command.operation) {
            Operation.Activate -> CommandDto.Activate(command.id, command.targetId, command.expectedVersion, token ?: return@withLock Outcome.Error(Failure.ProposalExpired), command.acceptPartial)
            Operation.Replace -> CommandDto.Replace(command.id, command.targetId, command.expectedVersion, command.proposalId ?: return@withLock Outcome.Error(Failure.InvalidInput), command.proposalVersion ?: return@withLock Outcome.Error(Failure.InvalidInput), token ?: return@withLock Outcome.Error(Failure.ProposalExpired), command.acceptPartial)
            Operation.Pause -> CommandDto.Pause(command.id, command.targetId, command.expectedVersion)
            Operation.Resume -> CommandDto.Resume(command.id, command.targetId, command.expectedVersion)
            Operation.Delete -> CommandDto.Delete(command.id, command.targetId, command.expectedVersion)
        }
        try { journal.write(user, command.id) } catch (_: java.io.IOException) { return@withLock Outcome.Error(Failure.StorageFailure) }
        commandInMemory = dto
        val result = authorized(bearer) { api.command(it, dto) }
        if (sessions.observeSession().first()?.id != user) return@withLock Outcome.Error(Failure.SessionRequired)
        finish(user, command.id, result)
    }
    private suspend fun <T> authorized(initial: String, call: suspend (String) -> retrofit2.Response<T>): ApiResult<T> {
        val first = apiCall { call(initial) }
        if (first !is ApiResult.Rejected || first.httpStatus != 401) return first
        val fresh = sessions.bearer(rejected = initial) ?: return first
        val retry = apiCall { call(fresh) }
        if (retry is ApiResult.Rejected && retry.httpStatus == 401) sessions.invalidate()
        return retry
    }
    private suspend fun recover(bearer: String, user: String, id: String): Outcome<CommandReceipt> {
        val result = authorized(bearer) { api.commandReceipt(it, id) }
        // If the process is still alive, an explicit retry may resend exactly the same UUID/body.
        // After process death only the UUID remains; never manufacture a second POST.
        if (result is ApiResult.Rejected && result.httpStatus == 404) {
            val same = commandInMemory
            return if (same?.id == id) finish(user, id, authorized(bearer) { api.command(it, same) })
                else Outcome.Error(Failure.OperationPending)
        }
        if (sessions.observeSession().first()?.id != user) return Outcome.Error(Failure.SessionRequired)
        return finish(user, id, result)
    }
    private fun finish(user: String, id: String, result: ApiResult<CommandReceiptDto>): Outcome<CommandReceipt> = when (result) {
        is ApiResult.Value -> if (result.body.id != id) Outcome.Error(Failure.TemporaryFailure) else when (val r = result.body) {
            is CommandReceiptDto.Pending -> if ((commandInMemory is CommandDto.Activate || commandInMemory is CommandDto.Replace) && r.watchId != null)
                Outcome.Success(CommandReceipt(id,checkNotNull(r.watchId),null,acceptedInBackground = true))
                else Outcome.Error(Failure.OperationPending)
            is CommandReceiptDto.Failed -> { journal.clear(user); commandInMemory = null; Outcome.Error(errorFailure(r.error.code)) }
            is CommandReceiptDto.Succeeded -> { journal.clear(user); commandInMemory = null; Outcome.Success(CommandReceipt(id, r.watchId, r.watchVersion, r.deleted)) }
        }
        is ApiResult.Rejected -> {
            if (result.httpStatus in setOf(400,403,409,410,429)) { journal.clear(user); commandInMemory = null }
            Outcome.Error(result.failure())
        }
        else -> Outcome.Error(result.failure())
    }
    override suspend fun getFinding(id: String): Outcome<Finding> {
        val bearer = bearer() ?: return Outcome.Error(Failure.SessionRequired)
        val user = owner
        val result = authorized(bearer) { api.finding(it, id) }
        if (sessions.observeSession().first()?.id != user) return Outcome.Error(Failure.SessionRequired)
        return when (result) { is ApiResult.Value -> mapped { result.body.domain() }; else -> Outcome.Error(result.failure()) }
    }
}
private inline fun <T> mapped(block: () -> T): Outcome<T> = try { Outcome.Success(block()) } catch (_: IllegalArgumentException) { Outcome.Error(Failure.TemporaryFailure) }
internal fun instant(value: String): Long {
    require(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,6})?Z").matches(value))
    val normalized = value.replace(Regex("\\.(\\d{1,6})Z$")) { "." + it.groupValues[1].padEnd(3,'0').take(3) + "Z" }
    return requireNotNull(SimpleDateFormat(if (normalized.contains('.')) "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" else "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC"); isLenient = false }.parse(normalized)).time
}
internal fun ProposalDto.domain() = Proposal(id, version, RequestDraft(draft.text, draft.answers.joinToString("\n") { it.value }, draft.language,
    when(draft.mode) { "once" -> FollowMode.Once; "continuous" -> FollowMode.Continuous; else -> throw IllegalArgumentException() }, draft.revision, draft.answers.associate { it.questionId to it.value }), instant(expiresAt), partial, sources.map { it.url }, intervalHours, limits.activePerAccount,
    title, criteria, exclusions, coverage, limitations, method, backgroundPreparation, preparationFailure)
internal fun WatchDto.domain() = Watch(id, plan.domain(), version,
    when(status) { "pending" -> WatchStatus.Pending; "active" -> WatchStatus.Active; "paused" -> WatchStatus.Paused; "completed" -> WatchStatus.Completed; "suspended_by_capacity" -> WatchStatus.SuspendedByCapacity; else -> throw IllegalArgumentException() },
    when(health) { "not_checked_yet" -> Health.NotCheckedYet; "healthy" -> Health.Healthy; "degraded" -> Health.Degraded; else -> throw IllegalArgumentException() },
    lastSuccessAt?.let(::instant), lastFailureAt?.let(::instant), baselineAt?.let(::instant), nextCheckAt?.let(::instant),
    when(remoteState) { "starting" -> RemoteState.Starting; "running" -> RemoteState.Running; "stopping" -> RemoteState.Stopping; "stopped" -> RemoteState.Stopped; "unknown" -> RemoteState.Unknown; else -> throw IllegalArgumentException() })
internal fun FindingDto.domain(): Finding {
    require(evidence.isNotEmpty())
    return Finding(id, watchId, planId, planVersion, criteriaSnapshot.joinToString("\n"), instant(observedAt),
        (evidence.first().publication as? TemporalEvidenceDto.Instant)?.value?.let(::instant), (event as? TemporalEvidenceDto.Instant)?.value?.let(::instant), evidence.first().url,
        title, fact, reason, evidence.map { Proof(it.url, it.quote, instant(it.observedAt), (it.publication as? TemporalEvidenceDto.Date)?.value) }, (event as? TemporalEvidenceDto.Date)?.value, noveltyBasis, correctsFindingId)
}
internal fun errorFailure(code: String) = when(code) {
    "session_required" -> Failure.SessionRequired; "access_denied" -> Failure.AccessDenied; "not_found" -> Failure.NotFound
    "proposal_expired", "command_expired" -> Failure.ProposalExpired; "version_conflict" -> Failure.VersionConflict; "command_conflict" -> Failure.CommandConflict
    "quota_exceeded" -> Failure.QuotaExceeded; "invalid_input" -> Failure.InvalidInput; "unsupported_client" -> Failure.Unsupported; else -> Failure.TemporaryFailure
}
private fun ApiResult<*>.failure(): Failure = when(this) { is ApiResult.Rejected -> error?.code?.let(::errorFailure) ?: if(httpStatus==401) Failure.SessionRequired else Failure.TemporaryFailure; else -> Failure.TemporaryFailure }
