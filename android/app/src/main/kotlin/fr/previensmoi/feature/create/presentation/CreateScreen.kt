package fr.previensmoi.feature.create.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.previensmoi.R
import fr.previensmoi.domain.model.*
import fr.previensmoi.feature.common.presentation.*
import java.util.Locale

@Composable
fun RequestFields(state: CreateUiState, vm: CreateViewModel) {
    var siteExpanded by rememberSaveable { mutableStateOf(state.draft.preferredSite.isNotBlank()) }
    Text(stringResource(R.string.question), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    OutlinedTextField(
        state.draft.text,
        vm::updateText,
        label = { Text(stringResource(R.string.request_label)) },
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest, focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        placeholder = { Text(stringResource(R.string.v1_request_example)) },
        minLines = 3,
        maxLines = 6,
        enabled = state.loaded && state.step != CreationStep.Confirming,
        modifier = Modifier.fillMaxWidth().testTag("request"),
    )
    if (siteExpanded || state.draft.preferredSite.isNotBlank())
        OutlinedTextField(
            state.draft.preferredSite,
            vm::updateSite,
            label = { Text(stringResource(R.string.v1_site_label)) },
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest, focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            supportingText = {
                Text(
                    stringResource(
                        if (state.invalidSite) R.string.v1_site_error else R.string.v1_site_hint,
                    ),
                )
            },
            isError = state.invalidSite,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth().testTag("preferred_site"),
        )
    else
        TextButton(onClick = { siteExpanded = true }) { Text(stringResource(R.string.v1_add_site)) }
    OutlinedTextField(
        state.draft.region,
        vm::updateRegion,
        label = { Text(stringResource(R.string.v1_region)) },
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest, focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("region"),
    )
}

@Composable
private fun ModeChoice(
    mode: FollowMode,
    selected: FollowMode,
    onSelect: (FollowMode) -> Unit,
    enabled: Boolean,
) {
    Surface(
        color =
            if (selected == mode) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = 52.dp)
                .selectable(
                    selected == mode,
                    onClick = { onSelect(mode) },
                    enabled = enabled,
                    role = Role.RadioButton,
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag(if (mode == FollowMode.Continuous) "continuous" else "once"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RadioButton(selected == mode, onClick = null, enabled = enabled)
            Text(
                stringResource(
                    if (mode == FollowMode.Continuous) R.string.v1_mode_continuous
                    else R.string.v1_mode_once,
                ),
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (selected == mode) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ConfirmationSetting(label: String, value: String, onClick: (() -> Unit)? = null) {
    val clickable = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
    Row(
        Modifier.fillMaxWidth().then(clickable).heightIn(min = 56.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        if (onClick != null)
            AppIcon(
                R.drawable.symbol_chevron_right,
                stringResource(R.string.v1_change),
                tint = MaterialTheme.colorScheme.primary,
            )
    }
}

@Composable
fun CreateScreen(
    state: CreateUiState,
    vm: CreateViewModel,
    login: (() -> Unit)? = null,
    remote: Boolean = false,
) {
    var questionIndex by rememberSaveable(state.questions.map { it.id }.joinToString()) { mutableIntStateOf(0) }
    val contentKey = when (state.step) {
        CreationStep.Editing, CreationStep.Evaluating -> "request"
        CreationStep.Ready, CreationStep.Confirming -> "confirmation"
        CreationStep.Clarifying -> "clarifying:$questionIndex"
        else -> state.step.name
    }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = {
        focus.clearFocus()
        keyboard?.hide()
        vm.evaluate(Locale.getDefault().language)
    }
    val footer: (@Composable ColumnScope.() -> Unit)? =
        if (state.step in setOf(CreationStep.Ready, CreationStep.Confirming)) {
            {
                Button(
                    onClick = vm::confirm,
                    enabled =
                        state.step != CreationStep.Confirming &&
                            !state.optionsUpdating &&
                            (state.proposal?.partial != true || state.partialAccepted),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("confirm"),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        stringResource(
                            if (state.step == CreationStep.Confirming || state.optionsUpdating)
                                R.string.v1_wait
                            else R.string.v1_confirm,
                        ),
                    )
                }
                TextButton(
                    onClick = vm::revise,
                    enabled = state.step != CreationStep.Confirming && !state.optionsUpdating,
                ) {
                    Text(stringResource(R.string.revise))
                }
            }
        } else null
    Page(footer = footer, contentKey = contentKey) {
        if (state.storageFailed) FailureText(Failure.StorageFailure)
        when (state.step) {
            CreationStep.Editing,
            CreationStep.Evaluating -> {
                RequestFields(state, vm)
                FailureText(state.error)
                Button(
                    onClick = submit,
                    enabled =
                        state.loaded &&
                            state.draft.text.isNotBlank() &&
                            state.step != CreationStep.Evaluating,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("evaluate"),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    if (state.step == CreationStep.Evaluating) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(
                        stringResource(
                            if (state.step == CreationStep.Evaluating) R.string.v1_wait
                            else R.string.analyse,
                        ),
                    )
                }
                if (state.draft.text.isNotBlank())
                    TextButton(
                        onClick = vm::discard,
                        enabled = state.step != CreationStep.Evaluating,
                    ) {
                        Text(stringResource(R.string.discard))
                    }
            }
            CreationStep.Clarifying -> {
                val question = state.questions.getOrNull(questionIndex)
                Text(
                    question?.label ?: stringResource(R.string.clarification_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                SecondaryText(state.draft.text)
                if (question == null)
                    OutlinedTextField(
                        state.draft.answer,
                        vm::updateAnswer,
                        label = { Text(stringResource(R.string.answer_label)) },
                        modifier = Modifier.fillMaxWidth().testTag("answer"),
                    )
                else if (question.options.isEmpty())
                    OutlinedTextField(
                        state.draft.answers[question.id].orEmpty(),
                        { vm.updateAnswerFor(question.id, it) },
                        label = { Text(question.label) },
                        modifier = Modifier.fillMaxWidth().testTag("answer_${question.id}"),
                    )
                else
                    question.options.forEach { option ->
                        Row(
                            Modifier.fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(
                                    state.draft.answers[question.id] == option,
                                    onClick = { vm.updateAnswerFor(question.id, option) },
                                    role = Role.RadioButton,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(state.draft.answers[question.id] == option, onClick = null)
                            Text(option, Modifier.weight(1f))
                        }
                    }
                FailureText(state.error)
                Button(
                    onClick = {
                        if (questionIndex < state.questions.lastIndex) questionIndex++
                        else vm.continueClarification(Locale.getDefault().language)
                    },
                    enabled = question == null || !question.required || !state.draft.answers[question.id].isNullOrBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("evaluate"),
                ) {
                    Text(stringResource(R.string.v1_continue))
                }
                TextButton(onClick = vm::revise) { Text(stringResource(R.string.revise)) }
            }
            CreationStep.Unsupported -> {
                Text(
                    stringResource(
                        if (remote) R.string.remote_unavailable_title
                        else R.string.unsupported_title,
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(state.rejection ?: stringResource(R.string.unsupported_body))
                Button(onClick = vm::revise) { Text(stringResource(R.string.revise)) }
            }
            CreationStep.Login -> {
                Text(
                    stringResource(R.string.v1_login_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                SecondaryText(stringResource(R.string.v1_login_body))
                FailureText(state.error)
                Button(
                    onClick = login ?: vm::signIn,
                    modifier = Modifier.fillMaxWidth().testTag("login"),
                ) {
                    Text(stringResource(if (remote) R.string.sign_in else R.string.login_action))
                }
                TextButton(onClick = vm::revise) { Text(stringResource(R.string.revise)) }
            }
            CreationStep.Ready,
            CreationStep.Confirming -> {
                val p = state.proposal ?: return@Page
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            p.title.ifBlank { p.draft.text },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium,
                        )
                        criteriaForDisplay(p)
                            .forEach {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                    }
                }
                SectionCard(stringResource(R.string.v1_after_result)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ModeChoice(
                            FollowMode.Continuous,
                            state.draft.mode,
                            vm::chooseMode,
                            state.step != CreationStep.Confirming && !state.optionsUpdating,
                        )
                        ModeChoice(
                            FollowMode.Once,
                            state.draft.mode,
                            vm::chooseMode,
                            state.step != CreationStep.Confirming && !state.optionsUpdating,
                        )
                    }
                }
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        ConfirmationSetting(
                            stringResource(R.string.v1_region),
                            state.draft.region,
                            if (state.step == CreationStep.Ready && !state.optionsUpdating)
                                vm::revise
                            else null,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ConfirmationSetting(
                            stringResource(R.string.v1_site_preferred),
                            state.draft.preferredSite
                                .takeIf { it.isNotBlank() }
                                ?.let(::sourceDomain) ?: stringResource(R.string.v1_no_site),
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ConfirmationSetting(
                            stringResource(R.string.v1_frequency),
                            if (p.intervalHours == 1) stringResource(R.string.v1_hourly)
                            else stringResource(R.string.v1_interval, p.intervalHours),
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ConfirmationSetting(
                            stringResource(R.string.v1_sources),
                            stringResource(
                                if (p.backgroundPreparation) R.string.v1_sources_later
                                else R.string.v1_sources_ready,
                            ),
                        )
                    }
                }
                if (!p.backgroundPreparation && p.partial) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            state.partialAccepted,
                            vm::acceptPartial,
                            modifier = Modifier.testTag("accept_partial"),
                        )
                        Text(
                            p.coverage.ifBlank { stringResource(R.string.partial_text) },
                            Modifier.weight(1f),
                        )
                    }
                }
                SecondaryText(
                    stringResource(
                        if (p.method == "web_search") R.string.v1_initial_results
                        else R.string.baseline_info,
                    ),
                )
                Text(
                    stringResource(R.string.v2_trial_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FailureText(state.error)
            }
        }
    }
}
