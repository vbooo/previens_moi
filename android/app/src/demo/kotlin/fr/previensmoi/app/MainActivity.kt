package fr.previensmoi.app

import android.os.Bundle
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
import fr.previensmoi.feature.common.presentation.PreviensTheme
import fr.previensmoi.data.demo.DemoScenario
import fr.previensmoi.feature.create.presentation.*
import fr.previensmoi.feature.watches.presentation.*
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable private data object Home : NavKey
@Serializable private data object Create : NavKey
@Serializable private data class Detail(val id: String) : NavKey
@Serializable private data class Result(val id: String) : NavKey

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val watches: WatchesViewModel by viewModels()
    private val creation: CreateViewModel by viewModels()
    private val demo: DemoViewModel by viewModels()
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PreviensTheme() {
                val state by watches.state.collectAsStateWithLifecycle()
                val create by creation.state.collectAsStateWithLifecycle()
                val scenario by demo.scenario.collectAsStateWithLifecycle()
                val focus = LocalFocusManager.current
                val keyboard = LocalSoftwareKeyboardController.current
                val stack = rememberNavBackStack(Home)
                var tools by rememberSaveable { mutableStateOf(false) }
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
                        actions = { TextButton(onClick = { tools = !tools }, modifier = Modifier.testTag("tools")) { Text(stringResource(R.string.demo_tools)) } })
                }) { insets ->
                    Column(Modifier.fillMaxSize().padding(insets).imePadding()) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(stringResource(R.string.demo_banner), Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
                        }
                        if (tools) DemoTools(scenario, select = { demo.select(it); tools = false },
                            check = { demo.simulateCheck(); tools = false }, reset = { demo.reset(); tools = false })
                        NavDisplay(modifier = Modifier.weight(1f), backStack = stack,
                            onBack = { if (stack.size > 1) stack.removeLastOrNull() }, entryProvider = entryProvider {
                                entry<Home> { HomeScreen(state, create, creation, preview = {
                                    focus.clearFocus(); keyboard?.hide(); creation.startNew(); creation.evaluate(Locale.getDefault().language); stack.add(Create)
                                }, refresh = watches::refresh, open = { watches.clearCommandError(); stack.add(Detail(it)) }) }
                                entry<Create> { CreateScreen(create, creation) }
                                entry<Detail> { key -> DetailScreen(key.id, state, watches,
                                    edit = { creation.edit(it); stack.add(Create) }, openFinding = { stack.add(Result(it)) }) }
                                entry<Result> { key -> FindingScreen(key.id, state) }
                            })
                    }
                }
            }
        }
    }
}
private fun scenarioLabel(s: DemoScenario) = when(s) {
    DemoScenario.Normal -> R.string.demo_normal
    DemoScenario.Clarification -> R.string.demo_clarification
    DemoScenario.Partial -> R.string.demo_partial
    DemoScenario.Unsupported -> R.string.demo_unsupported
    DemoScenario.NoResults -> R.string.demo_no_results
    DemoScenario.Quota -> R.string.demo_quota
    DemoScenario.SourceFailure -> R.string.demo_source_failure
    DemoScenario.Offline -> R.string.demo_offline
    DemoScenario.CommandFailure -> R.string.demo_command_failure
    DemoScenario.LostResponse -> R.string.demo_lost_response
    DemoScenario.Expired -> R.string.demo_expired
    DemoScenario.VersionConflict -> R.string.demo_conflict
}
@Composable private fun DemoTools(scenario: DemoScenario, select: (DemoScenario) -> Unit, check: () -> Unit, reset: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(stringResource(R.string.demo_tools_title), style = MaterialTheme.typography.titleSmall)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.testTag("scenario_menu")) { Text(stringResource(scenarioLabel(scenario))) }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                DemoScenario.entries.forEach { item -> DropdownMenuItem(text = { Text(stringResource(scenarioLabel(item))) },
                    onClick = { expanded = false; select(item) }, modifier = Modifier.testTag("scenario_${item.name}")) }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = check, modifier = Modifier.testTag("simulate_check")) { Text(stringResource(R.string.simulate_check)) }
            TextButton(onClick = reset, modifier = Modifier.testTag("reset_demo")) { Text(stringResource(R.string.reset_demo)) }
        }
    }
}
