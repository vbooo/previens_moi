package fr.previensmoi.app

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.previensmoi.R
import fr.previensmoi.domain.model.*
import fr.previensmoi.feature.common.presentation.PreviensTheme
import fr.previensmoi.feature.create.presentation.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Admission fixtures never start an evaluation, purchase or watch. */
@RunWith(AndroidJUnit4::class)
class CreationAccessUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val draft =
        RequestDraft("Promotions PlayStation 5", answers = mapOf("region" to "France"))
    private val proposal =
        Proposal(
            "fixture",
            1,
            draft,
            Long.MAX_VALUE,
            title = draft.text,
            backgroundPreparation = true,
            trialDays = 7,
        )

    @OptIn(ExperimentalMaterial3Api::class)
    private fun show(state: State<CreateUiState>, openSubscription: () -> Unit = {}) {
        val vm = ViewModelProvider(compose.activity)[CreateViewModel::class.java]
        compose.activity.setContent {
            PreviensTheme {
                Scaffold(topBar = { TopAppBar(title = { Text("Préviens-moi") }) }) { insets ->
                    Box(Modifier.padding(insets)) {
                        CreateScreen(
                            state.value,
                            vm,
                            remote = true,
                            subscription = openSubscription,
                        )
                    }
                }
            }
        }
    }

    private fun state(step: CreationStep, error: Failure, paid: Boolean = false) =
        CreateUiState(
            loaded = true,
            draft = draft,
            step = step,
            error = error,
            proposal = proposal.copy(accessTier = if (paid) "plus" else null),
        )

    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap =
            checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        java.io
            .File(compose.activity.getExternalFilesDir(null), "access-$name.png")
            .outputStream()
            .use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test
    fun trialAndExpiredSubscriptionOfferPlusAtEveryCreationStep() {
        val rendered = mutableStateOf(state(CreationStep.Editing, Failure.TrialEnded))
        var opened = 0
        show(rendered) { opened++ }
        for (error in listOf(Failure.TrialEnded, Failure.SubscriptionRequired)) {
            for (step in
                listOf(CreationStep.Editing, CreationStep.Clarifying, CreationStep.Ready)) {
                compose.runOnIdle { rendered.value = state(step, error) }
                val action = compose.onNodeWithTag("creation_subscription")
                if (step != CreationStep.Ready) action.performScrollTo()
                action.assertIsEnabled().performClick()
                compose.onNodeWithTag("evaluate").assertDoesNotExist()
                compose.onNodeWithTag("confirm").assertDoesNotExist()
            }
        }
        compose.runOnIdle { rendered.value = state(CreationStep.Ready, Failure.TrialEnded) }
        compose
            .onNodeWithText(compose.activity.getString(R.string.trial_ended_body))
            .assertIsDisplayed()
        capture("trial-ended")
        assertEquals(6, opened)
    }

    @Test
    fun activeWatchLimitOffersPlusOnlyForFreeTrial() {
        val rendered = mutableStateOf(state(CreationStep.Ready, Failure.ActiveLimit))
        show(rendered)
        compose.onNodeWithTag("creation_subscription").assertIsDisplayed()
        compose.runOnIdle {
            rendered.value = state(CreationStep.Ready, Failure.ActiveLimit, paid = true)
        }
        compose.onNodeWithTag("creation_subscription").assertDoesNotExist()
        compose.runOnIdle {
            rendered.value = state(CreationStep.Ready, Failure.MonthlyLimit, paid = true)
        }
        compose.onNodeWithTag("creation_subscription").assertDoesNotExist()
    }

    @Test
    fun globalCapacityKeepsRequestAndNeverSuggestsPayment() {
        val rendered = mutableStateOf(state(CreationStep.Editing, Failure.QuotaExceeded))
        show(rendered)
        compose.onNodeWithTag("request").assertTextContains(draft.text)
        compose.onNodeWithTag("creation_subscription").assertDoesNotExist()
        compose.onNodeWithTag("evaluate").performScrollTo().assertIsEnabled()
        compose.onNodeWithText(compose.activity.getString(R.string.error_quota)).assertIsDisplayed()
        capture("service-capacity")
        compose.runOnIdle { rendered.value = state(CreationStep.Ready, Failure.QuotaExceeded) }
        compose.onNodeWithTag("creation_subscription").assertDoesNotExist()
        compose.onNodeWithTag("confirm").assertIsEnabled()
        compose.onNodeWithText(compose.activity.getString(R.string.error_quota)).assertIsDisplayed()
        capture("confirmation-capacity")
    }
}
