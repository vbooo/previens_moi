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
            val otp = server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!; assertEquals("/auth/v1/otp",otp.url.encodedPath)
            assertEquals("public-test",otp.headers["apikey"])
            val verification = server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!; assertTrue(verification.body!!.utf8().contains("123456"))
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
            assertEquals("POST",server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!.method)
            val resumed = server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!;assertEquals("GET",resumed.method);assertTrue(resumed.url.encodedPath.endsWith(id))
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
            val first = server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!;server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!;val retry = server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!
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
    @Test fun `pending evaluation polls same identifier and clarification preserves question ids`() = runTest {
        MockWebServer().apply { start() }.use { server ->
            val session = auth(server, MemoryVault())
            val repository = RemoteRepositories(ApiClient.create(server.url("/"),true),session,MemoryPending(),IdGenerator { "eval-id" })
            server.enqueue(response("""{"token":"preview-secret","expiresAt":"2099-10-21T00:00:00Z"}"""))
            server.enqueue(response("""{"id":"eval-id","status":"pending","draftRevision":2,"updatedAt":"2026-10-20T00:00:00Z"}""",202))
            val questions = """{"id":"eval-id","status":"clarification","draftRevision":2,"updatedAt":"2026-10-20T00:00:00Z","questions":[{"id":"zone","label":"Où ?","options":["France","Belgique"],"required":true},{"id":"budget","label":"Budget ?","options":[],"required":false}]}"""
            server.enqueue(response(questions))
            val result = repository.evaluate(RequestDraft("Produit",revision=2)) as Outcome.Success
            val items = (result.value as Evaluation.Questions).items
            assertEquals(listOf("zone","budget"),items.map { it.id });assertEquals(listOf("France","Belgique"),items[0].options)
            server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!;val post=server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!;val poll=server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!
            assertEquals("GET",poll.method);assertTrue(poll.url.encodedPath.endsWith("eval-id"))
            server.enqueue(response(questions.replace("\"draftRevision\":2","\"draftRevision\":3")))
            repository.evaluate(RequestDraft("Produit",revision=3,answers=mapOf("zone" to "France","budget" to "100 €")))
            val answered=ApiClient.json.parseToJsonElement(server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!.body!!.utf8()).jsonObject
            assertEquals("eval-id",answered["previousEvaluationId"]!!.jsonPrimitive.content)
            assertEquals(setOf("zone","budget"),answered["draft"]!!.jsonObject["answers"]!!.jsonArray.map { it.jsonObject["questionId"]!!.jsonPrimitive.content }.toSet())
            assertTrue(post.body!!.utf8().contains("eval-id"))
        }
    }
    @Test fun `evaluation polling is bounded and explicit retry reuses request`() = runTest {
        MockWebServer().apply { start() }.use { server ->
            var generated=0
            val repository=RemoteRepositories(ApiClient.create(server.url("/"),true),auth(server,MemoryVault()),MemoryPending(),IdGenerator { generated++; "eval-id" }, evaluationPolls = 3, evaluationPollDelayMillis = 0)
            server.enqueue(response("""{"token":"preview-secret","expiresAt":"2099-10-21T00:00:00Z"}"""))
            val pending="""{"id":"eval-id","status":"pending","draftRevision":0,"updatedAt":"2026-10-20T00:00:00Z"}"""
            repeat(3) { server.enqueue(response(pending)) }
            assertEquals(Outcome.Error(Failure.AnalysisPending),repository.evaluate(RequestDraft("test")))
            assertEquals(4,server.requestCount);server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!;val original=server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!.body!!.utf8();repeat(2){ server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!! }
            server.enqueue(response(pending.replace("\"draftRevision\":0","\"draftRevision\":99")))
            assertEquals(Outcome.Error(Failure.VersionConflict),repository.evaluate(RequestDraft("test")))
            assertEquals(original,server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!.body!!.utf8());assertEquals(1,generated)
        }
    }

    @Test fun `general admission can finish after the old short polling window without another POST`() = runTest {
        MockWebServer().apply { start() }.use { server ->
            val repository=RemoteRepositories(ApiClient.create(server.url("/"),true),auth(server,MemoryVault()),MemoryPending(),IdGenerator { "eval-id" })
            server.enqueue(response("""{"token":"preview-secret","expiresAt":"2099-10-21T00:00:00Z"}"""))
            repeat(12) { server.enqueue(response("""{"id":"eval-id","status":"pending","draftRevision":0,"updatedAt":"2026-10-20T00:00:00Z"}""")) }
            server.enqueue(response("""{"id":"eval-id","status":"unsupported","draftRevision":0,"updatedAt":"2026-10-20T00:00:00Z","reason":"unqualified_method","explanation":"Cette source ne permet pas de vérifier les critères."}"""))
            assertTrue(repository.evaluate(RequestDraft("test")) is Outcome.Success)
            val requests=(0 until server.requestCount).map { server.takeRequest(5,java.util.concurrent.TimeUnit.SECONDS)!! }
            assertEquals(1,requests.count { it.method=="POST"&&it.url.encodedPath.endsWith("understandings") })
            assertTrue(requests.filter { it.method=="GET" }.all { it.url.encodedPath.endsWith("eval-id") })
        }
    }

    @Test fun `push expiry rejects missing future invalid and stale deliveries`() {
        val now=1_000_000L
        assertNull(pushExpiry(0,86400,now));assertNull(pushExpiry(now+300001,86400,now))
        assertNull(pushExpiry(now,86401,now));assertNull(pushExpiry(now,0,now))
        assertNull(pushExpiry(now-1000,1,now));assertEquals(now+86400000,pushExpiry(now,86400,now))
    }

    @Test fun `uncertain analysis survives repository recreation with same encrypted-journal request identity`() = runTest {
        MockWebServer().apply { start() }.use { server ->
            val cache=object: EvaluationJournal { var value: EvaluationCache?=null;override fun read()=value;override fun write(value: EvaluationCache){this.value=value} }
            val api=ApiClient.create(server.url("/"),true);val session=auth(server,MemoryVault())
            server.enqueue(response("""{"token":"preview-secret","expiresAt":"2099-10-21T00:00:00Z"}"""))
            server.enqueue(response("""{"id":"eval-id","status":"pending","draftRevision":0,"updatedAt":"2026-10-20T00:00:00Z"}""",202));server.enqueue(response("{}",503))
            val first=RemoteRepositories(api,session,MemoryPending(),IdGenerator { "eval-id" },cache)
            assertEquals(Outcome.Error(Failure.TemporaryFailure),first.evaluate(RequestDraft("secret-demand")))
            server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!;val posted=server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!;server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!
            val second=RemoteRepositories(api,session,MemoryPending(),IdGenerator { error("A second id must not be generated") },cache)
            server.enqueue(response("""{"id":"eval-id","status":"unsupported","draftRevision":0,"updatedAt":"2026-10-20T00:00:00Z","reason":"unqualified_method","explanation":"Not qualified"}"""))
            assertTrue(second.evaluate(RequestDraft("secret-demand")) is Outcome.Success)
            val replay=server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!;assertEquals(posted.url.encodedPath,replay.url.encodedPath);assertEquals(posted.body!!.utf8(),replay.body!!.utf8());assertEquals("preview-secret",replay.headers["X-Preview-Token"])
            assertFalse(cache.value.toString().contains("secret"))
        }
    }
    @Test fun `analysis storage failure prevents model POST and expired preview renews within one user request`() = runTest {
        MockWebServer().apply { start() }.use { server ->
            val api=ApiClient.create(server.url("/"),true);val session=auth(server,MemoryVault())
            val broken=object: EvaluationJournal {override fun read(): EvaluationCache?=null;override fun write(value: EvaluationCache){throw java.io.IOException()} }
            server.enqueue(response("""{"token":"preview-secret","expiresAt":"2099-10-21T00:00:00Z"}"""))
            assertEquals(Outcome.Error(Failure.StorageFailure),RemoteRepositories(api,session,MemoryPending(),IdGenerator { "eval" },broken).evaluate(RequestDraft("test")))
            assertEquals(1,server.requestCount);server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!
            val expired=object: EvaluationJournal {var value=EvaluationCache("old-secret",null,null,1);override fun read()=value;override fun write(value: EvaluationCache){this.value=value} }
            val repository=RemoteRepositories(api,session,MemoryPending(),IdGenerator { "new-eval" },expired)
            server.enqueue(response("""{"token":"fresh-secret","expiresAt":"2099-10-21T00:00:00Z"}"""))
            server.enqueue(response("""{"id":"new-eval","status":"unsupported","draftRevision":0,"updatedAt":"2026-10-20T00:00:00Z","reason":"unqualified_method","explanation":"Not qualified"}"""))
            assertTrue(repository.evaluate(RequestDraft("test")) is Outcome.Success);assertEquals(3,server.requestCount)
            assertTrue(server.takeRequest(5,java.util.concurrent.TimeUnit.SECONDS)!!.url.encodedPath.endsWith("preview-sessions"))
            val understanding=server.takeRequest(5,java.util.concurrent.TimeUnit.SECONDS)!!
            assertTrue(understanding.url.encodedPath.endsWith("understandings"));assertEquals("fresh-secret",understanding.headers["X-Preview-Token"])
        }
    }


    @Test fun `activation returns accepted preparation without waiting for provider and retains recovery identifier`() = runTest {
        MockWebServer().apply { start() }.use { server ->
            val session=auth(server,MemoryVault(Credentials("a","token","refresh",9_000_000)))
            val journal=MemoryPending()
            val cache=object: EvaluationJournal {override fun read()=EvaluationCache("preview",null,null);override fun write(value: EvaluationCache){} }
            val repository=RemoteRepositories(ApiClient.create(server.url("/"),true),session,journal,IdGenerator { "unused" },cache)
            server.enqueue(response("{}",404));repository.get("p")
            server.enqueue(response("""{"id":"cmd","state":"pending","updatedAt":"2026-10-20T00:00:00Z","remoteState":"starting","watchId":"w"}""",202))
            val result=repository.execute(Command("cmd",Operation.Activate,"p",1,acceptPartial=true)) as Outcome.Success
            assertTrue(result.value.acceptedInBackground);assertEquals("w",result.value.watchId);assertEquals("cmd",journal.read("a"));assertEquals(2,server.requestCount)
            server.takeRequest(5,java.util.concurrent.TimeUnit.SECONDS)!!
            assertEquals("POST",server.takeRequest(5,java.util.concurrent.TimeUnit.SECONDS)!!.method)
        }
    }
    @Test fun `pending preparation can be cancelled after repository recreation without another activation`() = runTest {
        MockWebServer().apply { start() }.use { server ->
            val session=auth(server,MemoryVault(Credentials("a","token","refresh",9_000_000)))
            val journal=MemoryPending();journal.write("a","activation-id")
            val repository=RemoteRepositories(ApiClient.create(server.url("/"),true),session,journal,IdGenerator { "unused" })
            server.enqueue(response("""{"id":"activation-id","state":"pending","updatedAt":"2026-10-20T00:00:00Z","remoteState":"starting","watchId":"w"}""",202))
            server.enqueue(response("""{"id":"cancel-id","state":"pending","updatedAt":"2026-10-20T00:00:00Z","remoteState":"stopping","watchId":"w"}""",202))
            assertEquals(Outcome.Error(Failure.OperationPending),repository.execute(Command("cancel-id",Operation.Pause,"w",1)))
            assertEquals("cancel-id",journal.read("a"))
            assertEquals("GET",server.takeRequest(5,java.util.concurrent.TimeUnit.SECONDS)!!.method)
            val posted=server.takeRequest(5,java.util.concurrent.TimeUnit.SECONDS)!!
            assertEquals("POST",posted.method);assertTrue(posted.body!!.utf8().contains("pause"))
        }
    }

}
