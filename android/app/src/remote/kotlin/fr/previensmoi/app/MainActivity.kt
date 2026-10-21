package fr.previensmoi.app

import android.os.Bundle
import android.os.Build
import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import fr.previensmoi.R
import fr.previensmoi.feature.session.presentation.*
import fr.previensmoi.feature.create.presentation.*
import fr.previensmoi.feature.watches.presentation.*
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable private data object Home : NavKey
@Serializable private data object Account : NavKey
@Serializable private data object Create : NavKey
@Serializable private data class Detail(val id: String) : NavKey
@Serializable private data class Result(val id: String) : NavKey

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var requestedFinding by mutableStateOf<String?>(null)
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); requestedFinding = intent.getStringExtra("findingId") }
    private val watches: WatchesViewModel by viewModels()
    private val creation: CreateViewModel by viewModels()
    private val session: SessionViewModel by viewModels()
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedFinding = intent.getStringExtra("findingId")
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                val state by watches.state.collectAsStateWithLifecycle()
                val create by creation.state.collectAsStateWithLifecycle()
                val account by session.state.collectAsStateWithLifecycle()
                val focus = LocalFocusManager.current
                val keyboard = LocalSoftwareKeyboardController.current
                val stack = rememberNavBackStack(Home)
                var returnToProposal by rememberSaveable { mutableStateOf(false) }
                val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
                LaunchedEffect(requestedFinding, account.session) {
                    val id = requestedFinding
                    if (id != null) {
                        if (account.session == null) { if (stack.lastOrNull() != Account) stack.add(Account) }
                        else { watches.loadFinding(id); if (stack.lastOrNull() != Result(id)) stack.add(Result(id)) }
                    }
                }
                LaunchedEffect(requestedFinding, state.findingRequestId, state.findingError, state.findings) {
                    val id = requestedFinding
                    if (id != null && state.findingRequestId == id) {
                        if (state.findings.any { it.id == id } || state.findingError in setOf(fr.previensmoi.domain.model.Failure.NotFound, fr.previensmoi.domain.model.Failure.AccessDenied)) requestedFinding = null
                        else if (state.findingError == fr.previensmoi.domain.model.Failure.SessionRequired && stack.lastOrNull() != Account) stack.add(Account)
                    }
                }
                LaunchedEffect(account.session) {
                    if (account.session != null) watches.refresh()
                    if (account.session != null) { creation.resumeAfterSignIn(); if (returnToProposal && stack.lastOrNull() == Account) stack.removeLastOrNull(); returnToProposal = false }
                }
                LaunchedEffect(create.activatedId) {
                    create.activatedId?.let { id ->
                        creation.consumeActivation()
                        if (stack.lastOrNull() == Create) { stack.clear(); stack.add(Home); stack.add(Detail(id)) }
                    }
                }
                LaunchedEffect(state.deletedId) {
                    state.deletedId?.let { watches.consumeDeletion(); stack.clear(); stack.add(Home) }
                }
                Scaffold(topBar = {
                    TopAppBar(title = { Text(stringResource(R.string.app_title)) },
                        navigationIcon = { if (stack.size > 1) TextButton(onClick = { stack.removeLastOrNull() }) { Text(stringResource(R.string.back)) } },
                        actions = { TextButton(onClick = { stack.add(Account) }) { Text(stringResource(R.string.account_title)) } })
                }) { insets ->
                    Column(Modifier.fillMaxSize().padding(insets).imePadding()) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(stringResource(R.string.remote_banner), Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
                        }
                        NavDisplay(modifier = Modifier.weight(1f), backStack = stack,
                            onBack = { if (stack.size > 1) stack.removeLastOrNull() }, entryProvider = entryProvider {
                                entry<Home> { HomeScreen(state, create, creation, preview = {
                                    focus.clearFocus(); keyboard?.hide(); creation.startNew(); creation.evaluate(Locale.getDefault().language); stack.add(Create)
                                }, refresh = watches::refresh, open = { watches.clearCommandError(); stack.add(Detail(it)) }, remote = true, signedIn = account.session != null, signIn = { stack.add(Account) }) }
                                entry<Account> {
                                    Column(Modifier.fillMaxSize()) {
                                        if (Build.VERSION.SDK_INT >= 33 && account.session != null) TextButton(onClick = { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text(stringResource(R.string.notification_permission)) }
                                        Box(Modifier.weight(1f)) { SessionScreen(account, session) }
                                    }
                                }
                                entry<Create> { CreateScreen(create, creation, login = { returnToProposal = true; stack.add(Account) }, remote = true) }
                                entry<Detail> { key -> DetailScreen(key.id, state, watches,
                                    edit = { creation.edit(it); stack.add(Create) }, openFinding = { stack.add(Result(it)) }, remote = true) }
                                entry<Result> { key -> FindingScreen(key.id, state, remote = true) }
                            })
                    }
                }
            }
        }
    }
}