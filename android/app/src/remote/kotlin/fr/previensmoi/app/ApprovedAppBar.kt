package fr.previensmoi.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/** Compact context bar; content starts directly below the navigation actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WatchAppBar(
    loading: Boolean,
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    colors: TopAppBarColors,
) {
    Column {
        TopAppBar(
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = colors,
        )
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("app_bar_progress"))
    }
}
