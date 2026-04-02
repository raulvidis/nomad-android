package com.nomad.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NomadColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Background,
    primaryContainer = PrimaryDim,
    onPrimaryContainer = Primary,
    secondary = Accent,
    onSecondary = Background,
    secondaryContainer = Accent.copy(alpha = 0.15f),
    onSecondaryContainer = Accent,
    tertiary = Primary,
    error = Danger,
    onError = Background,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = Surface.copy(alpha = 0.8f),
    onSurfaceVariant = OnSurface.copy(alpha = 0.7f),
    outline = PrimaryDim,
    outlineVariant = Primary.copy(alpha = 0.3f)
)

@Composable
fun NomadTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NomadColorScheme,
        typography = NomadTypography,
        content = content
    )
}
