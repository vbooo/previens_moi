package fr.previensmoi.app

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.previensmoi.R
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.*
import fr.previensmoi.feature.common.presentation.PreviensTheme
import fr.previensmoi.feature.create.presentation.*
import fr.previensmoi.feature.watches.presentation.*
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Offline regressions for refresh and sign-out privacy after the V1 list redesign. */
@RunWith(AndroidJUnit4::class)
class PreparationFlowTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 @Test fun refreshKeepsVisibleListAndSignedOutHidesPrivateRows() {
  var refreshes=0;val signedIn=mutableStateOf(true)
  val proposal=Proposal("p",1,RequestDraft("Private watch fixture"),Long.MAX_VALUE,title="Watch fixture")
  val watch=Watch("w",proposal,health=Health.Healthy,lastSuccess=0,baselineAt=0,nextCheck=3600000,remoteState=RemoteState.Running)
  val proposals=object:ProposalRepository {override suspend fun evaluate(draft:RequestDraft):Outcome<Evaluation> = Outcome.Error(Failure.Unsupported);override suspend fun get(id:String):Outcome<Proposal> = Outcome.Success(proposal)}
  val watches=object:WatchRepository {override fun observeList()=flowOf(listOf(watch));override suspend fun refresh()=RefreshResult.Success;override suspend fun execute(command:Command):Outcome<CommandReceipt> = Outcome.Error(Failure.Unsupported)}
  val sessions=object:SessionRepository {override fun observeSession()=flowOf(Session("fixture"));override suspend fun signIn():Outcome<Session> = Outcome.Error(Failure.Unsupported)}
  val drafts=object:DraftRepository {override suspend fun load():Outcome<RequestDraft> = Outcome.Success(RequestDraft());override suspend fun save(draft:RequestDraft):Outcome<Unit> = Outcome.Success(Unit);override suspend fun clear():Outcome<Unit> = Outcome.Success(Unit)}
  compose.runOnUiThread {
   val create=CreateViewModel(proposals,watches,sessions,drafts,IdGenerator {"fixture"},SavedStateHandle())
   compose.activity.setContent {PreviensTheme {HomeScreen(WatchesUiState(watches=listOf(watch),refreshFailed=true),CreateUiState(),create,{}, {refreshes++}, {},remote=true,signedIn=signedIn.value)}}
  }
  compose.onNodeWithText("Watch fixture").assertIsDisplayed()
  compose.onNodeWithTag("watch_list").performTouchInput {swipe(start=center.copy(y=10f),end=center.copy(y=height*.8f),durationMillis=500)}
  compose.waitUntil(5000){refreshes>0};compose.onNodeWithText("Watch fixture").assertIsDisplayed()
  compose.runOnUiThread{signedIn.value=false};compose.waitForIdle()
  compose.onNodeWithTag("watch_w").assertDoesNotExist();compose.onNodeWithText(compose.activity.getString(R.string.stale_content)).assertDoesNotExist();compose.onNodeWithTag("signed_out_watches").assertIsDisplayed()
 }
}
