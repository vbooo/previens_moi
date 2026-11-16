package fr.previensmoi.app

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.previensmoi.domain.model.*
import fr.previensmoi.feature.common.presentation.PreviensTheme
import fr.previensmoi.feature.subscription.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Controlled native states, with no real Play purchase or provider request. */
@RunWith(AndroidJUnit4::class)
class SubscriptionUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun capture(name: String) {
        compose.waitForIdle()
        val screenshot =
            checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        java.io
            .File(compose.activity.getExternalFilesDir(null), "billing-$name.png")
            .outputStream()
            .use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        screenshot.recycle()
    }

    @Test
    fun subscriptionStatesKeepCheckoutDisabledUntilProductAndServerAreReady() {
        val state =
            mutableStateOf(SubscriptionUiState(loaded = true, access = SubscriptionAccess()))
        var subscribed = 0
        var restored = 0
        var managed = 0
        compose.activity.setContent {
            PreviensTheme {
                Scaffold { padding ->
                    Box(Modifier.padding(padding)) {
                        SubscriptionScreen(
                            state.value,
                            { subscribed++ },
                            { restored++ },
                            {},
                            { managed++ },
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("subscribe").assertIsNotEnabled()
        capture("unavailable")
        compose.runOnIdle {
            state.value =
                state.value.copy(
                    access = SubscriptionAccess(canPurchase = true, accountId = "a".repeat(64)),
                    product = SubscriptionProduct("6,99 €"),
                )
        }
        compose.onNodeWithTag("subscribe").assertIsEnabled().performClick()
        assertEquals(1, subscribed)
        capture("offer")
        compose.runOnIdle {
            state.value =
                state.value.copy(
                    access =
                        state.value.access!!.copy(
                            status = SubscriptionStatus.Active,
                            active = true,
                            remainingCreations = 3,
                            expiresAt = 4102444800000,
                        )
                )
        }
        compose.onNodeWithTag("subscribe").assertDoesNotExist()
        compose
            .onNodeWithTag("subscription_remaining")
            .performScrollTo()
            .assertTextContains("3", substring = true)
        capture("active")
        compose.onNodeWithTag("manage_subscription").performScrollTo().performClick()
        assertEquals(1, managed)
        compose.runOnIdle {
            state.value =
                state.value.copy(
                    access =
                        state.value.access!!.copy(
                            status = SubscriptionStatus.Pending,
                            active = false,
                        ),
                    problem = BillingProblem.Pending,
                )
        }
        compose.onNodeWithTag("subscribe").assertIsNotEnabled()
        compose.onNodeWithTag("restore_purchases").performScrollTo().performClick()
        assertEquals(1, restored)
        capture("pending")
        compose.runOnIdle {
            state.value = SubscriptionUiState(loaded = true, problem = BillingProblem.Offline)
        }
        compose.onNodeWithTag("subscription_retry").assertIsEnabled()
        compose.onNodeWithTag("subscribe").assertDoesNotExist()
    }
}
