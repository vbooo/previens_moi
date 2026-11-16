package fr.previensmoi.feature.session.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.previensmoi.R
import fr.previensmoi.feature.common.presentation.*

@Composable
fun SessionScreen(
    state: SessionUiState,
    vm: SessionViewModel,
    forConfirmation: Boolean = false,
    notificationsEnabled: Boolean = true,
    notificationSettings: () -> Unit = {},
    subscriptionAction: (() -> Unit)? = null,
    hasSubscription: Boolean = false,
    manageSubscription: () -> Unit = {},
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val canSend = !state.busy && state.resendSeconds == 0 && state.email.contains('@')
    val canVerify = !state.busy && state.code.length == 6
    val send = {
        if (canSend) {
            focus.clearFocus()
            keyboard?.hide()
            vm.send()
        }
    }
    val verify = {
        if (canVerify) {
            focus.clearFocus()
            keyboard?.hide()
            vm.verify()
        }
    }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var privacy by rememberSaveable { mutableStateOf(false) }
    if (deleting)
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text(stringResource(R.string.erase_account)) },
            text = { Column { Text(stringResource(R.string.v1_erase_body)); if(hasSubscription) { Text(stringResource(R.string.billing_delete_warning));TextButton(manageSubscription){Text(stringResource(R.string.billing_manage))} } } },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleting = false
                        vm.requestDeletion()
                    },
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                ) {
                    Text(stringResource(R.string.v1_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    if (privacy)
        AlertDialog(
            onDismissRequest = { privacy = false },
            title = { Text(stringResource(R.string.v1_privacy)) },
            text = {
                Text(
                    stringResource(R.string.v1_privacy_body),
                    Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(onClick = { privacy = false }) {
                    Text(stringResource(R.string.v1_close))
                }
            },
        )
    Page {
        if (state.deletionAccepted) Text(stringResource(R.string.erasure_pending))
        if (state.session != null) {
            Text(
                state.session.email.ifBlank { stringResource(R.string.connected_account) },
                style = MaterialTheme.typography.titleLarge,
            )
            subscriptionAction?.let { action ->
                OutlinedButton(action,modifier=Modifier.fillMaxWidth().testTag("open_subscription")){Text(stringResource(R.string.billing_title))}
            }
            Spacer(Modifier.height(8.dp))
            SectionTitle(stringResource(R.string.v1_notifications))
            SecondaryText(
                stringResource(
                    if (notificationsEnabled) R.string.v1_notifications_on
                    else R.string.v1_notifications_off,
                ),
            )
            TextButton(notificationSettings) {
                Text(stringResource(R.string.v1_notification_settings))
            }
            HorizontalDivider()
            TextButton(onClick = { privacy = true }) { Text(stringResource(R.string.v1_privacy)) }
            TextButton(
                vm::signOut,
                enabled = !state.busy,
                modifier = Modifier.testTag("sign_out"),
            ) {
                Text(stringResource(R.string.sign_out))
            }
            TextButton(
                onClick = { deleting = true },
                enabled = !state.busy,
                colors =
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.erase_account))
            }
        } else if (!state.sent) {
            Text(
                stringResource(
                    if (forConfirmation) R.string.v1_login_title else R.string.v1_sign_in_title,
                ),
                style = MaterialTheme.typography.headlineSmall,
            )
            SecondaryText(stringResource(R.string.v1_login_body))
            OutlinedTextField(
                state.email,
                vm::email,
                enabled = !state.busy,
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest, focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                label = { Text(stringResource(R.string.email_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { send() }),
                modifier = Modifier.fillMaxWidth().testTag("email"),
            )
            FailureText(state.error)
            Button(
                send,
                enabled = canSend,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("send_code"),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    if (state.resendSeconds > 0)
                        stringResource(R.string.v1_resend_wait, state.resendSeconds)
                    else stringResource(if (state.busy) R.string.v1_wait else R.string.send_code),
                )
            }
        } else {
            Text(
                stringResource(R.string.v1_check_email),
                style = MaterialTheme.typography.headlineSmall,
            )
            SecondaryText(stringResource(R.string.v1_code_sent, state.email))
            OutlinedTextField(
                state.code,
                vm::code,
                enabled = !state.busy,
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest, focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                label = { Text(stringResource(R.string.v1_code_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { verify() }),
                modifier = Modifier.fillMaxWidth().testTag("otp"),
            )
            if (state.error != null)
                Text(
                    stringResource(R.string.v1_code_error),
                    color = MaterialTheme.colorScheme.error,
                )
            Button(
                verify,
                enabled = canVerify,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("verify_code"),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(stringResource(if (state.busy) R.string.v1_wait else R.string.v1_validate))
            }
            TextButton(vm::send, enabled = !state.busy && state.resendSeconds == 0) {
                Text(
                    if (state.resendSeconds > 0)
                        stringResource(R.string.v1_resend_wait, state.resendSeconds)
                    else stringResource(R.string.resend_code),
                )
            }
            TextButton(vm::changeEmail, enabled = !state.busy) {
                Text(stringResource(R.string.v1_change_email))
            }
        }
    }
}
