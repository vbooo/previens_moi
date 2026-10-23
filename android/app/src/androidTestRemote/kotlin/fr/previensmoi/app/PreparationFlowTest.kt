package fr.previensmoi.app

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.previensmoi.R
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.*
import fr.previensmoi.feature.create.presentation.*
import fr.previensmoi.feature.watches.presentation.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Native UI proof using explicit fixtures: no cloud/model/source request. */
@RunWith(AndroidJUnit4::class)
class PreparationFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun capture(name:String) {
        compose.waitForIdle()
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        val bitmap=checkNotNull(automation.takeScreenshot())
        val file=java.io.File(compose.activity.getExternalFilesDir(null),name)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
        automation.executeShellCommand("cp ${file.absolutePath} /data/local/tmp/workflow-$name").close()
    }
    private fun tap(tag:String) { compose.onNodeWithTag(tag).performScrollTo().performClick() }
    @Test fun understandConfirmLeaveAndRefreshPreparation() {
        val watchesFlow=MutableStateFlow<List<Watch>>(emptyList());var confirmations=0;var refreshes=0
        val proposals=object:ProposalRepository {
            override suspend fun evaluate(draft:RequestDraft)=Outcome.Success(Evaluation.Ready(Proposal("proposal",1,draft,Long.MAX_VALUE,partial=true,title="Concert de Mira à Tours",criteria=listOf("Concert de Mira à Tours"),coverage="Sources recherchées après confirmation",method="web_search",backgroundPreparation=true)))
            override suspend fun get(id:String)=Outcome.Success(Proposal("proposal",1,RequestDraft("Concert de Mira à Tours",language="fr"),Long.MAX_VALUE,partial=true,title="Concert de Mira à Tours",criteria=listOf("Concert de Mira à Tours"),coverage="Sources recherchées après confirmation",method="web_search",backgroundPreparation=true))
        }
        var lastDraft=RequestDraft()
        val stableProposals=object:ProposalRepository {
            private var p:Proposal?=null
            override suspend fun evaluate(draft:RequestDraft):Outcome<Evaluation> { lastDraft=draft;val result=proposals.evaluate(draft);p=(result.value as Evaluation.Ready).proposal;return result }
            override suspend fun get(id:String)=Outcome.Success(checkNotNull(p))
        }
        val watches=object:WatchRepository {
            override fun observeList()=watchesFlow
            override suspend fun refresh():RefreshResult { refreshes++;return RefreshResult.Success }
            override suspend fun execute(command:Command):Outcome<CommandReceipt> {confirmations++;val p=(stableProposals.get("proposal") as Outcome.Success).value;watchesFlow.value=listOf(Watch("watch",p,status=WatchStatus.Pending,baselineAt=null,remoteState=RemoteState.Starting));return Outcome.Success(CommandReceipt(command.id,"watch",null,acceptedInBackground=true))}
        }
        val sessions=object:SessionRepository {override fun observeSession()=flowOf(Session("fixture"));override suspend fun signIn():Outcome<Session> = Outcome.Success(Session("fixture"))}
        val drafts=object:DraftRepository {override suspend fun load():Outcome<RequestDraft> = Outcome.Success(RequestDraft());override suspend fun save(draft:RequestDraft):Outcome<Unit> = Outcome.Success(Unit);override suspend fun clear():Outcome<Unit> = Outcome.Success(Unit)}
        compose.runOnUiThread {
            val vm=CreateViewModel(stableProposals,watches,sessions,drafts,IdGenerator { "fixture" },SavedStateHandle())
            compose.activity.setContent {
                MaterialTheme(colorScheme=if(isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                    val state by vm.state.collectAsState();val list by watchesFlow.collectAsState();var editing by remember { mutableStateOf(false) }
                    LaunchedEffect(state.activatedId) { if(state.activatedId!=null) editing=false }
                    if(editing) CreateScreen(state,vm,remote=true)
                    else HomeScreen(WatchesUiState(watches=list),state,vm,preview={},refresh={refreshes++},open={},remote=true,signedIn=true,newRequest={editing=true})
                }
            }
        }
        compose.waitUntil(5000) { compose.onAllNodesWithTag("new_request").fetchSemanticsNodes().isNotEmpty() }
        val locale=InstrumentationRegistry.getArguments().getString("captureLocale")?:"fixture"
        capture("home-$locale.png");tap("new_request")
        compose.waitUntil(5000) {compose.onAllNodesWithTag("request").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("request").performTextReplacement("Préviens-moi des concerts de Mira à Tours")
        // Remove the IME with system Back; the screen itself stays open.
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent 4").close()
        tap("evaluate")
        compose.waitUntil(5000) {compose.onAllNodesWithTag("confirm").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText(compose.activity.getString(R.string.understanding_title)).assertIsDisplayed()
        compose.onNodeWithTag("accept_partial").assertDoesNotExist();assertEquals(0,confirmations)
        capture("understanding-$locale.png");tap("confirm")
        compose.waitUntil(5000) {compose.onAllNodesWithText(compose.activity.getString(R.string.preparing_title)).fetchSemanticsNodes().isNotEmpty()}
        assertEquals(1,confirmations);assertTrue(lastDraft.text.contains("Mira"))
        compose.onNodeWithTag("new_request").assertIsDisplayed();capture("preparing-$locale.png")
        val before=refreshes;tap("refresh_watches");assertEquals(before+1,refreshes)
        compose.onNodeWithTag("watch_list").performTouchInput { swipe(start=center.copy(y=10f),end=center.copy(y=height*.8f),durationMillis=500) }
        compose.waitUntil(5000) {refreshes>before+1}
    }
}
