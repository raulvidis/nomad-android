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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nomad.android.ui.components.PipBoyBg
import com.nomad.android.ui.components.PipBoyButton
import com.nomad.android.ui.components.PipBoyCard
import com.nomad.android.ui.components.PipBoyDivider
import com.nomad.android.ui.components.PipBoyGreen
import com.nomad.android.ui.components.PipBoyGreenDim
import com.nomad.android.ui.components.PipBoyProgressBar
import com.nomad.android.ui.components.PipBoyText

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PipBoyBg)
            .padding(16.dp)
    ) {
        PipBoyText(
            text = "ROBCO INDUSTRIES (TM)",
            style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
            color = PipBoyGreenDim,
        )
        PipBoyText(
            text = "SYSTEM CONFIGURATION",
            style = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace),
            color = PipBoyGreen,
        )
        PipBoyDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // AI Engine section
        PipBoyCard(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionTitle("AI ENGINE")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Engine: ${uiState.data.aiStatus?.modelName ?: "N/A"}",
                color = PipBoyGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            Text(
                text = "Type: ${uiState.data.aiStatus?.engineType?.displayName ?: "Unknown"}",
                color = PipBoyGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .border(1.dp, if (uiState.data.aiStatus?.isReady == true) PipBoyGreen else PipBoyGreenDim)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "STATUS: ${if (uiState.data.aiStatus?.isReady == true) "READY" else "STANDBY"}",
                    color = if (uiState.data.aiStatus?.isReady == true) PipBoyGreen else PipBoyGreenDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content Packs section
        PipBoyCard(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionTitle("CONTENT PACKS")
            Spacer(modifier = Modifier.height(8.dp))
            uiState.data.contentPacks.forEach { pack ->
                Text(
                    text = "${pack.name} .......... ${if (pack.isDownloaded) "DOWNLOADED" else "AVAILABLE"}",
                    color = if (pack.isDownloaded) PipBoyGreen else PipBoyGreenDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                PipBoyButton(text = "DOWNLOAD", onClick = { })
                Spacer(modifier = Modifier.width(8.dp))
                PipBoyButton(text = "DELETE", onClick = { })
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Storage section
        PipBoyCard(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionTitle("STORAGE")
            Spacer(modifier = Modifier.height(8.dp))
            PipBoyProgressBar(
                progress = uiState.data.storageMetrics?.usedPercent?.div(100f) ?: 0f,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${uiState.data.storageMetrics?.usedBytes?.div(1_000_000_000) ?: 0} GB / ${uiState.data.storageMetrics?.totalBytes?.div(1_000_000_000) ?: 0} GB USED",
                color = PipBoyGreenDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Display/Theme section
        PipBoyCard(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionTitle("DISPLAY")
            Spacer(modifier = Modifier.height(8.dp))
            viewModel.availableThemes.forEach { theme ->
                val isSelected = uiState.data.currentTheme == theme.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setTheme(theme.id) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val selectorChar = if (isSelected) "[X]" else "[ ]"
                    Text(
                        text = "$selectorChar ${theme.name}",
                        color = if (isSelected) PipBoyGreen else PipBoyGreenDim,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // About section
        PipBoyCard(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionTitle("ABOUT")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "NOMAD v1.0.0",
                color = PipBoyGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            Text(
                text = "VAULT-TEC SURVIVAL SYSTEMS",
                color = PipBoyGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            Text(
                text = "Offline-first survival knowledge",
                color = PipBoyGreenDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = PipBoyGreen,
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp
    )
}
