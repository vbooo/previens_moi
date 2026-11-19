package fr.previensmoi.feature.support

import androidx.lifecycle.SavedStateHandle
import fr.previensmoi.data.remote.ApiClient
import fr.previensmoi.data.remote.RemoteSupportRepository
import fr.previensmoi.data.remote.SupportRequestDto
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import mockwebserver3.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SupportViewModelTest {
    private fun response(body: String, code: Int = 202) =
        MockResponse.Builder()
            .code(code)
            .body(body)
            .addHeader("Content-Type", "application/json")
            .build()

    @Test
    fun `blank or invalid email stays local and closing preserves the draft`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            MockWebServer().use { server ->
                server.start()
                val saved = SavedStateHandle()
                val vm =
                    SupportViewModel(
                        RemoteSupportRepository(
                            ApiClient.create(server.url("/functions/v1/"), true),
                        ),
                        saved,
                    )
                vm.open()
                vm.send("0.2.4")
                runCurrent()
                assertEquals(SupportProblem.Invalid, vm.state.value.problem)
                vm.message("A synthetic question")
                vm.email("bad\nmail")
                vm.send("0.2.4")
                runCurrent()
                assertEquals(0, server.requestCount)
                vm.category("idea")
                vm.email("reply@example.test")
                vm.close()
                val restored =
                    SupportViewModel(
                        RemoteSupportRepository(
                            ApiClient.create(server.url("/functions/v1/"), true),
                        ),
                        saved,
                    )
                restored.open()
                assertEquals("A synthetic question", restored.state.value.message)
                assertEquals("idea", restored.state.value.category)
                assertEquals("reply@example.test", restored.state.value.email)
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `uncertain retry reuses identity and acknowledged success clears private draft`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                MockWebServer().use { server ->
                    server.start()
                    val saved = SavedStateHandle()
                    val vm =
                        SupportViewModel(
                            RemoteSupportRepository(
                                ApiClient.create(server.url("/functions/v1/"), true),
                            ),
                            saved,
                        )
                    vm.open()
                    vm.message("Synthetic support test")
                    vm.email("reply@example.test")
                    server.enqueue(response("{}", 503))
                    vm.send("0.2.4")
                    vm.send("0.2.4")
                    withContext(Dispatchers.IO) {
                        while (vm.state.value.busy) {
                            delay(10)
                            testScheduler.runCurrent()
                        }
                    }
                    val first = server.takeRequest(3, TimeUnit.SECONDS)!!
                    val payload = Json.parseToJsonElement(first.body!!.utf8()).jsonObject
                    val id = payload.getValue("id").jsonPrimitive.content
                    assertFalse(vm.state.value.received)
                    assertEquals("Synthetic support test", vm.state.value.message)
                    assertEquals(1, server.requestCount)
                    server.enqueue(response("""{"id":"$id","state":"received"}"""))
                    vm.send("0.2.5")
                    withContext(Dispatchers.IO) {
                        while (vm.state.value.busy) {
                            delay(10)
                            testScheduler.runCurrent()
                        }
                    }
                    val retry = server.takeRequest(3, TimeUnit.SECONDS)!!
                    assertEquals(payload, Json.parseToJsonElement(retry.body!!.utf8()).jsonObject)
                    assertEquals("/functions/v1/public/v1/support", first.url.encodedPath)
                    assertNull(first.headers["Authorization"])
                    assertTrue(vm.state.value.received)
                    assertEquals("", vm.state.value.message)
                    assertEquals("", vm.state.value.email)
                    assertFalse(
                        SupportRequestDto(
                                id,
                                "question",
                                "private text",
                                "private@example.test",
                                "0.2.4",
                                "fr",
                            )
                            .toString()
                            .contains("private"),
                    )
                    vm.send("0.2.4")
                    runCurrent()
                    assertEquals(2, server.requestCount)
                    vm.done()
                    assertFalse(vm.state.value.opened)
                }
            } finally {
                Dispatchers.resetMain()
            }
        }
}
