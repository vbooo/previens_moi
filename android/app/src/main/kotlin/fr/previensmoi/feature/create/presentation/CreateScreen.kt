package fr.previensmoi.feature.create.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import fr.previensmoi.R
import fr.previensmoi.domain.model.FollowMode
import fr.previensmoi.feature.common.presentation.*
import java.util.Locale

@Composable
fun RequestFields(state: CreateUiState, vm: CreateViewModel) {
    Text(stringResource(R.string.question), style = MaterialTheme.typography.headlineMedium)
    OutlinedTextField(value = state.draft.text, onValueChange = vm::updateText,
        label = { Text(stringResource(R.string.request_label)) },
        supportingText = { Text(stringResource(R.string.request_hint)) }, minLines = 2, maxLines = 5,
        enabled = state.loaded && state.step != CreationStep.Confirming,
        modifier = Modifier.fillMaxWidth().testTag("request"))
    Row(Modifier.fillMaxWidth().toggleable(value = state.draft.mode == FollowMode.Continuous, role = Role.Switch,
        onValueChange = { vm.updateMode(if (it) FollowMode.Continuous else FollowMode.Once) }).testTag("continuous"),
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Switch(checked = state.draft.mode == FollowMode.Continuous, onCheckedChange = null)
        Text(stringResource(R.string.continuous), modifier = Modifier.weight(1f).padding(top = 12.dp))
    }
    Text(stringResource(R.string.once_hint), style = MaterialTheme.typography.bodySmall)
}

@Composable
fun CreateScreen(state: CreateUiState, vm: CreateViewModel, login: (() -> Unit)? = null, remote: Boolean = false) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Page {
        if (state.resetNotice) Text(stringResource(R.string.reset_notice))
        if (state.storageFailed) FailureText(fr.previensmoi.domain.model.Failure.StorageFailure)
        when (state.step) {
            CreationStep.Editing, CreationStep.Evaluating, CreationStep.Clarifying -> {
                RequestFields(state, vm)
                if (state.step == CreationStep.Clarifying) {
                    Text(stringResource(R.string.clarification_title), style = MaterialTheme.typography.titleLarge)
                    if (state.questions.isEmpty()) OutlinedTextField(state.draft.answer, vm::updateAnswer,
                        label = { Text(stringResource(R.string.answer_label)) }, supportingText = { Text(stringResource(R.string.answer_hint)) },
                        modifier = Modifier.fillMaxWidth().testTag("answer"))
                    state.questions.forEach { question ->
                        Text(question.label, style = MaterialTheme.typography.titleMedium)
                        if (question.options.isNotEmpty()) {
                            question.options.forEach { option ->
                                Row(Modifier.fillMaxWidth().toggleable(state.draft.answers[question.id] == option, role = Role.RadioButton,
                                    onValueChange = { vm.updateAnswerFor(question.id, option) }).padding(vertical = 4.dp)) {
                                    RadioButton(state.draft.answers[question.id] == option, onClick = null)
                                    Text(option, Modifier.weight(1f).padding(top = 12.dp))
                                }
                            }
                        } else OutlinedTextField(state.draft.answers[question.id].orEmpty(), { vm.updateAnswerFor(question.id, it) },
                            label = { Text(question.label) }, modifier = Modifier.fillMaxWidth().testTag("answer_${question.id}"))
                    }
                }
                FailureText(state.error)
                if (state.step == CreationStep.Evaluating) { Busy(); Text(stringResource(R.string.understanding_loading)) }
                else Button(onClick = { focus.clearFocus(); keyboard?.hide(); vm.evaluate(Locale.getDefault().language) }, enabled = state.loaded,
                    modifier = Modifier.testTag("evaluate")) { Text(stringResource(R.string.analyse)) }
                TextButton(onClick = vm::discard) { Text(stringResource(R.string.discard)) }
            }
            CreationStep.Unsupported -> {
                Text(stringResource(if (remote) R.string.remote_unavailable_title else R.string.unsupported_title), style = MaterialTheme.typography.headlineMedium)
                Text(state.rejection ?: stringResource(if (remote) R.string.unqualified_body else R.string.unsupported_body))
                Button(onClick = vm::revise) { Text(stringResource(R.string.revise)) }
            }
            CreationStep.Login -> {
                Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(if (remote) R.string.email_purpose else R.string.login_info))
                FailureText(state.error)
                Button(onClick = login ?: vm::signIn, modifier = Modifier.testTag("login")) { Text(stringResource(R.string.login_action)) }
                TextButton(onClick = vm::revise) { Text(stringResource(R.string.revise)) }
            }
            CreationStep.Ready, CreationStep.Confirming -> {
                Text(stringResource(if (remote && state.proposal?.backgroundPreparation == true) R.string.understanding_title else R.string.proposal_title), style = MaterialTheme.typography.headlineMedium)
                val proposal = state.proposal
                if (proposal != null && remote && proposal.backgroundPreparation) {
                    proposal.criteria.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
                    Text(stringResource(R.string.prepare_explanation))
                    Text(stringResource(R.string.prepare_limits), style = MaterialTheme.typography.bodySmall)
                    FailureText(state.error)
                    if (state.step == CreationStep.Confirming) Busy()
                    else {
                        Button(onClick = vm::confirm, modifier = Modifier.fillMaxWidth().testTag("confirm")) { Text(stringResource(R.string.confirm_prepare)) }
                        TextButton(onClick = vm::revise) { Text(stringResource(R.string.revise)) }
                    }
                } else if (proposal != null) {
                    Text(stringResource(R.string.criteria_title), style = MaterialTheme.typography.titleMedium)
                    Text(proposal.draft.text)
                    if (proposal.draft.answer.isNotBlank()) Text(proposal.draft.answer)
                    Text(stringResource(if (proposal.draft.mode == FollowMode.Once) R.string.once_hint else R.string.continuous))
                    if (proposal.partial) {
                        Text(stringResource(R.string.partial_text))
                        Row(Modifier.fillMaxWidth().toggleable(state.partialAccepted, role = Role.Checkbox,
                            enabled = state.step != CreationStep.Confirming, onValueChange = vm::acceptPartial).testTag("accept_partial"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Checkbox(state.partialAccepted, onCheckedChange = null)
                            Text(stringResource(R.string.partial_accept), Modifier.weight(1f).padding(top = 12.dp))
                        }
                    }
                    if (remote) {
                        proposal.criteria.forEach { Text(it) }
                        Text(proposal.coverage)
                        proposal.exclusions.forEach { Text(it) }
                        proposal.limitations.forEach { Text(it) }
                        Text(stringResource(R.string.remote_terms, proposal.intervalHours, proposal.limit))
                    } else Text(stringResource(R.string.demo_terms, proposal.intervalHours, proposal.limit))
                    if (proposal.sources.isEmpty()) Text(stringResource(R.string.no_source))
                    else { Text(stringResource(R.string.source_label)); proposal.sources.forEach { Text(it, style = MaterialTheme.typography.bodySmall) } }
                    Text(stringResource(R.string.baseline_info))
                    FailureText(state.error)
                    if (state.step == CreationStep.Confirming) Busy()
                    else {
                        Button(onClick = vm::confirm, enabled = !proposal.partial || state.partialAccepted,
                            modifier = Modifier.testTag("confirm")) { Text(stringResource(if (state.editingId == null) R.string.activate else R.string.confirm_edit)) }
                        TextButton(onClick = vm::revise) { Text(stringResource(R.string.revise)) }
                    }
                }
            }
        }
    }
}
