package fr.previensmoi.feature.subscription

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

@Composable
fun SubscriptionSynchronizationEffect(viewModel: SubscriptionViewModel, ownerId: String?) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle, ownerId) {
        viewModel.bindOwner(ownerId)
        if (ownerId != null)
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.synchronizeWhileVisible()
            }
    }
}
