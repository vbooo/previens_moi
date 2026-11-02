package fr.previensmoi.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import fr.previensmoi.R
import fr.previensmoi.domain.model.*
import fr.previensmoi.feature.common.presentation.*
import fr.previensmoi.feature.create.presentation.*
import fr.previensmoi.feature.session.presentation.*
import fr.previensmoi.feature.watches.presentation.*
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable private data object Home : NavKey

@Serializable private data object Account : NavKey

@Serializable private data object Create : NavKey

@Serializable private data class Detail(val id: String) : NavKey

@Serializable private data class Result(val id: String) : NavKey

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var requestedFinding by mutableStateOf<String?>(null)
    private var requestedWatch by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedFinding = intent.getStringExtra("findingId")
        requestedWatch = intent.getStringExtra("watchId")
    }

    private val watches: WatchesViewModel by viewModels()
    private val creation: CreateViewModel by viewModels()
    private val session: SessionViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedFinding = intent.getStringExtra("findingId")
        requestedWatch = intent.getStringExtra("watchId")
        enableEdgeToEdge()
        setContent {
            PreviensTheme() {
                val state by watches.state.collectAsStateWithLifecycle()
                val create by creation.state.collectAsStateWithLifecycle()
                val account by session.state.collectAsStateWithLifecycle()
                WatchSynchronizationEffect(watches, account.session?.id)
                val focus = LocalFocusManager.current
                val keyboard = LocalSoftwareKeyboardController.current
                val stack = rememberNavBackStack(Home)
                var returnToProposal by rememberSaveable { mutableStateOf(false) }
                var notificationsEnabled by remember {
                    mutableStateOf(NotificationManagerCompat.from(this).areNotificationsEnabled())
                }
                val permission =
                    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                        notificationsEnabled =
                            NotificationManagerCompat.from(this).areNotificationsEnabled()
                    }
                val notificationSettings = {
                    startActivity(
                        if (Build.VERSION.SDK_INT >= 26)
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                        else
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                "package:$packageName".toUri(),
                            ),
                    )
                }
                val enableNotifications = {
                    if (
                        Build.VERSION.SDK_INT >= 33 &&
                            !notificationsEnabled &&
                            !shouldShowRequestPermissionRationale(
                                Manifest.permission.POST_NOTIFICATIONS,
                            )
                    )
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else notificationSettings()
                }
                var menuOpen by remember { mutableStateOf(false) }
                var deleting by remember { mutableStateOf<Watch?>(null) }
                val route = stack.lastOrNull()
                val back: () -> Unit = {
                    if (route == Account && returnToProposal) {
                        returnToProposal = false
                        creation.revise()
                        stack.removeLastOrNull()
                    } else if (
                        route == Create &&
                            create.step in
                                setOf(
                                    CreationStep.Ready,
                                    CreationStep.Clarifying,
                                    CreationStep.Unsupported,
                                )
                    )
                        creation.revise()
                    else if (stack.size > 1) stack.removeLastOrNull()
                    Unit
                }
                deleting?.let { watch ->
                    WatchDeleteDialog(
                        watch,
                        { deleting = null },
                        {
                            deleting = null
                            watches.command(watch, Operation.Delete)
                        },
                    )
                }
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    notificationsEnabled =
                        NotificationManagerCompat.from(this).areNotificationsEnabled()
                }
                LaunchedEffect(requestedWatch, account.session) {
                    requestedWatch?.let { id ->
                        if (account.session == null) {
                            if (stack.lastOrNull() != Account) stack.add(Account)
                        } else {
                            watches.refresh()
                            if (stack.lastOrNull() != Detail(id)) stack.add(Detail(id))
                            requestedWatch = null
                        }
                    }
                }
                LaunchedEffect(requestedFinding, account.session) {
                    val id = requestedFinding
                    if (id != null) {
                        if (account.session == null) {
                            if (stack.lastOrNull() != Account) stack.add(Account)
                        } else {
                            watches.loadFinding(id)
                        }
                    }
                }
                LaunchedEffect(
                    requestedFinding,
                    state.findingRequestId,
                    state.findingError,
                    state.findings,
                ) {
                    val id = requestedFinding
                    if (id != null && state.findingRequestId == id) {
                        val finding = state.findings.find { it.id == id }
                        if (finding != null) {
                            stack.clear()
                            stack.add(Home)
                            stack.add(Detail(finding.watchId))
                            stack.add(Result(id))
                            requestedFinding = null
                        } else if (
                            state.findingError in setOf(Failure.NotFound, Failure.AccessDenied)
                        ) {
                            stack.clear()
                            stack.add(Home)
                            stack.add(Result(id))
                            requestedFinding = null
                        } else if (
                            state.findingError ==
                                fr.previensmoi.domain.model.Failure.SessionRequired &&
                                stack.lastOrNull() != Account
                        )
                            stack.add(Account)
                    }
                }
                LaunchedEffect(account.session) {
                    if (account.session != null) {
                        creation.resumeAfterSignIn()
                        if (returnToProposal && stack.lastOrNull() == Account)
                            stack.removeLastOrNull()
                        returnToProposal = false
                        if (
                            stack.lastOrNull() == Account &&
                                requestedFinding == null &&
                                requestedWatch == null
                        )
                            stack.removeLastOrNull()
                    }
                }
                LaunchedEffect(create.step) {
                    if (create.step == CreationStep.Login && stack.lastOrNull() == Create) {
                        returnToProposal = true
                        stack.add(Account)
                    }
                }
                LaunchedEffect(create.activatedId) {
                    create.activatedId?.let { id ->
                        creation.consumeActivation()
                        watches.refresh()
                        if (stack.lastOrNull() == Create) {
                            stack.clear()
                            stack.add(Home)
                            stack.add(Detail(id))
                        }
                    }
                }
                LaunchedEffect(state.deletedId) {
                    state.deletedId?.let {
                        watches.consumeDeletion()
                        stack.clear()
                        stack.add(Home)
                    }
                }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    stringResource(
                                        when (route) {
                                            Home -> R.string.v1_home
                                            Account ->
                                                if (account.session == null) R.string.v1_connection
                                                else R.string.v1_account
                                            Create ->
                                                when (create.step) {
                                                    CreationStep.Ready,
                                                    CreationStep.Confirming ->
                                                        R.string.v1_confirmation
                                                    CreationStep.Clarifying -> R.string.v1_clarify
                                                    else -> R.string.v1_new
                                                }
                                            is Result -> R.string.v1_result
                                            else -> R.string.detail_title
                                        },
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            navigationIcon = {
                                if (stack.size > 1)
                                    IconButton(back) {
                                        AppIcon(
                                            R.drawable.symbol_arrow_back,
                                            stringResource(R.string.back),
                                        )
                                    }
                            },
                            actions = {
                                if (route == Home) {
                                    if (account.session != null)
                                        IconButton(
                                            watches::refresh,
                                            enabled = !state.refreshing,
                                            modifier = Modifier.testTag("refresh_watches"),
                                        ) {
                                            AppIcon(
                                                R.drawable.symbol_refresh,
                                                stringResource(R.string.refresh_short),
                                            )
                                        }
                                    TextButton(onClick = { stack.add(Account) }, modifier = Modifier.testTag("account")) {
                                        Text(
                                            stringResource(
                                                if (account.session == null) R.string.sign_in
                                                else R.string.connected_account,
                                            ),
                                        )
                                    }
                                }
                                if (route is Detail)
                                    RefreshWatchAction(state.refreshing, watches::refresh)
                                if (route is Detail)
                                    state.watches
                                        .find { it.id == route.id }
                                        ?.let { watch ->
                                            Box {
                                                IconButton(
                                                    onClick = { menuOpen = true },
                                                    modifier = Modifier.testTag("watch_menu"),
                                                ) {
                                                    AppIcon(
                                                        R.drawable.symbol_more_vert,
                                                        stringResource(R.string.v1_actions),
                                                    )
                                                }
                                                DropdownMenu(
                                                    menuOpen,
                                                    onDismissRequest = { menuOpen = false },
                                                ) {
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(stringResource(R.string.revise))
                                                        },
                                                        enabled = !state.commandPending,
                                                        onClick = {
                                                            menuOpen = false
                                                            creation.edit(watch)
                                                            stack.add(Create)
                                                        },
                                                        modifier = Modifier.testTag("edit"),
                                                    )
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(stringResource(R.string.delete))
                                                        },
                                                        enabled = !state.commandPending,
                                                        onClick = {
                                                            menuOpen = false
                                                            deleting = watch
                                                        },
                                                        modifier = Modifier.testTag("delete"),
                                                    )
                                                }
                                            }
                                        }
                            },
                            colors =
                                TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                ),
                        )
                    },
                    floatingActionButton = {
                        if (route == Home)
                            ExtendedFloatingActionButton(
                                onClick = {
                                    creation.startNew()
                                    stack.add(Create)
                                },
                                icon = { AppIcon(R.drawable.symbol_add) },
                                text = { Text(stringResource(R.string.new_watch)) },
                                modifier = Modifier.testTag("new_request"),
                            )
                    },
                ) { insets ->
                    Column(Modifier.fillMaxSize().padding(insets).imePadding()) {
                        NavDisplay(
                            modifier = Modifier.weight(1f),
                            backStack = stack,
                            onBack = back,
                            entryProvider =
                                entryProvider {
                                    entry<Home> {
                                        HomeScreen(
                                            state,
                                            create,
                                            creation,
                                            preview = {
                                                focus.clearFocus()
                                                keyboard?.hide()
                                                creation.startNew()
                                                creation.evaluate(Locale.getDefault().language)
                                                stack.add(Create)
                                            },
                                            refresh = watches::refresh,
                                            open = {
                                                watches.clearCommandError()
                                                stack.add(Detail(it))
                                            },
                                            remote = true,
                                            signedIn = account.session != null,
                                            signIn = { stack.add(Account) },
                                            newRequest = {
                                                creation.startNew()
                                                stack.add(Create)
                                            },
                                        )
                                    }
                                    entry<Account> {
                                        SessionScreen(
                                            account,
                                            session,
                                            forConfirmation = returnToProposal,
                                            notificationsEnabled = notificationsEnabled,
                                            notificationSettings = notificationSettings,
                                        )
                                    }
                                    entry<Create> {
                                        CreateScreen(
                                            create,
                                            creation,
                                            login = {
                                                returnToProposal = true
                                                stack.add(Account)
                                            },
                                            remote = true,
                                        )
                                    }
                                    entry<Detail> { key ->
                                        DetailScreen(
                                            key.id,
                                            state,
                                            watches,
                                            edit = {
                                                creation.edit(it)
                                                stack.add(Create)
                                            },
                                            openFinding = { stack.add(Result(it)) },
                                            remote = true,
                                            notificationsEnabled = notificationsEnabled,
                                            enableNotifications = enableNotifications,
                                        )
                                    }
                                    entry<Result> { key ->
                                        FindingScreen(key.id, state, remote = true)
                                    }
                                },
                        )
                    }
                }
            }
        }
    }
}
