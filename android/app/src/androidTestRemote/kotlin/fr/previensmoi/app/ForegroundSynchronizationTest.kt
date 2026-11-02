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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.platform.app.InstrumentationRegistry
import fr.previensmoi.R
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.*
import fr.previensmoi.feature.common.presentation.PreviensTheme
import fr.previensmoi.feature.create.presentation.*
import fr.previensmoi.feature.watches.presentation.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/** Server state changes only become visible after a read. No push, tap, Auth or provider calls. */
class ForegroundSynchronizationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val route = mutableStateOf("detail")
    private val owner = mutableStateOf<String?>("fixture")
    private class Service : WatchRepository, FindingRepository, ProposalRepository, SessionRepository, DraftRepository {
        val plan = Proposal("p", 2, RequestDraft("Offres Android Montpellier"), Long.MAX_VALUE,
            title = "Offres d’emploi développeur Android à Montpellier", backgroundPreparation = true,
            sources = listOf("https://example.test/jobs"))
        val pending = Watch("w", plan, status = WatchStatus.Pending, baselineAt = null, remoteState = RemoteState.Starting)
        val completed = pending.copy(status = WatchStatus.Completed, baselineAt = 1792767900000, lastSuccess = 1792767900000, remoteState = RemoteState.Stopped)
        val finding = Finding("f", "w", "p", 2, "Développeur Android à Montpellier", 1792767900000,
            source = "https://example.test/jobs", title = "Offres Android à Montpellier", fact = "Offres de développement Android à Montpellier, dont une en CDI.",
            reason = "La page mentionne Android et Montpellier.", noveltyBasis = "initial")
        val items = MutableStateFlow(listOf(pending))
        val results = MutableStateFlow(emptyList<Finding>())
        val reads = AtomicInteger()
        var finished = false
        override fun observeList() = items
        override fun observeFindings() = results
        override suspend fun refresh(): RefreshResult {
            reads.incrementAndGet()
            if (finished) { items.value = listOf(completed); results.value = listOf(finding) }
            return RefreshResult.Success
        }
        override suspend fun execute(command: Command): Outcome<CommandReceipt> = error("No mutation expected")
        override suspend fun getFinding(id: String): Outcome<Finding> = Outcome.Success(finding)
        override suspend fun evaluate(draft: RequestDraft): Outcome<Evaluation> = error("No analysis expected")
        override suspend fun get(id: String): Outcome<Proposal> = Outcome.Success(plan)
        override fun observeSession() = flowOf(Session("fixture"))
        override suspend fun signIn(): Outcome<Session> = error("No sign-in expected")
        override suspend fun load(): Outcome<RequestDraft> = Outcome.Success(RequestDraft())
        override suspend fun save(draft: RequestDraft): Outcome<Unit> = Outcome.Success(Unit)
        override suspend fun clear(): Outcome<Unit> = Outcome.Success(Unit)
    }
    @OptIn(ExperimentalMaterial3Api::class)
    private fun mount(service: Service) {
        compose.runOnUiThread {
            val vm = WatchesViewModel(service, service, IdGenerator { error("No new identifier") })
            val create = CreateViewModel(service, service, service, service, IdGenerator { "draft" }, SavedStateHandle())
            compose.activity.setContent {
                PreviensTheme {
                    WatchSynchronizationEffect(vm, owner.value)
                    val state by vm.state.collectAsStateWithLifecycle()
                    Scaffold(topBar = { TopAppBar(title = { Text(if (route.value == "result") "Résultat" else "Détail du suivi") }) }) { insets ->
                        Box(Modifier.padding(insets)) {
                            if (owner.value == null) Text("Déconnecté", Modifier.testTag("signed_out"))
                            else when (route.value) {
                                "home" -> HomeScreen(state, CreateUiState(), create, {}, vm::refresh, {}, remote = true)
                                "result" -> FindingScreen("f", state, remote = true)
                                else -> DetailScreen("w", state, vm, {}, { route.value = "result" }, remote = true)
                            }
                        }
                    }
                }
            }
        }
        compose.waitUntil(5000) { service.reads.get() >= 1 }
    }
    private fun capture(name: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val bitmap = checkNotNull(automation.takeScreenshot())
        val file = java.io.File(compose.activity.getExternalFilesDir(null), "sync-$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        automation.executeShellCommand("cp ${file.absolutePath} /data/local/tmp/sync-$name.png").close()
    }
    @Test fun preparationAndResultAppearInBothViewsWithoutTapOrNotification() {
        val service = Service(); mount(service)
        compose.onNodeWithText(compose.activity.getString(R.string.status_pending)).assertIsDisplayed()
        compose.runOnUiThread { service.finished = true }
        val completed = compose.activity.getString(R.string.v1_completed)
        compose.waitUntil(10000) { compose.onAllNodesWithText(completed).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(completed).assertIsDisplayed()
        capture("completed")
        compose.runOnUiThread { route.value = "home" }
        compose.onNodeWithText(completed).assertIsDisplayed()
        compose.runOnUiThread { route.value = "detail" }
        compose.onNodeWithTag("finding").performScrollTo().performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.v1_initial)).assertIsDisplayed()
        compose.onNodeWithText(service.finding.fact).assertIsDisplayed()
        compose.onNodeWithText("Vérifié à cette date. Consulte le site pour connaître les conditions actuelles.").assertDoesNotExist()
        compose.onNodeWithTag("open_source").assertIsDisplayed()
        capture("result")
    }
    @Test fun backgroundStopsReadsResumeReadsImmediatelyAndSignOutStopsAgain() {
        val service = Service(); mount(service)
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        val stopped = service.reads.get()
        Thread.sleep(3500)
        assertEquals(stopped, service.reads.get())
        service.finished = true
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitUntil(5000) { service.reads.get() > stopped }
        compose.onNodeWithText(compose.activity.getString(R.string.v1_completed)).assertIsDisplayed()
        compose.runOnUiThread { owner.value = null }
        compose.onNodeWithTag("signed_out").assertIsDisplayed()
        val signedOut = service.reads.get(); Thread.sleep(3500)
        assertEquals(signedOut, service.reads.get())
    }
}
