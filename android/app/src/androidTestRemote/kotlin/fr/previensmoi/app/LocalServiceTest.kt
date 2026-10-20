package fr.previensmoi.app

import androidx.compose.ui.test.*
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
        compose.onNodeWithText(compose.activity.getString(R.string.unqualified_body)).assertIsDisplayed()
    }
}
