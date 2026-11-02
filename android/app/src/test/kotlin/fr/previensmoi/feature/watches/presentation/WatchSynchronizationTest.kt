package fr.previensmoi.feature.watches.presentation

import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WatchSynchronizationTest {
    private class Service : WatchRepository, FindingRepository {
        val plan = Proposal("p", 2, RequestDraft("Android Montpellier"), Long.MAX_VALUE, backgroundPreparation = true)
        val pending = Watch("w", plan, status = WatchStatus.Pending, baselineAt = null, remoteState = RemoteState.Starting)
        val items = MutableStateFlow(listOf(pending))
        val results = MutableStateFlow(emptyList<Finding>())
        var reads = 0
        var commands = 0
        var onRead: suspend () -> RefreshResult = { RefreshResult.Success }
        override fun observeList() = items
        override fun observeFindings() = results
        override suspend fun getFinding(id: String): Outcome<Finding> = Outcome.Error(Failure.NotFound)
        override suspend fun execute(command: Command): Outcome<CommandReceipt> { commands++; error("Automatic reads must never submit a command") }
        override suspend fun refresh(): RefreshResult { reads++; return onRead() }
        fun vm() = WatchesViewModel(this, this, IdGenerator { error("No new command identifier") })
    }

    @Test fun `preparation becomes completed with its result without refresh tap or push`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val service = Service(); val vm = service.vm(); runCurrent()
            val job = launch { vm.synchronizeWhileVisible() }; runCurrent()
            assertEquals(1, service.reads)
            service.onRead = {
                service.items.value = listOf(service.pending.copy(status = WatchStatus.Completed, baselineAt = 1, remoteState = RemoteState.Stopped))
                service.results.value = listOf(Finding("f", "w", "p", 2, "Android Montpellier", 1, noveltyBasis = "initial"))
                RefreshResult.Success
            }
            advanceTimeBy(3_000); runCurrent()
            assertEquals(WatchStatus.Completed, vm.state.value.watches.single().status)
            assertEquals("f", vm.state.value.findings.single().id)
            assertFalse(vm.state.value.refreshing)
            assertEquals(0, service.commands)
            job.cancelAndJoin()
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `a new preparation wakes the settled foreground reader`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val service = Service(); service.items.value = emptyList()
            val vm = service.vm(); runCurrent()
            val job = launch { vm.synchronizeWhileVisible() }; runCurrent()
            assertEquals(1, service.reads)
            service.items.value = listOf(service.pending); runCurrent()
            assertEquals(2, service.reads)
            job.cancelAndJoin()
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `foreground reader cancels in flight and resumes immediately`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val service = Service(); var cancelled = false
            service.onRead = { try { awaitCancellation() } finally { cancelled = true } }
            val vm = service.vm(); runCurrent()
            val job = launch { vm.synchronizeWhileVisible() }; runCurrent()
            vm.refresh(); runCurrent(); assertEquals(1, service.reads)
            job.cancelAndJoin(); assertTrue(cancelled)
            advanceTimeBy(120_000); runCurrent(); assertEquals(1, service.reads)
            service.onRead = { RefreshResult.Success }
            val resumed = launch { vm.synchronizeWhileVisible() }; runCurrent()
            assertEquals(2, service.reads); assertFalse(vm.state.value.refreshFailed)
            resumed.cancelAndJoin()
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `offline refresh backs off preserves content and recovers without a tap`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val service = Service(); service.onRead = { RefreshResult.Offline }
            val vm = service.vm(); runCurrent()
            val job = launch { vm.synchronizeWhileVisible() }; runCurrent()
            assertTrue(vm.state.value.offline)
            assertEquals(service.pending, vm.state.value.watches.single())
            advanceTimeBy(29_999); runCurrent(); assertEquals(1, service.reads)
            service.onRead = { RefreshResult.Success }
            advanceTimeBy(1); runCurrent(); assertEquals(2, service.reads)
            assertFalse(vm.state.value.offline); assertFalse(vm.state.value.refreshFailed)
            job.cancelAndJoin()
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `settled foreground follows later updates without keeping fast polling`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val service = Service(); service.items.value = listOf(service.pending.copy(status = WatchStatus.Active, remoteState = RemoteState.Running, baselineAt = 0))
            val vm = service.vm(); runCurrent()
            val job = launch { vm.synchronizeWhileVisible() }; runCurrent()
            advanceTimeBy(29_999); runCurrent(); assertEquals(1, service.reads)
            advanceTimeBy(1); runCurrent(); assertEquals(2, service.reads)
            job.cancelAndJoin()
        } finally { Dispatchers.resetMain() }
    }
}
