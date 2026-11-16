package fr.previensmoi.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import fr.previensmoi.BuildConfig
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.StoreBilling
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/** One BillingClient per application; callbacks carry no entitlement authority. */
class GooglePlayBilling(context: Context) : StoreBilling {
    private val events = MutableSharedFlow<StoreNotice>(extraBufferCapacity = 16)
    override val purchases = events.asSharedFlow()
    private val connection = Mutex()
    private var details: ProductDetails? = null
    private val client =
        BillingClient.newBuilder(context.applicationContext)
            .setListener { result, values ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK)
                    values
                        .orEmpty()
                        .filter { PRODUCT in it.products }
                        .forEach { events.tryEmit(StoreNotice.Purchase(notice(it))) }
                else events.tryEmit(StoreNotice.Problem(problem(result)))
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .enableAutoServiceReconnection()
            .build()

    private suspend fun connected(): Boolean = connection.withLock {
        if (client.isReady) return@withLock true
        suspendCancellableCoroutine { continuation ->
            client.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingServiceDisconnected() = Unit

                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (continuation.isActive)
                            continuation.resume(
                                result.responseCode == BillingClient.BillingResponseCode.OK
                            )
                    }
                }
            )
        }
    }

    override suspend fun product(): BillingOutcome<SubscriptionProduct> =
        bounded<SubscriptionProduct> {
            if (BuildConfig.APPLICATION_ID != BuildConfig.PLAY_PACKAGE_NAME || !connected())
                return@bounded BillingOutcome.Error(BillingProblem.Unavailable)
            suspendCancellableCoroutine { continuation ->
                val params =
                    QueryProductDetailsParams.newBuilder()
                        .setProductList(
                            listOf(
                                QueryProductDetailsParams.Product.newBuilder()
                                    .setProductId(PRODUCT)
                                    .setProductType(BillingClient.ProductType.SUBS)
                                    .build()
                            )
                        )
                        .build()
                client.queryProductDetailsAsync(params) { result, response ->
                    val found = response.productDetailsList.firstOrNull { it.productId == PRODUCT }
                    val offer =
                        found?.subscriptionOfferDetails?.firstOrNull {
                            it.basePlanId == BASE_PLAN && it.offerId == null
                        }
                    val phase =
                        offer?.pricingPhases?.pricingPhaseList?.singleOrNull {
                            it.billingPeriod == "P1M" &&
                                it.recurrenceMode ==
                                    ProductDetails.RecurrenceMode.INFINITE_RECURRING
                        }
                    val value =
                        if (
                            result.responseCode == BillingClient.BillingResponseCode.OK &&
                                phase != null
                        ) {
                            details = found
                            BillingOutcome.Success(SubscriptionProduct(phase.formattedPrice))
                        } else {
                            details = null
                            BillingOutcome.Error(BillingProblem.Unavailable)
                        }
                    if (continuation.isActive) continuation.resume(value)
                }
            }
        }

    fun launch(activity: Activity, accountId: String): BillingOutcome<Unit> {
        val product = details ?: return BillingOutcome.Error(BillingProblem.Unavailable)
        val offer =
            product.subscriptionOfferDetails?.firstOrNull {
                it.basePlanId == BASE_PLAN && it.offerId == null
            } ?: return BillingOutcome.Error(BillingProblem.Unavailable)
        if (!Regex("[a-f0-9]{64}").matches(accountId))
            return BillingOutcome.Error(BillingProblem.WrongAccount)
        val item =
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(product)
                .setOfferToken(offer.offerToken)
                .build()
        val result =
            client.launchBillingFlow(
                activity,
                BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(item))
                    .setObfuscatedAccountId(accountId)
                    .build(),
            )
        return if (result.responseCode == BillingClient.BillingResponseCode.OK)
            BillingOutcome.Success(Unit)
        else BillingOutcome.Error(problem(result))
    }

    override suspend fun restore(): BillingOutcome<List<PurchaseNotice>> =
        bounded<List<PurchaseNotice>> {
            if (BuildConfig.APPLICATION_ID != BuildConfig.PLAY_PACKAGE_NAME || !connected())
                return@bounded BillingOutcome.Error(BillingProblem.Unavailable)
            suspendCancellableCoroutine { continuation ->
                client.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                ) { result, values ->
                    if (continuation.isActive)
                        continuation.resume(
                            if (result.responseCode == BillingClient.BillingResponseCode.OK)
                                BillingOutcome.Success(
                                    values.filter { PRODUCT in it.products }.map(::notice)
                                )
                            else BillingOutcome.Error(problem(result))
                        )
                }
            }
        }

    private suspend fun <T> bounded(block: suspend () -> BillingOutcome<T>): BillingOutcome<T> =
        withTimeoutOrNull(15_000) {
            try {
                block()
            } catch (_: IllegalStateException) {
                BillingOutcome.Error(BillingProblem.Unavailable)
            }
        } ?: BillingOutcome.Error(BillingProblem.Unavailable)

    private fun notice(purchase: Purchase) =
        PurchaseNotice(
            purchase.purchaseToken,
            purchase.purchaseState == Purchase.PurchaseState.PENDING,
            purchase.accountIdentifiers?.obfuscatedAccountId,
        )

    private fun problem(result: BillingResult) =
        when (result.responseCode) {
            BillingClient.BillingResponseCode.USER_CANCELED -> BillingProblem.Cancelled
            BillingClient.BillingResponseCode.NETWORK_ERROR -> BillingProblem.Offline
            else -> BillingProblem.Unavailable
        }

    companion object {
        const val PRODUCT = "previens_moi_plus"
        const val BASE_PLAN = "monthly"
    }
}
