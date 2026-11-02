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

private val Light = lightColorScheme(
    primary=Color(0xFF303D34), onPrimary=Color.White,
    primaryContainer=Color(0xFFE0E8DD), onPrimaryContainer=Color(0xFF26352A),
    secondary=Color(0xFF586456), onSecondary=Color.White,
    secondaryContainer=Color(0xFFE7EBE2), onSecondaryContainer=Color(0xFF303D34),
    tertiary=Color(0xFF765B14), onTertiary=Color.White,
    tertiaryContainer=Color(0xFFF4D38B), onTertiaryContainer=Color(0xFF302406),
    background=Color(0xFFF6F6F0), onBackground=Color(0xFF222820),
    surface=Color(0xFFF6F6F0), onSurface=Color(0xFF222820),
    surfaceContainerLowest=Color.White, surfaceContainerLow=Color(0xFFF0F1E9),
    surfaceContainer=Color(0xFFECEEE4), surfaceContainerHigh=Color(0xFFE5E8DD),
    surfaceContainerHighest=Color(0xFFDFE3D7),
    onSurfaceVariant=Color(0xFF5C645A), outline=Color(0xFF767E71), outlineVariant=Color(0xFFD8DED1),
    error=Color(0xFFBA1A1A), onError=Color.White, errorContainer=Color(0xFFFFDAD6), onErrorContainer=Color(0xFF410002),
)
private val Dark = darkColorScheme(
    primary=Color(0xFFDCE6D8), onPrimary=Color(0xFF243022),
    primaryContainer=Color(0xFF354132), onPrimaryContainer=Color(0xFFDAE5D6),
    secondary=Color(0xFFC0CBBA), onSecondary=Color(0xFF2B3627),
    secondaryContainer=Color(0xFF364332), onSecondaryContainer=Color(0xFFDFE9D8),
    tertiary=Color(0xFFE5C56F), onTertiary=Color(0xFF3C2E04),
    tertiaryContainer=Color(0xFF544319), onTertiaryContainer=Color(0xFFFFE4A3),
    background=Color(0xFF171C17), onBackground=Color(0xFFE0E6DB),
    surface=Color(0xFF171C17), onSurface=Color(0xFFE0E6DB),
    surfaceContainerLowest=Color(0xFF111611), surfaceContainerLow=Color(0xFF202620),
    surfaceContainer=Color(0xFF252C24), surfaceContainerHigh=Color(0xFF2C342B),
    surfaceContainerHighest=Color(0xFF374035),
    onSurfaceVariant=Color(0xFFBFC9B9), outline=Color(0xFF899582), outlineVariant=Color(0xFF414B3D),
    error=Color(0xFFFFB4AB), onError=Color(0xFF690005), errorContainer=Color(0xFF93000A), onErrorContainer=Color(0xFFFFDAD6),
)


data class StatusColors(
    val active: Color, val preparing: Color, val paused: Color, val warning: Color,
    val activeContainer: Color, val preparingContainer: Color, val pausedContainer: Color, val warningContainer: Color,
)
private val LightStatus = StatusColors(
    Color(0xFF186544), Color(0xFF805600), Color(0xFF555F52), Color(0xFF805600),
    Color(0xFFE1F2E7), Color(0xFFFFF0C9), Color(0xFFE8EDE3), Color(0xFFFFF0C9),
)
private val DarkStatus = StatusColors(
    Color(0xFF98DBB7), Color(0xFFECCC83), Color(0xFFC3CEC0), Color(0xFFECCC83),
    Color(0xFF223F2E), Color(0xFF43371C), Color(0xFF343E31), Color(0xFF43371C),
)
private val LocalStatus = staticCompositionLocalOf { LightStatus }

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
    CompositionLocalProvider(LocalStatus provides if (dark) DarkStatus else LightStatus) {
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
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
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
fun WatchStatusLabel(
    watch: Watch,
    contained: Boolean = false,
    compact: Boolean = false,
    operation: Operation? = null,
    onClick: (() -> Unit)? = null,
) {
    val failed =
        watch.preparationFailed
    val stopping = watch.remoteState == RemoteState.Stopping
    val unverified =
        watch.status == WatchStatus.Active &&
            (watch.nextCheck == null || watch.health != Health.Healthy)
    val tone =
        when {
            operation != null -> LocalStatus.current.preparing
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
    val background = when {
        operation != null -> LocalStatus.current.preparingContainer
        failed -> MaterialTheme.colorScheme.errorContainer
        stopping || unverified || watch.status == WatchStatus.SuspendedByCapacity -> LocalStatus.current.warningContainer
        watch.status in setOf(WatchStatus.Active, WatchStatus.Completed) -> LocalStatus.current.activeContainer
        watch.status == WatchStatus.Pending -> LocalStatus.current.preparingContainer
        else -> LocalStatus.current.pausedContainer
    }
    val label =
        when {
            operation == Operation.Pause -> R.string.v3_pausing
            operation == Operation.Resume -> R.string.v3_resuming
            operation == Operation.Delete -> R.string.v3_deleting
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
            operation != null -> R.drawable.symbol_schedule
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
                    containerColor = background,
                    labelColor = tone,
                ),
            border = null,
            modifier = Modifier.heightIn(min = 48.dp).testTag("status_explanation"),
        )
    } else {
        Surface(
            color = if (contained) background else Color.Transparent,
            shape = MaterialTheme.shapes.small,
        ) {
            Row(
                modifier =
                    if (contained) Modifier.padding(horizontal = 10.dp, vertical = if (compact) 5.dp else 7.dp)
                    else Modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(painterResource(icon), null, Modifier.size(if (compact) 16.dp else 18.dp), tint = tone)
                Text(
                    stringResource(label),
                    style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                    color = tone,
                )
            }
        }
    }
}
