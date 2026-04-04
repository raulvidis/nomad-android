package com.nomad.android.ui.maps

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.nomad.android.ui.theme.PipBoyBg
import com.nomad.android.ui.components.PipBoyButton
import com.nomad.android.ui.components.PipBoyDivider
import com.nomad.android.ui.components.PipBoyEmptyScreen
import com.nomad.android.ui.components.PipBoyErrorScreen
import com.nomad.android.ui.theme.PipBoyGreen
import com.nomad.android.ui.theme.PipBoyGreenDim
import com.nomad.android.ui.components.PipBoyLoadingScreen

@Composable
fun MapsScreen(
    viewModel: MapsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> PipBoyLoadingScreen("LOADING CARTOGRAPHY...")
        uiState.error != null -> PipBoyErrorScreen(message = uiState.error ?: "Unknown error", onRetry = { viewModel.loadMapData() })
        !uiState.data.isMapInitialized -> PipBoyEmptyScreen(
            message = "Map module not initialized",
            action = "INITIALIZE",
            onAction = { viewModel.loadMapData() }
        )
        else -> MapsContent(
            data = uiState.data,
            onToggleLayer = { viewModel.toggleLayer(it) }
        )
    }
}

@Composable
private fun MapsContent(
    data: MapsData,
    onToggleLayer: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PipBoyBg)
            .padding(16.dp)
    ) {
        Text(
            text = "ROBCO INDUSTRIES (TM) TERMLINK PROTOCOL",
            color = PipBoyGreenDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
        Text(
            text = "TACTICAL MAP — OFFLINE CARTOGRAPHY",
            color = PipBoyGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
        PipBoyDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, PipBoyGreen, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MAP MODULE",
                    color = PipBoyGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = if (data.hasOfflineTiles) "Offline tiles available" else "MapLibre GL Native integration pending",
                    color = PipBoyGreenDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Layer toggles
        data.layers.forEach { layer ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleLayer(layer.id) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val selector = if (layer.isEnabled) "[X]" else "[ ]"
                Text(
                    text = "$selector ${layer.name.uppercase()}",
                    color = if (layer.isEnabled) PipBoyGreen else PipBoyGreenDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("BASEMAP", "POI", "TOPO", "EMERGENCY").forEach { label ->
                PipBoyButton(text = label, onClick = { }, modifier = Modifier.padding(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "REGION: NOT SELECTED | ${if (data.hasOfflineTiles) "TILES LOADED" else "0 TILES LOADED"}",
            color = PipBoyGreenDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
    }
}
