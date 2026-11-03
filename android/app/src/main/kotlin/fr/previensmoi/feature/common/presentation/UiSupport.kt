package fr.previensmoi.feature.common.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.previensmoi.R
import fr.previensmoi.domain.model.Failure
import fr.previensmoi.domain.model.WatchStatus
import java.text.DateFormat
import java.util.Date

@Composable
fun Page(
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    spacing: Dp = 16.dp,
    contentKey: Any? = Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = key(contentKey) { rememberScrollState() }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.widthIn(max = 720.dp)
                        .fillMaxWidth()
                        .verticalScroll(scroll)
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing),
                    content = content,
                )
            }
            if (footer != null)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    tonalElevation = 2.dp,
                ) {
                    Column(
                        Modifier.widthIn(max = 720.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 12.dp, bottom = 8.dp),
                        content = footer,
                    )
                }
        }
    }
}

@Composable
fun FailureText(failure: Failure?) {
    if (failure == null) return
    val id =
        when (failure) {
            Failure.AnalysisPending -> R.string.analysis_pending
            Failure.OperationPending -> R.string.operation_pending
            Failure.OperationFailed -> R.string.operation_failed
            Failure.Offline -> R.string.error_offline
            Failure.TemporaryFailure -> R.string.error_temporary
            Failure.SessionRequired -> R.string.error_session
            Failure.AccessDenied -> R.string.error_access
            Failure.NotFound -> R.string.missing
            Failure.ProposalExpired -> R.string.error_expired
            Failure.VersionConflict -> R.string.error_version
            Failure.CommandConflict -> R.string.error_command
            Failure.QuotaExceeded -> R.string.error_quota
            Failure.Unsupported -> R.string.error_unsupported
            Failure.InvalidInput -> R.string.error_input
            Failure.StorageFailure -> R.string.storage_error
        }
    val pending = failure == Failure.AnalysisPending || failure == Failure.OperationPending
    Text(
        stringResource(id),
        color =
            if (pending) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
fun dateText(value: Long?): String =
    value?.let {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))
    } ?: stringResource(R.string.unknown_date)

fun statusLabel(status: WatchStatus) =
    when (status) {
        WatchStatus.Pending -> R.string.status_pending
        WatchStatus.Active -> R.string.status_active
        WatchStatus.Paused -> R.string.status_paused
        WatchStatus.Completed -> R.string.status_completed
        WatchStatus.SuspendedByCapacity -> R.string.status_capacity
    }

@Composable
fun Busy() {
    LinearProgressIndicator(Modifier.fillMaxWidth().testTag("content_progress"))
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(title)
        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

/** Native list items own their insets, without nesting cards or duplicating section headings. */
@Composable
fun CollectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(title)
        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = MaterialTheme.shapes.large) {
            Column(content = content)
        }
    }
}

/** Compact local date, with the year retained when the observation is from a different year. */
fun compactDateText(value: Long): String {
    val now = java.util.Calendar.getInstance()
    val observed = java.util.Calendar.getInstance().apply { timeInMillis = value }
    val pattern = if (observed.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR))
        "d MMM '·' HH:mm" else "d MMM yyyy '·' HH:mm"
    return java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).format(Date(value))
}

@Composable
fun RefreshWatchAction(refreshing: Boolean, refresh: () -> Unit) {
    val description = stringResource(R.string.refresh_short)
    IconButton(
        onClick = refresh,
        enabled = !refreshing,
        modifier =
            Modifier.testTag("refresh_watches").semantics { contentDescription = description },
    ) {
        if (refreshing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        else AppIcon(R.drawable.symbol_refresh)
    }
}
