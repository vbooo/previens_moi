package fr.previensmoi.app

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import fr.previensmoi.R
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.*
import fr.previensmoi.feature.common.presentation.PreviensTheme
import fr.previensmoi.feature.watches.presentation.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Native controls against deterministic command receipts; no cloud or provider requests. */
class PauseResumeUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private class Service : WatchRepository, FindingRepository {
        val plan = Proposal("p", 1, RequestDraft("Promotions PS5"), Long.MAX_VALUE,
            title = "Promotions sur la PlayStation 5", sources = listOf("https://example.test/ps5"), backgroundPreparation = true)
        val paused = Watch("w", plan, status = WatchStatus.Paused, health = Health.Healthy, baselineAt = 1)
        val items = MutableStateFlow(listOf(paused))
        val commands = mutableListOf<Command>()
        val stopped = CompletableDeferred<Unit>()
        override fun observeList() = items
        override fun observeFindings() = flowOf(emptyList<Finding>())
        override suspend fun getFinding(id: String): Outcome<Finding> = Outcome.Error(Failure.NotFound)
        override suspend fun refresh() = RefreshResult.Success
        override suspend fun execute(command: Command): Outcome<CommandReceipt> {
            commands += command
            if (command.operation == Operation.Resume) {
                items.value = listOf(paused.copy(status = WatchStatus.Pending, remoteState = RemoteState.Starting, version = 2))
                return Outcome.Success(CommandReceipt(command.id, "w", null, acceptedInBackground = true))
            }
            check(command.operation == Operation.Pause)
            if (commands.count { it.operation == Operation.Pause } == 1) return Outcome.Error(Failure.OperationPending)
            stopped.await()
            items.value = listOf(paused.copy(version = 3))
            return Outcome.Success(CommandReceipt(command.id, "w", 3))
        }
    }
    private fun tap(tag: String) = compose.onNodeWithTag(tag).performScrollTo().performClick()
    private fun await(tag: String) = compose.waitUntil(5000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun capture(name: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val bitmap = checkNotNull(automation.takeScreenshot())
        val file = java.io.File(compose.activity.getExternalFilesDir(null), "operations-$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        automation.executeShellCommand("cp ${file.absolutePath} /data/local/tmp/operations-$name.png").close()
    }
    @Test fun acceptedResumeIsCancellableAndPauseUsesOnlyTheStatusUntilConfirmed() {
        val service = Service()
        compose.runOnUiThread {
            var ids = 0
            val vm = WatchesViewModel(service, service, IdGenerator { "command-${++ids}" })
            compose.activity.setContent { PreviensTheme { val state by vm.state.collectAsState(); DetailScreen("w", state, vm, {}, {}, remote = true) } }
        }
        await("resume"); tap("resume")
        await("pause"); compose.onNodeWithTag("pause").assertIsEnabled()
        compose.onNodeWithText(compose.activity.getString(R.string.cancel_preparation)).assertIsDisplayed()
        compose.onNodeWithTag("retry_command").assertDoesNotExist()
        capture("resuming-cancellable")
        tap("pause")
        compose.onNodeWithText(compose.activity.getString(R.string.v3_pausing)).assertIsDisplayed()
        compose.onNodeWithTag("pause").assertDoesNotExist()
        compose.onNodeWithTag("resume").assertDoesNotExist()
        compose.onNodeWithText("L’arrêt du suivi est en cours de confirmation.").assertDoesNotExist()
        compose.onNodeWithText("Demande envoyée. Nous attendons la confirmation du service.").assertDoesNotExist()
        compose.onNodeWithTag("watch_status").performTouchInput { click() }
        capture("pausing")
        compose.runOnUiThread { service.stopped.complete(Unit) }
        await("resume"); compose.onNodeWithTag("resume").assertIsEnabled()
        compose.onNodeWithText(compose.activity.getString(R.string.status_paused)).assertIsDisplayed()
        assertEquals(listOf(Operation.Resume, Operation.Pause), service.commands.distinctBy { it.id }.map { it.operation })
        assertEquals(2L, service.commands.first { it.operation == Operation.Pause }.expectedVersion)
        capture("paused")
    }
    @Test fun pendingBeyondShortWaitDoesNotFlashRecoveryButRealReadFailureOffersIt() {
        val service = Service()
        val snapshot = mutableStateOf(WatchesUiState(watches = listOf(service.paused.copy(status = WatchStatus.Pending, remoteState = RemoteState.Starting)),
            commandOperation = Operation.Resume, commandWatchId = "w", commandUnconfirmed = true,
            commandPending = false, commandError = Failure.OperationPending))
        compose.runOnUiThread {
            val vm = WatchesViewModel(service, service, IdGenerator { "unused" })
            compose.activity.setContent { PreviensTheme { DetailScreen("w", snapshot.value, vm, {}, {}, remote = true) } }
        }
        compose.onNodeWithText(compose.activity.getString(R.string.v3_resuming)).assertIsDisplayed()
        compose.onNodeWithTag("retry_command").assertDoesNotExist()
        compose.onNodeWithTag("pause").assertDoesNotExist()
        compose.runOnUiThread { snapshot.value = snapshot.value.copy(commandError = Failure.TemporaryFailure) }
        compose.onNodeWithText(compose.activity.getString(R.string.v3_confirmation_unknown)).assertIsDisplayed()
        compose.onNodeWithTag("retry_command").assertIsEnabled()
    }
}
