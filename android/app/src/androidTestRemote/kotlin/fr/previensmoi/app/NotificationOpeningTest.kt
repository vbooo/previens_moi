package fr.previensmoi.app

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.test.platform.app.InstrumentationRegistry
import fr.previensmoi.R
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.*
import fr.previensmoi.feature.common.presentation.PreviensTheme
import fr.previensmoi.feature.session.presentation.SessionUiState
import fr.previensmoi.feature.watches.presentation.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Reuses production notification routing and result UI; server data is deliberately delayed. */
class NotificationOpeningTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val requested = mutableStateOf<String?>("finding")
    private val account = mutableStateOf(SessionUiState())
    private val requestKey = mutableIntStateOf(0)
    private var homeFrames = 0
    private var top: Any? = null
    private class Server : FindingRepository, WatchRepository {
        var calls = 0
        var response = CompletableDeferred<Outcome<Finding>>()
        val finding = Finding("finding", "watch", "plan", 1, "PS5", 1792767900000,
            title = "Promotion PS5", fact = "Une offre PS5 correspond à ta demande.", noveltyBasis = "initial")
        override fun observeFindings() = flowOf(emptyList<Finding>())
        override fun observeList() = flowOf(emptyList<Watch>())
        override suspend fun getFinding(id: String): Outcome<Finding> { calls++; return response.await() }
        override suspend fun refresh() = RefreshResult.Success
        override suspend fun execute(command: Command): Outcome<CommandReceipt> = error("No mutation")
    }
    @OptIn(ExperimentalMaterial3Api::class)
    private fun mount(server: Server, cold: Boolean = true) {
        if (!cold) requested.value = null
        compose.runOnUiThread {
            val vm = WatchesViewModel(server, server, IdGenerator { error("No new identifier") })
            compose.activity.setContent {
                PreviensTheme {
                    val state by vm.state.collectAsState()
                    val stack = rememberNavBackStack(*notificationInitialRoutes(requested.value, null).toTypedArray())
                    FindingNotificationNavigation(requested.value, account.value, state, stack, vm::requestFinding, { requested.value = null }, requestKey.intValue)
                    SideEffect { top = stack.lastOrNull() }
                    Scaffold(topBar = { TopAppBar(title = { Text("Résultat") }) }) { insets ->
                        Box(Modifier.padding(insets)) {
                            NavDisplay(backStack = stack, onBack = { if (!dismissFindingNotification(stack, requested.value, { requested.value = null })) stack.removeLastOrNull() }, entryProvider = entryProvider {
                                entry<Home> { SideEffect { homeFrames++ }; Text("Accueil", Modifier.testTag("home")) }
                                entry<Account> { Text("Connexion", Modifier.testTag("login")) }
                                entry<Detail> { Text("Suivi parent", Modifier.testTag("parent")) }
                                entry<Result> { key -> FindingScreen(key.id, state, remote = true, retry = { vm.loadFinding(key.id) }) }
                            })
                        }
                    }
                }
            }
        }
    }
    private fun await(tag: String) = compose.waitUntil(5000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun signInFixture() { compose.runOnUiThread { account.value = SessionUiState(session = Session("fixture"), loaded = true) } }
    private fun capture(name: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val bitmap = checkNotNull(automation.takeScreenshot())
        val file = java.io.File(compose.activity.getExternalFilesDir(null), "notification-$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        automation.executeShellCommand("cp ${file.absolutePath} /data/local/tmp/notification-$name.png").close()
    }
    @Test fun coldNotificationShowsResultLoadingBeforeSessionAndDataWithoutHomeOrLoginFlash() {
        val server = Server(); mount(server)
        await("finding_loading")
        compose.onNodeWithTag("home").assertDoesNotExist(); compose.onNodeWithTag("login").assertDoesNotExist()
        assertEquals(0, server.calls)
        signInFixture(); compose.waitUntil(5000) { server.calls == 1 }
        compose.onNodeWithTag("finding_loading").assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.missing)).assertDoesNotExist()
        assertEquals(0, homeFrames); capture("loading")
        compose.runOnUiThread { server.response.complete(Outcome.Success(server.finding)) }
        await("open_source")
        compose.onNodeWithText(server.finding.fact).assertIsDisplayed()
        compose.onNodeWithText("Trouvé dès la première recherche").assertDoesNotExist()
        assertEquals(0, homeFrames); assertEquals(Result("finding"), top)
        capture("result")
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        await("parent")
    }
    @Test fun warmNotificationNavigatesBeforeItsDelayedResponseAndBackIsNotHijacked() {
        val server = Server(); mount(server, cold = false); signInFixture()
        await("home")
        compose.runOnUiThread { requested.value = "finding"; requestKey.intValue++ }
        await("finding_loading")
        compose.onNodeWithTag("home").assertDoesNotExist()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        await("home")
        compose.runOnUiThread { server.response.complete(Outcome.Success(server.finding)) }
        compose.waitForIdle()
        compose.onNodeWithTag("home").assertIsDisplayed()
        compose.onNodeWithText(server.finding.fact).assertDoesNotExist()
    }
    @Test fun failedReadStaysOnResultAndRetryLoadsItWithoutHomeFallback() {
        val server = Server(); mount(server); signInFixture()
        await("finding_loading")
        compose.runOnUiThread { server.response.complete(Outcome.Error(Failure.TemporaryFailure)) }
        await("retry_finding")
        compose.onNodeWithTag("finding_loading").assertDoesNotExist()
        compose.onNodeWithTag("home").assertDoesNotExist(); assertEquals(0, homeFrames)
        server.response = CompletableDeferred()
        compose.onNodeWithTag("retry_finding").performClick()
        await("finding_loading")
        compose.runOnUiThread { server.response.complete(Outcome.Success(server.finding)) }
        await("open_source"); assertEquals(0, homeFrames)
    }
    @Test fun missingResultRemainsOnItsRouteAndConfirmedSignOutRequiresLogin() {
        val server = Server(); mount(server); signInFixture()
        await("finding_loading")
        compose.runOnUiThread { server.response.complete(Outcome.Error(Failure.NotFound)) }
        compose.waitUntil(5000) { compose.onAllNodesWithText(compose.activity.getString(R.string.missing)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("finding_loading").assertDoesNotExist()
        compose.onNodeWithTag("home").assertDoesNotExist(); assertEquals(0, homeFrames)
        compose.runOnUiThread { account.value = SessionUiState(loaded = true); requested.value = "private-finding" }
        await("login")
        assertEquals(1, server.calls)
        compose.onNodeWithText(server.finding.fact).assertDoesNotExist()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        await("home")
        assertEquals(1, server.calls)
    }

    @Test fun aRepeatedNotificationDoesNotReuseItsPreviousTerminalRead() {
        val server = Server(); mount(server); signInFixture()
        await("finding_loading")
        compose.runOnUiThread { server.response.complete(Outcome.Error(Failure.NotFound)) }
        compose.waitUntil(5000) { requested.value == null }
        server.response = CompletableDeferred()
        compose.runOnUiThread { requested.value = "finding"; requestKey.intValue++ }
        await("finding_loading")
        assertEquals("finding", requested.value)
        compose.runOnUiThread { server.response.complete(Outcome.Success(server.finding)) }
        await("open_source")
        // The content and parent-back-stack are published by separate UI effects.
        compose.waitUntil(5000) { requested.value == null }
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        await("parent")
    }

}
