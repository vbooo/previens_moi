package fr.previensmoi.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.previensmoi.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoundationsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun exampleSurvivesRecreationAndReturnsToList() {
        fun text(id: Int) = compose.activity.getString(id)
        val banner = text(R.string.demo_banner)
        val refresh = text(R.string.refresh)
        val open = text(R.string.open_detail)
        val detail = text(R.string.sample_detail)
        val title = text(R.string.title)
        compose.onNodeWithText(banner).assertIsDisplayed()
        compose.onNodeWithText(refresh).performClick()
        compose.onNodeWithText(open).performClick()
        compose.onNodeWithText(detail).assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText(detail).assertIsDisplayed()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText(title).assertIsDisplayed()
        compose.onNodeWithText(open).assertIsDisplayed()
        compose.onNodeWithText(banner).assertIsDisplayed()
    }
}
