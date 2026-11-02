package fr.previensmoi.feature.watches.presentation

import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FindingLoadingTest {
    private class Server : WatchRepository, FindingRepository {
        val responses = mutableMapOf<String, CompletableDeferred<Outcome<Finding>>>()
        override fun observeList() = flowOf(emptyList<Watch>())
        override fun observeFindings() = flowOf(emptyList<Finding>())
        override suspend fun getFinding(id: String) = responses.getValue(id).await()
        override suspend fun refresh() = RefreshResult.Success
        override suspend fun execute(command: Command): Outcome<CommandReceipt> = error("No mutation")
        fun vm() = WatchesViewModel(this, this, IdGenerator { error("No command ID") })
    }
    @Test fun `loading lasts until response and missing is not confused with loading`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val server = Server(); server.responses["f"] = CompletableDeferred()
            val vm = server.vm(); vm.loadFinding("f"); runCurrent()
            assertTrue(vm.state.value.findingLoading); assertNull(vm.state.value.findingError)
            server.responses.getValue("f").complete(Outcome.Error(Failure.NotFound)); runCurrent()
            assertFalse(vm.state.value.findingLoading); assertEquals(Failure.NotFound,vm.state.value.findingError)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun `late response from an older notification cannot replace the requested result`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val server = Server(); server.responses["a"] = CompletableDeferred(); server.responses["b"] = CompletableDeferred()
            val vm = server.vm(); vm.loadFinding("a"); runCurrent(); vm.loadFinding("b"); runCurrent()
            val b = Finding("b","w","p",1,"PS5",1)
            server.responses.getValue("b").complete(Outcome.Success(b)); runCurrent()
            server.responses.getValue("a").complete(Outcome.Error(Failure.NotFound)); runCurrent()
            assertEquals("b",vm.state.value.findingRequestId); assertFalse(vm.state.value.findingLoading)
            assertNull(vm.state.value.findingError); assertEquals(listOf(b),vm.state.value.findings)
        } finally { Dispatchers.resetMain() }
    }
}
