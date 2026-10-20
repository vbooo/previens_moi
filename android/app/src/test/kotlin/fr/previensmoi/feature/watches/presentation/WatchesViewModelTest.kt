package fr.previensmoi.feature.watches.presentation

import app.cash.turbine.test
import fr.previensmoi.domain.model.RefreshResult
import fr.previensmoi.domain.model.Watch
import fr.previensmoi.domain.repository.WatchRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WatchesViewModelTest {
    private class ControlledRepository : WatchRepository {
        val items = MutableStateFlow(listOf(Watch("existing")))
        val response = CompletableDeferred<RefreshResult>()
        var calls = 0
        override fun observeList() = items
        override suspend fun refresh(): RefreshResult { calls++; return response.await() }
    }

    @Test fun `failed refresh retains confirmed data and ignores double tap`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = ControlledRepository()
            val vm = WatchesViewModel(repository)
            runCurrent()
            vm.state.test {
                assertEquals(listOf(Watch("existing")), awaitItem().watches)
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
                assertEquals(listOf(Watch("existing")), state.watches)
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `successful refresh exposes repository data`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = ControlledRepository()
            val vm = WatchesViewModel(repository)
            vm.refresh()
            runCurrent()
            repository.items.value = listOf(Watch("new"))
            repository.response.complete(RefreshResult.Success)
            runCurrent()
            assertEquals(listOf(Watch("new")), vm.state.value.watches)
            assertFalse(vm.state.value.refreshing)
            assertFalse(vm.state.value.refreshFailed)
        } finally { Dispatchers.resetMain() }
    }
}
