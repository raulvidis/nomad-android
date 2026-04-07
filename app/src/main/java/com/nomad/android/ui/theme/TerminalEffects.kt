package com.nomad.android.ui.theme

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Terminal
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.composed
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.nomad.android.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Modifier.scanlineOverlay(): Modifier = this.then(
    Modifier.composed {
        val color = LocalNomadColors.current.primary
        Modifier.drawBehind {
            val lineSpacing = 4.dp.toPx()
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = color.copy(alpha = 0.05f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
                y += lineSpacing
            }
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
    val colors = LocalNomadColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background.copy(alpha = 0.98f))
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
    val colors = LocalNomadColors.current
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.US) }
    var currentTime by remember { mutableStateOf(timeFormat.format(Date())) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000L)
            currentTime = timeFormat.format(Date())
        }
    }

    val statusText = "SYS_STATUS: ${if (isAiOnline) "ONLINE" else "OFFLINE"} | BAT: $storagePercent% | $currentTime"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .drawBehind {
                drawLine(
                    color = colors.primary.copy(alpha = 0.2f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Terminal,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = statusText,
                color = colors.primary,
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.space_grotesk_bold, FontWeight.Bold)),
                    fontSize = 13.sp,
                    letterSpacing = 0.05.em,
                    shadow = Shadow(
                        color = colors.primary.copy(alpha = 0.5f),
                        offset = Offset(0f, 0f),
                        blurRadius = 4f,
                    ),
                ),
            )
        }

        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = "Settings",
            tint = colors.primary,
            modifier = Modifier.size(20.dp),
        )
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
    val colors = LocalNomadColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .drawBehind {
                drawLine(
                    color = colors.primary.copy(alpha = 0.3f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2.dp.toPx(),
                )
            },
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        TerminalTabs.forEach { tab ->
            val selected = currentRoute == tab.route ||
                (tab.route == "dashboard" && currentRoute.startsWith("dashboard"))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (selected) {
                            Modifier
                                .background(colors.primary, RoundedCornerShape(0.dp))
                                .border(1.dp, colors.primary.copy(alpha = 0.4f), RoundedCornerShape(0.dp))
                        } else {
                            Modifier
                        },
                    )
                    .clickable {
                        if (currentRoute != tab.route) {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    this.saveState = true
                                }
                                this.launchSingleTop = true
                                this.restoreState = true
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
                        tint = if (selected) colors.background else colors.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = tab.label.uppercase(),
                        maxLines = 1,
                        style = TextStyle(
                            fontFamily = FontFamily(Font(R.font.space_grotesk_semi_bold, FontWeight.SemiBold)),
                            fontSize = 10.sp,
                            letterSpacing = 0.05.em,
                        ),
                        color = if (selected) colors.background else colors.primary.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}
