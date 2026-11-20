package fr.previensmoi.feature.watches.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
    sessionReady: Boolean = true,
    newRequest: (() -> Unit)? = null,
) {
    PullToRefreshBox(
        state.refreshing && signedIn,
        { if (signedIn) refresh() },
        Modifier.fillMaxSize().testTag("watch_list"),
    ) {
        LazyColumn(
            Modifier.widthIn(max = 720.dp).fillMaxSize().align(Alignment.TopCenter),
            contentPadding =
                PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
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
            val firstLoad = remote && signedIn && !state.hasLoaded && state.watches.isEmpty() && !state.refreshFailed
            if (remote && (!sessionReady || firstLoad))
                item { WatchListSkeleton() }
            else if (remote && !signedIn)
                item {
                    EmptyWatches(true, signIn, R.string.sign_in, "sign_in_watches")
                    SecondaryText(
                        stringResource(R.string.sign_in_to_view_watches),
                        Modifier.padding(top = 16.dp).testTag("signed_out_watches"),
                    )
                }
            else {
                if (state.refreshFailed)
                    item {
                        Column {
                            SecondaryText(stringResource(if (state.hasLoaded || state.watches.isNotEmpty()) R.string.stale_content else R.string.v4_list_failed))
                            TextButton(refresh) { Text(stringResource(R.string.v1_retry)) }
                        }
                    }
                if (state.watches.isEmpty() && !state.refreshing && !state.refreshFailed && (!remote || state.hasLoaded))
                    item {
                        EmptyWatches(creation.loaded, newRequest)
                    }
                if (state.watches.isNotEmpty()) item {
                    Text(pluralStringResource(R.plurals.v5_watch_count, state.watches.size, state.watches.size),
                        Modifier.padding(bottom = 14.dp), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                itemsIndexed(state.watches, key = { _, watch -> watch.id }) { index, watch ->
                    val shape = RoundedCornerShape(
                        topStart = if (index == 0) 20.dp else 0.dp, topEnd = if (index == 0) 20.dp else 0.dp,
                        bottomStart = if (index == state.watches.lastIndex) 20.dp else 0.dp,
                        bottomEnd = if (index == state.watches.lastIndex) 20.dp else 0.dp,
                    )
                    Surface(onClick = { open(watch.id) }, shape = shape,
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier.testTag("watch_${watch.id}")) {
                        Column {
                            BoxWithConstraints {
                                val inlineMetadata = maxWidth >= 352.dp && LocalDensity.current.fontScale <= 1.15f
                            ListItem(
                                headlineContent = { Text(watchTitle(watch), style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                                supportingContent = {
                                    val badge: @Composable () -> Unit = {
                                        WatchStatusLabel(watch, contained = true, compact = true,
                                            operation = state.commandOperation.takeIf { state.commandUnconfirmed && state.commandWatchId == watch.id })
                                    }
                                    val meta: @Composable () -> Unit = {
                                        watch.lastSuccess?.let { SecondaryText(stringResource(R.string.v5_verified_at, compactDateText(it))) }
                                    }
                                    if (inlineMetadata && watch.status == WatchStatus.Active && watch.health == Health.Healthy && !state.commandUnconfirmed)
                                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) { badge(); meta() }
                                    else Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { badge(); meta() }
                                },
                                trailingContent = { AppIcon(R.drawable.symbol_chevron_right) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                            )
                            }
                            if (index != state.watches.lastIndex)
                                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyWatches(
    ready: Boolean, newRequest: (() -> Unit)?,
    actionLabel: Int = R.string.new_watch, actionTag: String = "new_request",
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().testTag("home_empty"),
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.v1_empty_title),
                style = MaterialTheme.typography.headlineSmall.copy(lineBreak = LineBreak.Heading),
                fontWeight = FontWeight.SemiBold,
            )
            Text(stringResource(R.string.v1_empty_body), style = MaterialTheme.typography.bodyMedium)
            if (newRequest != null) Button(
                onClick = newRequest,
                enabled = ready,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 52.dp).testTag(actionTag),
            ) {
                if (actionLabel == R.string.new_watch) {
                    AppIcon(R.drawable.symbol_add)
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(actionLabel))
            }
        }
    }
}

@Composable
private fun WatchListSkeleton() {
    val loadingDescription = stringResource(R.string.v5_loading)
    Column(Modifier.testTag("home_loading").semantics { contentDescription = loadingDescription }, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(65.dp, 14.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(4.dp)))
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = RoundedCornerShape(20.dp)) {
            Column {
                repeat(3) { index ->
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.fillMaxWidth(if (index == 1) .85f else .68f).height(18.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(4.dp)))
                        Box(Modifier.size(124.dp, 25.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(4.dp)))
                    }
                    if (index < 2) HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
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
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
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
private fun CheckTiming(watch: Watch, pausing: Boolean = false) {
    val next = when {
        watch.trialEnded || watch.subscriptionEnded -> stringResource(R.string.v2_not_scheduled)
        pausing -> stringResource(R.string.v3_pausing)
        watch.status == WatchStatus.Active && watch.nextCheck != null -> compactDateText(watch.nextCheck)
        watch.status == WatchStatus.Paused -> stringResource(R.string.v5_on_resume)
        else -> stringResource(R.string.v2_not_scheduled)
    }
    val last = watch.lastSuccess?.let(::compactDateText) ?: stringResource(R.string.v2_not_checked)
    val lastDate: @Composable () -> Unit = { TimingValue(stringResource(R.string.v2_last_success), last) }
    val nextDate: @Composable () -> Unit = { TimingValue(stringResource(R.string.v2_next_check), next) }
    if (LocalDensity.current.fontScale > 1.15f)
        Column(Modifier.testTag("check_timing"), verticalArrangement = Arrangement.spacedBy(12.dp)) { lastDate(); nextDate() }
    else Row(Modifier.fillMaxWidth().testTag("check_timing"), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.weight(1f)) { lastDate() }; Box(Modifier.weight(1f)) { nextDate() }
    }
}

@Composable
private fun TimingValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
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
    subscription: (() -> Unit)? = null,
) {
    val watch = state.watches.find { it.id == id }
    if (watch == null) {
        Page { if (state.refreshing) Busy() else Text(stringResource(R.string.missing)) }
        return
    }
    var deleting by rememberSaveable(id) { mutableStateOf(false) }
    var explainLimit by rememberSaveable(id) { mutableStateOf(false) }
    if (deleting)
        WatchDeleteDialog(
            watch,
            { deleting = false },
            {
                deleting = false
                vm.command(watch, Operation.Delete)
            },
        )
    if (explainLimit)
        AlertDialog(
            onDismissRequest = { explainLimit = false },
            title = { Text(stringResource(R.string.v2_capacity_title)) },
            text = { Text(stringResource(R.string.v2_capacity_body)) },
            confirmButton = {
                TextButton({ explainLimit = false }) { Text(stringResource(R.string.v1_close)) }
            },
            modifier = Modifier.testTag("capacity_dialog"),
        )
    val pendingOperation =
        state.commandOperation.takeIf { state.commandWatchId == id && state.commandUnconfirmed }
    val pausing = pendingOperation == Operation.Pause || watch.remoteState == RemoteState.Stopping
    val failed =
        watch.preparationFailed
    Page(spacing = 16.dp) {
        Text(watchTitle(watch), style = MaterialTheme.typography.headlineSmall.copy(lineBreak = LineBreak.Heading), fontWeight = FontWeight.SemiBold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.testTag("watch_status")) {
                    WatchStatusLabel(
                        watch,
                        contained = true,
                        operation = pendingOperation,
                        onClick =
                            if (watch.status == WatchStatus.SuspendedByCapacity && !watch.trialEnded && !watch.subscriptionEnded)
                                ({ explainLimit = true })
                            else null,
                    )
                }
                when {
                    watch.subscriptionEnded -> SecondaryText(stringResource(R.string.billing_required))
                    watch.trialEnded -> SecondaryText(stringResource(R.string.trial_ended_body))
                    pendingOperation != null || pausing -> Unit
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
                        SecondaryText(stringResource(if (watch.trialEnded) R.string.trial_ended_body else R.string.v2_limit_summary))
                    watch.status == WatchStatus.Completed ->
                        SecondaryText(stringResource(R.string.v1_completed_body))
                }
                if (watch.status != WatchStatus.Pending && !failed && pendingOperation == null && !pausing) CheckTiming(watch)
                if ((watch.trialEnded || watch.subscriptionEnded) && subscription != null) {
                    Button(subscription, modifier = Modifier.fillMaxWidth().testTag("watch_subscription")) { Text(stringResource(R.string.billing_title)) }
                }
                if (!watch.trialEnded && !watch.subscriptionEnded) watch.trialEndsAt?.let { SecondaryText(stringResource(R.string.trial_ends_at, dateText(it))) }
                if (
                    watch.health == Health.Degraded &&
                        !watch.trialEnded && !watch.subscriptionEnded && !failed &&
                        watch.status != WatchStatus.SuspendedByCapacity
                )
                    SecondaryText(
                        if (
                            watch.plan.checkedSources != null &&
                                watch.plan.totalSources != null &&
                                watch.plan.checkedSources < watch.plan.totalSources
                        )
                            pluralStringResource(
                                R.plurals.v1_partial_check,
                                watch.plan.checkedSources,
                                watch.plan.checkedSources,
                                watch.plan.totalSources,
                            )
                        else stringResource(R.string.v1_last_failed),
                    )
                if (watch.remoteState == RemoteState.Unknown)
                    SecondaryText(stringResource(R.string.remote_unknown))
                if (pendingOperation != null) {
                    // Accepted/pending is normal progress, including beyond the short receipt wait.
                    // Offer recovery only when reading the outcome actually failed.
                    if (!state.commandPending && state.commandError != Failure.OperationPending) {
                        Text(stringResource(R.string.v3_confirmation_unknown))
                        TextButton(vm::retryCommand, modifier = Modifier.testTag("retry_command")) {
                            Text(stringResource(R.string.refresh_short))
                        }
                    }
                } else if (!(failed && state.commandError == Failure.OperationFailed)) FailureText(state.commandError)
                if (state.commandPending && pendingOperation == null) Busy()
                if (watch.status in setOf(WatchStatus.Active, WatchStatus.Pending) &&
                    pendingOperation == null && !pausing && !state.commandPending && !state.commandUnconfirmed)
                    FilledTonalButton(
                        onClick = { vm.command(watch, Operation.Pause) },
                        enabled = !state.commandPending && !state.commandUnconfirmed && !pausing,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("pause"),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        AppIcon(
                            if (watch.status == WatchStatus.Pending) R.drawable.symbol_close
                            else R.drawable.symbol_pause,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(
                                if (watch.status == WatchStatus.Pending)
                                    R.string.cancel_preparation
                                else R.string.pause,
                            ),
                        )
                    }
                if (watch.status == WatchStatus.Paused && !watch.trialEnded && !watch.subscriptionEnded && !failed && !pausing &&
                    !state.commandPending && !state.commandUnconfirmed) {
                    Button(
                        onClick = { vm.command(watch, Operation.Resume) },
                        enabled = !state.commandPending && !state.commandUnconfirmed,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("resume"),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(stringResource(R.string.resume))
                    }
                }
            }
        }
        if (state.refreshFailed) SecondaryText(stringResource(R.string.stale_content))
        if (!remote)
            TextButton(
                vm::refresh,
                enabled = !state.refreshing,
                modifier = Modifier.testTag("refresh_watches"),
            ) {
                Text(stringResource(R.string.refresh_short))
            }
        if (!notificationsEnabled && watch.status in setOf(WatchStatus.Pending, WatchStatus.Active))
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.v5_notifications_disabled), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    TextButton(enableNotifications) { Text(stringResource(R.string.v5_enable)) }
                }
            }
        val findings =
            state.findings.filter { it.watchId == id }.sortedByDescending { it.observedAt }
        if (watch.status != WatchStatus.Pending && findings.isEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(stringResource(R.string.v1_results))
                SecondaryText(stringResource(R.string.no_results))
            }
        }
        if (findings.isNotEmpty()) CollectionCard(stringResource(R.string.v1_results)) {
            findings.forEachIndexed { index, finding ->
                key(finding.id) {
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                finding.title.ifBlank { stringResource(R.string.finding_title) },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        supportingContent = {
                            Column {
                                SecondaryText(
                                    stringResource(R.string.v1_verified, dateText(finding.observedAt)),
                                )
                            }
                        },
                        trailingContent = { AppIcon(R.drawable.symbol_chevron_right) },
                        modifier = Modifier.clickable { openFinding(finding.id) }.testTag("finding"),
                    )
                }
            }
        }
        SectionCard(stringResource(R.string.v1_criteria)) {
            if (criteriaForDisplay(watch.plan).isEmpty()) Text(watch.plan.draft.text)
            else
                criteriaForDisplay(watch.plan).forEach {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            watch.plan.draft.region
                .takeIf { it.isNotBlank() }
                ?.let { MetaRow(stringResource(R.string.v1_region), it) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            val behaviour = stringResource(if (watch.plan.draft.mode == FollowMode.Once) R.string.v5_notify_once else R.string.v5_notify_continuous)
            if (LocalDensity.current.fontScale > 1.15f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.v5_notifications), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(behaviour, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            else Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.v5_notifications), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(behaviour, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
        }
        CollectionCard(stringResource(R.string.v1_sources_followed)) {
            if (watch.plan.sources.isEmpty()) SecondaryText(
                stringResource(if (watch.status == WatchStatus.Pending) R.string.v5_sources_preparing else R.string.v1_no_sources),
                Modifier.padding(16.dp),
            )
            watch.plan.sources.forEachIndexed { index, url ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SourceLink(url, watch.plan.sourceLabels[url] ?: sourceDomain(url))
            }
        }
        if (!remote) {
            TextButton(onClick = { edit(watch) }, modifier = Modifier.testTag("edit")) {
                Text(stringResource(R.string.edit))
            }
            TextButton(onClick = { deleting = true }, modifier = Modifier.testTag("delete")) {
                Text(stringResource(R.string.delete))
            }
        }
    }
}

@Composable
fun FindingScreen(id: String, state: WatchesUiState, remote: Boolean = false, retry: (() -> Unit)? = null) {
    val finding = state.findings.find { it.id == id }
    val uri = LocalUriHandler.current
    val footer: (@Composable ColumnScope.() -> Unit)? =
        if (finding != null) {
            {
                Button(
                    onClick = { uri.openUri(finding.source) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("open_source"),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(stringResource(R.string.v1_open_site))
                    Spacer(Modifier.width(8.dp))
                    AppIcon(R.drawable.symbol_open_in_new)
                }
            }
        } else null
    Page(footer = footer) {
        if (finding == null) {
            if (remote && (state.findingRequestId != id || state.findingLoading)) {
                Box(Modifier.testTag("finding_loading")) { Busy() }
            } else if (state.findingError != null) {
                FailureText(state.findingError)
                if (retry != null && state.findingError in setOf(Failure.Offline, Failure.TemporaryFailure))
                    TextButton(retry, Modifier.testTag("retry_finding")) { Text(stringResource(R.string.refresh_short)) }
            } else Text(stringResource(R.string.missing))
            return@Page
        }
        Text(
            finding.title.ifBlank { stringResource(R.string.finding_title) },
            style = MaterialTheme.typography.headlineSmall.copy(lineBreak = LineBreak.Heading),
            fontWeight = FontWeight.SemiBold,
        )
        if (!remote) SecondaryText(stringResource(R.string.finding_info))
        Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Text(finding.fact.ifBlank { finding.criterion }, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
        }
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
    }
}
