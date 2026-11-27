package fr.previensmoi.app

import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.SessionRepository
import fr.previensmoi.feature.session.presentation.*
import fr.previensmoi.feature.common.presentation.PreviensTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Fake authentication only; never sends an email or contacts a cloud account. */
@RunWith(AndroidJUnit4::class)
class ReviewAccessUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun privacyIsAccessibleBeforeLoginAndPasswordUsesNormalCredentials() {
        var passwordCalls = 0
        var otpCalls = 0
        val sessions = MutableStateFlow<Session?>(null)
        val repo = object : SessionRepository {
            override fun observeSession() = sessions
            override suspend fun signIn(): Outcome<Session> = Outcome.Error(Failure.Unsupported)
            override suspend fun requestCode(email: String): Outcome<Unit> { otpCalls++; return Outcome.Success(Unit) }
            override suspend fun signInWithPassword(email: String, password: String): Outcome<Session> {
                passwordCalls++
                assertEquals("fixture@example.test", email)
                assertEquals("fixture-only-password", password)
                return Outcome.Error(Failure.SessionRequired)
            }
        }
        val vm = SessionViewModel(repo)
        compose.activity.setContent { PreviensTheme { SessionScreen(vm.state.collectAsState().value, vm) } }
        compose.onNodeWithTag("privacy_open").performScrollTo().performClick()
        compose.onNodeWithTag("privacy_full").assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(fr.previensmoi.R.string.v1_close)).performClick()
        compose.onNodeWithTag("password_mode").performScrollTo().performClick()
        compose.onNodeWithTag("email").performScrollTo().performTextInput("fixture@example.test")
        compose.onNodeWithTag("password").performScrollTo().performTextInput("fixture-only-password")
        compose.onNodeWithTag("password_sign_in").performScrollTo().performClick()
        compose.waitUntil(5000) { passwordCalls == 1 }
        compose.onNodeWithTag("password").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
        assertEquals(0, otpCalls)
        compose.onNodeWithTag("password_mode").performScrollTo().performClick()
        compose.onNodeWithTag("send_code").assertExists()
        assertNull(sessions.value)
    }
}
