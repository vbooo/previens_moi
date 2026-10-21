package fr.previensmoi.app

import android.os.Build
import android.view.WindowInsets
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import fr.previensmoi.feature.session.presentation.SessionViewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.previensmoi.BuildConfig
import fr.previensmoi.R
import fr.previensmoi.data.auth.KeystoreSessionVault
import java.net.HttpURLConnection
import java.net.URL
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in real mailbox flow; the code is supplied privately at runtime, never in the APK. */
@RunWith(AndroidJUnit4::class)
class CloudEmailSignInTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun tap(tag: String) {
        if (Build.VERSION.SDK_INT >= 30) compose.runOnUiThread {
            compose.activity.window.insetsController?.hide(WindowInsets.Type.ime())
        }
        if (Build.VERSION.SDK_INT >= 30) compose.waitUntil(5000) {
            compose.activity.window.decorView.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) != true
        }
        compose.waitForIdle()
        compose.onNodeWithTag(tag).performScrollTo().performClick()
    }
    private fun shown(id: Int) = compose.waitUntil(30000) {
        compose.onAllNodesWithText(compose.activity.getString(id)).fetchSemanticsNodes().isNotEmpty()
    }
    private fun suppliedCode(): String {
        val descriptor = instrumentation.uiAutomation.executeShellCommand("cat /data/local/tmp/previens-moi-email-code.txt")
        return android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText().trim() }
    }
    @Test fun realMailboxCodeConnectsAndSurvivesActivityRecreation() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("cloudEmailTestEnabled") == "true")
        assumeTrue(args.getString("emailPhase", "request") == "request")
        check(!BuildConfig.LOCAL_SERVICE && URL(BuildConfig.AUTH_URL).host.endsWith(".supabase.co"))
        val email = checkNotNull(args.getString("testEmail"))
        check(email.contains('@'))
        compose.onNodeWithText(compose.activity.getString(R.string.account_title)).performClick()
        if (compose.onAllNodesWithTag("sign_out").fetchSemanticsNodes().isNotEmpty()) tap("sign_out")
        compose.onNodeWithTag("email").performTextReplacement(email)
        val vm = ViewModelProvider(compose.activity)[SessionViewModel::class.java]
        assertEquals("Complete test email entered", email, vm.state.value.email)
        tap("send_code")
        try { shown(R.string.code_sent) } catch (failure: Throwable) {
            val state = vm.state.value
            println("Email request state: sent=${state.sent}, busy=${state.busy}, error=${state.error}, emailMatches=${state.email == email}")
            throw failure
        }
        instrumentation.sendStatus(0, android.os.Bundle().apply { putString("stream", "\nCLOUD_EMAIL_CODE_REQUESTED\n") })
        val deadline = System.currentTimeMillis() + 300000
        var code = ""
        while (System.currentTimeMillis() < deadline) {
            val candidate = suppliedCode()
            if (candidate.length == 6 && candidate.all { it.isDigit() }) { code = candidate; break }
            Thread.sleep(1000)
        }
        check(code.length == 6) { "Private email code was not supplied in time" }
        compose.onNodeWithTag("otp").performTextReplacement(code)
        tap("verify_code")
        shown(R.string.account_connected)
        val vault = KeystoreSessionVault(compose.activity)
        val credentials = checkNotNull(vault.read())
        val connection = URL(BuildConfig.SERVICE_URL + "api/v1/account").openConnection() as HttpURLConnection
        connection.connectTimeout = 15000; connection.readTimeout = 15000
        connection.setRequestProperty("Authorization", "Bearer " + credentials.accessToken)
        assertEquals("Real email session can access the cloud account", 200, connection.responseCode)
        connection.disconnect()
        val ciphertext = java.io.File(compose.activity.noBackupFilesDir, "session.enc").readBytes().toString(Charsets.ISO_8859_1)
        assertFalse("Session stored encrypted", ciphertext.contains(credentials.accessToken))
        assertFalse("Refresh token stored encrypted", ciphertext.contains(credentials.refreshToken))
        compose.activityRule.scenario.recreate()
        shown(R.string.account_connected)
        assertEquals(credentials.userId, checkNotNull(KeystoreSessionVault(compose.activity).read()).userId)
    }
    @Test fun verifyPreviouslyRequestedMailboxCodeAndRestoreSession() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("cloudEmailTestEnabled") == "true")
        assumeTrue(args.getString("emailPhase") == "resume")
        check(!BuildConfig.LOCAL_SERVICE && URL(BuildConfig.AUTH_URL).host.endsWith(".supabase.co"))
        val email = checkNotNull(args.getString("testEmail"))
        val code = suppliedCode()
        check(code.length == 6 && code.all { it.isDigit() }) { "Private code format invalid" }
        // Resume the already proven UI send after the human code-entry wait expired.
        // Use the real app repository and real Auth verification; no synthetic session injection.
        val repository = (compose.activity.application as PreviensMoiApplication).sessions
        val result = kotlinx.coroutines.runBlocking { repository.verifyCode(email, code) }
        assertTrue("Real mailbox code accepted by app repository", result is fr.previensmoi.domain.model.Outcome.Success)
        compose.onNodeWithText(compose.activity.getString(R.string.account_title)).performClick()
        shown(R.string.account_connected)
        val credentials = checkNotNull(KeystoreSessionVault(compose.activity).read())
        val connection = URL(BuildConfig.SERVICE_URL + "api/v1/account").openConnection() as HttpURLConnection
        connection.connectTimeout = 15000; connection.readTimeout = 15000
        connection.setRequestProperty("Authorization", "Bearer " + credentials.accessToken)
        assertEquals("Real email session can access cloud account", 200, connection.responseCode)
        connection.disconnect()
        val ciphertext = java.io.File(compose.activity.noBackupFilesDir, "session.enc").readBytes().toString(Charsets.ISO_8859_1)
        assertFalse("Access token stored encrypted", ciphertext.contains(credentials.accessToken))
        assertFalse("Refresh token stored encrypted", ciphertext.contains(credentials.refreshToken))
        compose.activityRule.scenario.recreate()
        shown(R.string.account_connected)
        assertEquals(credentials.userId, checkNotNull(KeystoreSessionVault(compose.activity).read()).userId)
    }

    @Test fun restoredEmailSessionCanSignOut() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("cloudEmailTestEnabled") == "true")
        assumeTrue(args.getString("emailPhase") == "restore")
        check(!BuildConfig.LOCAL_SERVICE && URL(BuildConfig.AUTH_URL).host.endsWith(".supabase.co"))
        val before = checkNotNull(KeystoreSessionVault(compose.activity).read())
        compose.onNodeWithText(compose.activity.getString(R.string.account_title)).performClick()
        shown(R.string.account_connected)
        val repository = (compose.activity.application as PreviensMoiApplication).sessions
        assertEquals(before.userId, repository.currentUserId())
        tap("sign_out")
        compose.waitUntil(30000) { compose.onAllNodesWithTag("email").fetchSemanticsNodes().size == 1 }
        assertNull("Local credentials removed on sign-out", KeystoreSessionVault(compose.activity).read())
        assertNull("Repository session cleared on sign-out", repository.currentUserId())
        assertFalse(java.io.File(compose.activity.noBackupFilesDir, "session.enc").exists())
    }

}
