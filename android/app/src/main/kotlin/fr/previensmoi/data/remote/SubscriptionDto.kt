package fr.previensmoi.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class PurchaseVerificationDto(val purchaseToken: String) {
    override fun toString() = "PurchaseVerificationDto([redacted])"
}

@Serializable
data class SubscriptionAccessDto(
    val status: String,
    val active: Boolean,
    val expiresAt: String?,
    val remainingCreations: Int,
    val canPurchase: Boolean,
    val accountId: String,
    val trialEndsAt: String?,
    val trialEnded: Boolean,
    val packageName: String,
    val productId: String,
    val basePlanId: String,
)
