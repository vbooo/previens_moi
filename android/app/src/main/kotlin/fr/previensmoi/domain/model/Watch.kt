package fr.previensmoi.domain.model

enum class WatchStatus { Pending, Active, Paused, Completed, SuspendedByCapacity }
enum class RemoteState { Starting, Running, Stopping, Stopped, Unknown }
enum class Health { NotCheckedYet, Healthy, Degraded }
enum class FollowMode { Once, Continuous }
data class RequestDraft(
    val text: String = "", val answer: String = "", val language: String = "fr",
    val mode: FollowMode = FollowMode.Once, val revision: Long = 0,
    val answers: Map<String, String> = emptyMap(),
)
val RequestDraft.preferredSite: String get() = answers["preferred_site"].orEmpty()
val RequestDraft.region: String get() = answers["region"].orEmpty()
data class Proposal(
    val id: String, val version: Long, val draft: RequestDraft, val expiresAt: Long,
    val partial: Boolean = false, val sources: List<String> = emptyList(),
    val intervalHours: Int = 24, val limit: Int = 1,
    val title: String = "", val criteria: List<String> = emptyList(), val exclusions: List<String> = emptyList(),
    val coverage: String = "", val limitations: List<String> = emptyList(), val method: String = "", val backgroundPreparation: Boolean = false, val preparationFailure: String? = null, val sourceLabels: Map<String,String> = emptyMap(), val checkedSources: Int? = null, val totalSources: Int? = null, val trialDays: Int? = null,
)
data class Watch(
    val id: String, val plan: Proposal, val version: Long = 1,
    val status: WatchStatus = WatchStatus.Active, val health: Health = Health.NotCheckedYet,
    val lastSuccess: Long? = null, val lastFailure: Long? = null,
    val baselineAt: Long?, val nextCheck: Long? = null, val remoteState: RemoteState = RemoteState.Stopped,
    val trialEndsAt: Long? = null, val trialEnded: Boolean = false,
)
data class Finding(
    val id: String, val watchId: String, val planId: String, val planVersion: Long,
    val criterion: String, val observedAt: Long, val publishedAt: Long? = null,
    val eventAt: Long? = null, val source: String = "https://example.invalid/announcement",
    val title: String = "", val fact: String = "", val reason: String = "", val evidence: List<Proof> = emptyList(), val eventDate: String? = null, val noveltyBasis: String = "", val correctsFindingId: String? = null,
)
data class Proof(val url: String, val quote: String, val observedAt: Long, val publicationDate: String? = null)
enum class Failure {
    OperationPending, OperationFailed, AnalysisPending,
    Offline, TemporaryFailure, SessionRequired, AccessDenied, NotFound, ProposalExpired,
    VersionConflict, CommandConflict, QuotaExceeded, ActiveLimit, TrialEnded, Unsupported, InvalidInput, StorageFailure,
}
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Error(val reason: Failure) : Outcome<Nothing>
}
sealed interface Evaluation {
    data class Ready(val proposal: Proposal) : Evaluation
    data object Clarify : Evaluation
    data class Questions(val items: List<ClarificationQuestion>) : Evaluation
    data object Unsupported : Evaluation
    data class Rejected(val reason: String, val explanation: String) : Evaluation
}
data class ClarificationQuestion(val id: String, val label: String, val options: List<String>, val required: Boolean)
enum class Operation { Activate, Pause, Resume, Delete, Replace }
data class Command(
    val id: String, val operation: Operation, val targetId: String,
    val expectedVersion: Long, val proposalId: String? = null, val proposalVersion: Long? = null, val acceptPartial: Boolean = false,
)
data class CommandReceipt(val commandId: String, val watchId: String, val version: Long?, val deleted: Boolean = false, val acceptedInBackground: Boolean = false)
data class Session(val id: String, val email: String = "")
sealed interface RefreshResult {
    data object Success : RefreshResult
    data object TemporaryFailure : RefreshResult
    data object Offline : RefreshResult
}
fun interface Clock { fun now(): Long }
fun interface IdGenerator { fun next(): String }

/** A resumed generation may fail while retaining results from an earlier run. */
val Watch.preparationFailed: Boolean get() = !trialEnded && status == WatchStatus.Paused &&
    health == Health.Degraded && (baselineAt == null || plan.preparationFailure != null)
