package fr.previensmoi.feature.subscription

import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionViewModelTest {
    private class Store : StoreBilling {
        val events = MutableSharedFlow<StoreNotice>()
        override val purchases = events.asSharedFlow()
        var owned: BillingOutcome<List<PurchaseNotice>> = BillingOutcome.Success(emptyList())

        override suspend fun product() = BillingOutcome.Success(SubscriptionProduct("6,99 €"))

        override suspend fun restore() = owned
    }

    private class Server : SubscriptionRepository {
        var access = SubscriptionAccess(canPurchase = true, accountId = "a".repeat(64))
        var gate: CompletableDeferred<BillingOutcome<SubscriptionAccess>>? = null
        val verified = mutableListOf<String>()
        var accessFailure: BillingProblem? = null

        override suspend fun access(): BillingOutcome<SubscriptionAccess> =
            accessFailure?.let { BillingOutcome.Error(it) } ?: BillingOutcome.Success(access)

        override suspend fun verify(token: String): BillingOutcome<SubscriptionAccess> {
            verified += token
            return (gate?.await()
                    ?: BillingOutcome.Success(
                        access.copy(status = SubscriptionStatus.Active, active = true)
                    ))
                .also { result -> if (result is BillingOutcome.Success) access = result.value }
        }
    }

    @Test
    fun `purchase stays unentitled until server confirms and second tap is ignored`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val server = Server()
            val store = Store()
            val vm = SubscriptionViewModel(server, store)
            vm.bindOwner("u")
            runCurrent()
            assertNotNull(vm.beginPurchase())
            assertNull(vm.beginPurchase())
            server.gate = CompletableDeferred()
            store.events.emit(
                StoreNotice.Purchase(PurchaseNotice("synthetic-token", false, "a".repeat(64)))
            )
            runCurrent()
            assertFalse(vm.state.value.access!!.active)
            assertTrue(vm.state.value.busy)
            server.gate!!.complete(
                BillingOutcome.Success(
                    server.access.copy(status = SubscriptionStatus.Active, active = true)
                )
            )
            runCurrent()
            assertTrue(vm.state.value.access!!.active)
            assertFalse(vm.state.value.busy)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `pending and cancellation never verify or change entitlement`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val server = Server()
            val store = Store()
            val vm = SubscriptionViewModel(server, store)
            vm.bindOwner("u")
            runCurrent()
            vm.beginPurchase()
            store.events.emit(StoreNotice.Purchase(PurchaseNotice("synthetic-token", true)))
            runCurrent()
            assertEquals(BillingProblem.Pending, vm.state.value.problem)
            assertFalse(vm.state.value.access!!.active)
            vm.refresh()
            runCurrent()
            vm.beginPurchase()
            store.events.emit(StoreNotice.Problem(BillingProblem.Cancelled))
            runCurrent()
            assertEquals(BillingProblem.Cancelled, vm.state.value.problem)
            assertTrue(server.verified.isEmpty())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `restoration recovers purchase when original callback was lost`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val server = Server()
            val store = Store()
            store.owned = BillingOutcome.Success(listOf(PurchaseNotice("synthetic-token", false)))
            val vm = SubscriptionViewModel(server, store)
            vm.bindOwner("u")
            runCurrent()
            assertTrue(
                vm.state.value.access!!.active
            ) // Cold-start restoration did not need the lost callback.
            vm.restore()
            runCurrent()
            assertTrue(server.verified.isNotEmpty())
            assertTrue(server.verified.all { it == "synthetic-token" })
            assertTrue(vm.state.value.restored)
            assertTrue(vm.state.value.access!!.active)
            store.owned = BillingOutcome.Error(BillingProblem.Offline)
            vm.restore()
            runCurrent()
            assertTrue(vm.state.value.access!!.active)
            assertEquals(BillingProblem.Offline, vm.state.value.problem)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `old account response and mismatched purchase cannot update new session`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val server = Server()
            val store = Store()
            val vm = SubscriptionViewModel(server, store)
            vm.bindOwner("u")
            runCurrent()
            server.gate = CompletableDeferred()
            store.events.emit(
                StoreNotice.Purchase(PurchaseNotice("synthetic-token", false, "a".repeat(64)))
            )
            runCurrent()
            vm.bindOwner(null)
            server.gate!!.complete(BillingOutcome.Success(server.access.copy(active = true)))
            runCurrent()
            assertNull(vm.state.value.access)
            server.access =
                server.access.copy(
                    accountId = "b".repeat(64),
                    active = false,
                    status = SubscriptionStatus.None,
                )
            vm.bindOwner("other")
            runCurrent()
            store.events.emit(
                StoreNotice.Purchase(PurchaseNotice("other-token", false, "a".repeat(64)))
            )
            runCurrent()
            assertFalse(vm.state.value.access!!.active)
            assertEquals(1, server.verified.size)
            assertEquals(BillingProblem.WrongAccount, vm.state.value.problem)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `pending existing purchase cannot launch a second checkout and tokens stay redacted`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val server = Server()
                server.access = server.access.copy(status = SubscriptionStatus.Pending)
                val vm = SubscriptionViewModel(server, Store())
                vm.bindOwner("u")
                runCurrent()
                assertNull(vm.beginPurchase())
                assertFalse(
                    PurchaseNotice("very-private-token", false)
                        .toString()
                        .contains("very-private-token")
                )
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `renewing purchase never publishes an expired offer before Google verification finishes`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val server = Server()
                server.access =
                    server.access.copy(status = SubscriptionStatus.Active, active = true)
                val store = Store()
                val vm = SubscriptionViewModel(server, store)
                vm.bindOwner("u")
                runCurrent()
                server.access =
                    server.access.copy(status = SubscriptionStatus.Expired, active = false)
                store.owned =
                    BillingOutcome.Success(listOf(PurchaseNotice("renewing-test-token", false)))
                server.gate = CompletableDeferred()
                vm.refresh()
                runCurrent()
                assertTrue(vm.state.value.busy)
                assertTrue(vm.state.value.access!!.active)
                assertNull(vm.beginPurchase())
                server.gate!!.complete(
                    BillingOutcome.Success(
                        server.access.copy(status = SubscriptionStatus.Active, active = true)
                    )
                )
                runCurrent()
                assertTrue(vm.state.value.access!!.active)
                assertFalse(vm.state.value.busy)
                assertNull(vm.beginPurchase())
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `failed renewal verification cannot enable a new purchase`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val server = Server()
            val store = Store()
            val vm = SubscriptionViewModel(server, store)
            vm.bindOwner("u")
            runCurrent()
            server.access = server.access.copy(status = SubscriptionStatus.Expired)
            store.owned =
                BillingOutcome.Success(listOf(PurchaseNotice("renewing-test-token", false)))
            server.gate = CompletableDeferred(BillingOutcome.Error(BillingProblem.Offline))
            vm.refresh()
            runCurrent()
            assertEquals(BillingProblem.Offline, vm.state.value.problem)
            assertNull(vm.beginPurchase())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `server failure is never announced as a successful restoration`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val server = Server()
            val vm = SubscriptionViewModel(server, Store())
            vm.bindOwner("u")
            runCurrent()
            server.accessFailure = BillingProblem.Offline
            vm.restore()
            runCurrent()
            assertFalse(vm.state.value.restored)
            assertEquals(BillingProblem.Offline, vm.state.value.problem)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
