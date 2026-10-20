package fr.previensmoi.feature.common.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.previensmoi.R
import fr.previensmoi.domain.model.Failure
import fr.previensmoi.domain.model.WatchStatus
import java.text.DateFormat
import java.util.Date

@Composable
fun Page(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}
@Composable fun FailureText(failure: Failure?) {
    if (failure == null) return
    val id = when (failure) {
        Failure.AnalysisPending -> R.string.analysis_pending
        Failure.OperationPending -> R.string.operation_pending
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
    Text(stringResource(id), color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
}
@Composable fun dateText(value: Long?): String = value?.let { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)) } ?: stringResource(R.string.unknown_date)
fun statusLabel(status: WatchStatus) = when(status) {
    WatchStatus.Pending -> R.string.status_pending
    WatchStatus.Active -> R.string.status_active
    WatchStatus.Paused -> R.string.status_paused
    WatchStatus.Completed -> R.string.status_completed
    WatchStatus.SuspendedByCapacity -> R.string.status_capacity
}
@Composable fun Busy() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        Text(stringResource(R.string.processing), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
}
