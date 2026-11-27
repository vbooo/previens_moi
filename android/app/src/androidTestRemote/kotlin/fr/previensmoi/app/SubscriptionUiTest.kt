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
        compose.onNodeWithTag("subscribe").assertDoesNotExist()
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
                        ),
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
        compose.onNodeWithTag("subscribe").assertDoesNotExist()
        compose.onNodeWithTag("restore_purchases").performScrollTo().performClick()
        assertEquals(1, restored)
        capture("pending")
        compose.runOnIdle {
            state.value = SubscriptionUiState(loaded = true, problem = BillingProblem.Offline)
        }
        compose.onNodeWithTag("subscription_retry").assertIsEnabled()
        compose.onNodeWithTag("subscribe").assertDoesNotExist()
    }

    @Test
    fun checkingHidesCheckoutAndCompletionIsVisibleAtTheBottom() {
        val state =
            mutableStateOf(
                SubscriptionUiState(
                    loaded = true,
                    busy = true,
                    access =
                        SubscriptionAccess(status = SubscriptionStatus.Expired, canPurchase = true),
                    product = SubscriptionProduct("6,99 €"),
                ),
            )
        compose.activity.setContent {
            PreviensTheme {
                Scaffold { padding ->
                    Box(Modifier.padding(padding)) {
                        SubscriptionScreen(
                            state.value,
                            {},
                            {},
                            {},
                            {},
                            dismissRestoreMessage = {
                                state.value = state.value.copy(restored = false)
                            },
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("subscribe").assertDoesNotExist()
        compose.onNodeWithTag("restore_purchases").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle {
            state.value =
                state.value.copy(
                    busy = false,
                    restored = true,
                    access =
                        state.value.access!!.copy(
                            status = SubscriptionStatus.Active,
                            active = true,
                        ),
                )
        }
        compose
            .onNodeWithText(compose.activity.getString(fr.previensmoi.R.string.billing_restored))
            .assertIsDisplayed()
        compose.onNodeWithTag("subscribe").assertDoesNotExist()
        capture("checked-active")
        compose.runOnIdle {
            state.value =
                state.value.copy(
                    access = state.value.access!!.copy(status = SubscriptionStatus.Cancelled),
                )
        }
        compose
            .onNodeWithText(
                compose.activity.getString(fr.previensmoi.R.string.billing_restored_cancelled),
            )
            .assertIsDisplayed()
        compose
            .onNodeWithText(compose.activity.getString(fr.previensmoi.R.string.billing_restored))
            .assertDoesNotExist()
        compose.onNodeWithTag("subscribe").assertDoesNotExist()
        capture("checked-cancelled")
        compose.runOnIdle {
            state.value =
                state.value.copy(
                    access =
                        state.value.access!!.copy(
                            status = SubscriptionStatus.Expired,
                            active = false,
                        ),
                )
        }
        compose
            .onNode(
                hasText(compose.activity.getString(fr.previensmoi.R.string.billing_expired)) and
                    hasAnyAncestor(hasTestTag("subscription_feedback")),
            )
            .assertIsDisplayed()
        compose.onNodeWithTag("subscribe").assertIsEnabled()
    }

    @Test
    fun paymentRecoveryStatesNeverOfferCheckoutOrClaimNoSubscription() {
        val state =
            mutableStateOf(
                SubscriptionUiState(
                    loaded = true,
                    access =
                        SubscriptionAccess(
                            status = SubscriptionStatus.Grace,
                            active = true,
                            expiresAt = 4102444800000,
                            canPurchase = true,
                        ),
                    product = SubscriptionProduct("6,99 €"),
                ),
            )
        var managed = 0
        compose.activity.setContent {
            PreviensTheme {
                Scaffold { padding ->
                    Box(Modifier.padding(padding)) {
                        SubscriptionScreen(
                            state.value,
                            {},
                            {},
                            {},
                            { managed++ },
                            { state.value = state.value.copy(restored = false) },
                        )
                    }
                }
            }
        }
        compose
            .onNodeWithTag("subscription_status")
            .assertTextEquals(compose.activity.getString(fr.previensmoi.R.string.billing_grace))
        compose
            .onNodeWithText(
                compose.activity.getString(
                    fr.previensmoi.R.string.billing_grace_until,
                    java.text.DateFormat.getDateTimeInstance(
                            java.text.DateFormat.MEDIUM,
                            java.text.DateFormat.SHORT,
                        )
                        .format(java.util.Date(4102444800000)),
                ),
            )
            .assertExists()
        compose.onNodeWithTag("subscribe").assertDoesNotExist()
        capture("grace")
        compose
            .onNodeWithTag("manage_subscription")
            .performScrollTo()
            .assertTextEquals(
                compose.activity.getString(fr.previensmoi.R.string.billing_fix_payment),
            )
            .performClick()
        assertEquals(1, managed)
        compose.runOnIdle {
            state.value =
                state.value.copy(
                    access =
                        state.value.access!!.copy(
                            status = SubscriptionStatus.Hold,
                            active = false,
                            expiresAt = 1,
                        ),
                )
        }
        compose
            .onNodeWithTag("subscription_status")
            .performScrollTo()
            .assertTextEquals(compose.activity.getString(fr.previensmoi.R.string.billing_hold))
        compose.onNodeWithTag("subscribe").assertDoesNotExist()
        compose
            .onNodeWithText(
                compose.activity.getString(fr.previensmoi.R.string.billing_not_available),
            )
            .assertDoesNotExist()
        capture("hold")
        compose.runOnIdle { state.value = state.value.copy(restored = true) }
        compose
            .onAllNodesWithText(compose.activity.getString(fr.previensmoi.R.string.billing_hold))
            .assertCountEquals(2)
        compose
            .onNodeWithText(
                compose.activity.getString(fr.previensmoi.R.string.billing_restore_none),
            )
            .assertDoesNotExist()
        compose.onNodeWithTag("subscribe").assertDoesNotExist()
    }

    @Test
    fun cachedSubscriptionKeepsStatusAndNeverOffersAnotherPurchase() {
        compose.activity.setContent {
            PreviensTheme {
                SubscriptionScreen(
                    SubscriptionUiState(
                        loaded = true,
                        access =
                            SubscriptionAccess(
                                status = SubscriptionStatus.Active,
                                active = true,
                                expiresAt = 4102444800000L,
                                fromCache = true,
                                remainingCreations = 3,
                            ),
                        problem = BillingProblem.Offline,
                    ),
                    {},
                    {},
                    {},
                    {},
                )
            }
        }
        compose
            .onNodeWithTag("subscription_status")
            .assertTextEquals(
                compose.activity.getString(fr.previensmoi.R.string.billing_active),
            )
        compose.onNodeWithTag("subscription_cached").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("subscription_message").assertDoesNotExist()
        compose.onNodeWithTag("subscribe").assertDoesNotExist()
        compose
            .onNodeWithTag("subscription_remaining")
            .performScrollTo()
            .assertTextContains("3", substring = true)
    }
}
