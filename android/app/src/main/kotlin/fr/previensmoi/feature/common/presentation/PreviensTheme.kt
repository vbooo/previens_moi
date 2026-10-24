package fr.previensmoi.feature.common.presentation

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import fr.previensmoi.R
import fr.previensmoi.domain.model.*
import java.net.URI

private val Light =
    lightColorScheme(
        primary = Color(0xFF324C85),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDCE4FF),
        onPrimaryContainer = Color(0xFF0B1B3C),
        secondary = Color(0xFF555F75),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE0E5F1),
        onSecondaryContainer = Color(0xFF182035),
        tertiary = Color(0xFF665D50),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFEFE1CE),
        onTertiaryContainer = Color(0xFF241B10),
        background = Color(0xFFF5F6FA),
        onBackground = Color(0xFF1B1E27),
        surface = Color(0xFFF5F6FA),
        onSurface = Color(0xFF1B1E27),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFEDF0F7),
        surfaceContainer = Color(0xFFE6EAF3),
        surfaceContainerHigh = Color(0xFFDFE4EF),
        surfaceContainerHighest = Color(0xFFD9DFEC),
        onSurfaceVariant = Color(0xFF464B59),
        outline = Color(0xFF747986),
        outlineVariant = Color(0xFFC5CAD6),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
    )
private val Dark =
    darkColorScheme(
        primary = Color(0xFFB2C5FF),
        onPrimary = Color(0xFF002A64),
        primaryContainer = Color(0xFF1D3D72),
        onPrimaryContainer = Color(0xFFDCE4FF),
        secondary = Color(0xFFBCC6DD),
        onSecondary = Color(0xFF263047),
        secondaryContainer = Color(0xFF3D475E),
        onSecondaryContainer = Color(0xFFE0E5F1),
        tertiary = Color(0xFFD2C5B4),
        onTertiary = Color(0xFF382F22),
        tertiaryContainer = Color(0xFF504636),
        onTertiaryContainer = Color(0xFFEFE1CE),
        background = Color(0xFF11141B),
        onBackground = Color(0xFFE2E5EF),
        surface = Color(0xFF11141B),
        onSurface = Color(0xFFE2E5EF),
        surfaceContainerLowest = Color(0xFF0C0F16),
        surfaceContainerLow = Color(0xFF1A1E27),
        surfaceContainer = Color(0xFF1F232D),
        surfaceContainerHigh = Color(0xFF292E38),
        surfaceContainerHighest = Color(0xFF343944),
        onSurfaceVariant = Color(0xFFC5CAD6),
        outline = Color(0xFF8E93A1),
        outlineVariant = Color(0xFF444956),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
    )

data class StatusColors(
    val active: Color,
    val preparing: Color,
    val paused: Color,
    val warning: Color,
)

private val LocalStatus = staticCompositionLocalOf {
    StatusColors(Color(0xFF146C43), Color(0xFF245E9C), Color(0xFF536168), Color(0xFF805600))
}

@Composable
fun PreviensTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(
        LocalStatus provides
            if (dark)
                StatusColors(
                    Color(0xFF8FD5AD),
                    Color(0xFFA8CCFF),
                    Color(0xFFBCC8CD),
                    Color(0xFFF4CC7A),
                )
            else
                StatusColors(
                    Color(0xFF146C43),
                    Color(0xFF245E9C),
                    Color(0xFF536168),
                    Color(0xFF805600),
                ),
    ) {
        MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
    }
}

@Composable
fun AppIcon(id: Int, description: String? = null, tint: Color = LocalContentColor.current) {
    Icon(painterResource(id), description, Modifier.size(24.dp), tint = tint)
}

fun sourceDomain(url: String): String =
    runCatching { URI(url).host?.removePrefix("www.") }.getOrNull() ?: url

fun watchTitle(watch: Watch): String = watch.plan.title.ifBlank { watch.plan.draft.text }

@Composable
fun SecondaryText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
fun MetaRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    val valueContent: @Composable (Modifier) -> Unit = { modifier ->
        if (onClick != null) TextButton(onClick, modifier = modifier) { Text(value) }
        else Text(value, modifier, style = MaterialTheme.typography.bodyMedium)
    }
    if (LocalDensity.current.fontScale > 1.15f)
        Column(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SecondaryText(label)
            valueContent(Modifier)
        }
    else
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecondaryText(label, Modifier.weight(1f))
            valueContent(Modifier.weight(1f))
        }
}

@Composable
fun WatchStatusLabel(watch: Watch, contained: Boolean = false, onClick: (() -> Unit)? = null) {
    val failed =
        watch.baselineAt == null &&
            watch.status == WatchStatus.Paused &&
            watch.health == Health.Degraded
    val stopping = watch.remoteState == RemoteState.Stopping
    val unverified =
        watch.status == WatchStatus.Active &&
            (watch.nextCheck == null || watch.health != Health.Healthy)
    val tone =
        when {
            failed -> MaterialTheme.colorScheme.error
            stopping || unverified -> LocalStatus.current.warning
            else ->
                when (watch.status) {
                    WatchStatus.Active,
                    WatchStatus.Completed -> LocalStatus.current.active
                    WatchStatus.Pending -> LocalStatus.current.preparing
                    WatchStatus.Paused -> LocalStatus.current.paused
                    WatchStatus.SuspendedByCapacity -> LocalStatus.current.warning
                }
        }
    val label =
        when {
            failed -> R.string.preparation_failed_title
            stopping -> R.string.v1_stopping
            unverified -> R.string.v1_check_problem
            else ->
                when (watch.status) {
                    WatchStatus.Active -> R.string.v1_active
                    WatchStatus.Pending -> R.string.status_pending
                    WatchStatus.Paused -> R.string.status_paused
                    WatchStatus.Completed -> R.string.v1_completed
                    WatchStatus.SuspendedByCapacity -> R.string.v1_suspended
                }
        }
    val icon =
        when {
            failed -> R.drawable.symbol_error
            stopping || unverified -> R.drawable.symbol_schedule
            else ->
                when (watch.status) {
                    WatchStatus.Active,
                    WatchStatus.Completed -> R.drawable.symbol_check_circle
                    WatchStatus.Pending -> R.drawable.symbol_schedule
                    else -> R.drawable.symbol_pause
                }
        }
    if (onClick != null) {
        AssistChip(
            onClick = onClick,
            label = { Text(stringResource(label)) },
            leadingIcon = { AppIcon(icon, tint = tone) },
            trailingIcon = { AppIcon(R.drawable.symbol_chevron_right, tint = tone) },
            colors =
                AssistChipDefaults.assistChipColors(
                    containerColor = tone.copy(alpha = 0.10f),
                    labelColor = tone,
                ),
            border = null,
            modifier = Modifier.heightIn(min = 48.dp).testTag("status_explanation"),
        )
    } else {
        Surface(
            color = if (contained) tone.copy(alpha = 0.10f) else Color.Transparent,
            shape = MaterialTheme.shapes.small,
        ) {
            Row(
                modifier =
                    if (contained) Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    else Modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppIcon(icon, tint = tone)
                Text(
                    stringResource(label),
                    style = MaterialTheme.typography.labelLarge,
                    color = tone,
                )
            }
        }
    }
}
