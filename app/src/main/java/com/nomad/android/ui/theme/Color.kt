package com.nomad.android.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val TerminalGreen = Color(0xFF14F195)
val TerminalGreenDim = Color(0xFF0A7A4C)
val TerminalAmber = Color(0xFFFFB000)
val TerminalAmberDim = Color(0xFF805800)
val TerminalBlue = Color(0xFF00BFFF)
val TerminalBlueDim = Color(0xFF006080)
val TerminalDanger = Color(0xFFFF3333)
val TerminalBg = Color(0xFF0C0C0C)
val TerminalSurface = Color(0xFF1A1A1A)
val TerminalBorder = Color(0xFF2AFF6A)
val TerminalOnBg = Color(0xFF14F195)
val TerminalOnSurface = Color(0xFF14F195)

data class NomadThemeColors(
    val primary: Color,
    val primaryDim: Color,
    val secondary: Color,
    val secondaryDim: Color,
    val accent: Color,
    val border: Color,
    val background: Color,
    val surface: Color,
    val onBackground: Color,
    val onSurface: Color,
    val danger: Color,
)

val GreenThemeColors = NomadThemeColors(
    primary = TerminalGreen,
    primaryDim = TerminalGreenDim,
    secondary = TerminalAmber,
    secondaryDim = TerminalAmberDim,
    accent = TerminalBlue,
    border = TerminalBorder,
    background = TerminalBg,
    surface = TerminalSurface,
    onBackground = TerminalOnBg,
    onSurface = TerminalOnSurface,
    danger = TerminalDanger,
)

val AmberThemeColors = NomadThemeColors(
    primary = Color(0xFFFFB000),
    primaryDim = Color(0xFF805800),
    secondary = Color(0xFFFF6600),
    secondaryDim = Color(0xFF803300),
    accent = Color(0xFFFFD700),
    border = Color(0xFFFFC040),
    background = TerminalBg,
    surface = TerminalSurface,
    onBackground = Color(0xFFFFB000),
    onSurface = Color(0xFFFFB000),
    danger = TerminalDanger,
)

val BlueThemeColors = NomadThemeColors(
    primary = Color(0xFF00BFFF),
    primaryDim = Color(0xFF006080),
    secondary = Color(0xFF00FF7F),
    secondaryDim = Color(0xFF008040),
    accent = Color(0xFF87CEEB),
    border = Color(0xFF40D0FF),
    background = TerminalBg,
    surface = TerminalSurface,
    onBackground = Color(0xFF00BFFF),
    onSurface = Color(0xFF00BFFF),
    danger = TerminalDanger,
)

val LocalNomadColors = staticCompositionLocalOf { GreenThemeColors }

data class NomadThemeColors(
    val primary: Color,
    val primaryDim: Color,
    val secondary: Color,
    val secondaryDim: Color,
    val accent: Color,
    val border: Color,
    val background: Color,
    val surface: Color,
    val onBackground: Color,
    val onSurface: Color,
    val danger: Color,
)

val GreenThemeColors = NomadThemeColors(
    primary = TerminalGreen,
    primaryDim = TerminalGreenDim,
    secondary = TerminalAmber,
    secondaryDim = TerminalAmberDim,
    accent = TerminalBlue,
    border = TerminalBorder,
    background = TerminalBg,
    surface = TerminalSurface,
    onBackground = TerminalOnBg,
    onSurface = TerminalOnSurface,
    danger = TerminalDanger,
)

val AmberThemeColors = NomadThemeColors(
    primary = Color(0xFFFFB000),
    primaryDim = Color(0xFF805800),
    secondary = Color(0xFFFF6600),
    secondaryDim = Color(0xFF803300),
    accent = Color(0xFFFFD700),
    border = Color(0xFFFFC040),
    background = TerminalBg,
    surface = TerminalSurface,
    onBackground = Color(0xFFFFB000),
    onSurface = Color(0xFFFFB000),
    danger = TerminalDanger,
)

val BlueThemeColors = NomadThemeColors(
    primary = Color(0xFF00BFFF),
    primaryDim = Color(0xFF006080),
    secondary = Color(0xFF00FF7F),
    secondaryDim = Color(0xFF008040),
    accent = Color(0xFF87CEEB),
    border = Color(0xFF40D0FF),
    background = TerminalBg,
    surface = TerminalSurface,
    onBackground = Color(0xFF00BFFF),
    onSurface = Color(0xFF00BFFF),
    danger = TerminalDanger,
)

val LocalNomadColors = staticCompositionLocalOf { GreenThemeColors }
