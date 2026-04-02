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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.nomad.android.ui.components.PipBoyAmber
import com.nomad.android.ui.components.PipBoyBg
import com.nomad.android.ui.components.PipBoyCard
import com.nomad.android.ui.components.PipBoyDivider
import com.nomad.android.ui.components.PipBoyGreen
import com.nomad.android.ui.components.PipBoyGreenDim
import com.nomad.android.ui.components.PipBoyListTile
import com.nomad.android.ui.components.PipBoyProgressBar
import com.nomad.android.ui.components.PipBoyStatusIndicator
import com.nomad.android.ui.components.PipBoySurface
import com.nomad.android.ui.components.PipBoyText
import com.nomad.android.ui.navigation.Routes

private data class QuickAccessItem(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val route: String,
)

@Composable
fun DashboardScreen(navController: NavHostController) {
    val quickAccessItems = remember {
        listOf(
            QuickAccessItem("🗺", "OFFLINE MAPS", "3 regions loaded", Routes.MAPS),
            QuickAccessItem("📚", "ARCHIVES", "2.1 GB", Routes.KNOWLEDGE),
            QuickAccessItem("🤖", "AI TERMINAL", "Gemma 4 E2B", Routes.CHAT),
            QuickAccessItem("🚨", "EMERGENCY", "ALWAYS READY", Routes.EMERGENCY),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PipBoyBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PipBoyText(
            text = "ROBCO INDUSTRIES (TM) TERMLINK PROTOCOL",
            style = TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
            color = PipBoyGreenDim,
        )

        PipBoyText(
            text = "WELCOME TO NOMAD - VAULT SURVIVAL SYSTEM",
            style = TextStyle(fontSize = 16.sp, fontFamily = FontFamily.Monospace),
            color = PipBoyGreen,
        )

        PipBoyDivider()

        PipBoyCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PipBoyText(
                    text = "SYSTEM STATUS",
                    style = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                    color = PipBoyAmber,
                )

                StatusRow("AI ENGINE", "ONLINE", true)
                StatusRow("STORAGE", "45%", true)
                StatusRow("CONTENT PACKS", "3 LOADED", true)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PipBoyText(
                text = "QUICK ACCESS",
                style = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                color = PipBoyAmber,
            )

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
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PipBoyText(
                text = "RECENT ACTIVITY",
                style = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                color = PipBoyAmber,
            )

            PipBoyListTile(
                title = "Searched: Water purification",
                subtitle = "2 hours ago",
                onClick = {},
            )
            PipBoyListTile(
                title = "Read: First Aid Basics",
                subtitle = "5 hours ago",
                onClick = {},
            )
            PipBoyListTile(
                title = "Chat: How to build shelter",
                subtitle = "1 day ago",
                onClick = {},
            )
        }

        PipBoyCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PipBoyText(
                    text = "AVAILABLE PACKS",
                    style = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                    color = PipBoyAmber,
                )

                PipBoyProgressBar(progress = 0.65f)

                PipBoyText(
                    text = "3.2 GB / 5.0 GB USED",
                    style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    color = PipBoyGreen,
                )
            }
        }

        PipBoyCard(
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
                PipBoyText(
                    text = "SYSTEM SETTINGS",
                    style = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                    color = PipBoyAmber,
                )
                Text(
                    text = "[ENTER] >",
                    color = PipBoyGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
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
        PipBoyStatusIndicator(label = label, isOnline = isOnline)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            color = PipBoyGreen,
            fontFamily = FontFamily.Monospace,
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
    PipBoyCard(
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
                text = item.emoji,
                fontSize = 24.sp,
            )
            Text(
                text = item.title,
                color = PipBoyGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
            )
            Text(
                text = item.subtitle,
                color = PipBoyGreenDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
        }
    }
}
