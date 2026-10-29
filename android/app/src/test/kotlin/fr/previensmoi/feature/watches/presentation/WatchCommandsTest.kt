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
        val initial =
            Watch("w", Proposal("p", 1, RequestDraft("request"), Long.MAX_VALUE), baselineAt = 0)
        val items = MutableStateFlow(listOf(initial))
        val calls = mutableListOf<Command>()
        var reply = CompletableDeferred<Outcome<CommandReceipt>>()
        var refreshed: RefreshResult = RefreshResult.Success

        override fun observeList() = items

        override fun observeFindings() = flowOf(emptyList<Finding>())

        override suspend fun getFinding(id: String): Outcome<Finding> =
            Outcome.Error(Failure.NotFound)

        override suspend fun refresh() = refreshed

        override suspend fun execute(command: Command): Outcome<CommandReceipt> {
            calls += command
            return reply.await()
        }
    }

    @Test
    fun `mutation waits for service confirmation and transport retry keeps same identity`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val server = Server()
                var count = 0
                val vm = WatchesViewModel(server, server, IdGenerator { "c-${++count}" })
                runCurrent()
                vm.command(server.initial, Operation.Pause)
                vm.command(server.initial, Operation.Pause)
                runCurrent()
                assertEquals(1, server.calls.size)
                assertTrue(vm.state.value.commandPending)
                assertEquals(WatchStatus.Active, vm.state.value.watches.single().status)
                server.reply.complete(Outcome.Error(Failure.TemporaryFailure))
                runCurrent()
                assertFalse(vm.state.value.commandPending)
                assertEquals(server.initial, vm.state.value.watches.single())
                server.reply = CompletableDeferred()
                vm.retryCommand()
                runCurrent()
                assertEquals(server.calls[0], server.calls[1])
                server.items.value =
                    listOf(server.initial.copy(status = WatchStatus.Paused, version = 2))
                server.reply.complete(
                    Outcome.Success(CommandReceipt(server.calls.last().id, "w", 2)),
                )
                runCurrent()
                assertEquals(WatchStatus.Paused, vm.state.value.watches.single().status)
                assertNull(vm.state.value.commandError)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `conflict surfaces current state and requires new user command with current version`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val server = Server()
                var count = 0
                val vm = WatchesViewModel(server, server, IdGenerator { "c-${++count}" })
                runCurrent()
                vm.command(server.initial, Operation.Delete)
                runCurrent()
                server.items.value = listOf(server.initial.copy(version = 2))
                server.reply.complete(Outcome.Error(Failure.VersionConflict))
                runCurrent()
                assertEquals(Failure.VersionConflict, vm.state.value.commandError)
                assertNull(vm.state.value.deletedId)
                assertEquals(1, server.calls.size)
                server.reply = CompletableDeferred()
                vm.command(vm.state.value.watches.single(), Operation.Delete)
                runCurrent()
                assertNotEquals(server.calls[0].id, server.calls[1].id)
                assertEquals(2L, server.calls[1].expectedVersion)
                server.items.value = emptyList()
                server.reply.complete(
                    Outcome.Success(
                        CommandReceipt(server.calls.last().id, "w", null, deleted = true),
                    ),
                )
                runCurrent()
                assertEquals("w", vm.state.value.deletedId)
                assertTrue(vm.state.value.watches.isEmpty())
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `pending pause stays locked and automatically publishes confirmed state`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val initial = Server().initial
            val items = MutableStateFlow(listOf(initial))
            val calls = mutableListOf<Command>()
            var reads = 0
            val repository =
                object : WatchRepository, FindingRepository {
                    override fun observeList() = items

                    override fun observeFindings() = flowOf(emptyList<Finding>())

                    override suspend fun getFinding(id: String): Outcome<Finding> =
                        Outcome.Error(Failure.NotFound)

                    override suspend fun refresh(): RefreshResult {
                        reads++
                        items.value =
                            listOf(
                                initial.copy(
                                    status = WatchStatus.Paused,
                                    remoteState = RemoteState.Stopped,
                                    version = 2,
                                    nextCheck = null,
                                ),
                            )
                        return RefreshResult.Success
                    }

                    override suspend fun execute(command: Command): Outcome<CommandReceipt> {
                        calls += command
                        return if (calls.size == 1) Outcome.Error(Failure.OperationPending)
                        else Outcome.Success(CommandReceipt(command.id, "w", 2))
                    }
                }
            var ids = 0
            val vm = WatchesViewModel(repository, repository, IdGenerator { "c-${++ids}" })
            runCurrent()
            vm.command(initial, Operation.Pause)
            runCurrent()
            assertTrue(vm.state.value.commandPending)
            assertTrue(vm.state.value.commandUnconfirmed)
            vm.command(initial, Operation.Pause)
            vm.command(initial, Operation.Resume)
            runCurrent()
            assertEquals(1, calls.size)
            assertEquals(1, ids)
            advanceTimeBy(2_000)
            runCurrent()
            assertEquals(2, calls.size)
            assertEquals(calls[0], calls[1])
            assertEquals(1, reads)
            assertEquals(WatchStatus.Paused, vm.state.value.watches.single().status)
            assertFalse(vm.state.value.commandPending)
            assertFalse(vm.state.value.commandUnconfirmed)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `bounded receipt wait offers explicit verification without a new identifier`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val server = Server()
            server.reply.complete(Outcome.Error(Failure.OperationPending))
            val vm = WatchesViewModel(server, server, IdGenerator { "same-command" })
            runCurrent()
            vm.command(server.initial, Operation.Pause)
            runCurrent()
            advanceTimeBy(60_000)
            runCurrent()
            assertFalse(vm.state.value.commandPending)
            assertTrue(vm.state.value.commandUnconfirmed)
            assertEquals(31, server.calls.size)
            vm.command(server.initial, Operation.Pause)
            runCurrent()
            assertEquals(31, server.calls.size)
            server.reply = CompletableDeferred()
            server.reply.complete(Outcome.Success(CommandReceipt("same-command", "w", 2)))
            vm.retryCommand()
            runCurrent()
            assertEquals(32, server.calls.size)
            assertTrue(server.calls.all { it.id == "same-command" })
            assertFalse(vm.state.value.commandUnconfirmed)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `confirmed receipt with failed refresh stays locked until state can be read`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val server = Server()
            server.refreshed = RefreshResult.Offline
            server.reply.complete(Outcome.Success(CommandReceipt("same", "w", 2)))
            val vm = WatchesViewModel(server, server, IdGenerator { "same" })
            runCurrent()
            vm.command(server.initial, Operation.Pause)
            runCurrent()
            assertTrue(vm.state.value.commandUnconfirmed)
            assertTrue(vm.state.value.offline)
            vm.command(server.initial, Operation.Pause)
            runCurrent()
            assertEquals(1, server.calls.size)
            server.refreshed = RefreshResult.Success
            server.items.value =
                listOf(
                    server.initial.copy(
                        status = WatchStatus.Paused,
                        version = 2,
                        remoteState = RemoteState.Stopped,
                    ),
                )
            vm.retryCommand()
            runCurrent()
            assertEquals(2, server.calls.size)
            assertEquals(server.calls[0], server.calls[1])
            assertFalse(vm.state.value.commandUnconfirmed)
            assertFalse(vm.state.value.offline)
        } finally {
            Dispatchers.resetMain()
        }
    }
    @Test
    fun `refresh reconciles a failed resume after the bounded wait with the same command`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val server = Server()
            server.items.value = listOf(server.initial.copy(status = WatchStatus.Paused))
            server.reply.complete(Outcome.Error(Failure.OperationPending))
            val vm = WatchesViewModel(server, server, IdGenerator { "same-resume" })
            runCurrent()
            vm.command(server.items.value.single(), Operation.Resume)
            runCurrent()
            advanceTimeBy(60_000)
            runCurrent()
            assertTrue(vm.state.value.commandUnconfirmed)
            server.items.value = listOf(server.initial.copy(
                status = WatchStatus.Paused, health = Health.Degraded, version = 2,
                baselineAt = null, remoteState = RemoteState.Stopped,
                plan = server.initial.plan.copy(preparationFailure = "source_unusable"),
            ))
            server.reply = CompletableDeferred(Outcome.Error(Failure.OperationFailed))
            vm.refresh()
            runCurrent()
            assertFalse(vm.state.value.commandPending)
            assertFalse(vm.state.value.commandUnconfirmed)
            assertNull(vm.state.value.commandOperation)
            assertEquals(Failure.OperationFailed, vm.state.value.commandError)
            assertEquals("source_unusable", vm.state.value.watches.single().plan.preparationFailure)
            assertEquals(32, server.calls.size)
            assertTrue(server.calls.all { it.id == "same-resume" && it.operation == Operation.Resume })
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun `terminal failure settles during automatic polling without a notification`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val server = Server()
            server.reply.complete(Outcome.Error(Failure.OperationPending))
            val vm = WatchesViewModel(server, server, IdGenerator { "resume" })
            runCurrent()
            vm.command(server.initial.copy(status = WatchStatus.Paused), Operation.Resume)
            runCurrent()
            server.reply = CompletableDeferred(Outcome.Error(Failure.OperationFailed))
            advanceTimeBy(2_000)
            runCurrent()
            assertEquals(2,server.calls.size)
            assertEquals(Failure.OperationFailed,vm.state.value.commandError)
            assertFalse(vm.state.value.commandUnconfirmed)
            assertNull(vm.state.value.commandWatchId)
        } finally { Dispatchers.resetMain() }
    }

}
