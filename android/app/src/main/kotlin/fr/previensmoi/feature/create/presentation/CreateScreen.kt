package fr.previensmoi.feature.create.presentation

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.previensmoi.R
import fr.previensmoi.domain.model.*
import fr.previensmoi.feature.common.presentation.*
import java.util.Locale

@Composable
fun RequestFields(state: CreateUiState, vm: CreateViewModel) {
    var siteExpanded by rememberSaveable { mutableStateOf(state.draft.preferredSite.isNotBlank()) }
    Text(stringResource(R.string.question), style = MaterialTheme.typography.headlineMedium)
    OutlinedTextField(
        state.draft.text,
        vm::updateText,
        label = { Text(stringResource(R.string.request_label)) },
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
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(
                selected == mode,
                onClick = { onSelect(mode) },
                enabled = enabled,
                role = Role.RadioButton,
            )
            .testTag(if (mode == FollowMode.Continuous) "continuous" else "once"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected == mode, onClick = null, enabled = enabled)
        Text(
            stringResource(
                if (mode == FollowMode.Continuous) R.string.v1_mode_continuous
                else R.string.v1_mode_once,
            ),
            Modifier.weight(1f),
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
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("confirm"),
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
    Page(footer = footer) {
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
                var questionIndex by
                    rememberSaveable(state.questions.map { it.id }.joinToString()) {
                        mutableIntStateOf(0)
                    }
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
                    enabled = question == null || !state.draft.answers[question.id].isNullOrBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("evaluate"),
                ) {
                    Text(stringResource(R.string.v1_continue))
                }
                TextButton(onClick = vm::revise) { Text(stringResource(R.string.revise)) }
            }
            CreationStep.Unsupported -> {
                Text(
                    stringResource(if(remote) R.string.remote_unavailable_title else R.string.unsupported_title),
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
                Text(
                    p.title.ifBlank { p.draft.text },
                    style = MaterialTheme.typography.headlineSmall,
                )
                p.criteria.forEach { SecondaryText(it) }
                Spacer(Modifier.height(4.dp))
                MetaRow(
                    stringResource(R.string.v1_region),
                    state.draft.region + " · " + stringResource(R.string.v1_change),
                    if (state.step == CreationStep.Ready && !state.optionsUpdating) vm::revise
                    else null,
                )
                MetaRow(
                    stringResource(R.string.v1_site_preferred),
                    state.draft.preferredSite.takeIf { it.isNotBlank() }?.let(::sourceDomain)
                        ?: stringResource(R.string.v1_no_site),
                )
                MetaRow(
                    stringResource(R.string.v1_frequency),
                    if (p.intervalHours == 1) stringResource(R.string.v1_hourly)
                    else stringResource(R.string.v1_interval, p.intervalHours),
                )
                MetaRow(
                    stringResource(R.string.v1_sources),
                    stringResource(
                        if (p.backgroundPreparation) R.string.v1_sources_later
                        else R.string.v1_sources_ready,
                    ),
                )
                SectionTitle(stringResource(R.string.v1_after_result))
                ModeChoice(
                    FollowMode.Continuous,
                    state.draft.mode,
                    vm::chooseMode,
                    state.step != CreationStep.Confirming,
                )
                ModeChoice(
                    FollowMode.Once,
                    state.draft.mode,
                    vm::chooseMode,
                    state.step != CreationStep.Confirming,
                )
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
                SecondaryText(stringResource(R.string.v1_trial_limits))
                FailureText(state.error)
            }
        }
    }
}
