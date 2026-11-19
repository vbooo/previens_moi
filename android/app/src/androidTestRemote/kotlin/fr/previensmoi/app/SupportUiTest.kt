package fr.previensmoi.app

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.previensmoi.feature.common.presentation.PreviensTheme
import fr.previensmoi.feature.support.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SupportUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun capture(name: String) {
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
        android.os.SystemClock.sleep(300)
        val bitmap =
            checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        java.io
            .File(compose.activity.getExternalFilesDir(null), "support-$name.png")
            .outputStream()
            .use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test
    fun helpIsAvailableBeforeSignInAndClosingKeepsDraft() {
        compose.onNodeWithTag("support_open").assertExists()
        capture("home")
        compose.onNodeWithTag("support_open").performClick()
        compose.onNodeWithTag("support_send").performScrollTo().assertIsNotEnabled()
        compose
            .onNodeWithTag("support_message")
            .performScrollTo()
            .performTextInput("Une idée pour Préviens-moi")
        compose.onNodeWithTag("support_idea").performScrollTo().performClick()
        capture("draft-keyboard")
        compose.onNodeWithTag("support_close").performScrollTo().performClick()
        compose.waitUntil(5000) {
            compose.onAllNodesWithTag("support_message").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithTag("support_open").performClick()
        compose.waitUntil(5000) {
            compose.onAllNodesWithTag("support_message").fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithTag("support_message").assertTextContains("Une idée pour Préviens-moi")
        compose.onNodeWithTag("support_send").performScrollTo().assertIsEnabled()
        capture("form")
    }

    @Test
    fun pendingFailureAndSuccessKeepActionsClear() {
        val state =
            mutableStateOf(SupportState(opened = true, message = "Synthetic problem", busy = true))
        var sends = 0
        var closed = 0
        compose.activity.setContent {
            PreviensTheme {
                SupportSheet(state.value, {}, {}, {}, { sends++ }, { closed++ }, { closed++ })
            }
        }
        compose.onNodeWithTag("support_send").performScrollTo().assertIsNotEnabled()
        capture("sending")
        compose.runOnIdle {
            state.value = state.value.copy(busy = false, problem = SupportProblem.Offline)
        }
        compose.onNodeWithTag("support_error").performScrollTo().assertExists()
        compose.onNodeWithTag("support_send").performScrollTo().assertIsEnabled().performClick()
        assertEquals(1, sends)
        capture("offline")
        compose.runOnIdle {
            state.value = state.value.copy(received = true, message = "", problem = null)
        }
        compose.onNodeWithTag("support_received").assertExists()
        compose.onNodeWithTag("support_send").assertDoesNotExist()
        capture("received")
    }
}
