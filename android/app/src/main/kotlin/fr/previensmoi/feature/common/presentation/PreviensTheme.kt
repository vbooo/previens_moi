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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import fr.previensmoi.R
import fr.previensmoi.domain.model.*
import java.net.URI

private val Light =
    lightColorScheme(
        primary = Color(0xFF006A6A),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF9CF1EF),
        onPrimaryContainer = Color(0xFF002020),
        secondary = Color(0xFF4A6363),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFCCE8E7),
        onSecondaryContainer = Color(0xFF051F1F),
        tertiary = Color(0xFF46617A),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFCEE5FF),
        onTertiaryContainer = Color(0xFF001D32),
        background = Color(0xFFF8FAFA),
        onBackground = Color(0xFF191C1D),
        surface = Color(0xFFF8FAFA),
        onSurface = Color(0xFF191C1D),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF0F4F4),
        surfaceContainer = Color(0xFFEAEFEF),
        surfaceContainerHigh = Color(0xFFE4E9E9),
        surfaceContainerHighest = Color(0xFFDEE3E3),
        onSurfaceVariant = Color(0xFF3F4949),
        outline = Color(0xFF6F7979),
        outlineVariant = Color(0xFFBEC9C8),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
    )
private val Dark =
    darkColorScheme(
        primary = Color(0xFF80D5D3),
        onPrimary = Color(0xFF003737),
        primaryContainer = Color(0xFF005050),
        onPrimaryContainer = Color(0xFF9CF1EF),
        secondary = Color(0xFFB0CCCB),
        onSecondary = Color(0xFF1C3534),
        secondaryContainer = Color(0xFF334B4B),
        onSecondaryContainer = Color(0xFFCCE8E7),
        tertiary = Color(0xFFAECAE6),
        onTertiary = Color(0xFF173349),
        tertiaryContainer = Color(0xFF2E4A61),
        onTertiaryContainer = Color(0xFFCEE5FF),
        background = Color(0xFF101718),
        onBackground = Color(0xFFDEE4E3),
        surface = Color(0xFF101718),
        onSurface = Color(0xFFDEE4E3),
        surfaceContainerLowest = Color(0xFF0B1212),
        surfaceContainerLow = Color(0xFF192020),
        surfaceContainer = Color(0xFF1D2424),
        surfaceContainerHigh = Color(0xFF272E2E),
        surfaceContainerHighest = Color(0xFF323939),
        onSurfaceVariant = Color(0xFFBEC9C8),
        outline = Color(0xFF899392),
        outlineVariant = Color(0xFF3F4949),
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
fun WatchStatusLabel(watch: Watch) {
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppIcon(icon, tint = tone)
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge, color = tone)
    }
}
