package fr.previensmoi.feature.watches.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.previensmoi.R
import fr.previensmoi.domain.model.*
import fr.previensmoi.feature.common.presentation.*
import fr.previensmoi.feature.create.presentation.*

@Composable
fun HomeScreen(state: WatchesUiState, creation: CreateUiState, createVm: CreateViewModel, preview: () -> Unit, refresh: () -> Unit, open: (String) -> Unit, remote: Boolean = false) {
    Page {
        RequestFields(creation, createVm)
        Text(stringResource(if (remote) R.string.unqualified_body else R.string.demo_explanation), style = MaterialTheme.typography.bodySmall)
        if (creation.resetNotice) Text(stringResource(R.string.reset_notice))
        if (creation.storageFailed) FailureText(Failure.StorageFailure)
        if (creation.step == CreationStep.Editing) FailureText(creation.error)
        Button(onClick = preview, enabled = creation.loaded, modifier = Modifier.testTag("preview")) { Text(stringResource(R.string.analyse)) }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text(stringResource(R.string.title), style = MaterialTheme.typography.titleLarge)
        if (state.refreshFailed) Text(stringResource(R.string.stale_content), color = MaterialTheme.colorScheme.error)
        if (state.refreshing) Busy()
        if (state.watches.isEmpty()) Text(stringResource(R.string.empty_watches))
        state.watches.forEach { watch ->
            Column(Modifier.fillMaxWidth().clickable { open(watch.id) }.testTag("watch_${watch.id}").padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(watch.plan.draft.text, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(statusLabel(watch.status)))
                HealthSummary(watch)
                if (state.findings.any { it.watchId == watch.id }) Text(stringResource(R.string.results_title))
            }
            HorizontalDivider()
        }
        TextButton(onClick = refresh, enabled = !state.refreshing) { Text(stringResource(R.string.refresh_list)) }
    }
}

@Composable private fun HealthSummary(watch: Watch) {
    Text(stringResource(when(watch.health) { Health.NotCheckedYet -> R.string.health_new; Health.Healthy -> R.string.health_ok; Health.Degraded -> R.string.health_bad }))
    if (watch.lastSuccess != null) Text(stringResource(R.string.last_success, dateText(watch.lastSuccess)), style = MaterialTheme.typography.bodySmall)
    if (watch.lastFailure != null) Text(stringResource(R.string.last_failure, dateText(watch.lastFailure)), style = MaterialTheme.typography.bodySmall)
}

@Composable
fun DetailScreen(id: String, state: WatchesUiState, vm: WatchesViewModel, edit: (Watch) -> Unit, openFinding: (String) -> Unit, remote: Boolean = false) {
    val watch = state.watches.find { it.id == id }
    var deleting by rememberSaveable(id) { mutableStateOf(false) }
    if (watch == null) { Page { Text(stringResource(R.string.missing)) }; return }
    Page {
        Text(stringResource(R.string.detail_title), style = MaterialTheme.typography.headlineMedium)
        Text(watch.plan.draft.text, style = MaterialTheme.typography.titleLarge)
        if (watch.plan.draft.answer.isNotBlank()) Text(watch.plan.draft.answer)
        Text(stringResource(statusLabel(watch.status)), modifier = Modifier.testTag("watch_status"))
        HealthSummary(watch)
        if (state.refreshFailed) Text(stringResource(R.string.stale_content), color = MaterialTheme.colorScheme.error)
        Text(stringResource(R.string.baseline_at, dateText(watch.baselineAt)))
        if (watch.remoteState == RemoteState.Stopping) Text(stringResource(R.string.remote_stopping))
        if (watch.remoteState == RemoteState.Unknown) Text(stringResource(R.string.remote_unknown))
        if (watch.nextCheck != null) Text(stringResource(R.string.next_check, dateText(watch.nextCheck)))
        Text(stringResource(if (remote) R.string.remote_terms else R.string.demo_terms, watch.plan.intervalHours, watch.plan.limit))
        if (watch.plan.partial) Text(stringResource(R.string.partial_text))
        Text(stringResource(if (remote) R.string.push_status_info else R.string.no_notifications), style = MaterialTheme.typography.bodySmall)
        FailureText(state.commandError)
        if (state.commandPending) Busy()
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (watch.status == WatchStatus.Active) OutlinedButton(onClick = { vm.command(watch, Operation.Pause) }, enabled = !state.commandPending,
                modifier = Modifier.testTag("pause")) { Text(stringResource(R.string.pause)) }
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
