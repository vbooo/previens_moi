package fr.previensmoi.feature.session.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import fr.previensmoi.R
import fr.previensmoi.feature.common.presentation.*

@Composable fun SessionScreen(state: SessionUiState, vm: SessionViewModel) {
    var deleting by rememberSaveable { mutableStateOf(false) }
    if (deleting) AlertDialog(onDismissRequest = { deleting = false },
        title = { Text(stringResource(R.string.erase_account)) },
        text = { Text(stringResource(R.string.erase_account_info)) },
        confirmButton = { TextButton(onClick = { deleting = false; vm.requestDeletion() }) { Text(stringResource(R.string.erase_account)) } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text(stringResource(R.string.cancel)) } })
    Page {
        Text(stringResource(R.string.account_title), style = MaterialTheme.typography.headlineMedium)
        if (state.deletionAccepted) Text(stringResource(R.string.erasure_pending))
        if (state.session != null) {
            Text(stringResource(R.string.account_connected))
            TextButton(onClick = { deleting = true }, enabled = !state.busy) { Text(stringResource(R.string.erase_account)) }
            Button(onClick = vm::signOut, enabled = !state.busy, modifier = Modifier.testTag("sign_out")) { Text(stringResource(R.string.sign_out)) }
        } else {
            Text(stringResource(R.string.email_purpose))
            OutlinedTextField(state.email, vm::email, enabled = !state.busy, singleLine = true,
                label = { Text(stringResource(R.string.email_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth().testTag("email"))
            Button(onClick = vm::send, enabled = !state.busy, modifier = Modifier.testTag("send_code")) { Text(stringResource(if(state.sent) R.string.resend_code else R.string.send_code)) }
            if (state.sent) {
                Text(stringResource(R.string.code_sent))
                OutlinedTextField(state.code, vm::code, enabled = !state.busy, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    label = { Text(stringResource(R.string.code_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth().testTag("otp"))
                Button(onClick = vm::verify, enabled = !state.busy && state.code.length == 6, modifier = Modifier.testTag("verify_code")) { Text(stringResource(R.string.verify_code)) }
            }
        }
        FailureText(state.error)
        if (state.busy) Busy()
    }
}
