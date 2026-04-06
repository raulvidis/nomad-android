package com.nomad.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private fun createColorScheme(colors: NomadThemeColors) = darkColorScheme(
    primary = colors.primary,
    onPrimary = colors.background,
    primaryContainer = colors.primaryDim,
    onPrimaryContainer = colors.primary,
    secondary = colors.secondary,
    onSecondary = colors.background,
    secondaryContainer = colors.secondary.copy(alpha = 0.15f),
    onSecondaryContainer = colors.secondary,
    tertiary = colors.accent,
    error = colors.danger,
    onError = colors.background,
    background = colors.background,
    onBackground = colors.onBackground,
    surface = colors.surface,
    onSurface = colors.onSurface,
    surfaceVariant = colors.surface.copy(alpha = 0.8f),
    onSurfaceVariant = colors.onSurface.copy(alpha = 0.7f),
    outline = colors.primaryDim,
    outlineVariant = colors.primary.copy(alpha = 0.3f),
)

@Composable
fun NomadTheme(
    themeId: String = "crt_green",
    content: @Composable () -> Unit
) {
    val themeColors = when (themeId) {
        "crt_amber" -> AmberThemeColors
        "crt_blue" -> BlueThemeColors
        else -> GreenThemeColors
    }
    val colorScheme = createColorScheme(themeColors)

    CompositionLocalProvider(LocalNomadColors provides themeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NomadTypography,
            content = content,
        )
    }
}
