package fr.previensmoi.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.previensmoi.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Captures the actual signed-out app. No sign-in, submission or provider request. */
@RunWith(AndroidJUnit4::class)
class StoreAssetsUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun capture(name: String) {
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
        val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        java.io.File(compose.activity.getExternalFilesDir(null), "store-$name.jpg").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
        }
        bitmap.recycle()
    }
    @Test fun capturePublicScreensWithoutSendingData() {
        compose.waitUntil(5000) { compose.onAllNodesWithTag("new_request_empty").fetchSemanticsNodes().size == 1 }
        compose.onNodeWithTag("new_request_empty").assertIsDisplayed()
        capture("01-home")
        compose.onNodeWithTag("new_request_empty").performClick()
        compose.onNodeWithTag("request").assertIsDisplayed()
        capture("02-request")
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.back)).performClick()
        compose.onNodeWithTag("support_open").performClick()
        compose.onNodeWithTag("support_send").performScrollTo().assertIsNotEnabled()
        capture("03-help")
    }
}
