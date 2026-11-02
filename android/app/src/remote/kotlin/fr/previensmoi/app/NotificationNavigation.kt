package fr.previensmoi.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavKey
import fr.previensmoi.domain.model.Failure
import fr.previensmoi.feature.session.presentation.SessionUiState
import fr.previensmoi.feature.watches.presentation.WatchesUiState
import kotlinx.serialization.Serializable

@Serializable internal data object Home : NavKey
@Serializable internal data object Account : NavKey
@Serializable internal data object Create : NavKey
@Serializable internal data class Detail(val id: String) : NavKey
@Serializable internal data class Result(val id: String) : NavKey

/** The first frame of a notification launch is its destination, not the home screen. */
internal fun notificationInitialRoutes(findingId: String?, watchId: String?): List<NavKey> =
    when {
        findingId != null -> listOf(Home, Result(findingId))
        watchId != null -> listOf(Home, Detail(watchId))
        else -> listOf(Home)
    }

@Composable
internal fun FindingNotificationNavigation(
    requestedId: String?,
    account: SessionUiState,
    state: WatchesUiState,
    stack: MutableList<NavKey>,
    load: (String) -> Long,
    consume: () -> Unit,
    requestKey: Int = 0,
) {
    var started by remember(requestedId, requestKey, account.session?.id) { mutableStateOf<Long?>(null) }
    LaunchedEffect(requestedId, requestKey, account.loaded, account.session?.id) {
        val id = requestedId ?: return@LaunchedEffect
        // Null before the first repository emission does not mean the user signed out.
        if (!account.loaded) return@LaunchedEffect
        if (account.session == null) {
            if (stack.lastOrNull() != Account) stack.add(Account)
        } else {
            if (stack.lastOrNull() != Result(id)) {
                stack.clear(); stack.add(Home); stack.add(Result(id))
            }
            started = load(id)
        }
    }
    LaunchedEffect(requestedId, requestKey, started, state.findingRequestId, state.findingRequestEpoch, state.findingLoading, state.findingError, state.findings) {
        val id = requestedId ?: return@LaunchedEffect
        val expected = started ?: return@LaunchedEffect
        if (state.findingRequestId != id || state.findingRequestEpoch < expected || state.findingLoading) return@LaunchedEffect
        val finding = state.findings.find { it.id == id }
        if (finding != null) {
            // Add the parent for Back without ever displaying it during loading.
            if (stack.lastOrNull() == Result(id)) {
                stack.clear(); stack.add(Home); stack.add(Detail(finding.watchId)); stack.add(Result(id))
            }
            consume()
        } else if (state.findingError == Failure.SessionRequired) {
            if (stack.lastOrNull() != Account) stack.add(Account)
        } else if (state.findingError in setOf(Failure.NotFound, Failure.AccessDenied)) {
            // Keep the result route for missing/error/retry; do not flash the home screen.
            consume()
        }
    }
}

/** Canceling sign-in for a notification must not return to an unrequested private loading page. */
internal fun dismissFindingNotification(stack: MutableList<NavKey>, requestedId: String?, consume: () -> Unit): Boolean {
    if (requestedId == null) return false
    val signingIn = stack.lastOrNull() == Account
    consume()
    if (signingIn) { stack.clear(); stack.add(Home) }
    return signingIn
}
