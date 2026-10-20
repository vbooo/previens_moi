package fr.previensmoi.data.remote

import fr.previensmoi.data.auth.*
import fr.previensmoi.domain.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.Assert.*
import org.junit.Test

private class MemoryVault(var value: Credentials? = null) : SessionVault {
    override fun read() = value
    override fun write(value: Credentials) { this.value = value }
    override fun clear() { value = null }
}
private class MemoryPending : PendingCommands {
    val values = mutableMapOf<String,String>()
    override fun read(owner: String) = values[owner]
    override fun write(owner: String, id: String) { values[owner] = id }
    override fun clear(owner: String) { values.remove(owner) }
}
class RemoteRepositoryTest {
    private val clock = Clock { 1_000_000 }
    private fun response(body: String, code: Int = 200) = MockResponse.Builder().code(code).addHeader("Content-Type","application/json").body(body).build()
    private fun auth(server: MockWebServer, vault: SessionVault) = RemoteSessionRepository(createAuthApi(server.url("/auth/v1/"),"public-test",true),vault,clock)
    @Test fun `email code flow stores session only after verification and never logs credentials`() = runTest {
        MockWebServer().apply { start() }.use { server ->
            val vault = MemoryVault(); val session = auth(server,vault)
            server.enqueue(response("{}")); server.enqueue(response("""{"access_token":"secret-access","refresh_token":"secret-refresh","expires_in":3600,"user":{"id":"account-a"}}"""))
            assertEquals(Outcome.Success(Unit), session.requestCode("test@example.test"))
            assertNull(vault.value)
            assertTrue(session.verifyCode("test@example.test","123456") is Outcome.Success)
            assertEquals("account-a",session.observeSession().first()?.id)
            assertEquals("Bearer secret-access",session.bearer())
            assertFalse(vault.value.toString().contains("secret"))
            val otp = server.takeRequest(); assertEquals("/auth/v1/otp",otp.url.encodedPath)
            assertEquals("public-test",otp.headers["apikey"])
            val verification = server.takeRequest(); assertTrue(verification.body!!.utf8().contains("123456"))
        }
    }
    @Test fun `concurrent refresh uses one rotation and logout clears local secrets even if server fails`() = runTest {
        MockWebServer().apply { start() }.use { server ->
            val vault = MemoryVault(Credentials("a","old","refresh",1));val session = auth(server,vault)
            server.enqueue(response("""{"access_token":"new","refresh_token":"rotated","expires_in":3600,"user":{"id":"a"}}"""))
            val one = async { session.bearer() }; val two = async { session.bearer() }
            assertEquals("Bearer new",one.await());assertEquals("Bearer new",two.await());assertEquals(1,server.requestCount)
            server.enqueue(response("{}",503));session.signOut();assertNull(vault.value);assertNull(session.observeSession().first())
        }
    }
    @Test fun `invalid refresh clears session while temporary server failure preserves it`() = runTest {
        MockWebServer().apply { start() }.use { server ->
            val vault = MemoryVault(Credentials("a","old","refresh",1));val session = auth(server,vault)
            server.enqueue(response("{}",503));assertNull(session.bearer());assertNotNull(vault.value)
            server.enqueue(response("{}",400));assertNull(session.bearer());assertNull(vault.value)
        }
    }
    @Test fun `pending command survives repository recreation and new action only reads original UUID`() = runTest {
        MockWebServer().apply { start() }.use { server ->
            val session = auth(server,MemoryVault(Credentials("a","token","refresh",9_000_000)))
            val journal = MemoryPending(); val api = ApiClient.create(server.url("/functions/v1/"),true)
            fun repository() = RemoteRepositories(api,session,journal,IdGenerator { "unused" })
            val id = "0199a6e0-0000-7000-8000-000000000001"
            val pending = """{"id":"$id","state":"pending","updatedAt":"2026-10-20T00:00:00Z","remoteState":"stopping","watchId":"w"}"""
            server.enqueue(response(pending,202))
            assertEquals(Outcome.Error(Failure.OperationPending), repository().execute(Command(id,Operation.Pause,"w",1)))
            assertEquals(id,journal.read("a"))
            server.enqueue(response(pending))
            assertEquals(Outcome.Error(Failure.OperationPending),repository().execute(Command("new-must-not-send",Operation.Delete,"w",1)))
            assertEquals("POST",server.takeRequest().method)
            val resumed = server.takeRequest();assertEquals("GET",resumed.method);assertTrue(resumed.url.encodedPath.endsWith(id))
            server.enqueue(response("""{"id":"$id","state":"succeeded","updatedAt":"2026-10-20T00:00:00Z","remoteState":"stopped","watchId":"w","watchVersion":2,"deleted":false}"""))
            assertEquals(Outcome.Error(Failure.VersionConflict),repository().execute(Command("again",Operation.Pause,"w",1)))
            assertNull(journal.read("a"))
        }
    }
    @Test fun `401 refresh retries a command once using identical body and identifier`() = runTest {
        MockWebServer().apply { start() }.use { server ->
            val session = auth(server,MemoryVault(Credentials("a","old","refresh",9_000_000)))
            val repository = RemoteRepositories(ApiClient.create(server.url("/"),true),session,MemoryPending(),IdGenerator { "unused" })
            server.enqueue(response("{}",401))
            server.enqueue(response("""{"access_token":"fresh","refresh_token":"rotated","expires_in":3600,"user":{"id":"a"}}"""))
            server.enqueue(response("""{"id":"cmd","state":"pending","updatedAt":"2026-10-20T00:00:00Z","remoteState":"stopping","watchId":"w"}""",202))
            assertEquals(Outcome.Error(Failure.OperationPending),repository.execute(Command("cmd",Operation.Pause,"w",1)))
            val first = server.takeRequest();server.takeRequest();val retry = server.takeRequest()
            assertEquals(first.body!!.utf8(),retry.body!!.utf8());assertEquals("Bearer fresh",retry.headers["Authorization"])
        }
    }
    @Test fun `server rejection after one successful refresh clears the obsolete session`() = runTest {
        MockWebServer().apply { start() }.use { server ->
            val vault = MemoryVault(Credentials("a","old","refresh",9_000_000)); val session = auth(server,vault)
            val repository = RemoteRepositories(ApiClient.create(server.url("/"),true),session,MemoryPending(),IdGenerator { "unused" })
            server.enqueue(response("{}",401))
            server.enqueue(response("""{"access_token":"fresh","refresh_token":"rotated","expires_in":3600,"user":{"id":"a"}}"""))
            server.enqueue(response("{}",401))
            assertEquals(Outcome.Error(Failure.SessionRequired),repository.getFinding("finding"))
            assertNull(vault.value); assertNull(session.observeSession().first()); assertEquals(3,server.requestCount)
        }
    }

    @Test fun `mapping preserves evidence and date precision and rejects unknown state`() {
        val fixture = ApiClient.json.parseToJsonElement(javaClass.getResourceAsStream("/application-v1.examples.json")!!.bufferedReader().readText()).jsonArray
        val f = fixture.first { it.jsonObject["name"]!!.jsonPrimitive.content == "unknown_publication_and_date_only_event" }.jsonObject["payload"]!!
        val finding = ApiClient.json.decodeFromJsonElement<FindingDto>(f).domain()
        assertNull(finding.eventAt);assertEquals("2026-11-12",finding.eventDate);assertNull(finding.publishedAt);assertTrue(finding.evidence.isNotEmpty())
        assertThrows(IllegalArgumentException::class.java) { instant("2026-11-12") }
    }
}
