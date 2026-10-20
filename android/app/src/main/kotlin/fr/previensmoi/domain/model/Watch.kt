package fr.previensmoi.domain.model

enum class WatchStatus { Active, Paused, Completed, SuspendedByCapacity }
enum class Health { NotCheckedYet, Healthy, Degraded }
enum class FollowMode { Once, Continuous }
data class RequestDraft(
    val text: String = "", val answer: String = "", val language: String = "fr",
    val mode: FollowMode = FollowMode.Once, val revision: Long = 0,
)
data class Proposal(
    val id: String, val version: Long, val draft: RequestDraft, val expiresAt: Long,
    val partial: Boolean = false, val sources: List<String> = emptyList(),
    val intervalHours: Int = 24, val limit: Int = 1,
)
data class Watch(
    val id: String, val plan: Proposal, val version: Long = 1,
    val status: WatchStatus = WatchStatus.Active, val health: Health = Health.NotCheckedYet,
    val lastSuccess: Long? = null, val lastFailure: Long? = null,
    val baselineAt: Long, val nextCheck: Long? = null,
)
data class Finding(
    val id: String, val watchId: String, val planId: String, val planVersion: Long,
    val criterion: String, val observedAt: Long, val publishedAt: Long? = null,
    val eventAt: Long? = null, val source: String = "https://example.invalid/announcement",
)
enum class Failure {
    Offline, TemporaryFailure, SessionRequired, AccessDenied, NotFound, ProposalExpired,
    VersionConflict, CommandConflict, QuotaExceeded, Unsupported, InvalidInput, StorageFailure,
}
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Error(val reason: Failure) : Outcome<Nothing>
}
sealed interface Evaluation {
    data class Ready(val proposal: Proposal) : Evaluation
    data object Clarify : Evaluation
    data object Unsupported : Evaluation
}
enum class Operation { Activate, Pause, Resume, Delete, Replace }
data class Command(
    val id: String, val operation: Operation, val targetId: String,
    val expectedVersion: Long, val proposalId: String? = null, val proposalVersion: Long? = null,
)
data class CommandReceipt(val commandId: String, val watchId: String, val version: Long?, val deleted: Boolean = false)
data class Session(val id: String)
sealed interface RefreshResult {
    data object Success : RefreshResult
    data object TemporaryFailure : RefreshResult
    data object Offline : RefreshResult
}
fun interface Clock { fun now(): Long }
fun interface IdGenerator { fun next(): String }
