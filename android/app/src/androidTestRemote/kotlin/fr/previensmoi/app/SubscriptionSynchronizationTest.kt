package fr.previensmoi.app

import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.*
import fr.previensmoi.feature.subscription.*
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SubscriptionSynchronizationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun expiryRefreshesWithoutTapAndBackgroundCancelsReads() {
        val reads = AtomicInteger()
        var access =
            SubscriptionAccess(
                SubscriptionStatus.Active,
                active = true,
                expiresAt = System.currentTimeMillis() + 2000,
                canPurchase = false,
            )
        val repo =
            object : SubscriptionRepository {
                override suspend fun access(): BillingOutcome<SubscriptionAccess> {
                    reads.incrementAndGet()
                    return BillingOutcome.Success(access)
                }

                override suspend fun verify(token: String): BillingOutcome<SubscriptionAccess> =
                    error("No purchase")
            }
        val store =
            object : StoreBilling {
                override val purchases = emptyFlow<StoreNotice>()

                override suspend fun product(): BillingOutcome<SubscriptionProduct> =
                    error("No catalog needed")

                override suspend fun restore(): BillingOutcome<List<PurchaseNotice>> =
                    error("No purchase")
            }
        compose.runOnUiThread {
            val vm = SubscriptionViewModel(repo, store)
            compose.activity.setContent {
                SubscriptionSynchronizationEffect(vm, "synthetic-owner")
                val state by vm.state.collectAsStateWithLifecycle()
                Text(state.access?.status?.name ?: "Loading", Modifier.testTag("membership_status"))
            }
        }
        compose.waitUntil(5000) { reads.get() > 0 }
        compose.onNodeWithTag("membership_status").assertTextEquals("Active")
        compose.runOnUiThread {
            access = access.copy(status = SubscriptionStatus.Expired, active = false)
        }
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Expired").fetchSemanticsNodes().isNotEmpty()
        }
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        val stopped = reads.get()
        Thread.sleep(1200)
        assertEquals(stopped, reads.get())
        compose.runOnUiThread {
            access =
                access.copy(
                    status = SubscriptionStatus.Active,
                    active = true,
                    expiresAt = System.currentTimeMillis() + 600_000,
                )
        }
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitUntil(5000) { reads.get() > stopped }
        compose.onNodeWithTag("membership_status").assertTextEquals("Active")
    }
}
