package fr.previensmoi.data.demo

import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class DemoScenario {
    Normal, Clarification, Partial, Unsupported, NoResults, Quota, SourceFailure,
    Offline, CommandFailure, LostResponse, Expired, VersionConflict,
}

/** In-memory test double. It does not interpret text, search, schedule or send notifications. */
class DemoService(private val clock: Clock, private val ids: IdGenerator, private val capacity: Int = 1) :
    WatchRepository, ProposalRepository, FindingRepository, SessionRepository {
    private val mutex = Mutex()
    private val proposals = mutableMapOf<String, Proposal>()
    private val activated = mutableMapOf<Pair<String, Long>, String>()
    private data class StoredReceipt(val owner: String, val command: Command, val receipt: CommandReceipt)
    private val receipts = mutableMapOf<String, StoredReceipt>()
    private val watches = MutableStateFlow<List<Watch>>(emptyList())
    private val findings = MutableStateFlow<List<Finding>>(emptyList())
    private val session = MutableStateFlow<Session?>(null)
    private val selectedScenario = MutableStateFlow(DemoScenario.Normal)
    val scenario = selectedScenario.asStateFlow()

    suspend fun selectScenario(value: DemoScenario) = mutex.withLock { selectedScenario.value = value }
    override fun observeList() = watches.asStateFlow()
    override fun observeFindings() = findings.asStateFlow()
    override fun observeSession() = session.asStateFlow()
    private fun unavailable(): Failure? = if (scenario.value == DemoScenario.Offline) Failure.Offline else null

    override suspend fun signIn(): Outcome<Session> = mutex.withLock {
        unavailable()?.let { return@withLock Outcome.Error(it) }
        val identity = session.value ?: Session("synthetic-person")
        session.value = identity
        Outcome.Success(identity)
    }

    override suspend fun evaluate(draft: RequestDraft): Outcome<Evaluation> = mutex.withLock {
        unavailable()?.let { return@withLock Outcome.Error(it) }
        if (draft.text.isBlank()) return@withLock Outcome.Error(Failure.InvalidInput)
        if (scenario.value == DemoScenario.Unsupported) return@withLock Outcome.Success(Evaluation.Unsupported)
        if (scenario.value == DemoScenario.Clarification && draft.answer.isBlank()) {
            return@withLock Outcome.Success(Evaluation.Clarify)
        }
        val proposal = Proposal(
            id = ids.next(), version = draft.revision + 1, draft = draft.copy(),
            expiresAt = if (scenario.value == DemoScenario.Expired) clock.now() else clock.now() + 86_400_000L,
            partial = scenario.value == DemoScenario.Partial,
            sources = if (scenario.value == DemoScenario.NoResults) emptyList() else listOf("https://example.invalid/announcements"),
            limit = capacity,
        )
        proposals[proposal.id] = proposal
        Outcome.Success(Evaluation.Ready(proposal))
    }

    override suspend fun configure(proposal: Proposal, mode: FollowMode): Outcome<Proposal> = mutex.withLock {
        val current=proposals[proposal.id] ?: return@withLock Outcome.Error(Failure.NotFound)
        if(current.version!=proposal.version)return@withLock Outcome.Error(Failure.VersionConflict)
        val next=current.copy(version=current.version+1,draft=current.draft.copy(mode=mode))
        proposals[next.id]=next;Outcome.Success(next)
    }
    override suspend fun get(id: String): Outcome<Proposal> = mutex.withLock {
        unavailable()?.let { return@withLock Outcome.Error(it) }
        val proposal = proposals[id] ?: return@withLock Outcome.Error(Failure.NotFound)
        if (proposal.expiresAt <= clock.now()) Outcome.Error(Failure.ProposalExpired) else Outcome.Success(proposal)
    }
    override suspend fun getFinding(id: String): Outcome<Finding> = mutex.withLock {
        unavailable()?.let { return@withLock Outcome.Error(it) }
        if (session.value == null) return@withLock Outcome.Error(Failure.SessionRequired)
        findings.value.firstOrNull { it.id == id }?.let { Outcome.Success(it) } ?: Outcome.Error(Failure.NotFound)
    }
    override suspend fun refresh(): RefreshResult = mutex.withLock {
        if (unavailable() != null) RefreshResult.Offline else RefreshResult.Success
    }

    override suspend fun execute(command: Command): Outcome<CommandReceipt> = mutex.withLock {
        val owner = session.value?.id ?: return@withLock Outcome.Error(Failure.SessionRequired)
        unavailable()?.let { return@withLock Outcome.Error(it) }
        receipts[command.id]?.let {
            if (it.owner != owner) return@withLock Outcome.Error(Failure.AccessDenied)
            return@withLock if (it.command == command) Outcome.Success(it.receipt) else Outcome.Error(Failure.CommandConflict)
        }
        if (scenario.value == DemoScenario.CommandFailure) return@withLock Outcome.Error(Failure.TemporaryFailure)
        if (scenario.value == DemoScenario.VersionConflict) return@withLock Outcome.Error(Failure.VersionConflict)
        val result = if (command.operation == Operation.Activate) activate(command) else mutate(command)
        if (result is Outcome.Success) {
            receipts[command.id] = StoredReceipt(owner, command, result.value)
            // Mutation and receipt survive a simulated lost transport response.
            if (scenario.value == DemoScenario.LostResponse) return@withLock Outcome.Error(Failure.TemporaryFailure)
        }
        result
    }

    private fun validatedProposal(id: String, version: Long): Outcome<Proposal> {
        val proposal = proposals[id] ?: return Outcome.Error(Failure.NotFound)
        if (proposal.version != version) return Outcome.Error(Failure.VersionConflict)
        if (proposal.expiresAt <= clock.now()) return Outcome.Error(Failure.ProposalExpired)
        return Outcome.Success(proposal)
    }
    private fun hasCapacity(excluding: String? = null): Boolean = scenario.value != DemoScenario.Quota &&
        watches.value.count { it.id != excluding && it.status != WatchStatus.Completed } < capacity

    private fun activate(c: Command): Outcome<CommandReceipt> {
        activated[c.targetId to c.expectedVersion]?.let { id ->
            val watch = watches.value.firstOrNull { it.id == id } ?: return Outcome.Error(Failure.NotFound)
            return Outcome.Success(CommandReceipt(c.id, id, watch.version))
        }
        val checked = validatedProposal(c.targetId, c.expectedVersion)
        if (checked is Outcome.Error) return checked
        val proposal = (checked as Outcome.Success).value
        if (!hasCapacity()) return Outcome.Error(Failure.QuotaExceeded)
        val watch = Watch(ids.next(), proposal, health = Health.Healthy, lastSuccess = clock.now(), baselineAt = clock.now(), nextCheck = clock.now() + proposal.intervalHours * 3_600_000L)
        watches.value = watches.value + watch
        activated[c.targetId to c.expectedVersion] = watch.id
        return Outcome.Success(CommandReceipt(c.id, watch.id, watch.version))
    }

    private fun mutate(c: Command): Outcome<CommandReceipt> {
        val old = watches.value.firstOrNull { it.id == c.targetId } ?: return Outcome.Error(Failure.NotFound)
        if (old.version != c.expectedVersion) return Outcome.Error(Failure.VersionConflict)
        if (c.operation == Operation.Delete) {
            watches.value = watches.value.filterNot { it.id == old.id }
            findings.value = findings.value.filterNot { it.watchId == old.id }
            return Outcome.Success(CommandReceipt(c.id, old.id, null, deleted = true))
        }
        val updated = when (c.operation) {
            Operation.Pause -> when (old.status) {
                WatchStatus.Active -> old.copy(status = WatchStatus.Paused, version = old.version + 1, nextCheck = null)
                WatchStatus.Paused -> old
                else -> return Outcome.Error(Failure.Unsupported)
            }
            Operation.Resume -> {
                if (old.status == WatchStatus.Active) old
                else {
                    if (old.status == WatchStatus.Completed) return Outcome.Error(Failure.Unsupported)
                    if (!hasCapacity(old.id)) return Outcome.Error(Failure.QuotaExceeded)
                    old.copy(status = WatchStatus.Active, version = old.version + 1, baselineAt = clock.now(),
                        health = Health.Healthy, lastSuccess = clock.now(), nextCheck = clock.now() + old.plan.intervalHours * 3_600_000L)
                }
            }
            Operation.Replace -> {
                if (old.status == WatchStatus.Completed) return Outcome.Error(Failure.Unsupported)
                val checked = validatedProposal(c.proposalId ?: return Outcome.Error(Failure.InvalidInput), c.proposalVersion ?: return Outcome.Error(Failure.InvalidInput))
                if (checked is Outcome.Error) return checked
                val plan = (checked as Outcome.Success).value
                old.copy(plan = plan, version = old.version + 1, baselineAt = clock.now(), health = Health.Healthy, lastSuccess = clock.now(),
                    nextCheck = if (old.status == WatchStatus.Active) clock.now() + plan.intervalHours * 3_600_000L else null)
            }
            else -> return Outcome.Error(Failure.InvalidInput)
        }
        watches.value = watches.value.map { if (it.id == old.id) updated else it }
        return Outcome.Success(CommandReceipt(c.id, old.id, updated.version))
    }

    /** Explicit scenario button only. There is deliberately no timer or network worker. */
    suspend fun simulateCheck() = mutex.withLock {
        val now = clock.now()
        watches.value = watches.value.map { watch ->
            if (watch.status != WatchStatus.Active) return@map watch
            if (scenario.value == DemoScenario.Quota) return@map watch.copy(status = WatchStatus.SuspendedByCapacity, version = watch.version + 1, nextCheck = null)
            if (scenario.value == DemoScenario.SourceFailure || scenario.value == DemoScenario.Offline) {
                return@map watch.copy(health = Health.Degraded, lastFailure = now)
            }
            val previous = findings.value.any { it.watchId == watch.id && it.planId == watch.plan.id }
            val creates = scenario.value != DemoScenario.NoResults && !previous
            if (creates) findings.value = findings.value + Finding(ids.next(), watch.id, watch.plan.id,
                watch.plan.version, watch.plan.draft.text, now, eventAt = now + 7 * 86_400_000L)
            val completed = creates && watch.plan.draft.mode == FollowMode.Once
            watch.copy(health = Health.Healthy, lastSuccess = now,
                status = if (completed) WatchStatus.Completed else watch.status,
                version = if (completed) watch.version + 1 else watch.version,
                nextCheck = if (completed) null else now + watch.plan.intervalHours * 3_600_000L)
        }
    }

    suspend fun reset() = mutex.withLock {
        proposals.clear(); activated.clear(); receipts.clear()
        watches.value = emptyList(); findings.value = emptyList(); session.value = null
        selectedScenario.value = DemoScenario.Normal
    }
}
