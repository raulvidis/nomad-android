# Terminal UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the NOMAD Android app from Pip-Boy-themed to a clean, readable retro terminal interface while keeping the green color scheme.

**Architecture:** MVVM + Compose UI. Changes span theme layer (colors, typography, effects), component layer (all UI components), app shell (layout structure), and all 6 screen composables. No changes to ViewModels, repositories, or data layer.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Compose Navigation, Hilt DI

---

## File Map

| Action | File | Responsibility |
|--------|------|----------------|
| Create | `app/src/main/res/font/jetbrains_mono_regular.ttf` | JetBrains Mono Regular font |
| Create | `app/src/main/res/font/jetbrains_mono_medium.ttf` | JetBrains Mono Medium font |
| Create | `app/src/main/res/font/jetbrains_mono_bold.ttf` | JetBrains Mono Bold font |
| Modify | `app/src/main/java/com/nomad/android/ui/theme/Color.kt` | Consolidate palette to Terminal* names |
| Modify | `app/src/main/java/com/nomad/android/ui/theme/Theme.kt` | Use consolidated Terminal* palette |
| Modify | `app/src/main/java/com/nomad/android/ui/theme/Type.kt` | JetBrains Mono typography |
| Rename | `PipBoyEffects.kt` → `TerminalEffects.kt` | CRT effects, status bar, bottom nav |
| Rename | `PipBoyComponents.kt` → `TerminalComponents.kt` | All reusable UI components |
| Modify | `app/src/main/java/com/nomad/android/NomadApp.kt` | Remove Scaffold, restructure shell |
| Modify | `app/src/main/java/com/nomad/android/ui/navigation/NavHost.kt` | Update imports |
| Modify | `app/src/main/java/com/nomad/android/ui/dashboard/DashboardScreen.kt` | Update imports, spacing, remove Pip-Boy refs |
| Modify | `app/src/main/java/com/nomad/android/ui/maps/MapsScreen.kt` | Update imports, use TerminalTextField |
| Modify | `app/src/main/java/com/nomad/android/ui/knowledge/KnowledgeScreen.kt` | Update imports, spacing |
| Modify | `app/src/main/java/com/nomad/android/ui/chat/ChatScreen.kt` | Update imports, spacing |
| Modify | `app/src/main/java/com/nomad/android/ui/emergency/EmergencyScreen.kt` | Update imports, spacing |
| Modify | `app/src/main/java/com/nomad/android/ui/settings/SettingsScreen.kt` | Update imports, consistent spacing |
| Modify | `app/src/main/java/com/nomad/android/ui/onboarding/OnboardingScreen.kt` | Use theme colors, remove Pip-Boy refs |

---

### Task 1: Color System Consolidation

**Files:**
- Modify: `app/src/main/java/com/nomad/android/ui/theme/Color.kt`

- [ ] **Step 1: Rewrite Color.kt with consolidated Terminal* palette**

Replace the entire file contents with:

```kotlin
package com.nomad.android.ui.theme

import androidx.compose.ui.graphics.Color

// Terminal color palette — single source of truth
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

// Legacy aliases — remove after all screens migrated
@Deprecated("Use TerminalGreen", ReplaceWith("TerminalGreen"))
val PipBoyGreen = TerminalGreen
@Deprecated("Use TerminalGreenDim", ReplaceWith("TerminalGreenDim"))
val PipBoyGreenDim = TerminalGreenDim
@Deprecated("Use TerminalAmber", ReplaceWith("TerminalAmber"))
val PipBoyAmber = TerminalAmber
@Deprecated("Use TerminalAmberDim", ReplaceWith("TerminalAmberDim"))
val PipBoyAmberDim = TerminalAmberDim
@Deprecated("Use TerminalBlue", ReplaceWith("TerminalBlue"))
val PipBoyBlue = TerminalBlue
@Deprecated("Use TerminalBlueDim", ReplaceWith("TerminalBlueDim"))
val PipBoyBlueDim = TerminalBlueDim
@Deprecated("Use TerminalDanger", ReplaceWith("TerminalDanger"))
val PipBoyDanger = TerminalDanger
@Deprecated("Use TerminalBg", ReplaceWith("TerminalBg"))
val PipBoyBg = TerminalBg
@Deprecated("Use TerminalSurface", ReplaceWith("TerminalSurface"))
val PipBoySurface = TerminalSurface
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/nomad/android/ui/theme/Color.kt
git commit -m "refactor: consolidate color palette to Terminal* names with legacy aliases"
```

---

### Task 2: Typography with JetBrains Mono

**Files:**
- Create: `app/src/main/res/font/jetbrains_mono_regular.ttf`
- Create: `app/src/main/res/font/jetbrains_mono_medium.ttf`
- Create: `app/src/main/res/font/jetbrains_mono_bold.ttf`
- Modify: `app/src/main/java/com/nomad/android/ui/theme/Type.kt`

- [ ] **Step 1: Download JetBrains Mono font files**

Download from https://github.com/JetBrains/JetBrainsMono/releases (latest release, `JetBrainsMono-*.zip`):
- `JetBrainsMono-Regular.ttf` → rename to `jetbrains_mono_regular.ttf` → place in `app/src/main/res/font/`
- `JetBrainsMono-Medium.ttf` → rename to `jetbrains_mono_medium.ttf` → place in `app/src/main/res/font/`
- `JetBrainsMono-Bold.ttf` → rename to `jetbrains_mono_bold.ttf` → place in `app/src/main/res/font/`

```bash
mkdir -p app/src/main/res/font/
# Download and extract (adjust version as needed)
curl -L -o /tmp/jetbrains-mono.zip "https://github.com/JetBrains/JetBrainsMono/releases/download/v8.000/JetBrainsMono-8.000.zip"
unzip -j /tmp/jetbrains-mono.zip "fonts/ttf/JetBrainsMono-Regular.ttf" -d app/src/main/res/font/ && mv app/src/main/res/font/JetBrainsMono-Regular.ttf app/src/main/res/font/jetbrains_mono_regular.ttf
unzip -j /tmp/jetbrains-mono.zip "fonts/ttf/JetBrainsMono-Medium.ttf" -d app/src/main/res/font/ && mv app/src/main/res/font/JetBrainsMono-Medium.ttf app/src/main/res/font/jetbrains_mono_medium.ttf
unzip -j /tmp/jetbrains-mono.zip "fonts/ttf/JetBrainsMono-Bold.ttf" -d app/src/main/res/font/ && mv app/src/main/res/font/JetBrainsMono-Bold.ttf app/src/main/res/font/jetbrains_mono_bold.ttf
rm /tmp/jetbrains-mono.zip
```

- [ ] **Step 2: Rewrite Type.kt with JetBrains Mono**

Replace the entire file contents with:

```kotlin
package com.nomad.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nomad.android.R

private val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

val NomadTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 29.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/font/ app/src/main/java/com/nomad/android/ui/theme/Type.kt
git commit -m "feat: add JetBrains Mono font with improved typography hierarchy"
```

---

### Task 3: Theme.kt Update

**Files:**
- Modify: `app/src/main/java/com/nomad/android/ui/theme/Theme.kt`

- [ ] **Step 1: Rewrite Theme.kt to use consolidated Terminal* palette**

Replace the entire file contents with:

```kotlin
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
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/nomad/android/ui/theme/Theme.kt
git commit -m "refactor: update theme to use consolidated Terminal* color palette"
```

---

### Task 4: TerminalEffects (rename from PipBoyEffects)

**Files:**
- Create: `app/src/main/java/com/nomad/android/ui/theme/TerminalEffects.kt`
- Delete: `app/src/main/java/com/nomad/android/ui/theme/PipBoyEffects.kt`

- [ ] **Step 1: Create TerminalEffects.kt with refined CRT effects, renamed status bar, new bottom nav**

```kotlin
package com.nomad.android.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.composed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.core.*

fun Modifier.scanlineOverlay(): Modifier = this.then(
    Modifier.drawBehind {
        val lineSpacing = 4.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = TerminalGreen.copy(alpha = 0.03f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
            y += lineSpacing
        }
    }
)

fun Modifier.crtFlicker(): Modifier = this.then(
    Modifier.composed {
        val infiniteTransition = rememberInfiniteTransition(label = "crtFlicker")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.98f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "flickerAlpha",
        )
        Modifier.graphicsLayer { this.alpha = alpha }
    }
)

@Composable
fun CrtScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBg.copy(alpha = 0.98f))
            .scanlineOverlay()
            .crtFlicker(),
    ) {
        content()
    }
}

@Composable
fun TerminalStatusBar(
    modifier: Modifier = Modifier,
    isAiOnline: Boolean = true,
    storagePercent: Int = 67,
) {
    val dateFormat = remember { SimpleDateFormat("MM.dd.yyyy HH:mm", Locale.US) }
    var currentTime by remember { mutableStateOf(dateFormat.format(Date())) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000L)
            currentTime = dateFormat.format(Date())
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalSurface)
            .border(width = 1.dp, color = TerminalGreenDim)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "NOMAD",
            color = TerminalGreen,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(
                    com.nomad.android.R.font.jetbrains_mono_bold,
                    FontWeight.Bold,
                ),
            ),
            fontSize = 13.sp,
            letterSpacing = 3.sp,
        )

        Text(
            text = currentTime,
            color = TerminalGreen.copy(alpha = 0.8f),
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(
                    com.nomad.android.R.font.jetbrains_mono_regular,
                    FontWeight.Normal,
                ),
            ),
            fontSize = 12.sp,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            if (isAiOnline) TerminalGreen else TerminalDanger,
                        ),
                )
                Text(
                    text = if (isAiOnline) "ONLINE" else "OFFLINE",
                    color = if (isAiOnline) TerminalGreen else TerminalDanger,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(
                            com.nomad.android.R.font.jetbrains_mono_regular,
                            FontWeight.Normal,
                        ),
                    ),
                    fontSize = 10.sp,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(TerminalAmber),
                )
                Text(
                    text = "$storagePercent%",
                    color = TerminalAmber,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(
                            com.nomad.android.R.font.jetbrains_mono_regular,
                            FontWeight.Normal,
                        ),
                    ),
                    fontSize = 10.sp,
                )
            }
        }
    }
}

data class TerminalTab(
    val route: String,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
)

val TerminalTabs = listOf(
    TerminalTab("dashboard", "Dashboard", Icons.Filled.Home, Icons.Outlined.Home),
    TerminalTab("maps", "Maps", Icons.Filled.Map, Icons.Outlined.Map),
    TerminalTab("knowledge", "Knowledge", Icons.Filled.Book, Icons.Outlined.Book),
    TerminalTab("chat", "Chat", Icons.Filled.Chat, Icons.Outlined.Chat),
    TerminalTab("emergency", "Emergency", Icons.Filled.Bookmark, Icons.Outlined.Bookmark),
    TerminalTab("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun TerminalBottomNav(
    navController: NavController,
    currentRoute: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalSurface)
            .border(width = 1.dp, color = TerminalGreenDim, shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        TerminalTabs.forEach { tab ->
            val selected = currentRoute == tab.route ||
                (tab.route == "dashboard" && currentRoute.startsWith("dashboard"))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .then(
                        if (selected) {
                            Modifier.border(
                                width = 2.dp,
                                color = TerminalGreen,
                                shape = RoundedCornerShape(4.dp),
                            )
                        } else {
                            Modifier
                        },
                    )
                    .clickable {
                        if (currentRoute != tab.route) {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                        tint = if (selected) TerminalGreen else TerminalGreenDim,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = tab.label.uppercase(),
                        fontFamily = androidx.compose.ui.text.font.FontFamily(
                            androidx.compose.ui.text.font.Font(
                                com.nomad.android.R.font.jetbrains_mono_medium,
                                FontWeight.Medium,
                            ),
                        ),
                        fontSize = 10.sp,
                        color = if (selected) TerminalGreen else TerminalGreenDim,
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Delete old PipBoyEffects.kt**

```bash
rm app/src/main/java/com/nomad/android/ui/theme/PipBoyEffects.kt
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nomad/android/ui/theme/TerminalEffects.kt app/src/main/java/com/nomad/android/ui/theme/PipBoyEffects.kt
git commit -m "refactor: rename PipBoyEffects to TerminalEffects, refine CRT, add Settings tab"
```

---

### Task 5: TerminalComponents (rename from PipBoyComponents)

**Files:**
- Create: `app/src/main/java/com/nomad/android/ui/components/TerminalComponents.kt`
- Delete: `app/src/main/java/com/nomad/android/ui/components/PipBoyComponents.kt`

- [ ] **Step 1: Create TerminalComponents.kt with refined components**

```kotlin
package com.nomad.android.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nomad.android.R
import com.nomad.android.ui.theme.TerminalBg
import com.nomad.android.ui.theme.TerminalBorder
import com.nomad.android.ui.theme.TerminalDanger
import com.nomad.android.ui.theme.TerminalGreen
import com.nomad.android.ui.theme.TerminalGreenDim
import com.nomad.android.ui.theme.TerminalSurface

enum class TerminalButtonVariant { NORMAL, DANGER, AMBER, DISABLED }

enum class TerminalButtonSize { LARGE, MEDIUM, SMALL }

@Composable
fun TerminalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: TerminalButtonVariant = TerminalButtonVariant.NORMAL,
    size: TerminalButtonSize = TerminalButtonSize.MEDIUM,
    tintColor: Color? = null,
) {
    val resolvedVariant = if (!enabled) TerminalButtonVariant.DISABLED else variant

    val defaultColor = when (resolvedVariant) {
        TerminalButtonVariant.NORMAL -> tintColor ?: TerminalGreen
        TerminalButtonVariant.DANGER -> TerminalDanger
        TerminalButtonVariant.AMBER -> com.nomad.android.ui.theme.TerminalAmber
        TerminalButtonVariant.DISABLED -> Color(0xFF444444)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor = if (isPressed && resolvedVariant != TerminalButtonVariant.DISABLED) {
        defaultColor
    } else {
        TerminalBg
    }

    val contentColor = if (isPressed && resolvedVariant != TerminalButtonVariant.DISABLED) {
        TerminalBg
    } else {
        defaultColor
    }

    val minHeight = when (size) {
        TerminalButtonSize.LARGE -> 48.dp
        TerminalButtonSize.MEDIUM -> 40.dp
        TerminalButtonSize.SMALL -> 32.dp
    }

    val fontSize = when (size) {
        TerminalButtonSize.LARGE -> 14.sp
        TerminalButtonSize.MEDIUM -> 13.sp
        TerminalButtonSize.SMALL -> 11.sp
    }

    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .border(2.dp, defaultColor, RoundedCornerShape(8.dp))
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = contentColor,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
            ),
            fontSize = fontSize,
        )
    }
}

@Composable
fun TerminalCard(
    modifier: Modifier = Modifier,
    header: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalSurface, RoundedCornerShape(8.dp))
            .border(2.dp, TerminalBorder, RoundedCornerShape(8.dp))
            .padding(12.dp),
    ) {
        if (header != null) {
            Text(
                text = header.uppercase(),
                color = TerminalGreen,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
                ),
                fontSize = 14.sp,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(TerminalGreenDim.copy(alpha = 0.3f)),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        content()
    }
}

@Composable
fun TerminalText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = TerminalGreen,
    glow: Boolean = false,
) {
    val mergedStyle = style.merge(
        TextStyle(
            color = color,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            ),
            shadow = if (glow) {
                Shadow(
                    color = color,
                    blurRadius = 2f,
                    offset = Offset(0f, 0f),
                )
            } else {
                null
            },
        ),
    )

    Text(
        text = text,
        modifier = modifier,
        style = mergedStyle,
    )
}

@Composable
fun TerminalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    singleLine: Boolean = false,
    enabled: Boolean = true,
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) TerminalGreen else TerminalGreenDim

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label.uppercase(),
                color = TerminalGreenDim,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                ),
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .border(2.dp, borderColor, RoundedCornerShape(6.dp))
                .background(TerminalBg, RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .padding(12.dp)
                .onFocusChanged { isFocused = it.isFocused },
            textStyle = TextStyle(
                color = TerminalGreen,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                ),
                fontSize = 14.sp,
            ),
            singleLine = singleLine,
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            color = TerminalGreenDim,
                            fontFamily = androidx.compose.ui.text.font.FontFamily(
                                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                            ),
                            fontSize = 14.sp,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
fun TerminalProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = TerminalGreen,
) {
    val segmentCount = 12
    val segmentGap = 2.dp
    val filledSegments = (progress.coerceIn(0f, 1f) * segmentCount).toInt()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .background(TerminalGreenDim.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(segmentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until segmentCount) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(
                        if (i < filledSegments) color else Color.Transparent,
                        RoundedCornerShape(1.dp),
                    ),
            )
        }
    }
}

@Composable
fun TerminalStatusIndicator(
    label: String,
    isOnline: Boolean,
    modifier: Modifier = Modifier,
) {
    val dotColor = if (isOnline) TerminalGreen else TerminalGreenDim

    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isOnline) 0.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blinkAlpha",
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    dotColor.copy(alpha = blinkAlpha),
                    CircleShape,
                ),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = TerminalGreen,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            ),
            fontSize = 12.sp,
        )
    }
}

@Composable
fun TerminalDivider(
    modifier: Modifier = Modifier,
    color: Color = TerminalGreenDim,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}

@Composable
fun TerminalListTile(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TerminalGreen,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                ),
                fontSize = 14.sp,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = TerminalGreenDim,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 12.sp,
                )
            }
        }
        Text(
            text = ">",
            color = TerminalGreen,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            ),
            fontSize = 16.sp,
        )
    }
}

@Composable
fun TerminalSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = text.uppercase(),
            color = TerminalGreen,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
            ),
            fontSize = 16.sp,
            letterSpacing = 1.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TerminalGreenDim.copy(alpha = 0.3f)),
        )
    }
}

@Composable
fun TerminalLoadingScreen(
    message: String = "INITIALIZING...",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            color = TerminalGreen,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
            ),
            fontSize = 16.sp,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "[████████░░░░░░░░░░░░]",
            color = TerminalGreenDim,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            ),
            fontSize = 14.sp,
        )
    }
}

@Composable
fun TerminalErrorScreen(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "ERROR: $message",
            color = TerminalDanger,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            ),
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(16.dp))
        TerminalButton(text = "RETRY", onClick = onRetry, variant = TerminalButtonVariant.DANGER)
    }
}

@Composable
fun TerminalEmptyScreen(
    message: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "NO DATA FOUND",
            color = TerminalGreenDim,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            ),
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = TerminalGreenDim,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            ),
            fontSize = 12.sp,
        )
        if (action != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            TerminalButton(text = action, onClick = onAction)
        }
    }
}
```

- [ ] **Step 2: Delete old PipBoyComponents.kt**

```bash
rm app/src/main/java/com/nomad/android/ui/components/PipBoyComponents.kt
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nomad/android/ui/components/TerminalComponents.kt app/src/main/java/com/nomad/android/ui/components/PipBoyComponents.kt
git commit -m "refactor: rename PipBoyComponents to TerminalComponents with refined design"
```

---

### Task 6: App Shell — Remove Scaffold, Restructure Layout

**Files:**
- Modify: `app/src/main/java/com/nomad/android/NomadApp.kt`

- [ ] **Step 1: Rewrite NomadApp.kt without Scaffold**

Replace the entire file contents with:

```kotlin
package com.nomad.android

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nomad.android.ui.onboarding.OnboardingScreen
import com.nomad.android.ui.onboarding.OnboardingViewModel
import com.nomad.android.ui.theme.CrtScreen
import com.nomad.android.ui.theme.NomadTheme
import com.nomad.android.ui.theme.TerminalStatusBar
import com.nomad.android.ui.theme.TerminalBottomNav
import com.nomad.android.ui.navigation.NomadNavHost

@Composable
fun NomadApp() {
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val isOnboardingComplete by onboardingViewModel.isOnboardingComplete.collectAsStateWithLifecycle(initialValue = false)
    val onboardingState by onboardingViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

    NomadTheme {
        CrtScreen {
            if (!isOnboardingComplete) {
                OnboardingScreen(viewModel = onboardingViewModel)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                ) {
                    TerminalStatusBar(
                        isAiOnline = onboardingState.data.selectedModel.isNotEmpty(),
                        storagePercent = onboardingState.data.hardwareInfo?.let {
                            val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
                            val total = stat.blockCountLong * stat.blockSizeLong
                            val avail = stat.availableBlocksLong * stat.blockSizeLong
                            if (total > 0) ((total - avail).toFloat() / total.toFloat() * 100).toInt() else 0
                        } ?: 0,
                    )
                    NomadNavHost(navController = navController, modifier = Modifier.weight(1f))
                    TerminalBottomNav(navController = navController, currentRoute = currentRoute)
                }
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/nomad/android/NomadApp.kt
git commit -m "refactor: remove Scaffold, use CrtScreen as root with full-bleed layout"
```

---

### Task 7: Update NavHost imports

**Files:**
- Modify: `app/src/main/java/com/nomad/android/ui/navigation/NavHost.kt`

- [ ] **Step 1: Update imports in NavHost.kt**

No structural changes needed — screens still exist at same paths. The NavHost itself doesn't import from theme/components directly. This task is a no-op since NavHost.kt only imports screen composables and Routes.

- [ ] **Step 1: Verify build compiles**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```

Expected: Should fail at this point since screens still reference old component names. This is expected — we'll fix screens in subsequent tasks.

---

### Task 8: Dashboard Screen

**Files:**
- Modify: `app/src/main/java/com/nomad/android/ui/dashboard/DashboardScreen.kt`

- [ ] **Step 1: Rewrite DashboardScreen.kt with new components, spacing, and no Pip-Boy references**

Replace the entire file contents with:

```kotlin
package com.nomad.android.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.nomad.android.R
import com.nomad.android.ui.components.TerminalCard
import com.nomad.android.ui.components.TerminalDivider
import com.nomad.android.ui.components.TerminalEmptyScreen
import com.nomad.android.ui.components.TerminalErrorScreen
import com.nomad.android.ui.components.TerminalListTile
import com.nomad.android.ui.components.TerminalLoadingScreen
import com.nomad.android.ui.components.TerminalProgressBar
import com.nomad.android.ui.components.TerminalSectionHeader
import com.nomad.android.ui.components.TerminalStatusIndicator
import com.nomad.android.ui.components.TerminalText
import com.nomad.android.ui.navigation.Routes
import com.nomad.android.ui.theme.TerminalBg
import com.nomad.android.ui.theme.TerminalGreen
import com.nomad.android.ui.theme.TerminalGreenDim

private data class QuickAccessItem(
    val title: String,
    val subtitle: String,
    val route: String,
)

@Composable
fun DashboardScreen(
    navController: NavHostController,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> TerminalLoadingScreen("SCANNING SYSTEMS...")
        uiState.error != null -> TerminalErrorScreen(message = uiState.error ?: "Unknown error", onRetry = { viewModel.refreshStatus() })
        else -> DashboardContent(
            data = uiState.data,
            navController = navController,
            onRefresh = { viewModel.refreshStatus() },
        )
    }
}

@Composable
private fun DashboardContent(
    data: DashboardData,
    navController: NavHostController,
    onRefresh: () -> Unit,
) {
    val quickAccessItems = remember {
        listOf(
            QuickAccessItem("OFFLINE MAPS", "3 regions loaded", Routes.MAPS),
            QuickAccessItem("ARCHIVES", "2.1 GB", Routes.KNOWLEDGE),
            QuickAccessItem("AI TERMINAL", "Gemma 4 E2B", Routes.CHAT),
            QuickAccessItem("EMERGENCY", "Always ready", Routes.EMERGENCY),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TerminalText(
            text = "NOMAD SURVIVAL SYSTEM",
            color = TerminalGreen,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 20.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
                ),
            ),
        )

        TerminalSectionHeader(text = "System Status")

        TerminalCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusRow("AI Engine", data.aiStatus?.modelName ?: "N/A", data.aiStatus?.isReady ?: false)
                StatusRow("Storage", "${data.storageMetrics?.usedPercent ?: 0}%", true)
                StatusRow("Content Packs", "${data.contentPackCount} loaded", true)
            }
        }

        TerminalSectionHeader(text = "Quick Access")

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                quickAccessItems.take(2).forEach { item ->
                    QuickAccessCard(
                        item = item,
                        onClick = { navController.navigate(item.route) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                quickAccessItems.drop(2).forEach { item ->
                    QuickAccessCard(
                        item = item,
                        onClick = { navController.navigate(item.route) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (data.recentActivity.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TerminalSectionHeader(text = "Recent Activity")

                data.recentActivity.forEach { activity ->
                    TerminalListTile(
                        title = activity,
                        subtitle = null,
                        onClick = {},
                    )
                }
            }
        } else {
            TerminalEmptyScreen(message = "No recent activity")
        }

        TerminalCard(header = "Storage") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TerminalProgressBar(progress = data.storageMetrics?.usedPercent?.div(100f) ?: 0f)
                TerminalText(
                    text = "${data.storageMetrics?.usedBytes?.div(1_000_000_000) ?: 0} GB / ${data.storageMetrics?.totalBytes?.div(1_000_000_000) ?: 0} used",
                    color = TerminalGreenDim,
                    style = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                )
            }
        }

        TerminalCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { navController.navigate(Routes.SETTINGS) },
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TerminalText(
                    text = "System Settings",
                    color = TerminalGreen,
                    style = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                )
                Text(
                    text = ">",
                    color = TerminalGreen,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String, isOnline: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TerminalStatusIndicator(label = label, isOnline = isOnline)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            color = TerminalGreen,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            ),
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun QuickAccessCard(
    item: QuickAccessItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TerminalCard(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.title,
                color = TerminalGreen,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                ),
                fontSize = 14.sp,
            )
            Text(
                text = item.subtitle,
                color = TerminalGreenDim,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                ),
                fontSize = 12.sp,
            )
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/nomad/android/ui/dashboard/DashboardScreen.kt
git commit -m "refactor: update DashboardScreen with Terminal components and sectioned layout"
```

---

### Task 9: Maps Screen

**Files:**
- Modify: `app/src/main/java/com/nomad/android/ui/maps/MapsScreen.kt`

- [ ] **Step 1: Rewrite MapsScreen.kt with new components and TerminalTextField**

Replace the entire file contents with:

```kotlin
package com.nomad.android.ui.maps

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nomad.android.R
import com.nomad.android.data.local.entity.LocationSavedPointEntity
import com.nomad.android.ui.components.TerminalButton
import com.nomad.android.ui.components.TerminalButtonVariant
import com.nomad.android.ui.components.TerminalCard
import com.nomad.android.ui.components.TerminalDivider
import com.nomad.android.ui.components.TerminalEmptyScreen
import com.nomad.android.ui.components.TerminalErrorScreen
import com.nomad.android.ui.components.TerminalListTile
import com.nomad.android.ui.components.TerminalLoadingScreen
import com.nomad.android.ui.components.TerminalSectionHeader
import com.nomad.android.ui.components.TerminalText
import com.nomad.android.ui.components.TerminalTextField
import com.nomad.android.ui.theme.TerminalAmber
import com.nomad.android.ui.theme.TerminalBg
import com.nomad.android.ui.theme.TerminalGreen
import com.nomad.android.ui.theme.TerminalGreenDim
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MapsScreen(
    viewModel: MapsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions.values.all { it }
        viewModel.setLocationPermissionGranted(granted)
    }

    when {
        uiState.isLoading -> TerminalLoadingScreen("LOADING CARTOGRAPHY...")
        uiState.error != null -> TerminalErrorScreen(
            message = uiState.error ?: "Unknown error",
            onRetry = { viewModel.loadMapData() },
        )
        !uiState.data.isMapInitialized -> TerminalEmptyScreen(
            message = "Map module not initialized",
            action = "INITIALIZE",
            onAction = { viewModel.loadMapData() },
        )
        else -> MapsContent(
            data = uiState.data,
            onToggleLayer = { viewModel.toggleLayer(it) },
            hasPermission = uiState.data.hasLocationPermission,
            onRequestPermission = {
                locationPermissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            },
            onStartTracking = { viewModel.startTracking() },
            onStopTracking = { viewModel.stopTracking() },
            onSaveLocation = { name, notes -> viewModel.saveLocation(name, notes) },
            onDeletePoint = { id -> viewModel.deleteSavedPoint(id) },
            onRefreshLocation = { viewModel.requestCurrentLocation() },
        )
    }
}

@Composable
private fun MapsContent(
    data: MapsData,
    onToggleLayer: (String) -> Unit,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit,
    onSaveLocation: (String, String) -> Unit,
    onDeletePoint: (String) -> Unit,
    onRefreshLocation: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TerminalText(
            text = "Offline Cartography",
            color = TerminalGreen,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 20.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
                ),
            ),
        )

        if (!hasPermission) {
            TerminalCard(header = "Location Permission Required") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TerminalText(
                        text = "GPS access is needed for coordinates and tracking",
                        color = TerminalAmber,
                        style = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    )
                    TerminalButton(text = "Grant Permission", onClick = onRequestPermission)
                }
            }
        }

        TerminalCard(header = "GPS Coordinates") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TerminalText(
                        text = data.currentLocationText,
                        color = if (data.currentLocationText == "NO FIX") TerminalGreenDim else TerminalGreen,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = if (data.currentLocationText == "NO FIX") 14.sp else 18.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily(
                                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                            ),
                        ),
                    )
                    if (hasPermission) {
                        TerminalButton(
                            text = "Refresh",
                            onClick = onRefreshLocation,
                            modifier = Modifier.width(100.dp),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val statusIcon = if (hasPermission && data.currentLocationText != "NO FIX") "OK" else "--"
                    TerminalText(
                        text = "$statusIcon ${data.currentLatitude ?: "---"}",
                        color = TerminalGreenDim,
                        style = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                    )
                    TerminalText(
                        text = "$statusIcon ${data.currentLongitude ?: "---"}",
                        color = TerminalGreenDim,
                        style = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                    )
                }
            }
        }

        if (hasPermission) {
            TerminalCard(header = "Location Tracking") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val statusText = if (data.isTracking) "TRACKING (60s)" else "IDLE"
                        val statusColor = if (data.isTracking) TerminalGreen else TerminalGreenDim
                        TerminalText(
                            text = statusText,
                            color = statusColor,
                            style = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                        )
                        TerminalText(
                            text = "${data.snapshotCount} snapshots",
                            color = TerminalGreenDim,
                            style = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (data.isTracking) {
                            TerminalButton(
                                text = "Stop Tracking",
                                onClick = onStopTracking,
                                variant = TerminalButtonVariant.DANGER,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            TerminalButton(
                                text = "Start Tracking",
                                onClick = onStartTracking,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            TerminalCard(header = "Save Location") {
                var saveName by remember { mutableStateOf("") }
                var saveNotes by remember { mutableStateOf("") }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TerminalTextField(
                        value = saveName,
                        onValueChange = { saveName = it },
                        placeholder = "Location name",
                        label = "Name",
                        singleLine = true,
                    )
                    TerminalTextField(
                        value = saveNotes,
                        onValueChange = { saveNotes = it },
                        placeholder = "Notes (optional)",
                        label = "Notes",
                        singleLine = false,
                    )
                    TerminalButton(
                        text = "Save Current Position",
                        onClick = {
                            if (saveName.isNotBlank()) {
                                onSaveLocation(saveName.trim(), saveNotes.trim())
                                saveName = ""
                                saveNotes = ""
                            }
                        },
                        enabled = saveName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        size = com.nomad.android.ui.components.TerminalButtonSize.LARGE,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            data.layers.forEach { layer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleLayer(layer.id) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val selector = if (layer.isEnabled) "[X]" else "[ ]"
                    Text(
                        text = "$selector ${layer.name.uppercase()}",
                        color = if (layer.isEnabled) TerminalGreen else TerminalGreenDim,
                        fontFamily = androidx.compose.ui.text.font.FontFamily(
                            androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                        ),
                        fontSize = 12.sp,
                    )
                }
            }
        }

        if (hasPermission && data.savedPoints.isNotEmpty()) {
            TerminalCard(header = "Saved Locations (${data.savedPoints.size})") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    data.savedPoints.forEach { point ->
                        SavedLocationRow(point = point, onDelete = onDeletePoint)
                    }
                }
            }
        } else if (hasPermission) {
            TerminalEmptyScreen(message = "No saved locations yet — save one above")
        }

        Text(
            text = "Region: ${data.regionName ?: "Not selected"} | ${if (data.hasOfflineTiles) "Tiles loaded" else "0 tiles loaded"}",
            color = TerminalGreenDim,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            ),
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun SavedLocationRow(
    point: LocationSavedPointEntity,
    onDelete: (String) -> Unit,
) {
    val df = remember { SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()) }
    val timeStr = df.format(Date(point.timestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = point.name.uppercase(),
                    color = TerminalGreen,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                    ),
                    fontSize = 13.sp,
                )
                Text(
                    text = "${"%.6f".format(point.latitude)}, ${"%.6f".format(point.longitude)}",
                    color = TerminalGreenDim,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 11.sp,
                )
                Text(
                    text = "$timeStr | Alt: ${"%.1f".format(point.altitude)}m",
                    color = TerminalGreenDim.copy(alpha = 0.7f),
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 10.sp,
                )
                if (point.notes.isNotBlank()) {
                    Text(
                        text = "Note: ${point.notes}",
                        color = TerminalAmber.copy(alpha = 0.8f),
                        fontFamily = androidx.compose.ui.text.font.FontFamily(
                            androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                        ),
                        fontSize = 10.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            TerminalButton(
                text = "Del",
                onClick = { onDelete(point.id) },
                variant = TerminalButtonVariant.DANGER,
                size = com.nomad.android.ui.components.TerminalButtonSize.SMALL,
            )
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/nomad/android/ui/maps/MapsScreen.kt
git commit -m "refactor: update MapsScreen with Terminal components and TextField"
```

---

### Task 10: Knowledge Screen

**Files:**
- Modify: `app/src/main/java/com/nomad/android/ui/knowledge/KnowledgeScreen.kt`

- [ ] **Step 1: Rewrite KnowledgeScreen.kt**

Replace the entire file contents with:

```kotlin
package com.nomad.android.ui.knowledge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState as rememberHScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.nomad.android.R
import com.nomad.android.ui.components.TerminalDivider
import com.nomad.android.ui.components.TerminalEmptyScreen
import com.nomad.android.ui.components.TerminalErrorScreen
import com.nomad.android.ui.components.TerminalLoadingScreen
import com.nomad.android.ui.components.TerminalText
import com.nomad.android.ui.components.TerminalTextField
import com.nomad.android.ui.theme.TerminalAmber
import com.nomad.android.ui.theme.TerminalBg
import com.nomad.android.ui.theme.TerminalGreen
import com.nomad.android.ui.theme.TerminalGreenDim
import com.nomad.android.ui.theme.TerminalSurface
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun KnowledgeScreen(
    viewModel: KnowledgeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> TerminalLoadingScreen("INDEXING ARCHIVES...")
        uiState.error != null -> TerminalErrorScreen(message = uiState.error ?: "Unknown error", onRetry = { viewModel.loadArticles() })
        uiState.data.articles.isEmpty() -> TerminalEmptyScreen(
            message = "No articles available. Download content packs in Settings.",
        )
        uiState.data.filteredArticles.isEmpty() && uiState.data.searchQuery.isNotBlank() -> TerminalEmptyScreen(
            message = "No articles match '${uiState.data.searchQuery}'",
        )
        uiState.data.filteredArticles.isEmpty() -> TerminalEmptyScreen(
            message = "No articles in this category. Download content packs in Settings.",
        )
        else -> KnowledgeContent(
            data = uiState.data,
            onSearch = { viewModel.search(it) },
            onSelectCategory = { viewModel.selectCategory(it) },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
        )
    }
}

@Composable
private fun KnowledgeContent(
    data: KnowledgeData,
    onSearch: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    var searchText by remember { mutableStateOf("") }

    LaunchedEffect(searchText) {
        delay(300)
        onSearch(searchText)
    }

    val categoryColor = mapOf(
        "Survival" to TerminalGreen,
        "First Aid" to TerminalAmber,
        "Navigation" to Color(0xFF64B5F6),
        "Shelter" to Color(0xFFCE93D8),
        "All" to TerminalGreen,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TerminalText(
            text = "Offline Knowledge Base",
            color = TerminalGreen,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 20.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
                ),
            ),
        )

        TerminalTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = "Search archives...",
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberHScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            data.categories.forEach { category ->
                val isSelected = category == data.selectedCategory
                val bgColor = if (isSelected) TerminalGreen else Color.Transparent
                val textColor = if (isSelected) TerminalBg else TerminalGreenDim
                val borderColor = if (isSelected) TerminalGreen else TerminalGreenDim

                Box(
                    modifier = Modifier
                        .border(2.dp, borderColor, RoundedCornerShape(6.dp))
                        .background(bgColor, RoundedCornerShape(6.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelectCategory(category) },
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = category,
                        color = textColor,
                        fontFamily = androidx.compose.ui.text.font.FontFamily(
                            androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                        ),
                        fontSize = 12.sp,
                    )
                }
            }
        }

        var expandedArticleId by remember { mutableStateOf<String?>(null) }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(data.filteredArticles, key = { it.id }) { article ->
                val isExpanded = expandedArticleId == article.id

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isExpanded) TerminalSurface else Color.Transparent,
                            RoundedCornerShape(6.dp),
                        )
                        .then(
                            if (isExpanded) Modifier.border(2.dp, TerminalGreenDim, RoundedCornerShape(6.dp))
                            else Modifier,
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                expandedArticleId = if (isExpanded) null else article.id
                            },
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = article.category,
                                    color = categoryColor[article.category] ?: TerminalGreen,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                                    ),
                                    fontSize = 11.sp,
                                )
                                if (article.isFavorite) {
                                    Text(
                                        text = "★",
                                        color = TerminalAmber,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily(
                                            androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                                        ),
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = article.title,
                                color = TerminalGreen,
                                fontFamily = androidx.compose.ui.text.font.FontFamily(
                                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                                ),
                                fontSize = 14.sp,
                            )
                        }
                        Text(
                            text = if (isExpanded) "[-]" else "[+]",
                            color = TerminalGreen,
                            fontFamily = androidx.compose.ui.text.font.FontFamily(
                                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                            ),
                            fontSize = 14.sp,
                        )
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(TerminalGreenDim.copy(alpha = 0.3f)),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = article.content,
                                color = TerminalAmber,
                                fontFamily = androidx.compose.ui.text.font.FontFamily(
                                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                                ),
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "${data.filteredArticles.size} / ${data.articles.size} articles",
            color = TerminalGreenDim,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            ),
            fontSize = 11.sp,
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/nomad/android/ui/knowledge/KnowledgeScreen.kt
git commit -m "refactor: update KnowledgeScreen with Terminal components"
```

---

### Task 11: Chat Screen

**Files:**
- Modify: `app/src/main/java/com/nomad/android/ui/chat/ChatScreen.kt`

- [ ] **Step 1: Rewrite ChatScreen.kt**

Replace the entire file contents with:

```kotlin
package com.nomad.android.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nomad.android.R
import com.nomad.android.ui.components.TerminalButton
import com.nomad.android.ui.components.TerminalCard
import com.nomad.android.ui.components.TerminalDivider
import com.nomad.android.ui.components.TerminalEmptyScreen
import com.nomad.android.ui.components.TerminalErrorScreen
import com.nomad.android.ui.components.TerminalLoadingScreen
import com.nomad.android.ui.components.TerminalText
import com.nomad.android.ui.components.TerminalTextField
import com.nomad.android.ui.theme.TerminalAmber
import com.nomad.android.ui.theme.TerminalBg
import com.nomad.android.ui.theme.TerminalGreen
import com.nomad.android.ui.theme.TerminalGreenDim
import com.nomad.android.ui.theme.TerminalSurface

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> TerminalLoadingScreen("ESTABLISHING NEURAL LINK...")
        uiState.error != null -> TerminalErrorScreen(
            message = uiState.error ?: "Unknown error",
            onRetry = { viewModel.loadRecentSessions() },
        )
        uiState.data.messages.isEmpty() && uiState.data.currentSessionId == null -> {
            TerminalEmptyScreen(
                message = "No active session. Start a new conversation.",
                action = "New Session",
                onAction = { viewModel.newSession() },
            )
        }
        else -> ChatContent(
            data = uiState.data,
            onSendMessage = { viewModel.sendMessage(it) },
            onNewSession = { viewModel.newSession() },
            onSelectFilter = { viewModel.selectFilter(it) },
        )
    }
}

@Composable
private fun ChatContent(
    data: ChatData,
    onSendMessage: (String) -> Unit,
    onNewSession: () -> Unit,
    onSelectFilter: (String) -> Unit,
) {
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            TerminalText(
                text = data.sessions.firstOrNull()?.title ?: "AI Terminal",
                color = TerminalGreen,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 18.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
                    ),
                ),
            )
        }

        TerminalDivider(modifier = Modifier.padding(bottom = 4.dp))

        if (data.messages.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No messages — start a new session",
                    color = TerminalGreenDim,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 13.sp,
                )
            }
        } else {
            val listState = rememberLazyListState()

            LaunchedEffect(data.messages.size, data.isStreaming) {
                if (data.messages.isNotEmpty()) {
                    val targetIndex = data.messages.size - 1 + if (data.isStreaming) 1 else 0
                    listState.animateScrollToItem(targetIndex)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(data.messages, key = { "${it.sessionId}_${it.timestamp}_${it.role}" }) { message ->
                    MessageBubble(
                        isUser = message.role == "user",
                        text = message.content,
                    )
                }
                if (data.isStreaming) {
                    item { TypingIndicator() }
                }
            }
        }

        Column {
            TerminalDivider(color = TerminalGreen)

            Spacer(modifier = Modifier.height(8.dp))

            ContextFilterRow(
                filters = data.contextFilters,
                selectedFilter = data.selectedFilter,
                onFilterSelected = onSelectFilter,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TerminalTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = "Enter query...",
                    singleLine = true,
                )

                Spacer(modifier = Modifier.width(8.dp))

                TerminalButton(
                    text = "Send",
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !data.isStreaming,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MessageBubble(isUser: Boolean, text: String) {
    val borderColor = if (isUser) TerminalGreen else TerminalAmber
    val prefix = if (isUser) "QUERY" else "RESPONSE"
    val prefixColor = if (isUser) TerminalGreen else TerminalAmber

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TerminalSurface, RoundedCornerShape(6.dp))
            .drawBehind {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 3.dp.toPx(),
                )
            }
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Text(
            text = prefix,
            color = prefixColor,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
            ),
            fontSize = 10.sp,
            letterSpacing = 1.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            color = if (isUser) TerminalGreen else TerminalAmber,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            ),
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun TypingIndicator() {
    var dotCount by remember { mutableStateOf(1) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(400)
            dotCount = (dotCount % 3) + 1
        }
    }

    val dots = ".".repeat(dotCount)

    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Processing$dots",
            color = TerminalAmber,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
            ),
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ContextFilterRow(
    filters: List<String>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        filters.forEach { label ->
            val isSelected = label == selectedFilter
            val borderColor = if (isSelected) TerminalGreen else TerminalGreenDim
            val bgColor = if (isSelected) TerminalGreen.copy(alpha = 0.15f) else TerminalSurface
            val textColor = if (isSelected) TerminalGreen else TerminalGreenDim

            Box(
                modifier = Modifier
                    .border(2.dp, borderColor, RoundedCornerShape(6.dp))
                    .background(bgColor, RoundedCornerShape(6.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onFilterSelected(label) },
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = textColor,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 11.sp,
                )
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/nomad/android/ui/chat/ChatScreen.kt
git commit -m "refactor: update ChatScreen with Terminal components"
```

---

### Task 12: Emergency Screen

**Files:**
- Modify: `app/src/main/java/com/nomad/android/ui/emergency/EmergencyScreen.kt`

- [ ] **Step 1: Rewrite EmergencyScreen.kt**

Replace the entire file contents with:

```kotlin
package com.nomad.android.ui.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nomad.android.R
import com.nomad.android.ui.components.TerminalCard
import com.nomad.android.ui.components.TerminalDivider
import com.nomad.android.ui.components.TerminalSectionHeader
import com.nomad.android.ui.components.TerminalText
import com.nomad.android.ui.theme.TerminalAmber
import com.nomad.android.ui.theme.TerminalBg
import com.nomad.android.ui.theme.TerminalGreen
import com.nomad.android.ui.theme.TerminalGreenDim
import com.nomad.android.ui.theme.TerminalSurface

private data class FirstAidTopic(
    val title: String,
    val steps: List<String>,
)

private val firstAidTopics = listOf(
    FirstAidTopic("CPR", listOf(
        "Check responsiveness — tap shoulders, shout",
        "Call emergency services if possible",
        "Place heel of hand on center of chest",
        "Push hard and fast: 100-120 compressions/min",
        "Depth: at least 2 inches for adults",
        "Give 2 rescue breaths every 30 compressions if trained",
        "Continue until help arrives or person recovers",
    )),
    FirstAidTopic("Severe Bleeding", listOf(
        "Apply direct pressure with clean cloth",
        "Elevate wound above heart if possible",
        "Apply pressure bandage firmly",
        "Do NOT remove soaked cloths — add more on top",
        "Apply tourniquet only as last resort (limb injuries)",
        "Keep victim warm and calm",
        "Seek medical help immediately",
    )),
    FirstAidTopic("Burns", listOf(
        "Cool burn under running water for 20 minutes",
        "Remove jewelry/clothing near burn (not stuck fabric)",
        "Cover with clean, non-stick dressing",
        "Do NOT apply ice, butter, or ointments",
        "Do NOT pop blisters",
        "For chemical burns: flush with water for 20+ minutes",
        "Seek medical help for large or deep burns",
    )),
    FirstAidTopic("Fractures", listOf(
        "Do NOT move the injured limb",
        "Immobilize with splint (rigid material + padding)",
        "Splint should extend beyond joints above and below fracture",
        "Apply cold pack wrapped in cloth (20 min on, 20 min off)",
        "Elevate if possible to reduce swelling",
        "Check circulation below injury (pulse, color, warmth)",
        "Seek medical help — do not attempt to realign bone",
    )),
    FirstAidTopic("Shock", listOf(
        "Lay person flat on their back",
        "Elevate legs 12 inches (unless head/neck/back injury)",
        "Keep warm with blanket or clothing",
        "Do NOT give food or water",
        "Loosen tight clothing",
        "Turn on side if vomiting or bleeding from mouth",
        "Begin CPR if no breathing — seek help immediately",
    )),
    FirstAidTopic("Choking", listOf(
        "Ask: 'Are you choking?' — if they can't speak, act",
        "Give 5 back blows between shoulder blades",
        "Give 5 abdominal thrusts (Heimlich maneuver)",
        "Alternate 5 back blows and 5 thrusts",
        "For infants: face down, 5 back blows, then chest thrusts",
        "If person becomes unconscious: begin CPR",
        "Call emergency services as soon as possible",
    )),
    FirstAidTopic("Hypothermia", listOf(
        "Move to warm, dry shelter",
        "Remove wet clothing",
        "Warm gradually with blankets, body heat, warm drinks",
        "Do NOT rewarm too quickly (no hot water)",
        "Do NOT give alcohol",
        "Warm core first (chest, neck, head, groin)",
        "Seek medical help — hypothermia is life-threatening",
    )),
    FirstAidTopic("Snake Bite", listOf(
        "Stay calm — keep heart rate low",
        "Immobilize bitten limb, keep below heart level",
        "Remove rings, watches, tight clothing",
        "Clean wound gently with soap and water",
        "Cover with clean, dry dressing",
        "Do NOT cut wound, suck venom, or apply tourniquet",
        "Seek medical help — note snake appearance if possible",
    )),
)

private data class ChecklistCategory(
    val name: String,
    val items: List<Pair<String, Boolean>>,
)

private val survivalChecklist = listOf(
    ChecklistCategory("Water", listOf(
        "Water bottles or containers" to false,
        "Water purification tablets" to false,
        "Portable water filter" to false,
        "Metal pot for boiling" to false,
    )),
    ChecklistCategory("Food", listOf(
        "Non-perishable food (3-day supply)" to false,
        "Manual can opener" to false,
        "High-energy snacks (nuts, bars)" to false,
        "Fishing kit or snares" to false,
    )),
    ChecklistCategory("Shelter", listOf(
        "Emergency blanket or tarp" to false,
        "Rope or paracord (50ft minimum)" to false,
        "Duct tape" to false,
        "Knife or multi-tool" to false,
    )),
    ChecklistCategory("First Aid", listOf(
        "Bandages and gauze" to false,
        "Antiseptic wipes" to false,
        "Pain relievers" to false,
        "Personal medications" to false,
    )),
    ChecklistCategory("Navigation", listOf(
        "Physical map of area" to false,
        "Compass" to false,
        "Whistle (signal)" to false,
        "Flashlight with extra batteries" to false,
    )),
    ChecklistCategory("Fire", listOf(
        "Waterproof matches or lighter" to false,
        "Fire starter (ferro rod, tinder)" to false,
        "Candle (fire starter + light)" to false,
    )),
)

@Composable
fun EmergencyScreen() {
    var expandedTopic by remember { mutableStateOf<String?>(null) }
    var checkedItems by rememberSaveable { mutableStateOf(survivalChecklist) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TerminalText(
            text = "Survival Reference",
            color = TerminalAmber,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 20.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
                ),
            ),
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                TerminalSectionHeader(text = "First Aid — Conditions A-Z")
            }

            items(firstAidTopics) { topic ->
                val isExpanded = expandedTopic == topic.title
                FirstAidRow(
                    topic = topic,
                    isExpanded = isExpanded,
                    onToggle = { expandedTopic = if (isExpanded) null else topic.title },
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                TerminalSectionHeader(text = "Survival Checklist")
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(checkedItems) { checklist ->
                ChecklistSection(
                    category = checklist.name,
                    items = checklist.items,
                    onToggle = { itemIndex ->
                        checkedItems = checkedItems.map { c ->
                            if (c.name == checklist.name) {
                                c.copy(items = c.items.mapIndexed { i, pair ->
                                    if (i == itemIndex) pair.first to !pair.second else pair
                                })
                            } else c
                        }
                    },
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                TerminalCard {
                    Text(
                        text = "All content available offline — no network required",
                        color = TerminalGreenDim,
                        fontFamily = androidx.compose.ui.text.font.FontFamily(
                            androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                        ),
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun FirstAidRow(
    topic: FirstAidTopic,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            )
            .background(TerminalSurface, RoundedCornerShape(6.dp))
            .border(2.dp, TerminalAmber, RoundedCornerShape(6.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = topic.title.uppercase(),
                color = TerminalAmber,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                ),
                fontSize = 13.sp,
            )
            Text(
                text = if (isExpanded) "[-]" else "[+]",
                color = TerminalGreen,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                ),
                fontSize = 12.sp,
            )
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(4.dp))
            topic.steps.forEachIndexed { i, step ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "${i + 1}.",
                        color = TerminalGreenDim,
                        fontFamily = androidx.compose.ui.text.font.FontFamily(
                            androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                        ),
                        fontSize = 11.sp,
                    )
                    Text(
                        text = step,
                        color = TerminalGreen,
                        fontFamily = androidx.compose.ui.text.font.FontFamily(
                            androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                        ),
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChecklistSection(
    category: String,
    items: List<Pair<String, Boolean>>,
    onToggle: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = category.uppercase(),
            color = TerminalGreen,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
            ),
            fontSize = 13.sp,
        )
        items.forEachIndexed { index, (item, checked) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onToggle(index) },
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val checkbox = if (checked) "[X]" else "[ ]"
                val color = if (checked) TerminalGreen else TerminalGreenDim
                Text(
                    text = checkbox,
                    color = color,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 12.sp,
                )
                Text(
                    text = item,
                    color = color,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 12.sp,
                )
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/nomad/android/ui/emergency/EmergencyScreen.kt
git commit -m "refactor: update EmergencyScreen with Terminal components"
```

---

### Task 13: Settings Screen

**Files:**
- Modify: `app/src/main/java/com/nomad/android/ui/settings/SettingsScreen.kt`

- [ ] **Step 1: Rewrite SettingsScreen.kt with consistent spacing and no Pip-Boy refs**

Replace the entire file contents with:

```kotlin
package com.nomad.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nomad.android.R
import com.nomad.android.ui.components.TerminalButton
import com.nomad.android.ui.components.TerminalButtonVariant
import com.nomad.android.ui.components.TerminalCard
import com.nomad.android.ui.components.TerminalDivider
import com.nomad.android.ui.components.TerminalProgressBar
import com.nomad.android.ui.components.TerminalSectionHeader
import com.nomad.android.ui.components.TerminalText
import com.nomad.android.ui.theme.TerminalAmber
import com.nomad.android.ui.theme.TerminalBg
import com.nomad.android.ui.theme.TerminalDanger
import com.nomad.android.ui.theme.TerminalGreen
import com.nomad.android.ui.theme.TerminalGreenDim

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TerminalText(
            text = "System Configuration",
            color = TerminalGreen,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 20.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
                ),
            ),
        )

        if (uiState.error != null) {
            TerminalCard {
                Text(
                    text = "Error: ${uiState.error}",
                    color = TerminalDanger,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 12.sp,
                )
            }
        }

        TerminalCard(header = "AI Engine") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Engine: ${uiState.data.aiStatus?.modelName ?: "N/A"}",
                    color = TerminalGreen,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 12.sp,
                )
                Text(
                    text = "Type: ${uiState.data.aiStatus?.engineType?.displayName ?: "Unknown"}",
                    color = TerminalGreen,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 12.sp,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .border(1.dp, if (uiState.data.aiStatus?.isReady == true) TerminalGreen else TerminalGreenDim),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Status: ${if (uiState.data.aiStatus?.isReady == true) "Ready" else "Standby"}",
                        color = if (uiState.data.aiStatus?.isReady == true) TerminalGreen else TerminalGreenDim,
                        fontFamily = androidx.compose.ui.text.font.FontFamily(
                            androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                        ),
                        fontSize = 11.sp,
                    )
                }
            }
        }

        TerminalCard(header = "Content Packs") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.data.contentPacks.forEach { pack ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = pack.name,
                                    color = if (pack.isDownloaded) TerminalGreen else TerminalGreenDim,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                                    ),
                                    fontSize = 13.sp,
                                )
                                if (pack.isDownloaded) {
                                    Text(
                                        text = "[OK]",
                                        color = TerminalGreen,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily(
                                            androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                                        ),
                                        fontSize = 10.sp,
                                    )
                                }
                            }
                            Text(
                                text = "${pack.type.uppercase()} — ${pack.size}",
                                color = TerminalGreenDim,
                                fontFamily = androidx.compose.ui.text.font.FontFamily(
                                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                                ),
                                fontSize = 11.sp,
                            )
                            if (pack.isDownloading) {
                                Spacer(modifier = Modifier.height(4.dp))
                                TerminalProgressBar(progress = pack.downloadProgress)
                                Text(
                                    text = "Downloading... ${(pack.downloadProgress * 100).toInt()}%",
                                    color = TerminalAmber,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                                    ),
                                    fontSize = 10.sp,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        when {
                            pack.isDownloading -> {}
                            pack.isDownloaded -> TerminalButton(
                                text = "Delete",
                                onClick = { viewModel.deletePack(pack.id) },
                                variant = TerminalButtonVariant.DANGER,
                                size = com.nomad.android.ui.components.TerminalButtonSize.SMALL,
                            )
                            else -> TerminalButton(
                                text = "Get",
                                onClick = { viewModel.downloadPack(pack.id) },
                                size = com.nomad.android.ui.components.TerminalButtonSize.SMALL,
                            )
                        }
                    }
                    if (pack != uiState.data.contentPacks.last()) {
                        TerminalDivider(color = TerminalGreenDim.copy(alpha = 0.3f))
                    }
                }
            }
        }

        TerminalCard(header = "Storage") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TerminalProgressBar(
                    progress = uiState.data.storageMetrics?.usedPercent?.div(100f) ?: 0f,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "${uiState.data.storageMetrics?.usedBytes?.div(1_000_000_000) ?: 0} GB / ${uiState.data.storageMetrics?.totalBytes?.div(1_000_000_000) ?: 0} used",
                    color = TerminalGreenDim,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 11.sp,
                )
            }
        }

        TerminalCard(header = "Display") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                viewModel.availableThemes.forEach { theme ->
                    val isSelected = uiState.data.currentTheme == theme.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setTheme(theme.id) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val selectorChar = if (isSelected) "[X]" else "[ ]"
                        Text(
                            text = "$selectorChar ${theme.name}",
                            color = if (isSelected) TerminalGreen else TerminalGreenDim,
                            fontFamily = androidx.compose.ui.text.font.FontFamily(
                                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                            ),
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }

        TerminalCard(header = "About") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "NOMAD v1.0.0",
                    color = TerminalGreen,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                    ),
                    fontSize = 13.sp,
                )
                Text(
                    text = "Offline-first survival knowledge",
                    color = TerminalGreenDim,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 11.sp,
                )
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/nomad/android/ui/settings/SettingsScreen.kt
git commit -m "refactor: update SettingsScreen with Terminal components and consistent spacing"
```

---

### Task 14: Onboarding Screen

**Files:**
- Modify: `app/src/main/java/com/nomad/android/ui/onboarding/OnboardingScreen.kt`

- [ ] **Step 1: Rewrite OnboardingScreen.kt — remove hardcoded colors, use theme colors, remove Pip-Boy/Vault-Tec refs**

Replace the entire file contents with:

```kotlin
package com.nomad.android.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nomad.android.R
import com.nomad.android.ui.components.TerminalButton
import com.nomad.android.ui.components.TerminalProgressBar
import com.nomad.android.ui.theme.TerminalAmber
import com.nomad.android.ui.theme.TerminalBg
import com.nomad.android.ui.theme.TerminalGreen
import com.nomad.android.ui.theme.TerminalGreenDim
import com.nomad.android.ui.theme.TerminalSurface
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isComplete by viewModel.isOnboardingComplete.collectAsStateWithLifecycle(initialValue = false)

    LaunchedEffect(isComplete) {
        if (isComplete) onComplete()
    }
    if (isComplete) return

    val currentStep = uiState.data.currentStep

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg),
        contentAlignment = Alignment.Center,
    ) {
        when (currentStep) {
            0 -> BootSequenceStep { viewModel.nextStep() }
            1 -> DeviceScanStep(
                hardwareInfo = uiState.data.hardwareInfo,
                onAdvance = { viewModel.nextStep() },
            )
            2 -> ModelSelectionStep(
                selectedModel = uiState.data.selectedModel,
                onSelectModel = { viewModel.selectModel(it) },
            ) { viewModel.nextStep() }
            3 -> DownloadPackStep { viewModel.nextStep() }
            else -> WelcomeStep { viewModel.completeOnboarding() }
        }
    }
}

@Composable
private fun BootSequenceStep(onAdvance: () -> Unit) {
    val lines = listOf(
        "NOMAD SURVIVAL SYSTEM",
        "Initializing core modules...",
        "Firmware v1.0.0 loaded",
        "System check: pass",
        "Power on: [OK]",
    )

    var visibleLines by remember { mutableIntStateOf(0) }
    var currentText by remember { mutableStateOf("") }
    var charIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        for (lineIndex in lines.indices) {
            currentText = ""
            charIndex = 0
            while (charIndex < lines[lineIndex].length) {
                currentText = lines[lineIndex].substring(0, charIndex + 1)
                charIndex++
                delay(25)
            }
            currentText = lines[lineIndex]
            visibleLines = lineIndex + 1
            delay(300)
        }
        delay(1500)
        onAdvance()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        for (i in 0 until visibleLines) {
            val isLastLine = i == visibleLines - 1
            Text(
                text = if (isLastLine) currentText else lines[i],
                color = TerminalGreen,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                ),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        if (visibleLines > 0 && currentText.length < (lines.getOrNull(visibleLines - 1)?.length ?: 0)) {
            Text(
                text = "_",
                color = TerminalGreen,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                ),
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun DeviceScanStep(
    hardwareInfo: HardwareInfo?,
    onAdvance: () -> Unit,
) {
    val ramMB = hardwareInfo?.totalRamMB ?: 0
    val storageMB = hardwareInfo?.availableStorageMB ?: 0
    val storageGB = "%.1f".format(storageMB / 1024.0)
    val hasNPU = hardwareInfo?.hasNPU ?: false
    val hasGPU = hardwareInfo?.hasGPU ?: false

    val specs = listOf(
        "RAM: $ramMB MB [OK]" to 500L,
        "STORAGE: $storageGB GB AVAILABLE [OK]" to 400L,
        "AI CAPABILITY: LITERT-LM SUPPORTED [OK]" to 600L,
        "GPU: ${if (hasGPU) "DETECTED" else "NOT FOUND"} [${if (hasGPU) "OK" else "WARN"}]" to 350L,
        "NPU: ${if (hasNPU) "AVAILABLE" else "NOT AVAILABLE"} [${if (hasNPU) "OK" else "WARN"}]" to 300L,
    )

    var visibleCount by remember { mutableIntStateOf(0) }
    var headerVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(400)
        headerVisible = true
        delay(600)
        for (i in specs.indices) {
            delay(specs[i].second)
            visibleCount = i + 1
        }
        delay(1000)
        onAdvance()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        AnimatedVisibility(
            visible = headerVisible,
            enter = fadeIn(tween(300)),
        ) {
            Text(
                text = "SCANNING HARDWARE...",
                color = TerminalGreen,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                ),
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }

        for (i in 0 until visibleCount) {
            AnimatedVisibility(
                visible = i < visibleCount,
                enter = slideInVertically(tween(200)) + fadeIn(tween(200)),
            ) {
                val text = specs[i].first
                val annotated = buildAnnotatedString {
                    val okIndex = text.lastIndexOf("[OK]")
                    if (okIndex >= 0) {
                        append(text.substring(0, okIndex))
                        withStyle(SpanStyle(color = TerminalGreen)) {
                            append("[OK]")
                        }
                    } else {
                        append(text)
                    }
                }
                Text(
                    text = annotated,
                    color = TerminalGreenDim,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ModelSelectionStep(
    selectedModel: String,
    onSelectModel: (String) -> Unit,
    onAdvance: () -> Unit,
) {
    val models = remember {
        listOf(
            Triple("GEMMA 4 E2B", "2.0 GB", "On-device LLM — download in Settings after setup"),
            Triple("FALLBACK", "N/A", "Rule-based offline responses — no download required"),
        )
    }

    var selectedIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "SELECT AI MODEL:",
            color = TerminalAmber,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
            ),
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 20.dp),
        )

        models.forEachIndexed { index, (name, size, desc) ->
            val isSelected = index == selectedIndex
            val borderColor = if (isSelected) TerminalGreen else TerminalGreenDim
            val textColor = if (isSelected) TerminalGreen else TerminalGreenDim

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = borderColor,
                    )
                    .background(TerminalSurface)
                    .clickable { selectedIndex = index }
                    .padding(16.dp)
                    .padding(bottom = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        color = textColor,
                        fontFamily = androidx.compose.ui.text.font.FontFamily(
                            androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                        ),
                        fontSize = 15.sp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "— $size",
                        color = if (isSelected) TerminalAmber else TerminalGreenDim,
                        fontFamily = androidx.compose.ui.text.font.FontFamily(
                            androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                        ),
                        fontSize = 13.sp,
                    )
                    if (index == 0 && isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "[RECOMMENDED]",
                            color = TerminalAmber,
                            fontFamily = androidx.compose.ui.text.font.FontFamily(
                                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                            ),
                            fontSize = 11.sp,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    color = if (isSelected) TerminalGreenDim else TerminalGreenDim.copy(alpha = 0.4f),
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 12.sp,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        TerminalButton(
            text = "CONFIRM SELECTION",
            onClick = {
                onSelectModel(models[selectedIndex].first)
                onAdvance()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DownloadPackStep(onAdvance: () -> Unit) {
    val files = listOf(
        "first_aid.json" to 0.1f,
        "survival.json" to 0.3f,
        "maps_region.pmtiles" to 0.7f,
    )

    var progress by remember { mutableFloatStateOf(0f) }
    var visibleFiles by remember { mutableIntStateOf(0) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
        label = "downloadProgress",
    )

    LaunchedEffect(Unit) {
        for ((index, fileProgress) in files.withIndex()) {
            progress = fileProgress.second
            delay(800)
            visibleFiles = index + 1
        }
        progress = 1f
        delay(800)
        onAdvance()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "INITIAL DATA PACK",
            color = TerminalAmber,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
            ),
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Text(
            text = "Downloading essential survival content...",
            color = TerminalGreenDim,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            ),
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 20.dp),
        )

        TerminalProgressBar(
            progress = animatedProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        for (i in 0 until visibleFiles) {
            val annotated = buildAnnotatedString {
                append(files[i].first + " ")
                withStyle(SpanStyle(color = TerminalGreen)) {
                    append("[OK]")
                }
            }
            Text(
                text = annotated,
                color = TerminalGreenDim,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                ),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun WelcomeStep(onComplete: () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(800)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "NOMAD",
                color = TerminalGreen,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
                ),
                fontSize = 48.sp,
                letterSpacing = 8.sp,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SURVIVAL SYSTEM READY",
                color = TerminalAmber,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                ),
                fontSize = 14.sp,
                letterSpacing = 2.sp,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "All systems operational. Stay safe out there.",
                color = TerminalGreenDim,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                ),
                fontSize = 13.sp,
            )

            Spacer(modifier = Modifier.height(40.dp))

            TerminalButton(
                text = "GET STARTED",
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth(0.8f),
            )
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/nomad/android/ui/onboarding/OnboardingScreen.kt
git commit -m "refactor: update OnboardingScreen with theme colors, remove Pip-Boy references"
```

---

### Task 15: Build Verification & Cleanup

**Files:**
- All modified files

- [ ] **Step 1: Run full debug build**

```bash
./gradlew assembleDebug 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL. If there are compilation errors, fix them by checking import paths and component references.

- [ ] **Step 2: Run lint**

```bash
./gradlew lint 2>&1 | tail -30
```

Expected: No new errors from our changes. Pre-existing lint warnings are acceptable.

- [ ] **Step 3: Remove deprecated aliases from Color.kt (optional cleanup)**

After all screens compile with Terminal* names, the PipBoy* aliases in Color.kt can be removed. Search for any remaining `PipBoy` usage:

```bash
grep -r "PipBoy" app/src/main/java/ --include="*.kt"
```

If no results, remove the deprecated aliases from Color.kt. If results remain, those files still need updating.

- [ ] **Step 4: Final commit**

```bash
git add -A && git commit -m "chore: terminal UI redesign complete — clean retro theme, no Pip-Boy refs"
```

---

## Self-Review

### Spec Coverage Check

| Spec Section | Task |
|---|---|
| App Shell & Layout (remove Scaffold) | Task 6 |
| Typography (JetBrains Mono) | Task 2 |
| Color System Consolidation | Task 1, Task 3 |
| CRT Effects Refined | Task 4 |
| Component System (Terminal*) | Task 5 |
| Bottom Navigation (bordered cell, 6 tabs) | Task 4 |
| Status Bar (rename, remove VAULT-TEC) | Task 4 |
| Dashboard Screen | Task 8 |
| Maps Screen | Task 9 |
| Knowledge Screen | Task 10 |
| Chat Screen | Task 11 |
| Emergency Screen | Task 12 |
| Settings Screen | Task 13 |
| Onboarding Screen | Task 14 |
| Build Verification | Task 15 |

All spec sections covered.

### Placeholder Scan
No TBD, TODO, or incomplete sections in any step. All code blocks are complete.

### Type Consistency
- `TerminalButtonVariant`, `TerminalButtonSize` enums defined in Task 5, used consistently in Tasks 8-14
- `TerminalCard` with optional `header` parameter defined in Task 5, used in Tasks 8-14
- `TerminalTextField` defined in Task 5, used in Tasks 9, 11
- `TerminalSectionHeader` defined in Task 5, used in Tasks 8, 9, 12
- `TerminalBottomNav` with 6 tabs defined in Task 4, wired in Task 6
- All font references use `R.font.jetbrains_mono_*` consistently

### Non-Goals Verified
- No light theme added
- No new routes added (Settings added to existing bottom nav)
- No ViewModel or data layer changes
- No new dependencies (JetBrains Mono bundled as font resources)
