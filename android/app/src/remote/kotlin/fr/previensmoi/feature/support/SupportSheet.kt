package fr.previensmoi.feature.support

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.previensmoi.R
import fr.previensmoi.feature.common.presentation.AppIcon

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SupportSheet(
    state: SupportState,
    message: (String) -> Unit,
    email: (String) -> Unit,
    category: (String) -> Unit,
    send: () -> Unit,
    close: () -> Unit,
    done: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = close,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.widthIn(max = 720.dp)
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        AppIcon(R.drawable.symbol_info)
                    }
                }
                Text(
                    stringResource(R.string.support_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = close, modifier = Modifier.testTag("support_close")) {
                    AppIcon(R.drawable.symbol_close, stringResource(R.string.support_done))
                }
            }
            if (state.received) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppIcon(R.drawable.symbol_check_circle)
                        Text(
                            stringResource(R.string.support_received),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier =
                                Modifier.testTag("support_received").semantics {
                                    liveRegion = LiveRegionMode.Polite
                                },
                        )
                        Text(
                            stringResource(R.string.support_received_body),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Button(
                    onClick = done,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    Text(stringResource(R.string.support_done))
                }
            } else {
                Text(
                    stringResource(R.string.support_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                            "question" to R.string.support_question,
                            "problem" to R.string.support_problem,
                            "idea" to R.string.support_idea,
                        )
                        .forEach { (value, label) ->
                            FilterChip(
                                selected = state.category == value,
                                onClick = { category(value) },
                                enabled = !state.busy,
                                label = { Text(stringResource(label)) },
                                leadingIcon =
                                    if (state.category == value)
                                        ({ AppIcon(R.drawable.symbol_check_circle) })
                                    else null,
                                colors =
                                    FilterChipDefaults.filterChipColors(
                                        containerColor =
                                            MaterialTheme.colorScheme.secondaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        selectedLeadingIconColor =
                                            MaterialTheme.colorScheme.onPrimary,
                                    ),
                                modifier = Modifier.heightIn(min = 48.dp).testTag("support_$value"),
                            )
                        }
                }
                OutlinedTextField(
                    shape = MaterialTheme.shapes.medium,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedContainerColor =
                                MaterialTheme.colorScheme.surfaceContainerLowest,
                        ),
                    value = state.message,
                    onValueChange = message,
                    enabled = !state.busy,
                    label = { Text(stringResource(R.string.support_message)) },
                    minLines = 4,
                    maxLines = 7,
                    modifier = Modifier.fillMaxWidth().testTag("support_message"),
                    supportingText = { Text("${state.message.length} / 2000") },
                )
                OutlinedTextField(
                    shape = MaterialTheme.shapes.medium,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedContainerColor =
                                MaterialTheme.colorScheme.surfaceContainerLowest,
                        ),
                    value = state.email,
                    onValueChange = email,
                    enabled = !state.busy,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    label = { Text(stringResource(R.string.support_email)) },
                    supportingText = { Text(stringResource(R.string.support_email_hint)) },
                    modifier = Modifier.fillMaxWidth().testTag("support_email"),
                )
                state.problem?.let { problem ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AppIcon(R.drawable.symbol_error)
                            Text(
                                stringResource(
                                    when (problem) {
                                        SupportProblem.Invalid -> R.string.support_invalid
                                        SupportProblem.Offline -> R.string.support_offline
                                        SupportProblem.Limit -> R.string.support_limit
                                        SupportProblem.Unavailable -> R.string.support_unavailable
                                    },
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier =
                                    Modifier.weight(1f).testTag("support_error").semantics {
                                        liveRegion = LiveRegionMode.Polite
                                    },
                            )
                        }
                    }
                }

                Text(
                    stringResource(R.string.support_privacy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = send,
                    shape = MaterialTheme.shapes.medium,
                    enabled = !state.busy && state.message.isNotBlank(),
                    modifier =
                        Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("support_send"),
                ) {
                    if (state.busy) {
                        CircularProgressIndicator(
                            Modifier.size(18.dp),
                            color = LocalContentColor.current,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                    } else {
                        AppIcon(R.drawable.symbol_send)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        stringResource(
                            if (state.busy) R.string.support_sending else R.string.support_send,
                        ),
                    )
                }
            }
        }
    }
}
