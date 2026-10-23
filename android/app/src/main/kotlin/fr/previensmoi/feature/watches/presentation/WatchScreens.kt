package fr.previensmoi.feature.watches.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.previensmoi.R
import fr.previensmoi.domain.model.*
import fr.previensmoi.feature.common.presentation.*
import fr.previensmoi.feature.create.presentation.*

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(state: WatchesUiState, creation: CreateUiState, createVm: CreateViewModel, preview: () -> Unit, refresh: () -> Unit, open: (String) -> Unit, remote: Boolean = false, signedIn: Boolean = true, signIn: () -> Unit = {}, newRequest: (() -> Unit)? = null) {
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = { if (signedIn) refresh() }, modifier = Modifier.fillMaxSize().testTag("watch_list")) {
    Page {
        if (remote && newRequest != null) {
            Button(onClick = newRequest, modifier = Modifier.fillMaxWidth().testTag("new_request")) { Text(stringResource(R.string.new_watch)) }
        } else {
        RequestFields(creation, createVm)
        Text(stringResource(if (remote) R.string.unqualified_body else R.string.demo_explanation), style = MaterialTheme.typography.bodySmall)
        if (creation.resetNotice) Text(stringResource(R.string.reset_notice))
        if (creation.storageFailed) FailureText(Failure.StorageFailure)
        if (creation.step == CreationStep.Editing) FailureText(creation.error)
        Button(onClick = preview, enabled = creation.loaded, modifier = Modifier.testTag("preview")) { Text(stringResource(R.string.analyse)) }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.title), style = MaterialTheme.typography.titleLarge)
            if (signedIn) TextButton(onClick = refresh, enabled = !state.refreshing, modifier = Modifier.testTag("refresh_watches")) { Text(stringResource(R.string.refresh_short)) }
        }
        if (remote && !signedIn) {
            Text(stringResource(R.string.sign_in_to_view_watches), modifier = Modifier.testTag("signed_out_watches"))
            TextButton(onClick = signIn, modifier = Modifier.testTag("sign_in_watches")) { Text(stringResource(R.string.sign_in)) }
            return@Page
        }
        if (state.refreshFailed) Text(stringResource(R.string.stale_content), color = MaterialTheme.colorScheme.error)
        if (state.refreshing) Busy()
        if (state.watches.isEmpty()) Text(stringResource(R.string.empty_watches))
        state.watches.forEach { watch ->
            Column(Modifier.fillMaxWidth().clickable { open(watch.id) }.testTag("watch_${watch.id}").padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(watch.plan.draft.text, style = MaterialTheme.typography.titleMedium)
                WatchProgress(watch, remote)
                HealthSummary(watch)
                if (state.findings.any { it.watchId == watch.id }) Text(stringResource(R.string.results_title))
            }
            HorizontalDivider()
        }
    }
    }
}

@Composable private fun WatchProgress(watch: Watch, remote: Boolean) {
    if (remote && watch.status == WatchStatus.Pending) {
        Text(stringResource(R.string.preparing_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.preparing_body), style = MaterialTheme.typography.bodyMedium)
    } else if (remote && watch.baselineAt == null && watch.health == Health.Degraded && watch.status == WatchStatus.Paused) {
        Text(stringResource(R.string.preparation_failed_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(when(watch.plan.preparationFailure) { "source_unusable" -> R.string.preparation_source_failed; "quota" -> R.string.preparation_quota_failed; else -> R.string.preparation_failed_body }), style = MaterialTheme.typography.bodyMedium)
    } else Text(stringResource(statusLabel(watch.status)))
}

@Composable private fun HealthSummary(watch: Watch) {
    if (watch.status == WatchStatus.Pending) return
    Text(stringResource(when(watch.health) { Health.NotCheckedYet -> R.string.health_new; Health.Healthy -> R.string.health_ok; Health.Degraded -> R.string.health_bad }))
    if (watch.lastSuccess != null) Text(stringResource(R.string.last_success, dateText(watch.lastSuccess)), style = MaterialTheme.typography.bodySmall)
    if (watch.lastFailure != null) Text(stringResource(R.string.last_failure, dateText(watch.lastFailure)), style = MaterialTheme.typography.bodySmall)
}

@Composable
fun DetailScreen(id: String, state: WatchesUiState, vm: WatchesViewModel, edit: (Watch) -> Unit, openFinding: (String) -> Unit, remote: Boolean = false) {
    val watch = state.watches.find { it.id == id }
    val uriHandler = LocalUriHandler.current
    var deleting by rememberSaveable(id) { mutableStateOf(false) }
    if (watch == null) { Page { if (state.refreshing) Busy() else Text(stringResource(R.string.missing)) }; return }
    Page {
        Text(stringResource(R.string.detail_title), style = MaterialTheme.typography.headlineMedium)
        Text(watch.plan.draft.text, style = MaterialTheme.typography.titleLarge)
        if (watch.plan.draft.answer.isNotBlank()) Text(watch.plan.draft.answer)
        Column(Modifier.testTag("watch_status")) { WatchProgress(watch, remote) }
        if (remote && watch.status == WatchStatus.Pending) {
            OutlinedButton(onClick = { vm.command(watch, Operation.Pause) }, enabled = !state.commandPending, modifier = Modifier.testTag("pause")) { Text(stringResource(R.string.cancel_preparation)) }
            TextButton(onClick = vm::refresh) { Text(stringResource(R.string.refresh_short)) }
            FailureText(state.commandError)
            return@Page
        }
        HealthSummary(watch)
        if (remote && watch.plan.backgroundPreparation && watch.plan.sources.isNotEmpty()) {
            Text(stringResource(R.string.prepared_sources), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.prepared_sources_body), style = MaterialTheme.typography.bodySmall)
            watch.plan.sources.forEach { url -> TextButton(onClick = { uriHandler.openUri(url) }, modifier = Modifier.testTag("prepared_source")) { Text(url) } }
        }
        if (state.refreshFailed) Text(stringResource(R.string.stale_content), color = MaterialTheme.colorScheme.error)
        if (watch.baselineAt != null) Text(stringResource(R.string.baseline_at, dateText(watch.baselineAt)))
        if (watch.remoteState == RemoteState.Stopping) Text(stringResource(R.string.remote_stopping))
        if (watch.remoteState == RemoteState.Unknown) Text(stringResource(R.string.remote_unknown))
        if (watch.nextCheck != null) Text(stringResource(R.string.next_check, dateText(watch.nextCheck)))
        Text(stringResource(if (remote) R.string.remote_terms else R.string.demo_terms, watch.plan.intervalHours, watch.plan.limit))
        if (watch.plan.partial) Text(stringResource(R.string.partial_text))
        Text(stringResource(if (remote) R.string.push_status_info else R.string.no_notifications), style = MaterialTheme.typography.bodySmall)
        FailureText(state.commandError)
        if (state.commandPending) Busy()
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (watch.status == WatchStatus.Active || remote && watch.status == WatchStatus.Pending) OutlinedButton(onClick = { vm.command(watch, Operation.Pause) }, enabled = !state.commandPending,
                modifier = Modifier.testTag("pause")) { Text(stringResource(if (watch.status == WatchStatus.Pending) R.string.cancel_preparation else R.string.pause)) }
            if (watch.status == WatchStatus.Paused || watch.status == WatchStatus.SuspendedByCapacity) OutlinedButton(onClick = { vm.command(watch, Operation.Resume) }, enabled = !state.commandPending,
                modifier = Modifier.testTag("resume")) { Text(stringResource(R.string.resume)) }
            if (watch.status != WatchStatus.Completed) OutlinedButton(onClick = { edit(watch) }, enabled = !state.commandPending,
                modifier = Modifier.testTag("edit")) { Text(stringResource(R.string.edit)) }
        }
        if (watch.status == WatchStatus.Paused) Text(stringResource(R.string.resume_info), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.results_title), style = MaterialTheme.typography.titleLarge)
        val findings = state.findings.filter { it.watchId == id }
        if (findings.isEmpty()) Text(stringResource(R.string.no_results))
        findings.forEach { finding ->
            OutlinedButton(onClick = { openFinding(finding.id) }, modifier = Modifier.testTag("finding")) { Text(stringResource(R.string.finding_title)) }
            Text(stringResource(R.string.observed_at, dateText(finding.observedAt)), style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = { deleting = true }, enabled = !state.commandPending,
            modifier = Modifier.testTag("delete"), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.delete)) }
    }
    if (deleting) AlertDialog(onDismissRequest = { deleting = false },
        title = { Text(stringResource(R.string.delete_title)) }, text = { Text(stringResource(R.string.delete_info)) },
        confirmButton = { TextButton(onClick = { deleting = false; vm.command(watch, Operation.Delete) }, modifier = Modifier.testTag("confirm_delete")) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text(stringResource(R.string.cancel)) } })
}

@Composable
fun FindingScreen(id: String, state: WatchesUiState, remote: Boolean = false) {
    val finding = state.findings.find { it.id == id }
    Page {
        if (finding == null) { if (state.findingError != null) FailureText(state.findingError) else Text(stringResource(R.string.missing)) }
        else {
            Text(stringResource(R.string.finding_title), style = MaterialTheme.typography.headlineMedium)
            if (!remote) Text(stringResource(R.string.finding_info))
            if (finding.title.isNotBlank()) Text(finding.title)
            if (finding.fact.isNotBlank()) Text(finding.fact)
            if (finding.reason.isNotBlank()) Text(finding.reason)
            if (finding.correctsFindingId != null) Text(stringResource(R.string.correction_notice))
            finding.evidence.forEach { proof -> Text(proof.quote); Text(proof.url); proof.publicationDate?.let { Text(it) } }
            finding.eventDate?.let { Text(it) }
            val watch = state.watches.find { it.id == finding.watchId }
            if (watch?.plan?.id != finding.planId || watch.plan.version != finding.planVersion) Text(stringResource(R.string.old_plan))
            Text(stringResource(R.string.why), style = MaterialTheme.typography.titleMedium)
            Text(finding.criterion)
            Text(stringResource(R.string.published_at, dateText(finding.publishedAt)), modifier = Modifier.testTag("publication"))
            Text(stringResource(R.string.observed_at, dateText(finding.observedAt)))
            Text(stringResource(R.string.event_at, dateText(finding.eventAt)))
            Text(stringResource(R.string.source_label)); Text(finding.source)
        }
    }
}
