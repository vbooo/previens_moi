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
import fr.previensmoi.feature.support.*
import fr.previensmoi.feature.watches.presentation.*
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun emptyHomeOffersCreationAndPopulatedHomeOpensWatch() {
        val create = ViewModelProvider(compose.activity)[CreateViewModel::class.java]
        val state = mutableStateOf(WatchesUiState(hasLoaded = true))
        var creations = 0
        var opened: String? = null
        compose.activity.setContent {
            PreviensTheme {
                Scaffold(
                    topBar = {
                        WatchAppBar(
                            false,
                            { Text(compose.activity.getString(R.string.v1_home)) },
                            {},
                            {},
                            TopAppBarDefaults.topAppBarColors(),
                        )
                    },
                ) { insets ->
                    Box(Modifier.padding(insets)) {
                        HomeScreen(
                            state.value,
                            CreateUiState(loaded = true),
                            create,
                            {},
                            {},
                            { opened = it },
                            remote = true,
                            newRequest = { creations++ },
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("home_empty").assertIsDisplayed()
        compose.onNodeWithTag("new_request").assertIsDisplayed().performClick()
        assertEquals(1, creations)
        capture("home-empty")
        val plan =
            Proposal(
                "visual",
                1,
                RequestDraft("Visual fixture"),
                Long.MAX_VALUE,
                title = "Promotions sur la PlayStation 5",
            )
        compose.runOnIdle {
            state.value =
                state.value.copy(
                    watches =
                        listOf(
                            Watch(
                                "active",
                                plan,
                                health = Health.Healthy,
                                lastSuccess = 1795161600000,
                                baselineAt = 0,
                                nextCheck = Long.MAX_VALUE,
                                remoteState = RemoteState.Running,
                            ),
                            Watch(
                                "paused",
                                plan.copy(title = "Offres d’emploi Android à Montpellier"),
                                status = WatchStatus.Paused,
                                baselineAt = 0,
                            ),
                        ),
                )
        }
        compose.onNodeWithTag("home_empty").assertDoesNotExist()
        compose.onNodeWithTag("watch_active").assertIsDisplayed().performClick()
        assertEquals("active", opened)
        capture("home-list")
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
