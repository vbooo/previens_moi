package fr.previensmoi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import fr.previensmoi.R
import fr.previensmoi.feature.watches.presentation.WatchesUiState
import fr.previensmoi.feature.watches.presentation.WatchesViewModel
import kotlinx.serialization.Serializable

@Serializable private data object Watches : NavKey
@Serializable private data object Example : NavKey

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: WatchesViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val stack = rememberNavBackStack(Watches)
                Scaffold { insets ->
                    Column(Modifier.fillMaxSize().padding(insets).padding(24.dp)) {
                        Text(stringResource(R.string.demo_banner), style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(24.dp))
                        NavDisplay(
                            backStack = stack,
                            onBack = { if (stack.size > 1) stack.removeLastOrNull() },
                            entryProvider = entryProvider {
                                entry<Watches> {
                                    WatchesScreen(state, viewModel::refresh, { stack.add(Example) })
                                }
                                entry<Example> {
                                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Text(stringResource(R.string.sample_title), style = MaterialTheme.typography.headlineMedium)
                                        Text(stringResource(R.string.sample_detail))
                                        TextButton(onClick = { stack.removeLastOrNull() }) { Text(stringResource(R.string.back)) }
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchesScreen(state: WatchesUiState, refresh: () -> Unit, openExample: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.title), style = MaterialTheme.typography.headlineLarge)
        if (state.watches.isEmpty()) Text(stringResource(R.string.empty))
        else {
            Text(stringResource(R.string.sample_title))
            OutlinedButton(onClick = openExample) { Text(stringResource(R.string.open_detail)) }
        }
        if (state.refreshFailed) Text(stringResource(R.string.failure), color = MaterialTheme.colorScheme.error)
        Button(onClick = refresh, enabled = !state.refreshing) {
            Text(stringResource(if (state.refreshing) R.string.loading else R.string.refresh))
        }
    }
}
