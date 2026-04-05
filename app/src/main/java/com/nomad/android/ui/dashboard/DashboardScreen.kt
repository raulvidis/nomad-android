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
import androidx.compose.foundation.layout.padding
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
        uiState.error != null -> TerminalErrorScreen(
            message = uiState.error ?: "Unknown error",
            onRetry = { viewModel.refreshStatus() },
        )
        else -> DashboardContent(
            data = uiState.data,
            navController = navController,
        )
    }
}

@Composable
private fun DashboardContent(
    data: DashboardData,
    navController: NavHostController,
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
    Row(verticalAlignment = Alignment.CenterVertically) {
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
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
