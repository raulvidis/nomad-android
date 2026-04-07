package com.nomad.android.ui.settings

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nomad.android.R
import com.nomad.android.ui.components.TerminalButton
import com.nomad.android.ui.components.TerminalButtonSize
import com.nomad.android.ui.components.TerminalButtonVariant
import com.nomad.android.ui.components.TerminalProgressBar
import com.nomad.android.ui.theme.BackgroundDark
import com.nomad.android.ui.theme.OutlineVariant
import com.nomad.android.ui.theme.PhosphorGreen
import com.nomad.android.ui.theme.PhosphorGreenDim
import com.nomad.android.ui.theme.PhosphorGreenGlow
import com.nomad.android.ui.theme.SurfaceContainerLow
import com.nomad.android.ui.theme.SurfaceContainerLowest
import com.nomad.android.ui.theme.TerminalDanger
import com.nomad.android.ui.theme.TertiaryAmber
import com.nomad.android.ui.theme.OnSurfaceVariant

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PageHeader()

        if (uiState.error != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceContainerLow)
                    .border(1.dp, TerminalDanger)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "ERROR: ${uiState.error}",
                    color = TerminalDanger,
                    fontFamily = FontFamily(Font(R.font.space_grotesk_regular)),
                    fontSize = 12.sp,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(5f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AiEngineCard(uiState)
                StorageCard(uiState)
                AmbientDataSection(uiState)
            }

            Column(
                modifier = Modifier.weight(7f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ContentPacksCard(uiState, viewModel)
                TerminalLogSection()
            }
        }
    }
}

@Composable
private fun PageHeader() {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "SYSTEM CONFIGURATION",
                color = PhosphorGreen,
                fontFamily = FontFamily(Font(R.font.space_grotesk_bold, FontWeight.Bold)),
                fontSize = 22.sp,
                letterSpacing = 0.05.em,
                style = TextStyle(
                    shadow = Shadow(
                        color = PhosphorGreenGlow,
                        blurRadius = 8f,
                        offset = Offset(0f, 0f),
                    ),
                ),
            )
            Text(
                text = "VER: 4.0.2-OMEGA",
                color = PhosphorGreenDim,
                fontFamily = FontFamily(Font(R.font.space_grotesk_regular)),
                fontSize = 10.sp,
                letterSpacing = 0.05.em,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .drawBehind {
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                PhosphorGreen.copy(alpha = 0.6f),
                                PhosphorGreen.copy(alpha = 0.1f),
                                Color.Transparent,
                            ),
                        ),
                    )
                },
        )
    }
}

@Composable
private fun AiEngineCard(uiState: SettingsUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLow),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxSize()
                .background(PhosphorGreen),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "AI ENGINE",
                    color = PhosphorGreen,
                    fontFamily = FontFamily(Font(R.font.space_grotesk_bold, FontWeight.Bold)),
                    fontSize = 14.sp,
                    letterSpacing = 0.05.em,
                )
                Text(
                    text = "MODULE_ID: 0x9A",
                    color = PhosphorGreenDim,
                    fontFamily = FontFamily(Font(R.font.space_grotesk_regular)),
                    fontSize = 10.sp,
                    letterSpacing = 0.05.em,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(PhosphorGreenDim.copy(alpha = 0.3f)),
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = uiState.data.aiStatus?.modelName ?: "N/A",
                color = PhosphorGreen,
                fontFamily = FontFamily(Font(R.font.space_grotesk_semi_bold, FontWeight.SemiBold)),
                fontSize = 16.sp,
                letterSpacing = 0.05.em,
                style = TextStyle(
                    shadow = Shadow(
                        color = PhosphorGreenGlow,
                        blurRadius = 4f,
                        offset = Offset(0f, 0f),
                    ),
                ),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "TYPE: ${uiState.data.aiStatus?.engineType?.displayName ?: "Unknown"}",
                color = OnSurfaceVariant,
                fontFamily = FontFamily(Font(R.font.space_grotesk_regular)),
                fontSize = 11.sp,
                letterSpacing = 0.05.em,
            )
            Spacer(modifier = Modifier.height(4.dp))

            val isReady = uiState.data.aiStatus?.isReady == true
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isReady) PhosphorGreen else PhosphorGreenDim),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isReady) "ONLINE" else "STANDBY",
                    color = if (isReady) PhosphorGreen else PhosphorGreenDim,
                    fontFamily = FontFamily(Font(R.font.space_grotesk_semi_bold, FontWeight.SemiBold)),
                    fontSize = 12.sp,
                    letterSpacing = 0.05.em,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TerminalButton(
                text = "RECALIBRATE CORE",
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                size = TerminalButtonSize.SMALL,
                variant = TerminalButtonVariant.NORMAL,
            )
        }
    }
}

@Composable
private fun StorageCard(uiState: SettingsUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLow)
            .padding(12.dp),
    ) {
        Text(
            text = "STORAGE",
            color = PhosphorGreen,
            fontFamily = FontFamily(Font(R.font.space_grotesk_bold, FontWeight.Bold)),
            fontSize = 14.sp,
            letterSpacing = 0.05.em,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PhosphorGreenDim.copy(alpha = 0.3f)),
        )
        Spacer(modifier = Modifier.height(8.dp))

        TerminalProgressBar(
            progress = uiState.data.storageMetrics?.usedPercent?.div(100f) ?: 0f,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${uiState.data.storageMetrics?.usedBytes?.div(1_000_000_000) ?: 0} GB / ${uiState.data.storageMetrics?.totalBytes?.div(1_000_000_000) ?: 0} GB",
            color = PhosphorGreenDim,
            fontFamily = FontFamily(Font(R.font.space_grotesk_regular)),
            fontSize = 11.sp,
            letterSpacing = 0.05.em,
        )
    }
}

@Composable
private fun AmbientDataSection(uiState: SettingsUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest)
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(SurfaceContainerLow.copy(alpha = 0.5f))
                .border(1.dp, OutlineVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "[ SIGNAL MONITOR ]",
                color = PhosphorGreenDim.copy(alpha = 0.4f),
                fontFamily = FontFamily(Font(R.font.space_grotesk_regular)),
                fontSize = 11.sp,
                letterSpacing = 0.05.em,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AmbientStat("LATENCY", "24ms")
            AmbientStat("NODES", "7/8")
            AmbientStat("PKT_LOSS", "0.2%")
        }
    }
}

@Composable
private fun AmbientStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = PhosphorGreenDim,
            fontFamily = FontFamily(Font(R.font.space_grotesk_regular)),
            fontSize = 9.sp,
            letterSpacing = 0.05.em,
        )
        Text(
            text = value,
            color = PhosphorGreen,
            fontFamily = FontFamily(Font(R.font.space_grotesk_semi_bold, FontWeight.SemiBold)),
            fontSize = 12.sp,
            letterSpacing = 0.05.em,
        )
    }
}

@Composable
private fun ContentPacksCard(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLow),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(PhosphorGreen),
        )
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "CONTENT PACKS",
                    color = PhosphorGreen,
                    fontFamily = FontFamily(Font(R.font.space_grotesk_bold, FontWeight.Bold)),
                    fontSize = 14.sp,
                    letterSpacing = 0.05.em,
                )
                Row(
                    modifier = Modifier
                        .background(PhosphorGreen)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${uiState.data.contentPacks.count { !it.isDownloaded && !it.isDownloading }} UPDATES AVAILABLE",
                        color = BackgroundDark,
                        fontFamily = FontFamily(Font(R.font.space_grotesk_semi_bold, FontWeight.SemiBold)),
                        fontSize = 9.sp,
                        letterSpacing = 0.05.em,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(PhosphorGreenDim.copy(alpha = 0.3f)),
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                uiState.data.contentPacks.forEach { pack ->
                    ContentPackItem(
                        pack = pack,
                        onDownload = { viewModel.downloadPack(pack.id) },
                        onDelete = { viewModel.deletePack(pack.id) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .border(
                        width = 2.dp,
                        color = PhosphorGreenDim.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(0.dp),
                    )
                    .clickable { }
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "LOAD EXTERNAL ARCHIVE +",
                    color = PhosphorGreenDim,
                    fontFamily = FontFamily(Font(R.font.space_grotesk_semi_bold, FontWeight.SemiBold)),
                    fontSize = 12.sp,
                    letterSpacing = 0.05.em,
                )
            }
        }
    }
}

@Composable
private fun ContentPackItem(
    pack: ContentPackInfo,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest)
            .border(
                width = 2.dp,
                color = if (pack.isDownloaded) PhosphorGreen else OutlineVariant,
            ),
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(48.dp)
                .background(if (pack.isDownloaded) PhosphorGreen else PhosphorGreenDim),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = pack.name.uppercase(),
                    color = if (pack.isDownloaded) PhosphorGreen else OnSurfaceVariant,
                    fontFamily = FontFamily(Font(R.font.space_grotesk_semi_bold, FontWeight.SemiBold)),
                    fontSize = 12.sp,
                    letterSpacing = 0.05.em,
                )
                when {
                    pack.isDownloading -> {
                        Text(
                            text = "${(pack.downloadProgress * 100).toInt()}%",
                            color = TertiaryAmber,
                            fontFamily = FontFamily(Font(R.font.space_grotesk_semi_bold, FontWeight.SemiBold)),
                            fontSize = 11.sp,
                            letterSpacing = 0.05.em,
                        )
                    }
                    pack.isDownloaded -> {
                        Text(
                            text = "ACTIVE",
                            color = PhosphorGreen,
                            fontFamily = FontFamily(Font(R.font.space_grotesk_semi_bold, FontWeight.SemiBold)),
                            fontSize = 10.sp,
                            letterSpacing = 0.05.em,
                        )
                    }
                    else -> {
                        TerminalButton(
                            text = "GET",
                            onClick = onDownload,
                            size = TerminalButtonSize.SMALL,
                        )
                    }
                }
            }
            if (pack.isDownloading) {
                Spacer(modifier = Modifier.height(4.dp))
                TerminalProgressBar(
                    progress = pack.downloadProgress,
                    modifier = Modifier.fillMaxWidth(),
                    color = TertiaryAmber,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${pack.type.uppercase()} — ${pack.size}",
                    color = PhosphorGreenDim,
                    fontFamily = FontFamily(Font(R.font.space_grotesk_regular)),
                    fontSize = 10.sp,
                    letterSpacing = 0.05.em,
                )
                if (pack.isDownloaded) {
                    Text(
                        text = "DELETE",
                        color = TerminalDanger,
                        fontFamily = FontFamily(Font(R.font.space_grotesk_semi_bold, FontWeight.SemiBold)),
                        fontSize = 10.sp,
                        letterSpacing = 0.05.em,
                        modifier = Modifier
                            .clickable { onDelete() }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalLogSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant)
            .padding(12.dp),
    ) {
        Text(
            text = "SYSTEM LOG",
            color = PhosphorGreen,
            fontFamily = FontFamily(Font(R.font.space_grotesk_bold, FontWeight.Bold)),
            fontSize = 12.sp,
            letterSpacing = 0.05.em,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PhosphorGreenDim.copy(alpha = 0.3f)),
        )
        Spacer(modifier = Modifier.height(8.dp))

        val logLines = listOf(
            "[OK] Core telemetry initialized",
            "[OK] Memory bank scan complete",
            "[--] Loading content indices...",
            "[OK] Storage subsystem online",
            "[OK] AI engine handshake verified",
            "[OK] Pack integrity checks passed",
            "[--] Monitoring node connections...",
        )

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            logLines.forEach { line ->
                val lineColor = when {
                    line.startsWith("[OK]") -> PhosphorGreenDim
                    line.startsWith("[--]") -> PhosphorGreenDim.copy(alpha = 0.5f)
                    else -> PhosphorGreenDim
                }
                Text(
                    text = line,
                    color = lineColor,
                    fontFamily = FontFamily(Font(R.font.space_grotesk_regular)),
                    fontSize = 10.sp,
                    letterSpacing = 0.02.em,
                )
            }

            BlinkingCursor()
        }
    }
}

@Composable
private fun BlinkingCursor() {
    val infiniteTransition = rememberInfiniteTransition(label = "cursorBlink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 530),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cursorAlpha",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "> _",
            color = PhosphorGreen.copy(alpha = cursorAlpha),
            fontFamily = FontFamily(Font(R.font.space_grotesk_regular)),
            fontSize = 10.sp,
        )
    }
}
