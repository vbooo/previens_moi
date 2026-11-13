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
    @Volatile private var extraWatches = emptyList<JSONObject>()
    @Volatile private var gate: CountDownLatch? = null
    @Volatile private var tokenLifetime = 3600
    private val commands = java.util.concurrent.CopyOnWriteArrayList<String>()
    private val authSends = java.util.concurrent.atomic.AtomicInteger()
    private val authVerifications = java.util.concurrent.atomic.AtomicInteger()
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
                    path.endsWith("/otp") -> { authSends.incrementAndGet(); response("{}") }
                    path.endsWith("/preview-sessions") -> response("""{"token":"local-fixture-preview","expiresAt":"2099-01-01T00:00:00Z"}""")
                    path.endsWith("/understandings") -> {
                        val body=JSONObject(request.body!!.utf8())
                        proposal.put("draft",body.getJSONObject("draft")).put("backgroundPreparation",true).put("partial",true)
                        response("""{"status":"ready","id":"${body.getString("id")}","draftRevision":${body.getJSONObject("draft").getLong("revision")},"updatedAt":"2026-11-02T12:00:00Z","proposal":$proposal}""")
                    }
                    path.contains("/proposals/") -> response(proposal.toString())
                    path.contains("/findings/") -> { findingGate?.await(15,TimeUnit.SECONDS); response(finding.toString()) }
                    path.endsWith("/verify") -> { authVerifications.incrementAndGet(); response("""{"access_token":"local-fixture-token","refresh_token":"local-fixture-refresh","expires_in":$tokenLifetime,"user":{"id":"019a05be-8000-7000-8000-000000000088","email":"fixture@example.test"}}""") }
                    path.endsWith("/token") -> response("{}",400)
                    path.endsWith("/logout") || path.contains("/devices/") -> MockResponse.Builder().code(204).addHeader("Connection", "close").build()
                    path.endsWith("/watches") -> {
                        gate?.await(15,TimeUnit.SECONDS)
                        if(listStatus!=200) error(listStatus) else response("""{"items":${if(listEmpty) "[]" else (listOf(watch)+extraWatches).joinToString(prefix="[",postfix="]")},"nextCursor":null,"serverTime":"2026-11-02T12:00:00Z"}""")
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
    private fun tap(tag:String) {
        waitTag(tag)
        val node=compose.onNodeWithTag(tag)
        val scrollable=generateSequence(node.fetchSemanticsNode().parent) { it.parent }
            .any { it.config.contains(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) }
        if (scrollable) node.performScrollTo()
        // Exercise the rendered control's action after scrolling. Platform IME inset animations
        // can move touch coordinates after hiding the keyboard, outside Compose's test clock.
        node.assertIsDisplayed().assertIsEnabled()
        node.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
    }
    private fun back() { activity!!.onActivity { it.onBackPressedDispatcher.onBackPressed() }; compose.waitForIdle() }
    @Test fun coldStartWaitsForConfirmedDataAndNewRetainsUnsubmittedInput() {
        signIn()
        runBlocking { LocalDraftRepository(context).save(RequestDraft("Old webhook draft")) }
        gate=CountDownLatch(1);launch(); waitTag("home_loading")
        compose.onNodeWithText(context.getString(R.string.v1_empty_title)).assertDoesNotExist()
        capture("loading")
        gate!!.countDown();gate=null;waitTag("watch_$watchId");capture("home")
        compose.onNodeWithTag("resume_draft").assertDoesNotExist()
        tap("new_request");compose.onNodeWithTag("request").assertTextContains("Old webhook draft")
        compose.onNodeWithTag("request").performTextReplacement("My new request")
        compose.waitForIdle(); activity!!.recreate();waitTag("request")
        compose.onNodeWithTag("request").assertTextContains("My new request")
        back();capture("draft-home");tap("new_request");capture("retained-request")
        compose.onNodeWithTag("request").assertTextContains("My new request")
        compose.onNodeWithText(context.getString(R.string.discard)).performScrollTo().performClick()
        compose.onNodeWithTag("request").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
    }
    @Test fun initialServerFailureIsNotAnEmptyAccountAndSignOutDoesNotStickLoading() {
        signIn();listStatus=503;launch()
        compose.waitUntil(20000) { compose.onAllNodesWithText(context.getString(R.string.v4_list_failed)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(context.getString(R.string.v1_empty_title)).assertDoesNotExist()
        capture("initial-error")
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
    private fun imeVisible(): Boolean {
        var shown = false
        activity!!.onActivity { shown = it.window.decorView.rootWindowInsets?.isVisible(android.view.WindowInsets.Type.ime()) == true }
        return shown
    }
    private fun focusAboveKeyboard(tag: String) {
        val node = compose.onNodeWithTag(tag)
        node.performScrollTo().performClick()
        compose.waitUntil(5000) { imeVisible() }
        node.assertIsDisplayed()
        var keyboardTop = 0
        activity!!.onActivity { current ->
            val view = current.window.decorView
            keyboardTop = view.height - view.rootWindowInsets.getInsets(android.view.WindowInsets.Type.ime()).bottom
        }
        // Both values are native window pixels, not dp or test-root coordinates.
        compose.waitUntil(5000) { node.fetchSemanticsNode().boundsInWindow.bottom <= keyboardTop + 2 }
        var statusBarBottom = 0
        activity!!.onActivity { statusBarBottom = it.window.decorView.rootWindowInsets.getInsets(android.view.WindowInsets.Type.statusBars()).top }
        val backButton = compose.onNodeWithContentDescription(context.getString(R.string.back))
        backButton.assertIsDisplayed()
        assertTrue("IME must not pan navigation under the status bar", backButton.fetchSemanticsNode().boundsInWindow.top >= statusBarBottom - 2)
    }
    @Test fun keyboardSubmitsOnlyValidEmailAndCodeThroughActualRepositories() {
        launch();tap("account");waitTag("email")
        compose.onNodeWithTag("email").performTextReplacement("invalid")
        focusAboveKeyboard("email");compose.onNodeWithTag("email").performImeAction()
        compose.onNodeWithTag("send_code").assertIsNotEnabled();assertEquals(0,authSends.get())
        compose.onNodeWithTag("email").performTextReplacement("fixture@example.test")
        focusAboveKeyboard("email");capture("forms-email")
        compose.onNodeWithTag("email").performImeAction();waitTag("otp")
        compose.waitUntil(5000) { !imeVisible() };assertEquals(1,authSends.get())
        compose.onNodeWithTag("otp").performTextReplacement("123")
        focusAboveKeyboard("otp");compose.onNodeWithTag("otp").performImeAction()
        compose.onNodeWithTag("verify_code").assertIsNotEnabled();assertEquals(0,authVerifications.get())
        compose.onNodeWithTag("otp").performTextReplacement("123456")
        focusAboveKeyboard("otp");capture("forms-code")
        compose.onNodeWithTag("otp").performImeAction();waitTag("watch_$watchId")
        compose.waitUntil(5000) { !imeVisible() };assertEquals(1,authVerifications.get())
    }
    @Test fun requestAndOptionalFieldsRemainReachableAboveKeyboard() {
        signIn()
        runBlocking { LocalDraftRepository(context).save(RequestDraft(text="Préviens-moi des prochains concerts.\nJe souhaite suivre les annonces publiques.\nConserver mes critères et ma ville.\nAvec des informations détaillées.",answers=mapOf("preferred_site" to "https://example.org/agenda","region" to "France"))) }
        launch();tap("new_request");waitTag("request")
        for (tag in listOf("request","preferred_site","region")) {
            focusAboveKeyboard(tag);capture("forms-$tag")
        }
        // Scrolling to the submission control while IME is open must remain possible.
        compose.onNodeWithTag("evaluate").performScrollTo().assertIsDisplayed().assertIsEnabled()
    }
    @Test fun notificationDetailHasOneHorizontalIndicatorBeforeDataAndDuringRefresh() {
        signIn();gate=CountDownLatch(1)
        activity=ActivityScenario.launch(Intent(context,MainActivity::class.java).putExtra("watchId",watchId))
        lateinit var model: fr.previensmoi.feature.watches.presentation.WatchesViewModel
        activity!!.onActivity { model=androidx.lifecycle.ViewModelProvider(it)[fr.previensmoi.feature.watches.presentation.WatchesViewModel::class.java] }
        compose.waitUntil(20000) { model.state.value.refreshing && model.state.value.ownerId != null }
        waitTag("content_progress")
        compose.onAllNodesWithTag("content_progress").assertCountEquals(1)
        compose.onNodeWithTag("app_bar_progress").assertDoesNotExist()
        compose.onNodeWithTag("watch_list").assertDoesNotExist()
        capture("single-detail-initial")
        gate!!.countDown();gate=null;waitTag("watch_status")
        compose.waitUntil(20000) { !model.state.value.refreshing }
        gate=CountDownLatch(1);tap("refresh_watches");waitTag("app_bar_progress")
        compose.onAllNodesWithTag("app_bar_progress").assertCountEquals(1)
        compose.onNodeWithTag("content_progress").assertDoesNotExist()
        compose.onNodeWithTag("watch_status").assertIsDisplayed()
        capture("single-detail-refresh")
        gate!!.countDown();gate=null
        compose.waitUntil(20000) { !model.state.value.refreshing }
        compose.onNodeWithTag("app_bar_progress").assertDoesNotExist()
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

    @Test fun countrySummaryIsNotRepeatedInDetailsOrConfirmation() {
        val plan = watch.getJSONObject("plan")
        plan.getJSONObject("draft").put("answers", org.json.JSONArray().put(JSONObject().put("questionId", "region").put("value", "France")))
        plan.put("criteria", org.json.JSONArray().put("Le pays de surveillance est la France").put("Promotions sur la PlayStation 5"))
        proposal = JSONObject(plan.toString())
        signIn();launch();waitTag("watch_$watchId");tap("watch_$watchId")
        compose.onNodeWithText("Promotions sur la PlayStation 5").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Le pays de surveillance est la France").assertDoesNotExist()
        compose.onNodeWithText("France").performScrollTo().assertIsDisplayed();capture("country-detail")
        back();tap("new_request");waitTag("request")
        compose.onNodeWithTag("request").performTextReplacement("Promotions PS5")
        tap("evaluate")
        try { waitTag("confirm") } catch (failure: Throwable) {
            activity!!.onActivity { current ->
                val state = androidx.lifecycle.ViewModelProvider(current)[fr.previensmoi.feature.create.presentation.CreateViewModel::class.java].state.value
                println("Controlled confirmation: step=${state.step}, error=${state.error}, loaded=${state.loaded}, requestMatches=${state.draft.text == "Promotions PS5"}, mockHttpRequests=${server.requestCount}")
            }
            throw failure
        }
        compose.onNodeWithText("Le pays de surveillance est la France").assertDoesNotExist()
        compose.onNodeWithText("Promotions sur la PlayStation 5").assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.v2_trial_note)).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.v1_initial_results)).assertDoesNotExist()
        capture("confirmation-clean")
        tap("confirmation_info")
        compose.onNodeWithText(context.getString(R.string.confirmation_info_title)).assertIsDisplayed()
        compose.onNodeWithText("Promotions sur la PlayStation 5").assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.v2_trial_note)).performScrollTo().assertIsDisplayed()
        capture("confirmation-info")
        activity!!.recreate()
        waitTag("close_confirmation_info")
        tap("close_confirmation_info")
        waitTag("confirm")
        compose.onNodeWithText(context.getString(R.string.v2_trial_note)).assertDoesNotExist()
        tap("confirmation_info");waitTag("close_confirmation_info");compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.onNodeWithTag("close_confirmation_info").assertDoesNotExist()
        tap("confirm");waitTag("watch_status")
        assertEquals(1, commands.size)
    }

    @Test fun captureIntegratedProductionScreensWithControlledHttp() {
        val suffix=InstrumentationRegistry.getArguments().getString("uiIntegrationCapture")
        assumeTrue(suffix!=null)
        val plan=watch.getJSONObject("plan")
        plan.put("title","Promotions PlayStation 5").put("intervalHours",1)
            .put("criteria",org.json.JSONArray().put("Promotions sur la PlayStation 5 disponibles en France."))
            .put("sources",org.json.JSONArray().put(JSONObject().put("url","https://direct.playstation.com/fr-fr/").put("label","PlayStation Direct")))
        proposal=JSONObject(plan.toString())
        watch.put("health","healthy").put("lastSuccessAt","2026-11-02T08:41:00Z").put("nextCheckAt","2026-11-02T09:41:00Z")
        val preparing=JSONObject(watch.toString()).put("id","019a05be-8000-7000-8000-000000000089")
            .put("status","pending").put("remoteState","starting").put("health","not_checked_yet").put("lastSuccessAt",JSONObject.NULL).put("baselineAt",JSONObject.NULL).put("nextCheckAt",JSONObject.NULL)
        preparing.getJSONObject("plan").put("title","Offres d’emploi Android à Montpellier")
            .put("criteria",org.json.JSONArray().put("Offres de développeur Android à Montpellier, en France."))
            .put("sources",org.json.JSONArray())
        val paused=JSONObject(watch.toString()).put("id","019a05be-8000-7000-8000-000000000090")
            .put("status","paused").put("remoteState","stopped").put("nextCheckAt",JSONObject.NULL)
        paused.getJSONObject("plan").put("title","Sortie du prochain iPhone")
            .put("criteria",org.json.JSONArray().put("Annonce officielle du prochain iPhone."))
            .put("sources",org.json.JSONArray().put(JSONObject().put("url","https://www.apple.com/fr/newsroom/").put("label","Apple Newsroom")))
        extraWatches=listOf(preparing,paused)
        signIn();gate=CountDownLatch(1);launch();waitTag("home_loading");capture("integrated-loading-$suffix")
        gate!!.countDown();gate=null;waitTag("watch_$watchId");capture("integrated-home-$suffix")
        tap("watch_$watchId");waitTag("pause");capture("integrated-active-$suffix")
        tap("pause");waitTag("resume");capture("integrated-paused-$suffix")
        back();tap("watch_019a05be-8000-7000-8000-000000000089");waitTag("pause");capture("integrated-preparing-$suffix")
        back();tap("new_request");waitTag("request");capture("integrated-request-$suffix")
        compose.onNodeWithTag("request").performTextReplacement("Promotions sur la PlayStation 5")
        tap("evaluate")
        try { waitTag("confirm") } catch (failure: Throwable) {
            activity!!.onActivity { current ->
                val state=androidx.lifecycle.ViewModelProvider(current)[fr.previensmoi.feature.create.presentation.CreateViewModel::class.java].state.value
                println("Controlled UI state: step=${state.step}, error=${state.error}, loaded=${state.loaded}, requestMatches=${state.draft.text == "Promotions sur la PlayStation 5"}, mockHttpRequests=${server.requestCount}")
            }
            throw failure
        }
        compose.onNodeWithText(proposal.getString("title")).assertIsDisplayed()
        capture("integrated-confirm-$suffix")
        back();back();tap("account");waitTag("sign_out");capture("integrated-account-$suffix")
        back()
        finding.put("title","Une promotion sur la PlayStation 5").put("fact","Une offre correspond à ta demande sur la source consultée.")
            .put("reason","La source mentionne une promotion sur la PlayStation 5 disponible en France.")
            .put("observedAt","2026-11-02T08:41:00Z").put("event",JSONObject.NULL)
        finding.getJSONArray("evidence").getJSONObject(0).put("url","https://direct.playstation.com/fr-fr/")
            .put("quote","Offre PlayStation 5 disponible en France.").put("publication",JSONObject.NULL)
        context.startActivity(Intent(context,MainActivity::class.java).putExtra("findingId",finding.getString("id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        compose.waitUntil(20000) { compose.onAllNodesWithText(finding.getString("fact")).fetchSemanticsNodes().isNotEmpty() }
        capture("integrated-result-$suffix")
    }

}
