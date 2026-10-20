package fr.previensmoi.feature.watches.presentation

import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WatchCommandsTest {
    private class Server : WatchRepository, FindingRepository {
        val initial = Watch("w", Proposal("p", 1, RequestDraft("request"), Long.MAX_VALUE), baselineAt = 0)
        val items = MutableStateFlow(listOf(initial))
        val calls = mutableListOf<Command>()
        var reply = CompletableDeferred<Outcome<CommandReceipt>>()
        override fun observeList() = items
        override fun observeFindings() = flowOf(emptyList<Finding>())
        override suspend fun getFinding(id: String): Outcome<Finding> = Outcome.Error(Failure.NotFound)
        override suspend fun refresh() = RefreshResult.Success
        override suspend fun execute(command: Command): Outcome<CommandReceipt> { calls += command; return reply.await() }
    }
    @Test fun `mutation waits for service confirmation and transport retry keeps same identity`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val server = Server(); var count = 0
            val vm = WatchesViewModel(server, server, IdGenerator { "c-${++count}" }); runCurrent()
            vm.command(server.initial, Operation.Pause); vm.command(server.initial, Operation.Pause); runCurrent()
            assertEquals(1, server.calls.size); assertTrue(vm.state.value.commandPending)
            assertEquals(WatchStatus.Active, vm.state.value.watches.single().status)
            server.reply.complete(Outcome.Error(Failure.TemporaryFailure)); runCurrent()
            assertFalse(vm.state.value.commandPending); assertEquals(server.initial, vm.state.value.watches.single())
            server.reply = CompletableDeferred(); vm.command(server.initial, Operation.Pause); runCurrent()
            assertEquals(server.calls[0], server.calls[1])
            server.items.value = listOf(server.initial.copy(status = WatchStatus.Paused, version = 2))
            server.reply.complete(Outcome.Success(CommandReceipt(server.calls.last().id, "w", 2))); runCurrent()
            assertEquals(WatchStatus.Paused, vm.state.value.watches.single().status); assertNull(vm.state.value.commandError)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun `conflict surfaces current state and requires new user command with current version`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val server = Server(); var count = 0
            val vm = WatchesViewModel(server, server, IdGenerator { "c-${++count}" }); runCurrent()
            vm.command(server.initial, Operation.Delete); runCurrent()
            server.items.value = listOf(server.initial.copy(version = 2))
            server.reply.complete(Outcome.Error(Failure.VersionConflict)); runCurrent()
            assertEquals(Failure.VersionConflict, vm.state.value.commandError); assertNull(vm.state.value.deletedId)
            assertEquals(1, server.calls.size)
            server.reply = CompletableDeferred(); vm.command(vm.state.value.watches.single(), Operation.Delete); runCurrent()
            assertNotEquals(server.calls[0].id, server.calls[1].id); assertEquals(2L, server.calls[1].expectedVersion)
            server.items.value = emptyList()
            server.reply.complete(Outcome.Success(CommandReceipt(server.calls.last().id, "w", null, deleted = true))); runCurrent()
            assertEquals("w", vm.state.value.deletedId); assertTrue(vm.state.value.watches.isEmpty())
        } finally { Dispatchers.resetMain() }
    }
}
