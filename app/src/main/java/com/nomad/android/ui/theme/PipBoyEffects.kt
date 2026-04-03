package com.nomad.android.ui.theme

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alert
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.Alert
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.animation.core.*

private val PipBoyGreen = Color(0xFF14F195)
private val PipBoyGreenDim = Color(0xFF0A7A4C)
private val PipBoyAmber = Color(0xFFFFB000)
private val PipBoyDanger = Color(0xFFFF3333)
private val PipBoyBackground = Color(0xFF0C0C0C)
private val PipBoySurface = Color(0xFF1A1A1A)

fun Modifier.scanlineOverlay(): Modifier = this.then(
    Modifier.drawBehind {
        val lineSpacing = 3.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = PipBoyGreen.copy(alpha = 0.06f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
            y += lineSpacing
        }
    }
)

fun Modifier.phosphorGlow(): Modifier = this.then(
    Modifier.graphicsLayer {
        shadowColor = PipBoyGreen
        ambientShadowColor = PipBoyGreen
        spotShadowColor = PipBoyGreen
        shadowElevation = 4.dp.toPx()
        elevation = 4.dp.toPx()
    }
)

fun Modifier.crtFlicker(): Modifier {
    return this.then(
        Modifier.graphicsLayer {
            val time = System.currentTimeMillis() / 1000f
            val flicker = 0.95f + 0.05f * (0.5f + 0.5f * sin(time * 3.7f * sin(time * 0.3f)))
            alpha = flicker
        }
    )
}

@Composable
fun CrtScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PipBoyBackground.copy(alpha = 0.95f))
            .scanlineOverlay()
            .crtFlicker()
    ) {
        content()
    }
}

@Composable
fun PipBoyStatusBar(
    modifier: Modifier = Modifier,
    isAiOnline: Boolean = true,
    storagePercent: Int = 67
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
            .background(PipBoySurface)
            .border(width = 1.dp, color = PipBoyGreenDim)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "VAULT-TEC",
            color = PipBoyGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            letterSpacing = 2.sp
        )

        Text(
            text = currentTime,
            color = PipBoyGreen.copy(alpha = 0.8f),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            if (isAiOnline) PipBoyGreen else PipBoyDanger
                        )
                )
                Text(
                    text = if (isAiOnline) "AI: ONLINE" else "AI: OFFLINE",
                    color = if (isAiOnline) PipBoyGreen else PipBoyDanger,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(PipBoyAmber)
                )
                Text(
                    text = "STORAGE: $storagePercent%",
                    color = PipBoyAmber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
        }
    }
}

data class PipBoyTab(
    val route: String,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

val PipBoyTabs = listOf(
    PipBoyTab("dashboard", "Dashboard", Icons.Filled.Home, Icons.Outlined.Home),
    PipBoyTab("maps", "Maps", Icons.Filled.Map, Icons.Outlined.Map),
    PipBoyTab("knowledge", "Knowledge", Icons.Filled.Book, Icons.Outlined.Book),
    PipBoyTab("chat", "Chat", Icons.Filled.Chat, Icons.Outlined.Chat),
    PipBoyTab("emergency", "Emergency", Icons.Filled.Alert, Icons.Outlined.Alert)
)

@Composable
fun PipBoyBottomNav(
    navController: NavController,
    currentRoute: String,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = PipBoyGreenDim),
        containerColor = PipBoySurface,
        contentColor = PipBoyGreen
    ) {
        PipBoyTabs.forEach { tab ->
            val selected = currentRoute == tab.route

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != tab.route) {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .then(
                                if (selected) {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = PipBoyGreen,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label,
                            tint = if (selected) PipBoyGreen else PipBoyGreenDim,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = tab.label.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = if (selected) PipBoyGreen else PipBoyGreenDim
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = PipBoyGreen,
                    unselectedIconColor = PipBoyGreenDim,
                    selectedTextColor = PipBoyGreen,
                    unselectedTextColor = PipBoyGreenDim
                )
            )
        }
    }
}
