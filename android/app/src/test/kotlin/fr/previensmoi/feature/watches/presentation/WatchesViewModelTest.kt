package fr.previensmoi.feature.watches.presentation

import app.cash.turbine.test
import fr.previensmoi.domain.model.RefreshResult
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WatchesViewModelTest {
    companion object {
        fun watch(id: String) = Watch(id, Proposal("p", 1, RequestDraft("example"), Long.MAX_VALUE), baselineAt = 0)
    }
    private class ControlledRepository : WatchRepository {
        val items = MutableStateFlow(listOf(watch("existing")))
        val response = CompletableDeferred<RefreshResult>()
        var calls = 0
        override fun observeList() = items
        override suspend fun execute(command: Command): Outcome<CommandReceipt> = error("Not used by refresh tests")
        override suspend fun refresh(): RefreshResult { calls++; return response.await() }
    }

    @Test fun `failed refresh retains confirmed data and ignores double tap`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = ControlledRepository()
            val vm = WatchesViewModel(repository, object : FindingRepository {
                override fun observeFindings() = flowOf(emptyList<Finding>())
                override suspend fun getFinding(id: String): Outcome<Finding> = Outcome.Error(Failure.NotFound)
            }, IdGenerator { "test-command" })
            runCurrent()
            vm.state.test {
                assertEquals(listOf(watch("existing")), awaitItem().watches)
                vm.refresh()
                vm.refresh()
                assertTrue(awaitItem().refreshing)
                runCurrent()
                assertEquals(1, repository.calls)
                repository.response.complete(RefreshResult.TemporaryFailure)
                runCurrent()
                val state = expectMostRecentItem()
                assertTrue(state.refreshFailed)
                assertFalse(state.refreshing)
                assertEquals(listOf(watch("existing")), state.watches)
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `successful refresh exposes repository data`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = ControlledRepository()
            val vm = WatchesViewModel(repository, object : FindingRepository {
                override fun observeFindings() = flowOf(emptyList<Finding>())
                override suspend fun getFinding(id: String): Outcome<Finding> = Outcome.Error(Failure.NotFound)
            }, IdGenerator { "test-command" })
            vm.refresh()
            runCurrent()
            repository.items.value = listOf(watch("new"))
            repository.response.complete(RefreshResult.Success)
            runCurrent()
            assertEquals(listOf(watch("new")), vm.state.value.watches)
            assertFalse(vm.state.value.refreshing)
            assertFalse(vm.state.value.refreshFailed)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun `initial failure stays unknown and does not claim an empty account`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = ControlledRepository().also { it.items.value = emptyList() }
            val vm = WatchesViewModel(repository, object : FindingRepository {
                override fun observeFindings() = flowOf(emptyList<Finding>())
                override suspend fun getFinding(id: String): Outcome<Finding> = Outcome.Error(Failure.NotFound)
            }, IdGenerator { "command" })
            vm.setOwner("owner"); vm.refresh(); runCurrent()
            assertFalse(vm.state.value.hasLoaded)
            repository.response.complete(RefreshResult.Offline); runCurrent()
            assertFalse(vm.state.value.hasLoaded)
            assertTrue(vm.state.value.offline)
            assertTrue(vm.state.value.refreshFailed)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `owner change discards late read outcome`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = ControlledRepository()
            val vm = WatchesViewModel(repository, object : FindingRepository {
                override fun observeFindings() = flowOf(emptyList<Finding>())
                override suspend fun getFinding(id: String): Outcome<Finding> = Outcome.Error(Failure.NotFound)
            }, IdGenerator { "command" })
            runCurrent(); vm.setOwner("old"); vm.refresh(); runCurrent()
            vm.setOwner(null)
            repository.response.complete(RefreshResult.Success); runCurrent()
            assertFalse(vm.state.value.hasLoaded)
            assertTrue(vm.state.value.watches.isEmpty())
        } finally { Dispatchers.resetMain() }
    }

}
