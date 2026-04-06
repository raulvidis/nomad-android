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
import androidx.navigation.NavGraph.Companion.findStartDestination
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.core.*
import com.nomad.android.R

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
                    R.font.jetbrains_mono_bold,
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
                    R.font.jetbrains_mono_regular,
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
                            R.font.jetbrains_mono_regular,
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
                            R.font.jetbrains_mono_regular,
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
                        tint = if (selected) TerminalGreen else TerminalGreenDim,
                        modifier = Modifier.size(24.dp),
                    )
                    if (selected) {
                        Text(
                            text = tab.label.uppercase(),
                            maxLines = 1,
                            fontFamily = androidx.compose.ui.text.font.FontFamily(
                                androidx.compose.ui.text.font.Font(
                                    R.font.jetbrains_mono_medium,
                                    FontWeight.Medium,
                                ),
                            ),
                            fontSize = 10.sp,
                            color = TerminalGreen,
                        )
                    }
                }
            }
        }
    }
}
