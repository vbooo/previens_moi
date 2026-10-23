package fr.previensmoi.feature.watches.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.previensmoi.R
import fr.previensmoi.domain.model.*
import fr.previensmoi.feature.common.presentation.*
import fr.previensmoi.feature.create.presentation.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: WatchesUiState,
    creation: CreateUiState,
    createVm: CreateViewModel,
    preview: () -> Unit,
    refresh: () -> Unit,
    open: (String) -> Unit,
    remote: Boolean = false,
    signedIn: Boolean = true,
    signIn: () -> Unit = {},
    newRequest: (() -> Unit)? = null,
) {
    PullToRefreshBox(
        state.refreshing && signedIn,
        { if (signedIn) refresh() },
        Modifier.fillMaxSize().testTag("watch_list"),
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!remote)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        RequestFields(creation, createVm)
                        Button(
                            preview,
                            enabled = creation.loaded,
                            modifier = Modifier.testTag("preview"),
                        ) {
                            Text(stringResource(R.string.analyse))
                        }
                        HorizontalDivider()
                    }
                }
            if (remote && !signedIn)
                item {
                    Column(
                        Modifier.padding(top = 64.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            stringResource(R.string.v1_empty_title),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        SecondaryText(stringResource(R.string.v1_empty_body))
                        SecondaryText(
                            stringResource(R.string.sign_in_to_view_watches),
                            Modifier.testTag("signed_out_watches"),
                        )
                        TextButton(signIn, modifier = Modifier.testTag("sign_in_watches")) {
                            Text(stringResource(R.string.sign_in))
                        }
                    }
                }
            else {
                if (state.refreshFailed)
                    item {
                        Column {
                            SecondaryText(stringResource(R.string.stale_content))
                            TextButton(refresh) { Text(stringResource(R.string.v1_retry)) }
                        }
                    }
                if (state.watches.isEmpty() && !state.refreshing)
                    item {
                        Column(
                            Modifier.padding(top = 72.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                stringResource(R.string.v1_empty_title),
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            SecondaryText(stringResource(R.string.v1_empty_body))
                        }
                    }
                items(state.watches, key = { it.id }) { watch ->
                    Column {
                        ListItem(
                            headlineContent = {
                                Text(
                                    watchTitle(watch),
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            supportingContent = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    WatchStatusLabel(watch)
                                    watch.lastSuccess?.let {
                                        SecondaryText(
                                            stringResource(R.string.last_success, dateText(it)),
                                        )
                                    }
                                }
                            },
                            trailingContent = { AppIcon(R.drawable.symbol_chevron_right) },
                            modifier =
                                Modifier.clickable { open(watch.id) }.testTag("watch_${watch.id}"),
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun WatchDeleteDialog(watch: Watch, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_title)) },
        text = { Text(stringResource(R.string.v1_delete_body)) },
        confirmButton = {
            TextButton(
                onConfirm,
                modifier = Modifier.testTag("confirm_delete"),
                colors =
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.v1_delete))
            }
        },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun SourceLink(url: String, label: String = sourceDomain(url)) {
    val uri = LocalUriHandler.current
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { if (label != sourceDomain(url)) SecondaryText(sourceDomain(url)) },
        trailingContent = {
            AppIcon(R.drawable.symbol_open_in_new, tint = MaterialTheme.colorScheme.primary)
        },
        modifier =
            Modifier.clickable { uri.openUri(url) }
                .heightIn(min = 56.dp)
                .testTag("prepared_source"),
    )
}

@Composable
fun DetailScreen(
    id: String,
    state: WatchesUiState,
    vm: WatchesViewModel,
    edit: (Watch) -> Unit,
    openFinding: (String) -> Unit,
    remote: Boolean = false,
    notificationsEnabled: Boolean = true,
    enableNotifications: () -> Unit = {},
) {
    val watch = state.watches.find { it.id == id }
    if (watch == null) {
        Page { if (state.refreshing) Busy() else Text(stringResource(R.string.missing)) }
        return
    }
    var deleting by rememberSaveable(id) { mutableStateOf(false) }
    if (deleting)
        WatchDeleteDialog(
            watch,
            { deleting = false },
            {
                deleting = false
                vm.command(watch, Operation.Delete)
            },
        )
    val failed =
        watch.baselineAt == null &&
            watch.status == WatchStatus.Paused &&
            watch.health == Health.Degraded
    Page {
        Text(watchTitle(watch), style = MaterialTheme.typography.headlineSmall)
        Column(Modifier.testTag("watch_status")) { WatchStatusLabel(watch) }
        when {
            watch.status == WatchStatus.Pending -> Text(stringResource(R.string.v1_preparing_body))
            failed -> {
                Text(
                    stringResource(
                        when (watch.plan.preparationFailure) {
                            "source_unusable" -> R.string.v1_source_failed
                            "quota" -> R.string.preparation_quota_failed
                            else -> R.string.v1_technical_failed
                        },
                    ),
                )
                Button(onClick = { edit(watch) }, modifier = Modifier.testTag("edit")) {
                    Text(stringResource(R.string.revise))
                }
            }
            watch.status == WatchStatus.SuspendedByCapacity ->
                Text(stringResource(R.string.v1_limit_body))
            watch.status == WatchStatus.Paused ->
                SecondaryText(stringResource(R.string.v1_paused_body))
            watch.status == WatchStatus.Completed ->
                SecondaryText(stringResource(R.string.v1_completed_body))
        }
        if (watch.status != WatchStatus.Pending) {
            watch.lastSuccess?.let {
                SecondaryText(stringResource(R.string.last_success, dateText(it)))
            }
            if (watch.health == Health.Degraded && !failed)
                SecondaryText(
                    if (
                        watch.plan.checkedSources != null &&
                            watch.plan.totalSources != null &&
                            watch.plan.checkedSources < watch.plan.totalSources
                    )
                        stringResource(
                            R.string.v1_partial_check,
                            watch.plan.checkedSources,
                            watch.plan.totalSources,
                        )
                    else stringResource(R.string.v1_last_failed),
                )
            if (watch.status == WatchStatus.Active && watch.nextCheck != null)
                SecondaryText(stringResource(R.string.next_check, dateText(watch.nextCheck)))
        }
        if (watch.remoteState == RemoteState.Stopping)
            SecondaryText(stringResource(R.string.v1_stopping_body))
        if (watch.remoteState == RemoteState.Unknown)
            SecondaryText(stringResource(R.string.remote_unknown))
        if (!notificationsEnabled && watch.status in setOf(WatchStatus.Pending, WatchStatus.Active))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(Modifier.padding(16.dp)) {
                    SectionTitle(stringResource(R.string.v1_alerts_phone))
                    TextButton(enableNotifications) {
                        Text(stringResource(R.string.v1_enable_notifications))
                    }
                }
            }
        FailureText(state.commandError)
        if (state.commandPending) Busy()
        if (watch.status in setOf(WatchStatus.Active, WatchStatus.Pending))
            OutlinedButton(
                onClick = { vm.command(watch, Operation.Pause) },
                enabled = !state.commandPending,
                modifier = Modifier.testTag("pause"),
            ) {
                AppIcon(
                    if (watch.status == WatchStatus.Pending) R.drawable.symbol_close
                    else R.drawable.symbol_pause,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(
                        if (watch.status == WatchStatus.Pending) R.string.cancel_preparation
                        else R.string.pause,
                    ),
                )
            }
        if (watch.status == WatchStatus.Paused && !failed)
            OutlinedButton(
                onClick = { vm.command(watch, Operation.Resume) },
                enabled = !state.commandPending,
                modifier = Modifier.testTag("resume"),
            ) {
                Text(stringResource(R.string.resume))
            }
        if (watch.status == WatchStatus.Paused && !failed)
            SecondaryText(stringResource(R.string.resume_info))
        if (state.refreshFailed) SecondaryText(stringResource(R.string.stale_content))
        TextButton(
            vm::refresh,
            enabled = !state.refreshing,
            modifier = Modifier.testTag("refresh_watches"),
        ) {
            Text(stringResource(R.string.refresh_short))
        }
        if (watch.status == WatchStatus.Pending) return@Page
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SectionTitle(stringResource(R.string.v1_results))
        val findings =
            state.findings.filter { it.watchId == id }.sortedByDescending { it.observedAt }
        if (findings.isEmpty()) SecondaryText(stringResource(R.string.no_results))
        findings.forEach { finding ->
            ListItem(
                headlineContent = {
                    Text(
                        finding.title.ifBlank { stringResource(R.string.finding_title) },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                supportingContent = {
                    Column {
                        if (finding.noveltyBasis == "initial")
                            Text(stringResource(R.string.v1_initial))
                        SecondaryText(
                            stringResource(R.string.v1_verified, dateText(finding.observedAt)),
                        )
                    }
                },
                trailingContent = { AppIcon(R.drawable.symbol_chevron_right) },
                modifier = Modifier.clickable { openFinding(finding.id) }.testTag("finding"),
            )
        }
        Spacer(Modifier.height(8.dp))
        SectionTitle(stringResource(R.string.v1_criteria))
        if (watch.plan.criteria.isEmpty()) Text(watch.plan.draft.text)
        else watch.plan.criteria.forEach { SecondaryText(it) }
        watch.plan.draft.region
            .takeIf { it.isNotBlank() }
            ?.let { MetaRow(stringResource(R.string.v1_region), it) }
        SecondaryText(
            stringResource(
                if (watch.plan.draft.mode == FollowMode.Once) R.string.v1_mode_once
                else R.string.v1_mode_continuous,
            ),
        )
        Spacer(Modifier.height(8.dp))
        SectionTitle(stringResource(R.string.v1_sources_followed))
        if (watch.plan.sources.isEmpty()) SecondaryText(stringResource(R.string.v1_no_sources))
        watch.plan.sources.forEach { url ->
            SourceLink(url, watch.plan.sourceLabels[url] ?: sourceDomain(url))
        }
        if (!remote) {
            TextButton(onClick = { edit(watch) }, modifier = Modifier.testTag("edit")) {
                Text(stringResource(R.string.edit))
            }
            TextButton(onClick = { deleting = true }, modifier = Modifier.testTag("delete")) {
                Text(stringResource(R.string.delete))
            }
        }
        if (watch.plan.sources.isNotEmpty()) SecondaryText(stringResource(R.string.v1_trial_limits))
    }
}

@Composable
fun FindingScreen(id: String, state: WatchesUiState, remote: Boolean = false) {
    val finding = state.findings.find { it.id == id }
    val uri = LocalUriHandler.current
    val footer: (@Composable ColumnScope.() -> Unit)? =
        if (finding != null) {
            {
                Button(
                    onClick = { uri.openUri(finding.source) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("open_source"),
                ) {
                    Text(stringResource(R.string.v1_open_site))
                    Spacer(Modifier.width(8.dp))
                    AppIcon(R.drawable.symbol_open_in_new)
                }
            }
        } else null
    Page(footer = footer) {
        if (finding == null) {
            if (state.findingError != null) FailureText(state.findingError)
            else Text(stringResource(R.string.missing))
            return@Page
        }
        Text(
            finding.title.ifBlank { stringResource(R.string.finding_title) },
            style = MaterialTheme.typography.headlineMedium,
        )
        if (!remote) SecondaryText(stringResource(R.string.finding_info))
        if (finding.noveltyBasis == "initial") SecondaryText(stringResource(R.string.v1_initial))
        Text(finding.fact.ifBlank { finding.criterion }, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        SectionTitle(stringResource(R.string.v1_why))
        SecondaryText(finding.reason.ifBlank { finding.criterion })
        if (finding.correctsFindingId != null)
            SecondaryText(stringResource(R.string.correction_notice))
        val watch = state.watches.find { it.id == finding.watchId }
        if (
            watch != null &&
                (watch.plan.id != finding.planId || watch.plan.version != finding.planVersion)
        )
            SecondaryText(stringResource(R.string.old_plan))
        MetaRow(stringResource(R.string.v1_source), sourceDomain(finding.source))
        MetaRow(stringResource(R.string.v1_verified_label), dateText(finding.observedAt))
        finding.publishedAt?.let {
            Text(
                stringResource(R.string.published_at, dateText(it)),
                Modifier.testTag("publication"),
            )
        }
        finding.eventDate?.let { MetaRow(stringResource(R.string.v1_event_date), it) }
        SecondaryText(stringResource(R.string.v1_result_conditions))
    }
}
