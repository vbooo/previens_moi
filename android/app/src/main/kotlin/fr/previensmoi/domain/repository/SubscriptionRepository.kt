package fr.previensmoi.domain.repository

import fr.previensmoi.domain.model.*
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    suspend fun access(): BillingOutcome<SubscriptionAccess>

    suspend fun check(): BillingOutcome<SubscriptionAccess> = access()

    suspend fun verify(token: String): BillingOutcome<SubscriptionAccess>
}

/** The Android BillingClient lives behind this boundary, never in the domain. */
interface StoreBilling {
    val purchases: Flow<StoreNotice>

    suspend fun product(): BillingOutcome<SubscriptionProduct>

    suspend fun restore(): BillingOutcome<List<PurchaseNotice>>
}
