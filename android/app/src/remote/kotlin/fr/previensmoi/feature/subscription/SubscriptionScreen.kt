package fr.previensmoi.feature.subscription

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.previensmoi.R
import fr.previensmoi.domain.model.*
import fr.previensmoi.feature.common.presentation.*

@Composable
fun SubscriptionScreen(
    state: SubscriptionUiState,
    subscribe: () -> Unit,
    restore: () -> Unit,
    refresh: () -> Unit,
    manage: () -> Unit,
    dismissRestoreMessage: () -> Unit = {},
) {
    val access = state.access
    val price = state.product?.formattedPrice ?: stringResource(R.string.billing_price_reference)
    val eligible =
        access?.canPurchase == true &&
            state.problem in setOf(null, BillingProblem.Cancelled) &&
            state.product != null &&
            access.active.not() &&
            access.status in setOf(SubscriptionStatus.None, SubscriptionStatus.Expired)
    val known =
        access?.status != null &&
            access.status !in setOf(SubscriptionStatus.None, SubscriptionStatus.Offered)
    val snackbar = remember { SnackbarHostState() }
    val restoreMessage =
        if (state.restored)
            stringResource(
                when {
                    access?.status == SubscriptionStatus.Offered -> R.string.billing_offered
                    access?.status == SubscriptionStatus.Grace -> R.string.billing_grace
                    access?.status == SubscriptionStatus.Hold -> R.string.billing_hold
                    access?.status == SubscriptionStatus.Paused -> R.string.billing_paused
                    access?.status == SubscriptionStatus.Pending -> R.string.billing_pending
                    access?.status == SubscriptionStatus.Expired -> R.string.billing_expired
                    access?.active != true -> R.string.billing_restore_none
                    access.status == SubscriptionStatus.Cancelled ->
                        R.string.billing_restored_cancelled
                    else -> R.string.billing_restored
                },
            )
        else null
    LaunchedEffect(restoreMessage) {
        if (restoreMessage != null) {
            snackbar.showSnackbar(restoreMessage, withDismissAction = true)
            dismissRestoreMessage()
        }
    }
    Box(Modifier.fillMaxSize()) {
        Page(
            footer =
                if (state.loaded && !state.busy && eligible) {
                    {
                        Button(
                            subscribe,
                            enabled = eligible && !state.busy,
                            modifier =
                                Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("subscribe"),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(
                                stringResource(
                                    if (state.busy) R.string.v1_wait
                                    else R.string.billing_subscribe,
                                    price,
                                ),
                            )
                        }
                    }
                } else null,
        ) {
            if (!state.loaded) {
                Busy()
                return@Page
            }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (access == null) {
                Text(
                    stringResource(R.string.billing_load_failed),
                    style = MaterialTheme.typography.titleMedium,
                )
                Button(
                    refresh,
                    enabled = !state.busy,
                    modifier = Modifier.testTag("subscription_retry"),
                ) {
                    Text(stringResource(R.string.v1_retry))
                }
                return@Page
            }
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            androidx.compose.ui.res.painterResource(R.drawable.brand_mark),
                            null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.width(24.dp).height(32.dp),
                        )
                        Text(
                            stringResource(R.string.billing_plus),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        if (access.status == SubscriptionStatus.Offered)
                            stringResource(R.string.billing_offered_terms)
                        else stringResource(R.string.billing_per_month, price),
                        style =
                            if (access.status == SubscriptionStatus.Offered)
                                MaterialTheme.typography.bodyLarge
                            else MaterialTheme.typography.headlineSmall,
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f),
                    )
                    Text(
                        stringResource(
                            when (access.status) {
                                SubscriptionStatus.Active -> R.string.billing_active
                                SubscriptionStatus.Offered -> R.string.billing_offered
                                SubscriptionStatus.Cancelled -> R.string.billing_cancelled
                                SubscriptionStatus.Grace -> R.string.billing_grace
                                SubscriptionStatus.Hold -> R.string.billing_hold
                                SubscriptionStatus.Paused -> R.string.billing_paused
                                SubscriptionStatus.Expired -> R.string.billing_expired
                                SubscriptionStatus.Pending -> R.string.billing_pending
                                SubscriptionStatus.None ->
                                    if (access.trialEnded) R.string.trial_ended
                                    else R.string.billing_offer_intro
                            },
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag("subscription_status"),
                    )
                    if (access.active && access.status != SubscriptionStatus.Offered)
                        access.expiresAt?.let {
                            Text(
                                stringResource(
                                    when (access.status) {
                                        SubscriptionStatus.Cancelled ->
                                            R.string.billing_access_until
                                        SubscriptionStatus.Grace -> R.string.billing_grace_until
                                        else ->
                                            if (access.fromCache) R.string.billing_access_until
                                            else R.string.billing_renewal
                                    },
                                    dateText(it),
                                ),
                            )
                        }
                    else if (!access.active && !access.trialEnded)
                        access.trialEndsAt?.let {
                            SecondaryText(stringResource(R.string.trial_ends_at, dateText(it)))
                        }
                }
            }
            SectionTitle(stringResource(R.string.billing_included))
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    SubscriptionBenefit(
                        R.drawable.symbol_check_circle,
                        R.string.billing_watch_title,
                        R.string.billing_watch_detail,
                    )
                    SubscriptionBenefit(
                        R.drawable.symbol_schedule,
                        R.string.billing_daily_title,
                        R.string.billing_daily_sources,
                    )
                    SubscriptionBenefit(
                        R.drawable.symbol_refresh,
                        R.string.billing_creation_title,
                        R.string.billing_creation_detail,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        AppIcon(
                            R.drawable.symbol_pause,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SecondaryText(
                            stringResource(R.string.billing_pause_free),
                            Modifier.weight(1f),
                        )
                    }
                }
            }
            if (access.active)
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            stringResource(R.string.billing_your_period),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            stringResource(R.string.billing_remaining, access.remainingCreations),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.testTag("subscription_remaining"),
                        )
                        LinearProgressIndicator(
                            progress = { access.remainingCreations.coerceIn(0, 5) / 5f },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        )
                        Text(
                            stringResource(R.string.billing_quota_definition),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            if (
                !access.active &&
                    !access.fromCache &&
                    !eligible &&
                    access.status in setOf(SubscriptionStatus.None, SubscriptionStatus.Expired)
            )
                SecondaryText(stringResource(R.string.billing_not_available))
            if (access.fromCache)
                SecondaryText(
                    stringResource(R.string.billing_cached_offline),
                    modifier = Modifier.testTag("subscription_cached"),
                )
            state.problem
                ?.takeUnless { access.fromCache && it == BillingProblem.Offline }
                ?.let { problem ->
                    Text(
                        stringResource(
                            when (problem) {
                                BillingProblem.Cancelled -> R.string.billing_purchase_cancelled
                                BillingProblem.Pending -> R.string.billing_pending
                                BillingProblem.WrongAccount -> R.string.billing_wrong_account
                                BillingProblem.InvalidPurchase -> R.string.billing_invalid
                                BillingProblem.Offline -> R.string.billing_offline
                                BillingProblem.Verification -> R.string.billing_verify_failed
                                BillingProblem.Unavailable -> R.string.billing_not_available
                            },
                        ),
                        color =
                            if (problem in setOf(BillingProblem.Cancelled, BillingProblem.Pending))
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("subscription_message"),
                    )
                }
            if (known)
                FilledTonalButton(
                    manage,
                    modifier =
                        Modifier.fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("manage_subscription"),
                ) {
                    Text(
                        stringResource(
                            if (
                                access.status in
                                    setOf(SubscriptionStatus.Grace, SubscriptionStatus.Hold)
                            )
                                R.string.billing_fix_payment
                            else R.string.billing_manage,
                        ),
                    )
                }
            OutlinedButton(
                restore,
                enabled = !state.busy,
                modifier =
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("restore_purchases"),
            ) {
                Text(
                    stringResource(
                        if (state.busy) R.string.billing_checking else R.string.billing_restore,
                    ),
                )
            }
            if (access.status in setOf(SubscriptionStatus.None, SubscriptionStatus.Expired)) {
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            stringResource(R.string.billing_how_title),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            stringResource(R.string.billing_terms),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            stringResource(R.string.billing_no_commitment),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter)
                .widthIn(max = 720.dp)
                .padding(16.dp)
                .testTag("subscription_feedback"),
        )
    }
}

@Composable
private fun SubscriptionBenefit(icon: Int, title: Int, detail: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                AppIcon(icon, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(detail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
