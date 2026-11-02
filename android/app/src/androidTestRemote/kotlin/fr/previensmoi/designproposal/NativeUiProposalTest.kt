package fr.previensmoi.designproposal

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.previensmoi.BuildConfig
import fr.previensmoi.app.MainActivity
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeUiProposalTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun captureProposalScreens() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("uiProposal")=="true" && BuildConfig.LOCAL_SERVICE)
        var page by mutableStateOf(ProposalPage.Home)
        var dark by mutableStateOf(false)
        var font by mutableFloatStateOf(1f)
        compose.runOnUiThread {
            compose.activity.setContent {
                val density=LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density,font)) {
                    NativeUiProposal(page,dark) {page=it}
                }
            }
        }
        val scenarios=listOf(
            Triple("home",ProposalPage.Home,false),
            Triple("active",ProposalPage.Active,false),
            Triple("paused",ProposalPage.Paused,false),
            Triple("preparing",ProposalPage.Preparing,false),
            Triple("loading",ProposalPage.Loading,false),
            Triple("home-dark",ProposalPage.Home,true),
            Triple("active-dark",ProposalPage.Active,true),
            Triple("active-large",ProposalPage.Active,false),
        )
        for((name,next,isDark) in scenarios) {
            compose.runOnUiThread {page=next;dark=isDark;font=if(name.endsWith("large")) 1.3f else 1f}
            compose.waitForIdle()
            if(next==ProposalPage.Loading) {
                compose.mainClock.autoAdvance=false
                compose.mainClock.advanceTimeBy(650)
                compose.waitForIdle()
            }
            // One native layout frame beyond the state transition; no entrance choreography.
            Thread.sleep(180)
            val bitmap=checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
            java.io.File(compose.activity.filesDir,"proposal-$name.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
            compose.mainClock.autoAdvance=true
            if(name=="active-large") {
                compose.onNodeWithTag("proposal_sources").performScrollTo()
                compose.waitForIdle()
                val scrolled=checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
                java.io.File(compose.activity.filesDir,"proposal-active-large-sources.png").outputStream().use {scrolled.compress(Bitmap.CompressFormat.PNG,100,it)}
                scrolled.recycle()
            }
        }
    }
}
