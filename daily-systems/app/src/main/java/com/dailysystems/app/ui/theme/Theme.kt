package com.dailysystems.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/** Follows the system theme; no dynamic color (SPEC §5). */
@Composable
fun DailySystemsTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (darkTheme) DarkAppColors else LightAppColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.onAccent, primaryContainer = c.accentSoft, onPrimaryContainer = c.ink,
            secondary = c.accent, onSecondary = c.onAccent,
            background = c.background, onBackground = c.ink,
            surface = c.surface, onSurface = c.ink, surfaceVariant = c.surface2, onSurfaceVariant = c.muted,
            surfaceContainerLowest = c.surface, surfaceContainerLow = c.surface, surfaceContainer = c.surface,
            surfaceContainerHigh = c.surface, surfaceContainerHighest = c.surface2,
            outline = c.line, outlineVariant = c.line, error = c.danger, onError = c.surface, scrim = Color.Black,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.onAccent, primaryContainer = c.accentSoft, onPrimaryContainer = c.ink,
            secondary = c.accent, onSecondary = c.onAccent,
            background = c.background, onBackground = c.ink,
            surface = c.surface, onSurface = c.ink, surfaceVariant = c.surface2, onSurfaceVariant = c.muted,
            surfaceContainerLowest = c.surface, surfaceContainerLow = c.surface, surfaceContainer = c.surface,
            surfaceContainerHigh = c.surface, surfaceContainerHighest = c.surface2,
            outline = c.line, outlineVariant = c.line, error = c.danger, onError = c.surface, scrim = Color.Black,
        )
    }
    CompositionLocalProvider(LocalAppColors provides c, LocalAppType provides AppTypeDefault) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}

/** Shorthand: `AppTheme.colors.muted`, `AppTheme.type.mono`. */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
    val type: AppType
        @Composable @ReadOnlyComposable get() = LocalAppType.current
}
