package fr.previensmoi.data.billing

import fr.previensmoi.data.auth.RemoteSessionRepository
import fr.previensmoi.data.remote.*
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.SubscriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class RemoteSubscriptionRepository(
    private val api: ApplicationApi,
    private val sessions: RemoteSessionRepository,
    private val cache: SubscriptionCache,
    private val clock: Clock,
) : SubscriptionRepository {
    override suspend fun access() = request(allowCache = true) { api.subscription(it) }

    override suspend fun check() = request(allowCache = false) { api.checkSubscription(it) }

    // A local snapshot must never validate a new purchase.
    override suspend fun verify(token: String) =
        request(allowCache = false) {
            api.verifyPurchase(it, PurchaseVerificationDto(token))
        }

    private suspend fun request(
        allowCache: Boolean,
        call: suspend (String) -> Response<SubscriptionAccessDto>,
    ): BillingOutcome<SubscriptionAccess> {
        val owner =
            sessions.currentUserId() ?: return BillingOutcome.Error(BillingProblem.WrongAccount)
        val bearer = sessions.bearer() ?: return unavailable(owner, allowCache)
        var result = apiCall { call(bearer) }
        if (result is ApiResult.Rejected && result.httpStatus == 401) {
            val refreshed = sessions.bearer(bearer) ?: return unavailable(owner, allowCache)
            result = apiCall { call(refreshed) }
        }
        if (sessions.currentUserId() != owner)
            return BillingOutcome.Error(BillingProblem.WrongAccount)
        return when (val value = result) {
            is ApiResult.Value -> {
                val access =
                    runCatching { decode(value.body) }.getOrNull()
                        ?: return BillingOutcome.Error(BillingProblem.Verification)
                withContext(Dispatchers.IO) {
                    // Storage failure cannot turn an already verified purchase into a rejection.
                    runCatching {
                        cache.write(SubscriptionSnapshot(owner, clock.now(), value.body))
                    }
                        .onFailure { runCatching { cache.clear() } }
                }
                if (sessions.currentUserId() != owner)
                    BillingOutcome.Error(BillingProblem.WrongAccount)
                else BillingOutcome.Success(access)
            }
            is ApiResult.ConnectionFailure -> unavailable(owner, allowCache)
            is ApiResult.Rejected ->
                BillingOutcome.Error(
                    when (value.error?.code) {
                        "purchase_account_mismatch",
                        "session_required" -> BillingProblem.WrongAccount
                        "purchase_invalid" -> BillingProblem.InvalidPurchase
                        else -> BillingProblem.Verification
                    },
                )
            else -> BillingOutcome.Error(BillingProblem.Verification)
        }
    }

    private suspend fun unavailable(
        owner: String,
        allowCache: Boolean,
    ): BillingOutcome<SubscriptionAccess> {
        if (sessions.currentUserId() != owner)
            return BillingOutcome.Error(BillingProblem.WrongAccount)
        val access = if (allowCache) cached(owner) else null
        if (sessions.currentUserId() != owner)
            return BillingOutcome.Error(BillingProblem.WrongAccount)
        return access?.let { BillingOutcome.Success(it) }
            ?: BillingOutcome.Error(BillingProblem.Offline)
    }

    private suspend fun cached(owner: String): SubscriptionAccess? =
        withContext(Dispatchers.IO) {
            try {
                val snapshot = cache.read() ?: return@withContext null
                val now = clock.now()
                val access = decode(snapshot.access)
                // No indefinite entitlement, no clock rollback, no reuse by another app account.
                if (
                    snapshot.owner != owner ||
                        now < snapshot.verifiedAt ||
                        (access.active && (access.expiresAt ?: 0L) <= now) ||
                        (!access.active && now - snapshot.verifiedAt > 86_400_000L)
                ) {
                    cache.clear()
                    null
                } else access.copy(canPurchase = false, fromCache = true)
            } catch (_: Exception) {
                runCatching { cache.clear() }
                null
            }
        }

    private fun decode(dto: SubscriptionAccessDto): SubscriptionAccess {
        require(
            dto.packageName == "fr.previensmoi.app" &&
                dto.productId == "previens_moi_plus" &&
                dto.basePlanId == "monthly" &&
                dto.remainingCreations in 0..5 &&
                Regex("[a-f0-9]{64}").matches(dto.accountId),
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
                    ) && dto.expiresAt != null),
        )
        return SubscriptionAccess(
            status,
            dto.active,
            dto.expiresAt?.let(::instant),
            dto.remainingCreations,
            dto.canPurchase,
            dto.accountId,
            dto.trialEndsAt?.let(::instant),
            dto.trialEnded,
        )
    }
}
