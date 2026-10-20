package fr.previensmoi.app

import androidx.compose.ui.test.*
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.SavedStateHandle
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.*
import fr.previensmoi.feature.create.presentation.CreateScreen
import kotlinx.coroutines.flow.flowOf
import android.os.Build
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelProvider
import fr.previensmoi.feature.create.presentation.CreateViewModel
import android.graphics.Bitmap
import fr.previensmoi.BuildConfig
import fr.previensmoi.R
import java.net.URL
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalServiceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val file = java.io.File(compose.activity.getExternalFilesDir(null),name)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        bitmap.recycle()
        // UTP may uninstall the target after tests; keep only these fixture screenshots for adb retrieval.
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("cp ${file.absolutePath} /data/local/tmp/previens-moi-s3-$name").close()
    }
    private fun shown(id: Int) { val text = compose.activity.getString(id); compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
    @Suppress("DEPRECATION")
    private fun closeKeyboard() {
        compose.runOnUiThread {
            if (Build.VERSION.SDK_INT >= 30) compose.activity.window.insetsController?.hide(WindowInsets.Type.ime())
            else compose.activity.getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(compose.activity.window.decorView.windowToken,0)
        }
        if (Build.VERSION.SDK_INT >= 30) compose.waitUntil(5000) { compose.activity.window.decorView.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) != true }
    }
    private fun tap(tag: String) { closeKeyboard(); compose.waitUntil(10000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size == 1 }; compose.onNodeWithTag(tag).performScrollTo().performClick() }
    private fun account() { compose.onNodeWithText(compose.activity.getString(R.string.account_title)).performClick() }
    @Test fun emailCodeSessionRecreationAndHonestAdmission() {
        assumeTrue("Requires dedicated local Supabase configuration",BuildConfig.LOCAL_SERVICE)
        val email = "android-${System.currentTimeMillis()}@example.test"
        account()
        if (compose.onAllNodesWithTag("sign_out").fetchSemanticsNodes().isNotEmpty()) tap("sign_out")
        compose.waitUntil(15000) { compose.onAllNodesWithTag("email").fetchSemanticsNodes().size == 1 }
        compose.onNodeWithTag("email").performTextReplacement(email)
        tap("send_code"); shown(R.string.code_sent)
        val json = Json { ignoreUnknownKeys = true }
        val list = json.parseToJsonElement(URL("http://10.0.2.2:56324/api/v1/messages").readText()).jsonObject["messages"]!!.jsonArray
        val item = list.first { row -> row.jsonObject["To"]!!.jsonArray.any { it.jsonObject["Address"]!!.jsonPrimitive.content == email } }.jsonObject
        val mail = json.parseToJsonElement(URL("http://10.0.2.2:56324/api/v1/message/" + item["ID"]!!.jsonPrimitive.content).readText()).jsonObject
        val code = Regex("\\b\\d{6}\\b").find(mail["Text"]!!.jsonPrimitive.content + mail["HTML"]!!.jsonPrimitive.content)!!.value
        compose.onNodeWithTag("otp").performTextReplacement(code)
        tap("verify_code")
        try { shown(R.string.account_connected) }
        catch (failure: Throwable) {
            val labels = listOf(R.string.error_session,R.string.storage_error,R.string.error_temporary,R.string.error_input)
            println("Auth failure categories: " + labels.map { compose.onAllNodesWithText(compose.activity.getString(it)).fetchSemanticsNodes().isNotEmpty() })
            capture("remote-auth-failure.png")
            throw failure
        }
        compose.activityRule.scenario.recreate(); shown(R.string.account_connected); capture("remote-account.png")
        // The on-disk session is ciphertext; do not print either its plaintext or bytes.
        val ciphertext = java.io.File(compose.activity.noBackupFilesDir,"session.enc").readBytes()
        assertFalse(ciphertext.toString(Charsets.ISO_8859_1).contains(email))
        assertFalse(ciphertext.toString(Charsets.ISO_8859_1).contains("refresh_token"))
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("request").performTextReplacement("Préviens-moi des annonces de ma salle")
        tap("preview")
        try { shown(R.string.remote_unavailable_title) }
        catch (failure: Throwable) {
            val state = ViewModelProvider(compose.activity)[CreateViewModel::class.java].state.value
            println("Preview state: ${state.step}, error=${state.error}, revision=${state.draft.revision}, language=${state.draft.language}, sameText=${state.draft.text == "Préviens-moi des annonces de ma salle"}")
            capture("remote-preview-failure.png")
            throw failure
        }
        capture("remote-unqualified.png")
        // A real Auth session does not qualify a monitoring method or create an activation.
        val evaluationFile=java.io.File(compose.activity.noBackupFilesDir,"evaluation.enc")
        assertTrue(evaluationFile.exists())
        assertFalse(evaluationFile.readBytes().decodeToString().contains("Préviens-moi des annonces de ma salle"))
        val rejection=ViewModelProvider(compose.activity)[CreateViewModel::class.java].state.value.rejection
        assertFalse(rejection.isNullOrBlank())
        compose.onNodeWithText(checkNotNull(rejection)).assertIsDisplayed()
    }
    @Test fun identifiedQuestionsRequiredAnswerAndHonestRejection() {
        var calls=0
        val proposals=object: ProposalRepository {
            override suspend fun get(id: String): Outcome<Proposal> = Outcome.Error(Failure.NotFound)
            override suspend fun evaluate(draft: RequestDraft): Outcome<Evaluation> {
                calls++
                return if(calls==1) Outcome.Success(Evaluation.Questions(listOf(
                    ClarificationQuestion("zone","Zone de livraison",listOf("France","Belgique"),true),
                    ClarificationQuestion("budget","Budget complémentaire",emptyList(),false))))
                else { assertEquals("France",draft.answers["zone"]);assertEquals("150",draft.answers["budget"]);Outcome.Success(Evaluation.Rejected("unqualified_method","Sources insuffisantes pour activer cette surveillance.")) }
            }
        }
        val watches=object: WatchRepository {
            override fun observeList()=flowOf(emptyList<Watch>())
            override suspend fun refresh(): RefreshResult=RefreshResult.Success
            override suspend fun execute(command: Command): Outcome<CommandReceipt> = Outcome.Error(Failure.Unsupported)
        }
        val sessions=object: SessionRepository {
            override fun observeSession()=flowOf<Session?>(null)
            override suspend fun signIn(): Outcome<Session> = Outcome.Error(Failure.Unsupported)
        }
        val drafts=object: DraftRepository {
            override suspend fun load(): Outcome<RequestDraft> = Outcome.Success(RequestDraft())
            override suspend fun save(draft: RequestDraft): Outcome<Unit> = Outcome.Success(Unit)
            override suspend fun clear(): Outcome<Unit> = Outcome.Success(Unit)
        }
        compose.runOnUiThread {
            val vm=CreateViewModel(proposals,watches,sessions,drafts,IdGenerator { "fixture" },SavedStateHandle())
            compose.activity.setContent { MaterialTheme { val state by vm.state.collectAsState();CreateScreen(state,vm,remote=true) } }
        }
        compose.waitUntil(5000) { compose.onAllNodesWithTag("request").fetchSemanticsNodes().size==1 }
        compose.onNodeWithTag("request").performTextReplacement("Produit de test")
        tap("evaluate")
        compose.onNodeWithText("Zone de livraison").performScrollTo().assertIsDisplayed()
        tap("evaluate")
        compose.onNodeWithText(compose.activity.getString(R.string.error_input)).performScrollTo().assertIsDisplayed()
        assertEquals(1,calls)
        compose.onNodeWithText("France").performScrollTo().performClick()
        compose.onNodeWithTag("answer_budget").performScrollTo().performTextReplacement("150")
        tap("evaluate")
        compose.waitUntil(5000) { compose.onAllNodesWithText("Sources insuffisantes pour activer cette surveillance.").fetchSemanticsNodes().size==1 }
        compose.onNodeWithText("Sources insuffisantes pour activer cette surveillance.").assertIsDisplayed()
        assertEquals(2,calls)
    }

    @Test fun notificationClaimPersistsAcrossInstancesAndIsScopedByAccount() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val owner=java.util.UUID.randomUUID().toString();val other=java.util.UUID.randomUUID().toString();val finding=java.util.UUID.randomUUID().toString()
        assertTrue(fr.previensmoi.data.push.NotificationClaims(context).claim(owner,finding,1_000))
        assertFalse(fr.previensmoi.data.push.NotificationClaims(context).claim(owner,finding,2_000))
        assertTrue(fr.previensmoi.data.push.NotificationClaims(context).claim(other,finding,2_000))
        for(id in listOf(owner,other)) java.io.File(context.noBackupFilesDir,"notification-claims-$id").delete()
    }

}
