package com.nomad.android.ui.maps

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nomad.android.data.local.entity.LocationSavedPointEntity
import com.nomad.android.ui.theme.PipBoyBg
import com.nomad.android.ui.components.PipBoyButton
import com.nomad.android.ui.components.PipBoyButtonVariant
import com.nomad.android.ui.components.PipBoyCard
import com.nomad.android.ui.components.PipBoyDivider
import com.nomad.android.ui.components.PipBoyEmptyScreen
import com.nomad.android.ui.components.PipBoyErrorScreen
import com.nomad.android.ui.components.PipBoyText
import com.nomad.android.ui.theme.PipBoyAmber
import com.nomad.android.ui.theme.PipBoyGreen
import com.nomad.android.ui.theme.PipBoyGreenDim
import com.nomad.android.ui.components.PipBoyLoadingScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MapsScreen(
    viewModel: MapsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions.values.all { it }
        viewModel.setLocationPermissionGranted(granted)
    }

    when {
        uiState.isLoading -> PipBoyLoadingScreen("LOADING CARTOGRAPHY...")
        uiState.error != null -> PipBoyErrorScreen(
            message = uiState.error ?: "Unknown error",
            onRetry = { viewModel.loadMapData() }
        )

        !uiState.data.isMapInitialized -> PipBoyEmptyScreen(
            message = "Map module not initialized",
            action = "INITIALIZE",
            onAction = { viewModel.loadMapData() }
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
                    )
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
            .background(PipBoyBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PipBoyText(
            text = "ROBCO INDUSTRIES (TM) TERMLINK PROTOCOL",
            style = TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
            color = PipBoyGreenDim,
        )
        PipBoyText(
            text = "TACTICAL MAP — OFFLINE CARTOGRAPHY",
            style = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace),
            color = PipBoyGreen,
        )
        PipBoyDivider()

        // Permission gate
        if (!hasPermission) {
            PipBoyCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PipBoyText(
                        text = "LOCATION PERMISSION REQUIRED",
                        style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        color = PipBoyAmber,
                    )
                    PipBoyText(
                        text = "GPS access needed for coordinates and tracking",
                        style = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                        color = PipBoyGreenDim,
                    )
                    PipBoyButton(
                        text = "GRANT PERMISSION",
                        onClick = onRequestPermission
                    )
                }
            }
        }

        // GPS Coordinates
        PipBoyCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PipBoyText(
                    text = "GPS COORDINATES",
                    style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    color = PipBoyGreen,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PipBoyText(
                        text = data.currentLocationText,
                        style = TextStyle(
                            fontSize = if (data.currentLocationText == "NO FIX") 14.sp else 18.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (data.currentLocationText == "NO FIX") PipBoyGreenDim else PipBoyGreen,
                    )
                    if (hasPermission) {
                        PipBoyButton(
                            text = "REFRESH",
                            onClick = onRefreshLocation,
                            modifier = Modifier.width(100.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val (latIcon, lonIcon) = if (hasPermission && data.currentLocationText != "NO FIX") {
                        "📡" to "📡"
                    } else {
                        "⚠" to "⚠"
                    }
                    PipBoyText(
                        text = "$latIcon ${data.currentLatitude ?: "---"}",
                        style = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                        color = PipBoyGreenDim,
                    )
                    PipBoyText(
                        text = "$lonIcon ${data.currentLongitude ?: "---"}",
                        style = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                        color = PipBoyGreenDim,
                    )
                }
            }
        }

        // Tracking Control
        if (hasPermission) {
            PipBoyCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PipBoyText(
                        text = "LOCATION TRACKING",
                        style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        color = PipBoyGreen,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val statusText = if (data.isTracking) "● TRACKING (60s intervals)" else "○ IDLE"
                        val statusColor = if (data.isTracking) PipBoyGreen else PipBoyGreenDim
                        PipBoyText(
                            text = statusText,
                            style = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                            color = statusColor,
                        )
                        PipBoyText(
                            text = "${data.snapshotCount} snapshots",
                            style = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                            color = PipBoyGreenDim,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (data.isTracking) {
                            PipBoyButton(
                                text = "STOP TRACKING",
                                onClick = onStopTracking,
                                variant = PipBoyButtonVariant.DANGER,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            PipBoyButton(
                                text = "START TRACKING",
                                onClick = onStartTracking,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Save Current Location
            PipBoyCard(modifier = Modifier.fillMaxWidth()) {
                var saveName by remember { mutableStateOf("") }
                var saveNotes by remember { mutableStateOf("") }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PipBoyText(
                        text = "SAVE LOCATION",
                        style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        color = PipBoyGreen,
                    )
                    BasicTextField(
                        value = saveName,
                        onValueChange = { saveName = it },
                        textStyle = TextStyle(
                            color = PipBoyGreen,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (saveName.isNotEmpty()) PipBoyGreen else PipBoyGreenDim,
                                RoundedCornerShape(4.dp)
                            )
                            .background(Color(0xFF0A140A))
                            .padding(8.dp),
                        decorationBox = { innerTextField ->
                            if (saveName.isEmpty()) {
                                Text(
                                    text = "Location name...",
                                    color = PipBoyGreenDim,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    BasicTextField(
                        value = saveNotes,
                        onValueChange = { saveNotes = it },
                        textStyle = TextStyle(
                            color = PipBoyGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (saveNotes.isNotEmpty()) PipBoyGreen else PipBoyGreenDim,
                                RoundedCornerShape(4.dp)
                            )
                            .background(Color(0xFF0A140A))
                            .padding(8.dp),
                        decorationBox = { innerTextField ->
                            if (saveNotes.isEmpty()) {
                                Text(
                                    text = "Notes (optional)...",
                                    color = PipBoyGreenDim,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    PipBoyButton(
                        text = "SAVE CURRENT POSITION",
                        onClick = {
                            if (saveName.isNotBlank()) {
                                onSaveLocation(saveName.trim(), saveNotes.trim())
                                saveName = ""
                                saveNotes = ""
                            }
                        },
                        enabled = saveName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Map Layer Toggles
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

        // Map Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                "BASEMAP" to "basemap",
                "POI" to "poi",
                "TOPO" to "topo",
                "EMERGENCY" to "emergency"
            ).forEach { (label, layerId) ->
                PipBoyButton(
                    text = label,
                    onClick = { onToggleLayer(layerId) },
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        // Offline Tiles Status
        Text(
            text = "REGION: ${data.regionName ?: "NOT SELECTED"} | ${if (data.hasOfflineTiles) "TILES LOADED" else "0 TILES LOADED"}",
            color = PipBoyGreenDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )

        // Saved Locations
        if (hasPermission && data.savedPoints.isNotEmpty()) {
            PipBoyCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PipBoyText(
                        text = "SAVED LOCATIONS (${data.savedPoints.size})",
                        style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        color = PipBoyGreen,
                    )
                    PipBoyDivider(color = PipBoyGreenDim.copy(alpha = 0.3f))
                    data.savedPoints.forEach { point ->
                        SavedLocationRow(point = point, onDelete = onDeletePoint)
                    }
                }
            }
        } else if (hasPermission) {
            PipBoyEmptyScreen(message = "No saved locations yet — save one above")
        }
    }
}

@Composable
private fun SavedLocationRow(
    point: LocationSavedPointEntity,
    onDelete: (String) -> Unit
) {
    val df = remember { SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()) }
    val timeStr = df.format(Date(point.timestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = point.name.uppercase(),
                    color = PipBoyGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                Text(
                    text = "${"%.6f".format(point.latitude)}, ${"%.6f".format(point.longitude)}",
                    color = PipBoyGreenDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
                Text(
                    text = "$timeStr | ALT: ${"%.1f".format(point.altitude)}m",
                    color = PipBoyGreenDim.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
                if (point.notes.isNotBlank()) {
                    Text(
                        text = "NOTE: ${point.notes}",
                        color = PipBoyAmber.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            PipBoyButton(
                text = "DEL",
                onClick = { onDelete(point.id) },
                variant = PipBoyButtonVariant.DANGER
            )
        }
    }
}
