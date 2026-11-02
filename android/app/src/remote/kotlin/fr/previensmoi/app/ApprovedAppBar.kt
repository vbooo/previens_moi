package fr.previensmoi.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** One bar vocabulary for the real navigation, with a larger collection title. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WatchAppBar(
    expanded: Boolean,
    loading: Boolean,
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    colors: TopAppBarColors,
) {
    Column {
        if (expanded) MediumTopAppBar(title = title, navigationIcon = navigationIcon, actions = actions, colors = colors)
        else TopAppBar(title = title, navigationIcon = navigationIcon, actions = actions, colors = colors)
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}
