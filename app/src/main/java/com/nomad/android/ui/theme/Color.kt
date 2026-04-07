package com.nomad.android.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val PhosphorGreen = Color(0xFF00FF41)
val PhosphorGreenDim = Color(0xFF00E639)
val PhosphorGreenGlow = Color(0xFF72FF70)
val SecondaryGreen = Color(0xFFA0D39E)
val SecondaryGreenDim = Color(0xFF225027)
val BackgroundDark = Color(0xFF131313)
val SurfaceContainerLowest = Color(0xFF0E0E0E)
val SurfaceContainerLow = Color(0xFF1C1B1B)
val SurfaceContainer = Color(0xFF201F1F)
val SurfaceContainerHigh = Color(0xFF2A2A2A)
val SurfaceContainerHighest = Color(0xFF353534)
val SurfaceDim = Color(0xFF131313)
val SurfaceBright = Color(0xFF3A3939)
val OnBackground = Color(0xFFE5E2E1)
val OnSurface = Color(0xFFE5E2E1)
val OnSurfaceVariant = Color(0xFFB9CCB2)
val OutlineColor = Color(0xFF84967E)
val OutlineVariant = Color(0xFF3B4B37)
val TerminalDanger = Color(0xFFFFB4AB)
val TerminalOnError = Color(0xFF690005)
val ErrorContainer = Color(0xFF93000A)
val OnErrorContainer = Color(0xFFFFDAD6)
val OnPrimary = Color(0xFF003907)
val OnPrimaryContainer = Color(0xFF007117)
val SecondaryFixed = Color(0xFFBBF0B9)
val OnSecondary = Color(0xFF073913)
val OnSecondaryFixed = Color(0xFF002106)
val SecondaryContainer = Color(0xFF225027)
val TertiaryAmber = Color(0xFFFFBA3F)
val TertiaryContainer = Color(0xFFFFD69A)

data class NomadColors(
    val primary: Color = PhosphorGreen,
    val primaryDim: Color = PhosphorGreenDim,
    val primaryGlow: Color = PhosphorGreenGlow,
    val secondary: Color = SecondaryGreen,
    val secondaryDim: Color = SecondaryGreenDim,
    val background: Color = BackgroundDark,
    val surfaceContainerLowest: Color = SurfaceContainerLowest,
    val surfaceContainerLow: Color = SurfaceContainerLow,
    val surfaceContainer: Color = SurfaceContainer,
    val surfaceContainerHigh: Color = SurfaceContainerHigh,
    val surfaceContainerHighest: Color = SurfaceContainerHighest,
    val surfaceDim: Color = SurfaceDim,
    val surfaceBright: Color = SurfaceBright,
    val onBackground: Color = OnBackground,
    val onSurface: Color = OnSurface,
    val onSurfaceVariant: Color = OnSurfaceVariant,
    val outline: Color = OutlineColor,
    val outlineVariant: Color = OutlineVariant,
    val danger: Color = TerminalDanger,
    val onPrimary: Color = OnPrimary,
    val onPrimaryContainer: Color = OnPrimaryContainer,
    val secondaryFixed: Color = SecondaryFixed,
    val onSecondary: Color = OnSecondary,
    val onSecondaryFixed: Color = OnSecondaryFixed,
    val secondaryContainer: Color = SecondaryContainer,
    val tertiary: Color = TertiaryAmber,
    val tertiaryContainer: Color = TertiaryContainer,
)

val LocalNomadColors = staticCompositionLocalOf { NomadColors() }
