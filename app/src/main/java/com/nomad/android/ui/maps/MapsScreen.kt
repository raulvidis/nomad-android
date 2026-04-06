package com.nomad.android.ui.maps

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nomad.android.R
import com.nomad.android.data.local.entity.LocationSavedPointEntity
import com.nomad.android.data.maps.OfflineRegion
import com.nomad.android.ui.theme.TerminalAmber
import com.nomad.android.ui.theme.TerminalBg
import com.nomad.android.ui.theme.TerminalDanger
import com.nomad.android.ui.theme.TerminalGreen
import com.nomad.android.ui.theme.TerminalGreenDim
import com.nomad.android.ui.theme.TerminalSurface
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet

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

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.data.isMapInitialized) {
            MapViewContainer(data = uiState.data, viewModel = viewModel)
        }

        CoordinatesOverlay(data = uiState.data)

        MapControlsOverlay(
            data = uiState.data,
            onAutoCenter = { viewModel.toggleAutoCenter() },
            onDownload = { viewModel.startRegionSelection() },
            onSavedPoints = { viewModel.toggleSavedPanel() },
            onRegions = { viewModel.toggleRegionList() },
            onRequestPermission = {
                locationPermissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            },
        )

        AnimatedVisibility(
            visible = uiState.data.showSavedPanel,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            SavedPointsPanel(
                points = uiState.data.savedPoints,
                onDelete = { viewModel.deleteSavedPoint(it) },
                onClose = { viewModel.toggleSavedPanel() },
            )
        }

        AnimatedVisibility(
            visible = uiState.data.showRegionList,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            RegionsPanel(
                regions = uiState.data.downloadedRegions,
                onDelete = { viewModel.deleteRegion(it) },
                onClose = { viewModel.toggleRegionList() },
            )
        }

        if (uiState.data.isSelectingRegion) {
            RegionSelectionOverlay(viewModel = viewModel)
        }

        if (uiState.data.isDownloading) {
            DownloadProgressOverlay(progress = uiState.data.downloadProgress)
        }
    }
}

@Composable
private fun MapViewContainer(
    data: MapsData,
    viewModel: MapsViewModel,
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

    DisposableEffect(Unit) {
        MapLibre.getInstance(context)
        onDispose { mapView?.onDestroy() }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val mv = MapView(ctx)
            mapView = mv
            mv.getMapAsync { map ->
                mapLibreMap = map
                map.uiSettings.isCompassEnabled = false
                map.uiSettings.isLogoEnabled = false
                map.uiSettings.isAttributionEnabled = false

                val tileSet = TileSet("tileset", listOf("https://tile.openstreetmap.org/{z}/{x}/{y}.png"))
                val styleBuilder = Style.Builder()
                    .withSource(RasterSource("osm-source", tileSet, 256, 0, 19))
                    .withLayer(RasterLayer("osm-layer", "osm-source"))

                map.setStyle(styleBuilder)

                val initialPos = if (data.currentLatitude != null && data.currentLongitude != null) {
                    CameraPosition.Builder()
                        .target(LatLng(data.currentLatitude, data.currentLongitude))
                        .zoom(12.0)
                        .build()
                } else {
                    CameraPosition.Builder()
                        .target(LatLng(48.8566, 2.3522))
                        .zoom(4.0)
                        .build()
                }
                map.cameraPosition = initialPos
            }
            mv.onCreate(null)
            mv.onStart()
            mv.onResume()
            mv
        },
        update = { mv ->
            val lat = data.currentLatitude
            val lon = data.currentLongitude
            if (data.isAutoCenter && lat != null && lon != null) {
                mapLibreMap?.animateCamera(
                    CameraUpdateFactory.newLatLng(LatLng(lat, lon)),
                    1000
                )
            }
        }
    )
}

@Composable
private fun CoordinatesOverlay(data: MapsData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 12.dp, end = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .background(TerminalBg.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                .border(1.dp, TerminalGreenDim, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = data.currentLocationText,
                color = if (data.currentLocationText == "NO FIX") TerminalGreenDim else TerminalGreen,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                ),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (data.isTracking) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(TerminalGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

@Composable
private fun MapControlsOverlay(
    data: MapsData,
    onAutoCenter: () -> Unit,
    onDownload: () -> Unit,
    onSavedPoints: () -> Unit,
    onRegions: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 12.dp, bottom = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(TerminalBg.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .border(1.dp, TerminalGreenDim, RoundedCornerShape(8.dp))
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MapControlButton(
                icon = {
                    Icon(
                        if (data.isAutoCenter) Icons.Filled.GpsFixed else Icons.Filled.GpsOff,
                        contentDescription = "Auto Center",
                        tint = if (data.isAutoCenter) TerminalGreen else TerminalGreenDim,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = onAutoCenter,
            )
            MapControlButton(
                icon = {
                    Icon(
                        Icons.Filled.MyLocation,
                        contentDescription = "Permission",
                        tint = TerminalAmber,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = onRequestPermission,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp)
                .background(TerminalBg.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .border(1.dp, TerminalGreenDim, RoundedCornerShape(8.dp))
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MapControlButton(
                icon = {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Download Region",
                        tint = TerminalGreen,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = onDownload,
            )
            MapControlButton(
                icon = {
                    Icon(
                        Icons.Filled.Bookmark,
                        contentDescription = "Saved Points",
                        tint = TerminalGreen,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = onSavedPoints,
            )
            MapControlButton(
                icon = {
                    Icon(
                        Icons.Filled.Layers,
                        contentDescription = "Regions",
                        tint = TerminalGreen,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = onRegions,
            )
        }
    }
}

@Composable
private fun MapControlButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

@Composable
private fun SavedPointsPanel(
    points: List<LocationSavedPointEntity>,
    onDelete: (String) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 60.dp)
            .background(TerminalBg.copy(alpha = 0.95f), RoundedCornerShape(8.dp))
            .border(1.dp, TerminalGreenDim, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "SAVED LOCATIONS (${points.size})",
                    color = TerminalGreen,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
                    ),
                    fontSize = 14.sp,
                )
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = TerminalGreenDim,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onClose),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (points.isEmpty()) {
                Text(
                    text = "No saved locations",
                    color = TerminalGreenDim,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 12.sp,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(points, key = { it.id }) { point ->
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
                                    fontSize = 12.sp,
                                )
                                Text(
                                    text = "%.6f, %.6f".format(point.latitude, point.longitude),
                                    color = TerminalGreenDim,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                                    ),
                                    fontSize = 10.sp,
                                )
                            }
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = TerminalDanger,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onDelete(point.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegionsPanel(
    regions: List<OfflineRegion>,
    onDelete: (String) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 60.dp)
            .background(TerminalBg.copy(alpha = 0.95f), RoundedCornerShape(8.dp))
            .border(1.dp, TerminalGreenDim, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "OFFLINE REGIONS (${regions.size})",
                    color = TerminalGreen,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
                    ),
                    fontSize = 14.sp,
                )
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = TerminalGreenDim,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onClose),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (regions.isEmpty()) {
                Text(
                    text = "No offline regions. Tap + to download.",
                    color = TerminalGreenDim,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 12.sp,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(regions, key = { it.id }) { region ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = region.name.uppercase(),
                                    color = TerminalGreen,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                                    ),
                                    fontSize = 12.sp,
                                )
                                Text(
                                    text = "Z${region.minZoom}-${region.maxZoom} | ${region.tileCount} tiles | ${"%.1f".format(region.sizeBytes / 1_048_576.0)}MB",
                                    color = TerminalGreenDim,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                                    ),
                                    fontSize = 10.sp,
                                )
                            }
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = TerminalDanger,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onDelete(region.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegionSelectionOverlay(
    viewModel: MapsViewModel,
) {
    var minZoom by remember { mutableStateOf(12) }
    var maxZoom by remember { mutableStateOf(15) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(TerminalBg, RoundedCornerShape(8.dp))
                .border(1.dp, TerminalGreen, RoundedCornerShape(8.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "DOWNLOAD OFFLINE MAP",
                color = TerminalGreen,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
                ),
                fontSize = 16.sp,
            )

            Text(
                text = "Navigate to the area you want to download, then set zoom levels.",
                color = TerminalGreenDim,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                ),
                fontSize = 11.sp,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Min Zoom: $minZoom",
                    color = TerminalGreen,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 13.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(10, 12, 14).forEach { z ->
                        Box(
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (minZoom == z) TerminalGreen else TerminalGreenDim,
                                    RoundedCornerShape(4.dp)
                                )
                                .background(
                                    if (minZoom == z) TerminalGreen.copy(alpha = 0.15f) else TerminalSurface,
                                    RoundedCornerShape(4.dp),
                                )
                                .clickable {
                                    minZoom = z
                                    viewModel.setZoomRange(z, maxZoom)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "$z",
                                color = if (minZoom == z) TerminalGreen else TerminalGreenDim,
                                fontFamily = androidx.compose.ui.text.font.FontFamily(
                                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                                ),
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Max Zoom: $maxZoom",
                    color = TerminalGreen,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 13.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(14, 15, 16, 17).forEach { z ->
                        Box(
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (maxZoom == z) TerminalGreen else TerminalGreenDim,
                                    RoundedCornerShape(4.dp)
                                )
                                .background(
                                    if (maxZoom == z) TerminalGreen.copy(alpha = 0.15f) else TerminalSurface,
                                    RoundedCornerShape(4.dp),
                                )
                                .clickable {
                                    maxZoom = z
                                    viewModel.setZoomRange(minZoom, z)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "$z",
                                color = if (maxZoom == z) TerminalGreen else TerminalGreenDim,
                                fontFamily = androidx.compose.ui.text.font.FontFamily(
                                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                                ),
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .border(1.dp, TerminalGreenDim, RoundedCornerShape(4.dp))
                        .background(TerminalSurface, RoundedCornerShape(4.dp))
                        .clickable { viewModel.cancelRegionSelection() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "CANCEL",
                        color = TerminalGreenDim,
                        fontFamily = androidx.compose.ui.text.font.FontFamily(
                            androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                        ),
                        fontSize = 13.sp,
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, TerminalGreen, RoundedCornerShape(4.dp))
                        .background(TerminalGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .clickable { viewModel.cancelRegionSelection() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "USE CURRENT VIEW",
                        color = TerminalGreen,
                        fontFamily = androidx.compose.ui.text.font.FontFamily(
                            androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
                        ),
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadProgressOverlay(
    progress: com.nomad.android.data.maps.DownloadProgress?,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(TerminalBg, RoundedCornerShape(8.dp))
                .border(1.dp, TerminalAmber, RoundedCornerShape(8.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "DOWNLOADING MAP TILES",
                color = TerminalAmber,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
                ),
                fontSize = 14.sp,
            )
            if (progress != null) {
                val pct = if (progress.total > 0) progress.downloaded * 100 / progress.total else 0
                val filledBlocks = pct / 5
                val emptyBlocks = 20 - filledBlocks
                val bar = "[" + "\u2588".repeat(filledBlocks) + "\u2591".repeat(emptyBlocks) + "]"
                Text(
                    text = bar,
                    color = TerminalGreen,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 14.sp,
                )
                Text(
                    text = "$pct% | ${progress.downloaded} / ${progress.total} tiles",
                    color = TerminalGreenDim,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 12.sp,
                )
                val sizeMB = "%.1f".format(progress.bytesDownloaded / 1_048_576.0)
                val totalMB = "%.1f".format(progress.estimatedTotalBytes / 1_048_576.0)
                Text(
                    text = "$sizeMB / $totalMB MB",
                    color = TerminalGreenDim,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 11.sp,
                )
            }
        }
    }
}
