package fr.previensmoi.domain.model

enum class SubscriptionStatus {
    None,
    Active,
    Cancelled,
    Grace,
    Hold,
    Paused,
    Expired,
    Pending,
}

data class SubscriptionAccess(
    val status: SubscriptionStatus = SubscriptionStatus.None,
    val active: Boolean = false,
    val expiresAt: Long? = null,
    val remainingCreations: Int = 5,
    val canPurchase: Boolean = false,
    val accountId: String = "",
    val trialEndsAt: Long? = null,
    val trialEnded: Boolean = false,
    val fromCache: Boolean = false,
)

data class SubscriptionProduct(val formattedPrice: String)

enum class BillingProblem {
    Unavailable,
    Offline,
    Cancelled,
    Pending,
    Verification,
    WrongAccount,
    InvalidPurchase,
}

sealed interface BillingOutcome<out T> {
    data class Success<T>(val value: T) : BillingOutcome<T>

    data class Error(val problem: BillingProblem) : BillingOutcome<Nothing>
}

data class PurchaseNotice(val token: String, val pending: Boolean, val accountId: String? = null) {
    override fun toString() = "PurchaseNotice([redacted])"
}

sealed interface StoreNotice {
    data class Purchase(val value: PurchaseNotice) : StoreNotice

    data class Problem(val problem: BillingProblem) : StoreNotice
}
