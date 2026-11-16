package fr.previensmoi.data.billing

import fr.previensmoi.data.auth.RemoteSessionRepository
import fr.previensmoi.data.remote.*
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.SubscriptionRepository
import retrofit2.Response

class RemoteSubscriptionRepository(
    private val api: ApplicationApi,
    private val sessions: RemoteSessionRepository,
) : SubscriptionRepository {
    override suspend fun access() = request { api.subscription(it) }

    override suspend fun verify(token: String) = request {
        api.verifyPurchase(it, PurchaseVerificationDto(token))
    }

    private suspend fun request(
        call: suspend (String) -> Response<SubscriptionAccessDto>
    ): BillingOutcome<SubscriptionAccess> {
        val owner =
            sessions.currentUserId() ?: return BillingOutcome.Error(BillingProblem.WrongAccount)
        val bearer = sessions.bearer() ?: return BillingOutcome.Error(BillingProblem.WrongAccount)
        var result = apiCall { call(bearer) }
        if (result is ApiResult.Rejected && result.httpStatus == 401) {
            val refreshed =
                sessions.bearer(bearer) ?: return BillingOutcome.Error(BillingProblem.WrongAccount)
            result = apiCall { call(refreshed) }
        }
        if (sessions.currentUserId() != owner)
            return BillingOutcome.Error(BillingProblem.WrongAccount)
        return when (val value = result) {
            is ApiResult.Value ->
                try {
                    val dto = value.body
                    require(
                        dto.packageName == "fr.previensmoi.app" &&
                            dto.productId == "previens_moi_plus" &&
                            dto.basePlanId == "monthly" &&
                            dto.remainingCreations in 0..5 &&
                            Regex("[a-f0-9]{64}").matches(dto.accountId)
                    )
                    val status =
                        when (dto.status) {
                            "none" -> SubscriptionStatus.None
                            "active" -> SubscriptionStatus.Active
                            "cancelled" -> SubscriptionStatus.Cancelled
                            "grace" -> SubscriptionStatus.Grace
                            "hold" -> SubscriptionStatus.Hold
                            "paused" -> SubscriptionStatus.Paused
                            "expired" -> SubscriptionStatus.Expired
                            "pending" -> SubscriptionStatus.Pending
                            else -> throw IllegalArgumentException()
                        }
                    require(
                        !dto.active ||
                            (status in
                                setOf(
                                    SubscriptionStatus.Active,
                                    SubscriptionStatus.Cancelled,
                                    SubscriptionStatus.Grace,
                                ) && dto.expiresAt != null)
                    )
                    BillingOutcome.Success(
                        SubscriptionAccess(
                            status,
                            dto.active,
                            dto.expiresAt?.let(::instant),
                            dto.remainingCreations,
                            dto.canPurchase,
                            dto.accountId,
                            dto.trialEndsAt?.let(::instant),
                            dto.trialEnded,
                        )
                    )
                } catch (_: Exception) {
                    BillingOutcome.Error(BillingProblem.Verification)
                }
            is ApiResult.ConnectionFailure -> BillingOutcome.Error(BillingProblem.Offline)
            is ApiResult.Rejected ->
                BillingOutcome.Error(
                    when (value.error?.code) {
                        "purchase_account_mismatch",
                        "session_required" -> BillingProblem.WrongAccount
                        "purchase_invalid" -> BillingProblem.InvalidPurchase
                        else -> BillingProblem.Verification
                    }
                )
            else -> BillingOutcome.Error(BillingProblem.Verification)
        }
    }
}
