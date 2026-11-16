@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
package fr.previensmoi.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

// Wire models deliberately stay in data. Timestamps remain strings until domain mapping;
// a calendar date must never become an invented midnight instant.
@Serializable data class ErrorDto(val code: String, val requestId: String, val retryAfterSeconds: Long?, val scope: String?)
@Serializable data class AnswerDto(val questionId: String, val value: String)
@Serializable data class DraftDto(val text: String, val language: String, val mode: String, val revision: Long, val answers: List<AnswerDto>)
@Serializable data class QuestionDto(val id: String, val label: String, val options: List<String>, val required: Boolean)
@Serializable data class LimitsDto(val activePerAccount: Int, val globalAvailable: Int, val previewRemaining: Int, val resetsAt: String?)
@Serializable data class ProposalOptionsDto(val version: Long, val mode: String)
@Serializable data class SourceDto(val url: String, val label: String)
@Serializable data class CheckCoverageDto(val verified:Int,val total:Int)
@Serializable data class ProposalDto(
    val id: String, val version: Long, val draft: DraftDto, val title: String,
    val criteria: List<String>, val exclusions: List<String>, val coverage: String,
    val partial: Boolean, val sources: List<SourceDto>, val method: String,
    val intervalHours: Int, val limitations: List<String>, val limits: LimitsDto, val expiresAt: String, val backgroundPreparation: Boolean = false, val preparationFailure: String? = null, val checkCoverage: CheckCoverageDto? = null, val trialDays: Int? = null, val accessTier: String? = null,
)
@Serializable data class PreviewSessionDto(val token: String, val expiresAt: String) {
    override fun toString() = "PreviewSessionDto(token=[redacted], expiresAt=$expiresAt)"
}
@Serializable data class EvaluationRequestDto(val id: String, val draft: DraftDto, val previousEvaluationId: String?)
@Serializable @JsonClassDiscriminator("status") sealed interface EvaluationDto {
    val id: String
    val draftRevision: Long
    val updatedAt: String
    @Serializable @SerialName("pending") data class Pending(override val id: String, override val draftRevision: Long, override val updatedAt: String): EvaluationDto
    @Serializable @SerialName("ready") data class Ready(override val id: String, override val draftRevision: Long, override val updatedAt: String, val proposal: ProposalDto): EvaluationDto
    @Serializable @SerialName("clarification") data class Clarification(override val id: String, override val draftRevision: Long, override val updatedAt: String, val questions: List<QuestionDto>): EvaluationDto
    @Serializable @SerialName("unsupported") data class Unsupported(override val id: String, override val draftRevision: Long, override val updatedAt: String, val reason: String, val explanation: String): EvaluationDto
    @Serializable @SerialName("failed") data class Failed(override val id: String, override val draftRevision: Long, override val updatedAt: String, val error: ErrorDto): EvaluationDto
}
@Serializable data class WatchDto(
    val id: String, val version: Long, val plan: ProposalDto, val status: String, val health: String,
    val lastSuccessAt: String?, val lastFailureAt: String?, val baselineAt: String?,
    val nextCheckAt: String?, val pendingCommandId: String?, val remoteState: String,
    val trialEndsAt: String? = null, val trialEnded: Boolean = false, val subscriptionEnded: Boolean = false,
)
@Serializable @JsonClassDiscriminator("precision") sealed interface TemporalEvidenceDto {
    val value: String
    @Serializable @SerialName("date") data class Date(override val value: String): TemporalEvidenceDto
    @Serializable @SerialName("instant") data class Instant(override val value: String): TemporalEvidenceDto
}
@Serializable data class EvidenceDto(val url: String, val quote: String, val observedAt: String, val publication: TemporalEvidenceDto?)
@Serializable data class FindingDto(
    val id: String, val watchId: String, val planId: String, val planVersion: Long,
    val title: String, val fact: String, val reason: String, val criteriaSnapshot: List<String>,
    val evidence: List<EvidenceDto>, val observedAt: String, val event: TemporalEvidenceDto?,
    val noveltyBasis: String, val correctsFindingId: String?,
)
@Serializable @JsonClassDiscriminator("operation") sealed interface CommandDto {
    val id: String
    @Serializable @SerialName("activate") data class Activate(override val id: String, val proposalId: String, val proposalVersion: Long, val proposalToken: String, val acceptPartial: Boolean): CommandDto {
        override fun toString() = "CommandDto(id=$id, proposalToken=[redacted])"
    }
    @Serializable @SerialName("replace") data class Replace(override val id: String, val watchId: String, val expectedVersion: Long, val proposalId: String, val proposalVersion: Long, val proposalToken: String, val acceptPartial: Boolean): CommandDto {
        override fun toString() = "CommandDto(id=$id, proposalToken=[redacted])"
    }
    @Serializable @SerialName("pause") data class Pause(override val id: String, val watchId: String, val expectedVersion: Long): CommandDto
    @Serializable @SerialName("resume") data class Resume(override val id: String, val watchId: String, val expectedVersion: Long): CommandDto
    @Serializable @SerialName("delete") data class Delete(override val id: String, val watchId: String, val expectedVersion: Long): CommandDto
}
@Serializable @JsonClassDiscriminator("state") sealed interface CommandReceiptDto {
    val id: String
    val updatedAt: String
    val remoteState: String
    @Serializable @SerialName("pending") data class Pending(override val id: String, override val updatedAt: String, override val remoteState: String, val watchId: String?): CommandReceiptDto
    @Serializable @SerialName("succeeded") data class Succeeded(override val id: String, override val updatedAt: String, override val remoteState: String, val watchId: String, val watchVersion: Long?, val deleted: Boolean): CommandReceiptDto {
        init { require(if (deleted) watchVersion == null else watchVersion != null && watchVersion > 0) }
    }
    @Serializable @SerialName("failed") data class Failed(override val id: String, override val updatedAt: String, override val remoteState: String, val watchId: String?, val error: ErrorDto): CommandReceiptDto
}
@Serializable data class WatchPageDto(val items: List<WatchDto>, val nextCursor: String?, val serverTime: String)
@Serializable data class FindingPageDto(val items: List<FindingDto>, val nextCursor: String?, val serverTime: String)
@Serializable data class DeviceRegistrationDto(val fcmToken: String, val language: String) {
    override fun toString() = "DeviceRegistrationDto(fcmToken=[redacted], language=$language)"
}
@Serializable data class AccountDto(val id: String, val language: String, val limits: LimitsDto)
@Serializable data class DeletionRequestDto(val id: String)
@Serializable data class DeletionReceiptDto(val id: String, val state: String, val accessRevokedAt: String, val description: String)
@Serializable data class HealthDto(val status: String, val apiVersion: String)
