package com.nomad.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NomadColorScheme = darkColorScheme(
    primary = TerminalGreen,
    onPrimary = TerminalBg,
    primaryContainer = TerminalGreenDim,
    onPrimaryContainer = TerminalGreen,
    secondary = TerminalAmber,
    onSecondary = TerminalBg,
    secondaryContainer = TerminalAmber.copy(alpha = 0.15f),
    onSecondaryContainer = TerminalAmber,
    tertiary = TerminalBlue,
    error = TerminalDanger,
    onError = TerminalBg,
    background = TerminalBg,
    onBackground = TerminalOnBg,
    surface = TerminalSurface,
    onSurface = TerminalOnSurface,
    surfaceVariant = TerminalSurface.copy(alpha = 0.8f),
    onSurfaceVariant = TerminalOnSurface.copy(alpha = 0.7f),
    outline = TerminalGreenDim,
    outlineVariant = TerminalGreen.copy(alpha = 0.3f),
)

@Composable
fun NomadTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NomadColorScheme,
        typography = NomadTypography,
        content = content,
    )
}
