package fr.previensmoi.app

import androidx.compose.ui.test.*
import android.os.Build
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import fr.previensmoi.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoundationsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun capture(name: String) {
        val args = InstrumentationRegistry.getArguments()
        val prefix = args.getString("capturePrefix") ?: return
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "$prefix-$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    private fun text(id: Int) = compose.activity.getString(id)
    private fun awaitTag(tag: String) {
        try { compose.waitUntil(5_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size == 1 } }
        catch (failure: Throwable) { println("Missing demo tag: $tag\n" + compose.onRoot().printToString()); throw failure }
    }
    @Suppress("DEPRECATION")
    private fun closeKeyboard() {
        compose.runOnUiThread {
            if (Build.VERSION.SDK_INT >= 30) compose.activity.window.insetsController?.hide(WindowInsets.Type.ime())
            else compose.activity.getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(compose.activity.window.decorView.windowToken,0)
        }
        if (Build.VERSION.SDK_INT >= 30) compose.waitUntil(5000) { compose.activity.window.decorView.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) != true }
    }
    private fun tap(tag: String) { closeKeyboard(); awaitTag(tag); runCatching { compose.onNodeWithTag(tag).performScrollTo() }; compose.onNodeWithTag(tag).performClick() }
    private fun status(id:Int) {
        val text=text(if(id==R.string.status_active) R.string.v1_active else if(id==R.string.status_completed) R.string.v1_completed else id)
        compose.waitUntil(5000){compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()}
        compose.onAllNodesWithText(text).onFirst().performScrollTo().assertIsDisplayed()
    }
    private fun shown(id: Int) {
        compose.waitUntil(5_000) { compose.onAllNodesWithText(text(id)).fetchSemanticsNodes().size == 1 }
        compose.onNodeWithText(text(id)).performScrollTo().assertIsDisplayed()
    }
    private fun scenario(name: String) {
        compose.onNodeWithTag("tools").performClick()
        compose.onNodeWithTag("scenario_menu").performClick()
        compose.onNodeWithTag("scenario_$name").performScrollTo().performClick()
    }
    private fun back() { compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() } }
    private fun check() { compose.onNodeWithTag("tools").performClick(); compose.onNodeWithTag("simulate_check").performClick() }
    private fun activate(continuous: Boolean = false) {
        compose.onNodeWithTag("request").performTextReplacement("Concerts de ma salle favorite")
        tap("preview"); awaitTag("confirm"); if(continuous)tap("continuous"); capture("proposal"); tap("confirm"); tap("login"); tap("confirm")
        status(R.string.status_active)
    }
    @Before fun resetDemo() {
        compose.onNodeWithTag("tools").performClick(); compose.onNodeWithTag("reset_demo").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("request") and isEnabled()).fetchSemanticsNodes().size == 1 }
        compose.onNodeWithTag("request").performTextReplacement("")
    }
    @Test fun nominalCreationRecreationBackAndPublishedDateUnknown() {
        capture("home"); activate(); capture("detail"); compose.activityRule.scenario.recreate()
        status(R.string.status_active)
        back(); awaitTag("request")
        compose.onAllNodes(hasTestTagPrefix("watch_") and hasClickAction()).onFirst().performScrollTo().performClick()
        check(); status(R.string.status_completed)
        tap("finding"); compose.onNodeWithTag("publication").assertDoesNotExist()
        shown(R.string.finding_info); capture("finding")
    }
    @Test fun clarificationAndPartialScopeRequireExplicitConsent() {
        scenario("Clarification")
        compose.onNodeWithTag("request").performTextReplacement("Nouveautes sur un sujet")
        tap("preview"); awaitTag("answer")
        compose.onNodeWithTag("answer").performScrollTo().performTextReplacement("Annonces et changements de date")
        scenario("Partial"); tap("evaluate"); awaitTag("confirm")
        compose.onNodeWithTag("confirm").assertIsNotEnabled(); capture("partial")
        tap("accept_partial"); tap("confirm")
        awaitTag("login"); compose.onNodeWithTag("login").performScrollTo().assertIsDisplayed(); tap("login")
        shown(R.string.partial_text)
        tap("confirm"); status(R.string.status_active)
    }
    @Test fun failedPauseThenResumeEditOldResultAndDelete() {
        activate(continuous = true); check()
        scenario("CommandFailure"); tap("pause")
        status(R.string.status_active)
        shown(R.string.error_temporary)
        scenario("Normal"); tap("pause")
        status(R.string.status_paused)
        tap("resume"); tap("edit"); awaitTag("request")
        compose.onNodeWithTag("request").performTextReplacement("Nouveaux criteres")
        tap("evaluate"); tap("confirm")
        tap("finding"); shown(R.string.old_plan)
        back(); tap("delete"); compose.onNodeWithTag("confirm_delete").performClick()
        awaitTag("request")
    }
    @Test fun noResultsAndSourceFailureAreDistinct() {
        activate(continuous = true); scenario("NoResults"); check()
        shown(R.string.no_results)
        status(R.string.status_active)
        scenario("SourceFailure"); check()
        shown(R.string.v1_last_failed)
        shown(R.string.no_results)
    }
    @Test fun unsupportedRequestCanBeCorrectedAndDraftSurvivesRecreation() {
        scenario("Unsupported"); compose.onNodeWithTag("request").performTextReplacement("Mon compte prive")
        tap("preview"); shown(R.string.unsupported_title)
        compose.onNodeWithText(text(R.string.revise)).performScrollTo().performClick()
        compose.onNodeWithTag("request").performTextReplacement("Brouillon conserve")
        compose.activityRule.scenario.recreate(); awaitTag("request")
        compose.onNodeWithTag("request").assertTextContains("Brouillon conserve")
    }
    private fun hasTestTagPrefix(prefix: String) = SemanticsMatcher("tag starts with $prefix") {
        it.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.TestTag) { "" }.startsWith(prefix)
    }
}
