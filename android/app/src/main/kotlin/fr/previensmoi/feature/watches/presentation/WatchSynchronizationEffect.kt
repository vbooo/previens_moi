package fr.previensmoi.feature.watches.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

/** Leaving the foreground or signing out cancels reads, including a read currently in flight. */
@Composable
fun WatchSynchronizationEffect(viewModel: WatchesViewModel, ownerId: String?) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle, ownerId) {
        if (ownerId != null) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.synchronizeWhileVisible()
        }
    }
}
