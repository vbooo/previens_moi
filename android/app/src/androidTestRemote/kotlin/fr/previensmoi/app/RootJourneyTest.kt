package fr.previensmoi.app

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.previensmoi.BuildConfig
import fr.previensmoi.R
import fr.previensmoi.data.local.LocalDraftRepository
import fr.previensmoi.domain.model.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import mockwebserver3.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith

/** Full production activity, DI, encrypted session, DataStore and HTTP repositories.
 * Only the HTTP service is controlled. Opt-in local build; never targets a live account. */
@RunWith(AndroidJUnit4::class)
class RootJourneyTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val app get() = context.applicationContext as PreviensMoiApplication
    private lateinit var server: MockWebServer
    private var activity: ActivityScenario<MainActivity>? = null
    private val watchId = "019a05be-8000-7000-8000-000000000002"
    private lateinit var watch: JSONObject
    private lateinit var finding: JSONObject
    @Volatile private var findingGate: CountDownLatch? = null
    private lateinit var proposal: JSONObject
    @Volatile private var listStatus = 200
    @Volatile private var listEmpty = false
    @Volatile private var gate: CountDownLatch? = null
    @Volatile private var tokenLifetime = 3600
    private val commands = java.util.concurrent.CopyOnWriteArrayList<String>()
    private fun response(body: String, status: Int = 200) = MockResponse.Builder().code(status).addHeader("Content-Type", "application/json").addHeader("Connection", "close").body(body).build()
    private fun error(status: Int) = response("""{"code":"temporary_failure","requestId":"019a05be-8000-7000-8000-000000000099","retryAfterSeconds":null,"scope":null}""",status)
    @Before fun setup() {
        assumeTrue(BuildConfig.LOCAL_SERVICE && BuildConfig.SERVICE_URL == "http://127.0.0.1:58888/functions/v1/")
        watch = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open("journey-watch.json").bufferedReader().use { it.readText() })
        finding = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open("journey-finding.json").bufferedReader().use { it.readText() })
        proposal = JSONObject(watch.getJSONObject("plan").toString())
        server = MockWebServer()
        server.dispatcher = object: Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path=request.url.encodedPath
                return when {
                    path.endsWith("/otp") -> response("{}")
                    path.endsWith("/preview-sessions") -> response("""{"token":"local-fixture-preview","expiresAt":"2099-01-01T00:00:00Z"}""")
                    path.endsWith("/understandings") -> {
                        val body=JSONObject(request.body!!.utf8())
                        proposal.put("draft",body.getJSONObject("draft")).put("backgroundPreparation",true).put("partial",true)
                        response("""{"status":"ready","id":"${body.getString("id")}","draftRevision":${body.getJSONObject("draft").getLong("revision")},"updatedAt":"2026-11-02T12:00:00Z","proposal":$proposal}""")
                    }
                    path.contains("/proposals/") -> response(proposal.toString())
                    path.contains("/findings/") -> { findingGate?.await(15,TimeUnit.SECONDS); response(finding.toString()) }
                    path.endsWith("/verify") -> response("""{"access_token":"local-fixture-token","refresh_token":"local-fixture-refresh","expires_in":$tokenLifetime,"user":{"id":"019a05be-8000-7000-8000-000000000088","email":"fixture@example.test"}}""")
                    path.endsWith("/token") -> response("{}",400)
                    path.endsWith("/logout") || path.contains("/devices/") -> MockResponse.Builder().code(204).addHeader("Connection", "close").build()
                    path.endsWith("/watches") -> {
                        gate?.await(15,TimeUnit.SECONDS)
                        if(listStatus!=200) error(listStatus) else response("""{"items":${if(listEmpty) "[]" else "[$watch]"},"nextCursor":null,"serverTime":"2026-11-02T12:00:00Z"}""")
                    }
                    path.endsWith("/findings") -> response("""{"items":[],"nextCursor":null,"serverTime":"2026-11-02T12:00:00Z"}""")
                    path.endsWith("/commands") && request.method=="POST" -> {
                        val body=JSONObject(request.body!!.utf8());commands.add(body.getString("id"))
                        val paused=body.getString("operation")=="pause"
                        if(body.getString("operation")=="activate") watch.put("plan",proposal)
                        synchronized(watch) {
                            watch.put("status",if(paused) "paused" else "active").put("remoteState",if(paused) "stopped" else "running")
                            watch.put("version",watch.getLong("version")+1)
                        }
                        response("""{"state":"succeeded","id":"${body.getString("id")}","updatedAt":"2026-11-02T12:00:00Z","remoteState":"${if(paused) "stopped" else "running"}","watchId":"$watchId","watchVersion":${watch.getLong("version")},"deleted":false}""")
                    }
                    path.endsWith("/$watchId") -> response(watch.toString())
                    else -> error(404)
                }
            }
        }
        server.start(58888)
        runBlocking { app.sessions.signOut(); LocalDraftRepository(context).clear() }
    }
    @After fun cleanup() {
        gate?.countDown(); findingGate?.countDown(); activity?.close()
        if(::server.isInitialized) { runBlocking { app.sessions.signOut(); LocalDraftRepository(context).clear() }; server.close() }
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val screenshot=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        java.io.File(context.filesDir,"qa-$name.png").outputStream().use { screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        screenshot.recycle()
    }
    private fun signIn() = runBlocking { val result=app.sessions.verifyCode("fixture@example.test","123456"); assertTrue("Controlled auth outcome: ${if(result is Outcome.Error) result.reason else "success"}", result is Outcome.Success) }
    private fun launch() { activity=ActivityScenario.launch(Intent(context,MainActivity::class.java)) }
    private fun waitTag(tag:String) = compose.waitUntil(20000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun tap(tag:String) { waitTag(tag); val node=compose.onNodeWithTag(tag); if (!node.isDisplayed()) node.performScrollTo(); node.performClick() }
    private fun back() { activity!!.onActivity { it.onBackPressedDispatcher.onBackPressed() }; compose.waitForIdle() }
    @Test fun coldStartWaitsForConfirmedDataAndNewIsDistinctFromResumeDraft() {
        signIn()
        runBlocking { LocalDraftRepository(context).save(RequestDraft("Old webhook draft")) }
        gate=CountDownLatch(1);launch(); waitTag("home_loading")
        compose.onNodeWithText(context.getString(R.string.v1_empty_title)).assertDoesNotExist()
        capture("loading")
        gate!!.countDown();gate=null;waitTag("watch_$watchId");capture("home")
        tap("resume_draft");compose.onNodeWithTag("request").assertTextContains("Old webhook draft")
        back();tap("new_request");capture("new")
        compose.onNodeWithTag("request").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
        compose.onNodeWithTag("request").performTextInput("My new request")
        compose.waitForIdle(); activity!!.recreate();waitTag("request")
        compose.onNodeWithTag("request").assertTextContains("My new request")
        back();tap("new_request")
        compose.onNodeWithTag("request").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
    }
    @Test fun initialServerFailureIsNotAnEmptyAccountAndSignOutDoesNotStickLoading() {
        signIn();listStatus=503;launch()
        compose.waitUntil(20000) { compose.onAllNodesWithText(context.getString(R.string.v4_list_failed)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(context.getString(R.string.v1_empty_title)).assertDoesNotExist()
        listStatus=200;tap("refresh_watches");waitTag("watch_$watchId")
        activity!!.recreate();waitTag("watch_$watchId")
        listStatus=503;tap("refresh_watches");waitTag("watch_$watchId")
        tap("account");tap("sign_out");waitTag("email");back();waitTag("signed_out_watches")
        compose.onNodeWithTag("home_loading").assertDoesNotExist()
        compose.onNodeWithTag("watch_$watchId").assertDoesNotExist()
    }
    @Test fun actualHttpPauseAndResumeUpdateTheSameWatchWithoutRepeatedCommands() {
        signIn();launch();tap("watch_$watchId");tap("pause");waitTag("resume")
        assertEquals(1,commands.size)
        tap("resume");waitTag("pause")
        assertEquals(2,commands.size); assertEquals(2,commands.distinct().size)
        tap("watch_menu");tap("edit");waitTag("request")
        compose.onNodeWithTag("request").assertTextContains(watch.getJSONObject("plan").getJSONObject("draft").getString("text"))
    }
    @Test fun expiredRefreshCredentialReturnsToSignedOutWithoutExposingPrivateWatches() {
        tokenLifetime=1;signIn();launch();waitTag("signed_out_watches")
        compose.onNodeWithTag("home_loading").assertDoesNotExist()
        compose.onNodeWithTag("watch_$watchId").assertDoesNotExist()
        assertNull(app.sessions.currentUserId())
    }
    @Test fun emailConnectionRequestConfirmationAndDetailUseActualRepositories() {
        launch();tap("account")
        compose.onNodeWithTag("email").performTextReplacement("fixture@example.test")
        tap("send_code");waitTag("otp")
        compose.onNodeWithTag("otp").performTextReplacement("123456")
        tap("verify_code");waitTag("watch_$watchId")
        tap("new_request");compose.onNodeWithTag("request").performTextInput("Concerts à Montpellier")
        tap("evaluate");waitTag("confirm");activity!!.recreate();waitTag("confirm");tap("confirm");waitTag("watch_status")
        assertEquals(1,commands.size)
        assertEquals("Concerts à Montpellier",watch.getJSONObject("plan").getJSONObject("draft").getString("text"))
    }
    @Test fun coldNotificationLoadsResultInPlaceThroughActualHttpClient() {
        signIn();findingGate=CountDownLatch(1)
        activity=ActivityScenario.launch(Intent(context,MainActivity::class.java).putExtra("findingId",finding.getString("id")))
        waitTag("finding_loading")
        compose.onNodeWithTag("watch_list").assertDoesNotExist()
        findingGate!!.countDown();findingGate=null
        compose.waitUntil(20000) { compose.onAllNodesWithText(finding.getString("fact")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("watch_list").assertDoesNotExist()
    }

}
