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
import com.nomad.android.ui.components.TerminalButtonSize
import com.nomad.android.ui.components.TerminalButtonVariant
import com.nomad.android.ui.components.TerminalCard
import com.nomad.android.ui.components.TerminalDivider
import com.nomad.android.ui.components.TerminalProgressBar
import com.nomad.android.ui.components.TerminalSectionHeader
import com.nomad.android.ui.components.TerminalText
import com.nomad.android.ui.theme.LocalNomadColors
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
    val colors = LocalNomadColors.current

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
                            .border(1.dp, if (uiState.data.aiStatus?.isReady == true) colors.primary else colors.primaryDim),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Status: ${if (uiState.data.aiStatus?.isReady == true) "Ready" else "Standby"}",
                        color = if (uiState.data.aiStatus?.isReady == true) colors.primary else colors.primaryDim,
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
                uiState.data.contentPacks.forEachIndexed { index, pack ->
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
                                    color = if (pack.isDownloaded) colors.primary else colors.primaryDim,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                                    ),
                                    fontSize = 13.sp,
                                )
                                if (pack.isDownloaded) {
                                    Text(
                                        text = "[OK]",
                                        color = colors.primary,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily(
                                            androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                                        ),
                                        fontSize = 10.sp,
                                    )
                                }
                            }
                            Text(
                                text = "${pack.type.uppercase()} — ${pack.size}",
                                color = colors.primaryDim,
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
                                size = TerminalButtonSize.SMALL,
                            )
                            else -> TerminalButton(
                                text = "Get",
                                onClick = { viewModel.downloadPack(pack.id) },
                                size = TerminalButtonSize.SMALL,
                            )
                        }
                    }
                    if (index < uiState.data.contentPacks.lastIndex) {
                        TerminalDivider(color = colors.primaryDim.copy(alpha = 0.3f))
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
                    color = colors.primaryDim,
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
                            color = if (isSelected) colors.primary else colors.primaryDim,
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
                    color = colors.primary,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                    ),
                    fontSize = 13.sp,
                )
                Text(
                    text = "Offline-first survival knowledge",
                    color = colors.primaryDim,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 11.sp,
                )
            }
        }
    }
}
