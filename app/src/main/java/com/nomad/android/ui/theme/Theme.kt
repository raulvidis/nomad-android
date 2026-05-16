package com.nomad.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val StitchColorScheme = darkColorScheme(
    primary = PhosphorGreen,
    onPrimary = OnPrimary,
    primaryContainer = PhosphorGreen,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = SecondaryGreen,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = SecondaryGreen,
    tertiary = TertiaryAmber,
    background = BackgroundDark,
    onBackground = OnBackground,
    surface = BackgroundDark,
    onSurface = OnSurface,
    surfaceVariant = SurfaceContainerHighest,
    onSurfaceVariant = OnSurfaceVariant,
    outline = OutlineColor,
    outlineVariant = OutlineVariant,
    error = TerminalDanger,
    onError = TerminalOnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    surfaceDim = SurfaceDim,
    surfaceBright = SurfaceBright,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
)

@Composable
fun NomadTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalNomadColors provides NomadColors()) {
        MaterialTheme(
            colorScheme = StitchColorScheme,
            typography = NomadTypography,
            content = content,
        )
    }
}
