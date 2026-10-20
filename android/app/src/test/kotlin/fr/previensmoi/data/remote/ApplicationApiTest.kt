package fr.previensmoi.data.remote

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class ApplicationApiTest {
    private val json = ApiClient.json
    private val fixtures: JsonArray by lazy {
        json.parseToJsonElement(requireNotNull(javaClass.getResourceAsStream("/application-v1.examples.json")).bufferedReader().use { it.readText() }).jsonArray
    }
    private fun payload(name: String): String = fixtures.first { it.jsonObject["name"]!!.jsonPrimitive.content == name }.jsonObject["payload"].toString()
    private fun response(body: String, code: Int = 200) = MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build()

    @Test fun `every positive contract example is decodable without losing unknown dates`() {
        fixtures.filter { it.jsonObject["valid"]!!.jsonPrimitive.boolean }.forEach { fixture ->
            val item = fixture.jsonObject
            val body = item["payload"].toString()
            when(item["schema"]!!.jsonPrimitive.content) {
                "PreviewSession" -> json.decodeFromString<PreviewSessionDto>(body)
                "Draft" -> json.decodeFromString<DraftDto>(body)
                "Evaluation" -> json.decodeFromString<EvaluationDto>(body)
                "Proposal" -> json.decodeFromString<ProposalDto>(body)
                "Watch" -> json.decodeFromString<WatchDto>(body)
                "Finding" -> json.decodeFromString<FindingDto>(body)
                "Command" -> json.decodeFromString<CommandDto>(body)
                "CommandReceipt" -> json.decodeFromString<CommandReceiptDto>(body)
                "WatchPage" -> json.decodeFromString<WatchPageDto>(body)
                "DeletionReceipt" -> json.decodeFromString<DeletionReceiptDto>(body)
                else -> fail("Fixture schema not covered: ${item["schema"]}")
            }
        }
        val finding = json.decodeFromString<FindingDto>(payload("unknown_publication_and_date_only_event"))
        assertNull(finding.evidence.single().publication)
        assertEquals(TemporalEvidenceDto.Date("2026-11-12"), finding.event)
    }

    @Test fun `202 remains pending and retry sends the same command with no duplicate identity`() = runTest {
        MockWebServer().apply { start() }.use { server ->
            server.enqueue(response(payload("command_pending"), 202))
            server.enqueue(response(payload("command_pending"), 202))
            val api = ApiClient.create(server.url("/functions/v1/"), allowLocalHttp = true)
            val command = json.decodeFromString<CommandDto>(payload("activate"))
            repeat(2) {
                val result = apiCall { api.command("Bearer test-only", command) }
                assertTrue(result is ApiResult.Value && result.httpStatus == 202 && result.body is CommandReceiptDto.Pending)
            }
            val first = server.takeRequest(5, TimeUnit.SECONDS)!!
            val second = server.takeRequest(5, TimeUnit.SECONDS)!!
            assertEquals("/functions/v1/api/v1/commands", first.url.encodedPath)
            assertEquals("Bearer test-only", first.headers["Authorization"])
            assertNull(first.headers["X-Preview-Token"])
            assertEquals(first.body!!.utf8(), second.body!!.utf8())
            assertEquals(2, server.requestCount)
        }
    }

    @Test fun `preview credentials do not leak into public requests and redirects are not followed`() = runTest {
        MockWebServer().apply { start() }.use { server ->
            MockWebServer().apply { start() }.use { other ->
                server.enqueue(MockResponse.Builder().code(307).addHeader("Location", other.url("/stolen")).build())
                server.enqueue(response("""{"status":"ok","apiVersion":"1"}"""))
                val api = ApiClient.create(server.url("/functions/v1/"), true)
                val result = apiCall { api.proposal("preview-only-secret", "opaque") }
                assertTrue(result is ApiResult.Rejected && result.httpStatus == 307)
                assertEquals(0, other.requestCount)
                assertTrue(apiCall { api.health() } is ApiResult.Value)
                assertEquals("preview-only-secret", server.takeRequest(5, TimeUnit.SECONDS)!!.headers["X-Preview-Token"])
                val publicRequest = server.takeRequest(5, TimeUnit.SECONDS)!!
                assertNull(publicRequest.headers["Authorization"])
                assertNull(publicRequest.headers["X-Preview-Token"])
            }
        }
    }

    @Test fun `quota retains machine readable error and does not become an empty success`() = runTest {
        MockWebServer().apply { start() }.use { server ->
            server.enqueue(response("""{"code":"quota_exceeded","requestId":"r","retryAfterSeconds":60,"scope":"global"}""", 429))
            val result = apiCall { ApiClient.create(server.url("/"), true).watches("Bearer test-only") }
            assertEquals(ApiResult.Rejected(429, ErrorDto("quota_exceeded", "r", 60, "global")), result)
        }
    }

    @Test fun `successful empty list is distinct from malformed and unknown responses`() = runTest {
        MockWebServer().apply { start() }.use { server ->
            server.enqueue(response(payload("empty_is_success")))
            server.enqueue(response("not-json"))
            server.enqueue(response("""{"id":"x","status":"new_future_status","draftRevision":1,"updatedAt":"x"}"""))
            val api = ApiClient.create(server.url("/"), true)
            val empty = apiCall { api.watches("Bearer test-only") }
            assertTrue(empty is ApiResult.Value && empty.body.items.isEmpty())
            assertEquals(ApiResult.InvalidResponse, apiCall { api.watches("Bearer test-only") })
            assertEquals(ApiResult.InvalidResponse, apiCall { api.evaluation("preview", "id") })
        }
    }

    @Test fun `unknown response fields are compatible and optional dates stay null`() = runTest {
        MockWebServer().apply { start() }.use { server ->
            val body = json.parseToJsonElement(payload("empty_is_success")).jsonObject.toMutableMap()
            body["newField"] = JsonPrimitive("future")
            server.enqueue(response(JsonObject(body).toString()))
            val result = apiCall { ApiClient.create(server.url("/"), true).watches("Bearer test-only", "next+token", 10) }
            assertTrue(result is ApiResult.Value)
            assertEquals("next+token", server.takeRequest(5, TimeUnit.SECONDS)!!.url.queryParameter("cursor"))
        }
    }

    @Test fun `204 deletion response is handled without a fake JSON body`() = runTest {
        MockWebServer().apply { start() }.use { server ->
            server.enqueue(MockResponse.Builder().code(204).build())
            assertEquals(ApiResult.Empty, apiCall { ApiClient.create(server.url("/"), true).removeDevice("Bearer test-only", "device") })
        }
    }

    @Test fun `large or non JSON errors retain their failure HTTP status`() = runTest {
        MockWebServer().apply { start() }.use { server ->
            server.enqueue(response("x".repeat(20_000), 503))
            val result = apiCall { ApiClient.create(server.url("/"), true).watches("Bearer test-only") }
            assertEquals(ApiResult.Rejected(503, null), result)
        }
    }

    @Test fun `cancellation propagates and transport failure does not claim airplane mode`() = runTest {
        assertEquals(ApiResult.ConnectionFailure, apiCall<Unit> { throw IOException("server unavailable") })
        val cancellation = CancellationException("user left")
        try { apiCall<Unit> { throw cancellation }; fail("Cancellation swallowed") }
        catch (actual: CancellationException) { assertSame(cancellation, actual) }
    }

    @Test fun `diagnostic strings redact preview and push secrets`() {
        val secret = "not-for-logs"
        assertFalse(PreviewSessionDto(secret, "date").toString().contains(secret))
        assertFalse(DeviceRegistrationDto(secret, "fr").toString().contains(secret))
        assertFalse(CommandDto.Activate("id", "p", 1, secret, false).toString().contains(secret))
        assertFalse(CommandDto.Replace("id", "w", 1, "p", 1, secret, false).toString().contains(secret))
    }

    @Test fun `cleartext and credentials in production base URL are rejected`() {
        listOf("http://api.example.invalid/", "https://name:password@api.example.invalid/", "https://api.example.invalid/?secret=x").forEach { url ->
            try { ApiClient.create(url.toHttpUrl()); fail("Unsafe URL accepted") }
            catch (_: IllegalArgumentException) { }
        }
    }
}
