package com.nomad.android.ui.maps

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nomad.android.R
import com.nomad.android.data.local.entity.LocationSavedPointEntity
import com.nomad.android.ui.components.TerminalButton
import com.nomad.android.ui.components.TerminalButtonSize
import com.nomad.android.ui.components.TerminalButtonVariant
import com.nomad.android.ui.components.TerminalCard
import com.nomad.android.ui.components.TerminalEmptyScreen
import com.nomad.android.ui.components.TerminalErrorScreen
import com.nomad.android.ui.components.TerminalLoadingScreen
import com.nomad.android.ui.components.TerminalText
import com.nomad.android.ui.components.TerminalTextField
import com.nomad.android.ui.theme.TerminalAmber
import com.nomad.android.ui.theme.TerminalBg
import com.nomad.android.ui.theme.TerminalGreen
import com.nomad.android.ui.theme.TerminalGreenDim
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MapsScreen(
    viewModel: MapsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions.values.all { it }
        viewModel.setLocationPermissionGranted(granted)
    }

    when {
        uiState.isLoading -> TerminalLoadingScreen("LOADING CARTOGRAPHY...")
        uiState.error != null -> TerminalErrorScreen(
            message = uiState.error ?: "Unknown error",
            onRetry = { viewModel.loadMapData() },
        )
        !uiState.data.isMapInitialized -> TerminalEmptyScreen(
            message = "Map module not initialized",
            action = "INITIALIZE",
            onAction = { viewModel.loadMapData() },
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
                    ),
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
            .background(TerminalBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TerminalText(
            text = "Offline Cartography",
            color = TerminalGreen,
            style = TextStyle(
                fontSize = 20.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
                ),
            ),
        )

        if (!hasPermission) {
            TerminalCard(header = "Location Permission Required") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TerminalText(
                        text = "GPS access is needed for coordinates and tracking",
                        color = TerminalAmber,
                        style = TextStyle(fontSize = 12.sp),
                    )
                    TerminalButton(text = "Grant Permission", onClick = onRequestPermission)
                }
            }
        }

        TerminalCard(header = "GPS Coordinates") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TerminalText(
                        text = data.currentLocationText,
                        color = if (data.currentLocationText == "NO FIX") TerminalGreenDim else TerminalGreen,
                        style = TextStyle(
                            fontSize = if (data.currentLocationText == "NO FIX") 14.sp else 18.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily(
                                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                            ),
                        ),
                    )
                    if (hasPermission) {
                        TerminalButton(
                            text = "Refresh",
                            onClick = onRefreshLocation,
                            modifier = Modifier.width(100.dp),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val statusIcon = if (hasPermission && data.currentLocationText != "NO FIX") "OK" else "--"
                    TerminalText(
                        text = "$statusIcon ${data.currentLatitude ?: "---"}",
                        color = TerminalGreenDim,
                        style = TextStyle(fontSize = 11.sp),
                    )
                    TerminalText(
                        text = "$statusIcon ${data.currentLongitude ?: "---"}",
                        color = TerminalGreenDim,
                        style = TextStyle(fontSize = 11.sp),
                    )
                }
            }
        }

        if (hasPermission) {
            TerminalCard(header = "Location Tracking") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val statusText = if (data.isTracking) "TRACKING (60s)" else "IDLE"
                        val statusColor = if (data.isTracking) TerminalGreen else TerminalGreenDim
                        TerminalText(
                            text = statusText,
                            color = statusColor,
                            style = TextStyle(fontSize = 11.sp),
                        )
                        TerminalText(
                            text = "${data.snapshotCount} snapshots",
                            color = TerminalGreenDim,
                            style = TextStyle(fontSize = 11.sp),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (data.isTracking) {
                            TerminalButton(
                                text = "Stop Tracking",
                                onClick = onStopTracking,
                                variant = TerminalButtonVariant.DANGER,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            TerminalButton(
                                text = "Start Tracking",
                                onClick = onStartTracking,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            TerminalCard(header = "Save Location") {
                var saveName by remember { mutableStateOf("") }
                var saveNotes by remember { mutableStateOf("") }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TerminalTextField(
                        value = saveName,
                        onValueChange = { saveName = it },
                        placeholder = "Location name",
                        label = "Name",
                        singleLine = true,
                    )
                    TerminalTextField(
                        value = saveNotes,
                        onValueChange = { saveNotes = it },
                        placeholder = "Notes (optional)",
                        label = "Notes",
                        singleLine = false,
                    )
                    TerminalButton(
                        text = "Save Current Position",
                        onClick = {
                            if (saveName.isNotBlank()) {
                                onSaveLocation(saveName.trim(), saveNotes.trim())
                                saveName = ""
                                saveNotes = ""
                            }
                        },
                        enabled = saveName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        size = TerminalButtonSize.LARGE,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            data.layers.forEach { layer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleLayer(layer.id) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val selector = if (layer.isEnabled) "[X]" else "[ ]"
                    Text(
                        text = "$selector ${layer.name.uppercase()}",
                        color = if (layer.isEnabled) TerminalGreen else TerminalGreenDim,
                        fontFamily = androidx.compose.ui.text.font.FontFamily(
                            androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                        ),
                        fontSize = 12.sp,
                    )
                }
            }
        }

        if (hasPermission && data.savedPoints.isNotEmpty()) {
            TerminalCard(header = "Saved Locations (${data.savedPoints.size})") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    data.savedPoints.forEach { point ->
                        SavedLocationRow(point = point, onDelete = onDeletePoint)
                    }
                }
            }
        } else if (hasPermission) {
            TerminalEmptyScreen(message = "No saved locations yet — save one above")
        }

        Text(
            text = "Region: ${data.regionName ?: "Not selected"} | ${if (data.hasOfflineTiles) "Tiles loaded" else "0 tiles loaded"}",
            color = TerminalGreenDim,
            fontFamily = androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            ),
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun SavedLocationRow(
    point: LocationSavedPointEntity,
    onDelete: (String) -> Unit,
) {
    val df = remember { SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()) }
    val timeStr = df.format(Date(point.timestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = point.name.uppercase(),
                    color = TerminalGreen,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                    ),
                    fontSize = 13.sp,
                )
                Text(
                    text = "${"%.6f".format(point.latitude)}, ${"%.6f".format(point.longitude)}",
                    color = TerminalGreenDim,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 11.sp,
                )
                Text(
                    text = "$timeStr | Alt: ${"%.1f".format(point.altitude)}m",
                    color = TerminalGreenDim.copy(alpha = 0.7f),
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 10.sp,
                )
                if (point.notes.isNotBlank()) {
                    Text(
                        text = "Note: ${point.notes}",
                        color = TerminalAmber.copy(alpha = 0.8f),
                        fontFamily = androidx.compose.ui.text.font.FontFamily(
                            androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                        ),
                        fontSize = 10.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            TerminalButton(
                text = "Del",
                onClick = { onDelete(point.id) },
                variant = TerminalButtonVariant.DANGER,
                size = TerminalButtonSize.SMALL,
            )
        }
    }
}
