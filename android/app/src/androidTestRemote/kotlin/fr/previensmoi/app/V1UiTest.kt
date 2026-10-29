package fr.previensmoi.app

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.previensmoi.R
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.*
import fr.previensmoi.feature.common.presentation.*
import fr.previensmoi.feature.create.presentation.*
import fr.previensmoi.feature.session.presentation.*
import fr.previensmoi.feature.watches.presentation.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Offline native screens with explicit fixtures. No provider, email or paid calls. */
@RunWith(AndroidJUnit4::class)
class V1UiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val args
        get() = InstrumentationRegistry.getArguments()

    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        runCatching { node.performScrollTo() }
        node.performClick()
    }

    private fun await(tag: String) {
        compose.waitUntil(5000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val a = InstrumentationRegistry.getInstrumentation().uiAutomation
        val b = checkNotNull(a.takeScreenshot())
        val file = java.io.File(compose.activity.getExternalFilesDir(null), "v1-$name.png")
        file.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        b.recycle()
        a.executeShellCommand("cp ${file.absolutePath} /data/local/tmp/v1-$name.png").close()
    }

    private class Service :
        ProposalRepository, WatchRepository, FindingRepository, SessionRepository, DraftRepository {
        val session = MutableStateFlow<Session?>(null)
        val list = MutableStateFlow<List<Watch>>(emptyList())
        val findings = MutableStateFlow<List<Finding>>(emptyList())
        var draft = RequestDraft()
        var p: Proposal? = null
        var confirmations = 0
        val submittedOperations = mutableListOf<Operation>()
        val submittedCommands = mutableListOf<Command>()
        var pauseGate: CompletableDeferred<Unit>? = null
        var resumeGate: CompletableDeferred<Unit>? = null

        override suspend fun evaluate(draft: RequestDraft): Outcome<Evaluation> {
            p =
                Proposal(
                    "p",
                    1,
                    draft,
                    Long.MAX_VALUE,
                    partial = true,
                    intervalHours = 1,
                    title = "Surveillance des promotions sur la PlayStation 5",
                    criteria =
                        listOf("Nouvelles promotions sur la PlayStation 5 disponibles en France."),
                    method = "web_search",
                    backgroundPreparation = true,
                )
            return Outcome.Success(Evaluation.Ready(p!!))
        }

        override suspend fun get(id: String): Outcome<Proposal> = Outcome.Success(p!!)

        override suspend fun configure(proposal: Proposal, mode: FollowMode): Outcome<Proposal> {
            p =
                proposal.copy(
                    version = proposal.version + 1,
                    draft = proposal.draft.copy(mode = mode),
                )
            return Outcome.Success(p!!)
        }

        override fun observeList() = list

        override fun observeFindings() = findings

        override suspend fun getFinding(id: String): Outcome<Finding> =
            Outcome.Success(findings.value.first { it.id == id })

        override suspend fun refresh() = RefreshResult.Success

        override suspend fun execute(c: Command): Outcome<CommandReceipt> {
            submittedOperations += c.operation
            submittedCommands += c
            if (c.operation == Operation.Pause && pauseGate != null) {
                if (submittedOperations.count { it == Operation.Pause } == 1)
                    return Outcome.Error(Failure.OperationPending)
                pauseGate!!.await()
                list.value =
                    list.value.map {
                        it.copy(
                            status = WatchStatus.Paused,
                            remoteState = RemoteState.Stopped,
                            version = 2,
                            nextCheck = null,
                        )
                    }
                return Outcome.Success(CommandReceipt(c.id, c.targetId!!, 2))
            }
            if (c.operation == Operation.Resume && resumeGate != null) {
                if (submittedOperations.count { it == Operation.Resume } == 1) {
                    list.value = list.value.map { it.copy(status = WatchStatus.Pending, remoteState = RemoteState.Starting) }
                    return Outcome.Error(Failure.OperationPending)
                }
                resumeGate!!.await()
                list.value = list.value.map { it.copy(status = WatchStatus.Paused, health = Health.Degraded,
                    remoteState = RemoteState.Stopped, version = 2, nextCheck = null,
                    plan = it.plan.copy(preparationFailure = "source_unusable")) }
                return Outcome.Error(Failure.OperationFailed)
            }
            if (c.operation == Operation.Activate) {
                confirmations++
                list.value =
                    listOf(
                        Watch(
                            "w",
                            p!!,
                            status = WatchStatus.Pending,
                            baselineAt = null,
                            remoteState = RemoteState.Starting,
                        ),
                    )
            }
            return Outcome.Success(CommandReceipt(c.id, "w", 1, acceptedInBackground = true))
        }

        override fun observeSession() = session

        override suspend fun signIn(): Outcome<Session> = Outcome.Error(Failure.SessionRequired)

        override suspend fun requestCode(email: String): Outcome<Unit> = Outcome.Success(Unit)

        override suspend fun verifyCode(email: String, code: String): Outcome<Session> {
            val s = Session("owner", email)
            session.value = s
            return Outcome.Success(s)
        }

        override suspend fun load(): Outcome<RequestDraft> = Outcome.Success(draft)

        override suspend fun save(draft: RequestDraft): Outcome<Unit> {
            this.draft = draft
            return Outcome.Success(Unit)
        }

        override suspend fun clear(): Outcome<Unit> {
            draft = RequestDraft()
            return Outcome.Success(Unit)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun requestConfirmSignInThenPreparationIsSingleActivation() {
        val service = Service()
        compose.runOnUiThread {
            val create =
                CreateViewModel(
                    service,
                    service,
                    service,
                    service,
                    IdGenerator { "c" },
                    SavedStateHandle(),
                )
            val account = SessionViewModel(service)
            val watches = WatchesViewModel(service, service, IdGenerator { "c2" })
            compose.activity.setContent {
                PreviensTheme {
                    val c by create.state.collectAsState()
                    val a by account.state.collectAsState()
                    val w by watches.state.collectAsState()
                    var route by remember { mutableStateOf("home") }
                    LaunchedEffect(c.step) { if (c.step == CreationStep.Login) route = "account" }
                    LaunchedEffect(a.session) { if (a.session != null) create.resumeAfterSignIn() }
                    LaunchedEffect(c.activatedId) { if (c.activatedId != null) route = "detail" }
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        when (route) {
                                            "home" -> "Mes suivis"
                                            "account" -> "Connexion"
                                            "detail" -> "Détail du suivi"
                                            else -> "Ton suivi"
                                        },
                                    )
                                },
                            )
                        },
                        floatingActionButton = {
                            if (route == "home")
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        create.startNew()
                                        route = "create"
                                    },
                                    icon = { AppIcon(R.drawable.symbol_add) },
                                    text = { Text("Nouveau suivi") },
                                    modifier = Modifier.testTag("new_request"),
                                )
                        },
                    ) { insets ->
                        Box(Modifier.padding(insets).imePadding()) {
                            when (route) {
                                "home" ->
                                    HomeScreen(
                                        w,
                                        c,
                                        create,
                                        {},
                                        watches::refresh,
                                        {},
                                        remote = true,
                                        signedIn = false,
                                    )
                                "create" -> CreateScreen(c, create, remote = true)
                                "account" -> SessionScreen(a, account, forConfirmation = true)
                                else ->
                                    DetailScreen(
                                        "w",
                                        w,
                                        watches,
                                        {},
                                        {},
                                        remote = true,
                                        notificationsEnabled = false,
                                    )
                            }
                        }
                    }
                }
            }
        }
        await("new_request")
        tap("new_request")
        await("request")
        compose
            .onNodeWithTag("request")
            .performTextReplacement("Préviens-moi des promotions sur la PlayStation 5 en France")
        compose.runOnUiThread {
            compose.activity.window.insetsController?.hide(android.view.WindowInsets.Type.ime())
        }
        tap("evaluate")
        await("confirm")
        compose.onNodeWithTag("confirm").assertIsDisplayed()
        compose.onNodeWithTag("continuous").assertIsDisplayed()
        compose.onNodeWithTag("once").assertIsDisplayed()
        assertEquals(0, service.confirmations)
        tap("continuous")
        tap("confirm")
        await("email")
        compose.onNodeWithTag("email").performTextInput("alex@example.test")
        tap("send_code")
        await("otp")
        compose.onNodeWithTag("otp").performTextInput("123456")
        tap("verify_code")
        await("watch_status")
        compose
            .onNodeWithText(compose.activity.getString(R.string.status_pending))
            .assertIsDisplayed()
        assertEquals(1, service.confirmations)
        assertEquals("France", service.p!!.draft.region)
        assertEquals(FollowMode.Continuous, service.p!!.draft.mode)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun suspendedStatusExplainsTrialAndRefreshStaysInToolbar() {
        val service = Service()
        var refreshes = 0
        val now = 1792767900000L
        val watch =
            Watch(
                "suspended",
                Proposal(
                    "p",
                    1,
                    RequestDraft("Une demande"),
                    Long.MAX_VALUE,
                    title = "Changements du texte de la page",
                ),
                status = WatchStatus.SuspendedByCapacity,
                health = Health.Degraded,
                baselineAt = now,
                lastSuccess = now,
                remoteState = RemoteState.Stopped,
            )
        compose.runOnUiThread {
            val watches = WatchesViewModel(service, service, IdGenerator { "c" })
            compose.activity.setContent {
                PreviensTheme {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Détail du suivi") },
                                actions = { RefreshWatchAction(false) { refreshes++ } },
                            )
                        },
                    ) { insets ->
                        Box(Modifier.padding(insets)) {
                            DetailScreen(
                                watch.id,
                                WatchesUiState(watches = listOf(watch)),
                                watches,
                                {},
                                {},
                                remote = true,
                            )
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag("resume").assertDoesNotExist()
        tap("status_explanation")
        compose.onNodeWithTag("capacity_dialog").assertIsDisplayed()
        capture("limit-explanation")
        compose.onNodeWithText(compose.activity.getString(R.string.v1_close)).performClick()
        compose.onAllNodesWithTag("refresh_watches").assertCountEquals(1)
        compose.onNodeWithTag("refresh_watches").performClick()
        assertEquals(1, refreshes)
        assertTrue(service.submittedOperations.isEmpty())
        val last =
            compose
                .onNodeWithText(compose.activity.getString(R.string.v2_last_success))
                .fetchSemanticsNode()
                .boundsInRoot
        val next =
            compose
                .onNodeWithText(compose.activity.getString(R.string.v2_next_check))
                .fetchSemanticsNode()
                .boundsInRoot
        assertEquals(last.top, next.top, 1f)
        assertTrue(last.right < next.left)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun failedResumeRefreshesDetailAndHomeWithoutOpeningANotification() {
        val service = Service()
        val gate = CompletableDeferred<Unit>()
        service.resumeGate = gate
        service.list.value = listOf(Watch("w", Proposal("p",1,
            RequestDraft("Promotions PS5",mode=FollowMode.Continuous),Long.MAX_VALUE,
            title="Promotions sur la PlayStation 5"), status=WatchStatus.Paused,
            baselineAt=1792767900000L, lastSuccess=1792767900000L, health=Health.Healthy))
        compose.runOnUiThread {
            val vm=WatchesViewModel(service,service,IdGenerator { "one-resume" })
            val create=CreateViewModel(service,service,service,service,IdGenerator { "create" },SavedStateHandle())
            compose.activity.setContent {
                PreviensTheme {
                    val state by vm.state.collectAsState()
                    var detail by remember { mutableStateOf(true) }
                    Scaffold(topBar={ TopAppBar(title={Text(if(detail) "Détail du suivi" else "Mes suivis")},
                        actions={
                            RefreshWatchAction(state.refreshing,vm::refresh)
                            TextButton({detail=!detail},Modifier.testTag("switch_view")) {Text(if(detail) "Mes suivis" else "Détail")}
                        }) }) { insets -> Box(Modifier.padding(insets)) {
                        if(detail) DetailScreen("w",state,vm,{},{},remote=true)
                        else HomeScreen(state,CreateUiState(),create,{detail=true},vm::refresh,{},remote=true)
                    } }
                }
            }
        }
        await("resume")
        tap("resume")
        compose.onNodeWithText(compose.activity.getString(R.string.v3_resuming)).assertIsDisplayed()
        tap("refresh_watches")
        compose.runOnUiThread { gate.complete(Unit) }
        // No notification intent is delivered to the Activity.
        compose.waitUntil(6000) { compose.onAllNodesWithText(compose.activity.getString(R.string.preparation_failed_title)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(compose.activity.getString(R.string.v3_resuming)).assertDoesNotExist()
        compose.onNodeWithText(compose.activity.getString(R.string.v1_source_failed)).assertIsDisplayed()
        capture("resume-failed-detail")
        tap("switch_view")
        compose.onNodeWithTag("watch_w").assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.preparation_failed_title)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.v3_resuming)).assertDoesNotExist()
        assertEquals(listOf("one-resume"),service.submittedCommands.map { it.id }.distinct())
        capture("resume-failed-home")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun pendingPauseDisablesRepeatedTapAndShowsConfirmedPauseWithoutRefresh() {
        val service = Service()
        val gate = CompletableDeferred<Unit>()
        service.pauseGate = gate
        val now = 1792767900000L
        val watch =
            Watch(
                "w",
                Proposal(
                    "p",
                    1,
                    RequestDraft("Promotions PS5", mode = FollowMode.Continuous),
                    Long.MAX_VALUE,
                    title = "Surveillance des promotions sur la PlayStation 5",
                    criteria = listOf("Promotions sur la PlayStation 5"),
                ),
                health = Health.Healthy,
                baselineAt = now,
                lastSuccess = now,
                nextCheck = now + 3600000,
                remoteState = RemoteState.Running,
            )
        service.list.value = listOf(watch)
        compose.runOnUiThread {
            val vm = WatchesViewModel(service, service, IdGenerator { "one-pause" })
            compose.activity.setContent {
                PreviensTheme {
                    val state by vm.state.collectAsState()
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Détail du suivi") },
                                actions = { RefreshWatchAction(state.refreshing, vm::refresh) },
                            )
                        },
                    ) { insets ->
                        Box(Modifier.padding(insets)) {
                            DetailScreen("w", state, vm, {}, {}, remote = true)
                        }
                    }
                }
            }
        }
        await("pause")
        tap("pause")
        compose.onNodeWithTag("pause").assertIsNotEnabled()
        compose.onNodeWithTag("pause").performTouchInput { click() }
        assertTrue(service.submittedCommands.map { it.id }.distinct().size <= 1)
        capture("pausing")
        compose.runOnUiThread { gate.complete(Unit) }
        await("resume")
        compose.onNodeWithTag("resume").assertIsEnabled()
        compose.onNodeWithTag("pause").assertDoesNotExist()
        compose
            .onNodeWithText(compose.activity.getString(R.string.status_paused))
            .assertIsDisplayed()
        assertEquals(listOf("one-pause"), service.submittedCommands.map { it.id }.distinct())
        capture("paused-confirmed")
        compose
            .onNodeWithText(compose.activity.getString(R.string.v3_continuous_description))
            .performScrollTo()
            .assertIsDisplayed()
        compose
            .onNodeWithText(compose.activity.getString(R.string.v1_mode_continuous))
            .assertDoesNotExist()
        capture("behaviour-description")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun renderProductionScreensWithExplicitFixtures() {
        val service = Service()
        val screen = mutableStateOf("home")
        val dark = args.getString("dark") == "true"
        val only = args.getString("screen")
        val names =
            only?.split(',')
                ?: listOf(
                    "home",
                    "request",
                    "confirm",
                    "clarify",
                    "preparing",
                    "active",
                    "result",
                    "email",
                    "code",
                    "empty",
                    "limit",
                    "failed",
                    "paused",
                    "account",
                    "delete",
                )
        val now = 1792767900000L
        val draft =
            RequestDraft(
                "Préviens-moi des promotions sur la PlayStation 5 en France.",
                mode = FollowMode.Continuous,
                answers = mapOf("region" to "France"),
            )
        val proposal =
            Proposal(
                "p",
                1,
                draft,
                Long.MAX_VALUE,
                partial = true,
                sources =
                    listOf(
                        "https://boutique.exemple.fr/ps5",
                        "https://enseigne.exemple.fr/ps5",
                        "https://autre.exemple.fr/ps5",
                    ),
                intervalHours = 1,
                title = "Surveillance des promotions sur la PlayStation 5",
                criteria = listOf("Nouvelles offres sur la PlayStation 5, disponibles en France."),
                method = "web_search",
                backgroundPreparation = true,
            )
        val watch =
            Watch(
                "w",
                proposal,
                health = Health.Healthy,
                baselineAt = now,
                lastSuccess = now,
                nextCheck = now + 3600000,
                remoteState = RemoteState.Running,
            )
        val finding =
            Finding(
                "f",
                "w",
                "p",
                1,
                proposal.criteria[0],
                now,
                source = proposal.sources[0],
                title = "Une promotion sur la PS5",
                fact =
                    "Une PlayStation 5 avec lecteur est proposée à 449 € par la boutique de démonstration, en France.",
                reason = "Cette offre concerne le produit et le pays choisis.",
                noveltyBasis = "initial",
            )
        compose.runOnUiThread {
            val create =
                CreateViewModel(
                    service,
                    service,
                    service,
                    service,
                    IdGenerator { "c" },
                    SavedStateHandle(),
                )
            val watches = WatchesViewModel(service, service, IdGenerator { "c" })
            val session = SessionViewModel(service)
            compose.activity.setContent {
                PreviensTheme(dark) {
                    val name = screen.value
                    val title =
                        when (name) {
                            "home",
                            "empty" -> R.string.v1_home
                            "request" -> R.string.v1_new
                            "confirm" -> R.string.v1_confirmation
                            "clarify" -> R.string.v1_clarify
                            "result" -> R.string.v1_result
                            "email",
                            "code" -> R.string.v1_connection
                            "account" -> R.string.v1_account
                            else -> R.string.detail_title
                        }
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(compose.activity.getString(title)) },
                                navigationIcon = {
                                    if (name !in listOf("home", "empty"))
                                        IconButton({}) {
                                            AppIcon(R.drawable.symbol_arrow_back, "Retour")
                                        }
                                },
                                actions = {
                                    if (name in listOf("home", "empty")) {
                                        IconButton({}) {
                                            AppIcon(R.drawable.symbol_refresh, "Actualiser")
                                        }
                                        TextButton({}) { Text("Connecté") }
                                    } else if (
                                        name in
                                            listOf(
                                                "active",
                                                "pausing",
                                                "unconfirmed",
                                                "delete",
                                                "limit",
                                                "paused",
                                                "failed",
                                                "preparing",
                                            )
                                    ) {
                                        RefreshWatchAction(false, {})
                                        IconButton({}) {
                                            AppIcon(R.drawable.symbol_more_vert, "Actions du suivi")
                                        }
                                    }
                                },
                            )
                        },
                        floatingActionButton = {
                            if (name in listOf("home", "empty"))
                                ExtendedFloatingActionButton(
                                    onClick = {},
                                    icon = { AppIcon(R.drawable.symbol_add) },
                                    text = { Text("Nouveau suivi") },
                                )
                        },
                    ) { insets ->
                        Box(Modifier.padding(insets)) {
                            when (name) {
                                "home",
                                "empty" ->
                                    HomeScreen(
                                        WatchesUiState(
                                            watches =
                                                if (name == "empty") emptyList()
                                                else
                                                    listOf(
                                                        watch,
                                                        watch.copy(
                                                            id = "w2",
                                                            status = WatchStatus.Paused,
                                                            plan =
                                                                proposal.copy(
                                                                    title =
                                                                        "Concerts de Stromae à Lyon",
                                                                ),
                                                        ),
                                                    ),
                                        ),
                                        CreateUiState(),
                                        create,
                                        {},
                                        {},
                                        {},
                                        remote = true,
                                    )
                                "request" ->
                                    CreateScreen(
                                        CreateUiState(draft = draft, loaded = true),
                                        create,
                                        remote = true,
                                    )
                                "confirm" ->
                                    CreateScreen(
                                        CreateUiState(
                                            draft = draft,
                                            loaded = true,
                                            step = CreationStep.Ready,
                                            proposal = proposal,
                                            partialAccepted = true,
                                        ),
                                        create,
                                        remote = true,
                                    )
                                "clarify" ->
                                    CreateScreen(
                                        CreateUiState(
                                            draft =
                                                draft.copy(
                                                    text =
                                                        "Préviens-moi quand le prochain iPhone sort",
                                                    answers =
                                                        mapOf("sortie" to "L’annonce officielle"),
                                                ),
                                            loaded = true,
                                            step = CreationStep.Clarifying,
                                            questions =
                                                listOf(
                                                    ClarificationQuestion(
                                                        "sortie",
                                                        "Quelle sortie veux-tu suivre ?",
                                                        listOf(
                                                            "L’annonce officielle",
                                                            "L’ouverture des précommandes",
                                                            "La disponibilité en magasin",
                                                        ),
                                                        true,
                                                    ),
                                                ),
                                        ),
                                        create,
                                        remote = true,
                                    )
                                "result" ->
                                    FindingScreen(
                                        "f",
                                        WatchesUiState(
                                            watches = listOf(watch),
                                            findings = listOf(finding),
                                        ),
                                        remote = true,
                                    )
                                "email" ->
                                    SessionScreen(
                                        SessionUiState(email = "alex@example.test"),
                                        session,
                                        forConfirmation = true,
                                    )
                                "code" ->
                                    SessionScreen(
                                        SessionUiState(email = "alex@example.test", sent = true),
                                        session,
                                        forConfirmation = true,
                                    )
                                "account" ->
                                    SessionScreen(
                                        SessionUiState(
                                            session = Session("fixture", "alex@example.test"),
                                        ),
                                        session,
                                    )
                                else -> {
                                    val w =
                                        when (name) {
                                            "preparing" ->
                                                watch.copy(
                                                    status = WatchStatus.Pending,
                                                    baselineAt = null,
                                                    lastSuccess = null,
                                                    nextCheck = null,
                                                    health = Health.NotCheckedYet,
                                                )
                                            "limit" ->
                                                watch.copy(
                                                    status = WatchStatus.SuspendedByCapacity,
                                                    nextCheck = null,
                                                )
                                            "failed" ->
                                                watch.copy(
                                                    status = WatchStatus.Paused,
                                                    baselineAt = null,
                                                    lastSuccess = null,
                                                    nextCheck = null,
                                                    health = Health.Degraded,
                                                    plan =
                                                        proposal.copy(
                                                            preparationFailure = "source_unusable",
                                                        ),
                                                )
                                            "paused" ->
                                                watch.copy(
                                                    status = WatchStatus.Paused,
                                                    nextCheck = null,
                                                )
                                            else -> watch
                                        }
                                    DetailScreen(
                                        "w",
                                        WatchesUiState(
                                            watches = listOf(w),
                                            commandPending = name == "pausing",
                                            commandUnconfirmed =
                                                name in listOf("pausing", "unconfirmed"),
                                            commandWatchId =
                                                if (name in listOf("pausing", "unconfirmed")) "w"
                                                else null,
                                            commandOperation =
                                                if (name in listOf("pausing", "unconfirmed"))
                                                    Operation.Pause
                                                else null,
                                            findings =
                                                if (name in listOf("preparing", "failed"))
                                                    emptyList()
                                                else listOf(finding),
                                        ),
                                        watches,
                                        {},
                                        {},
                                        remote = true,
                                        notificationsEnabled = name != "preparing",
                                    )
                                }
                            }
                        }
                    }
                    if (name == "delete") WatchDeleteDialog(watch, {}, {})
                }
            }
        }
        for (name in names) {
            compose.runOnUiThread { screen.value = name }
            compose.waitForIdle()
            if (args.getString("scroll") == "sources")
                compose.onAllNodesWithTag("prepared_source").onLast().performScrollTo()
            if (args.getString("scroll") == "disclosure")
                compose
                    .onNodeWithText(compose.activity.getString(R.string.v2_trial_note))
                    .performScrollTo()
            if (args.getString("scroll") == "choices")
                compose.onNodeWithTag("once").performScrollTo()
            capture(name + if (dark) "-dark" else args.getString("suffix", ""))
        }
    }
}
