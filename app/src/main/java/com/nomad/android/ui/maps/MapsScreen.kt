package com.nomad.android.ui.maps

import android.graphics.Color
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nomad.android.R
import com.nomad.android.data.local.entity.LocationSavedPointEntity
import com.nomad.android.data.local.entity.LocationSnapshotEntity
import com.nomad.android.data.local.entity.TrackRouteEntity
import com.nomad.android.data.maps.OfflineRegion
import com.nomad.android.ui.theme.TertiaryAmber
import com.nomad.android.ui.theme.BackgroundDark
import com.nomad.android.ui.theme.TerminalDanger
import com.nomad.android.ui.theme.PhosphorGreen
import com.nomad.android.ui.theme.PhosphorGreenDim
import com.nomad.android.ui.theme.SurfaceContainerLow
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private val SpaceGroteskBold = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.font.Font(R.font.space_grotesk_bold, FontWeight.Bold),
)
private val SpaceGroteskSemiBold = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.font.Font(R.font.space_grotesk_semi_bold, FontWeight.Medium),
)
private val SpaceGroteskRegular = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.font.Font(R.font.space_grotesk_regular, FontWeight.Normal),
)

private const val SOURCE_POSITION = "source-position"
private const val SOURCE_SAVED_POINTS = "source-saved-points"
private const val SOURCE_ACTIVE_TRACK = "source-active-track"
private const val SOURCE_TRACKBACK = "source-trackback"
private const val SOURCE_DISPLAYED_ROUTE = "source-displayed-route"

private const val LAYER_POSITION = "layer-position"
private const val LAYER_POSITION_RING = "layer-position-ring"
private const val LAYER_SAVED_POINTS = "layer-saved-points"
private const val LAYER_ACTIVE_TRACK = "layer-active-track"
private const val LAYER_TRACKBACK = "layer-trackback"
private const val LAYER_DISPLAYED_ROUTE = "layer-displayed-route"

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
            onRoutes = { viewModel.toggleRoutesPanel() },
            onToggleTracking = {
                if (uiState.data.isTracking) viewModel.stopTracking()
                else viewModel.startTracking()
            },
            onSaveLocation = { viewModel.showSaveLocationDialog() },
            onTrackback = { viewModel.toggleTrackback() },
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

        AnimatedVisibility(
            visible = uiState.data.showRoutesPanel,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            RoutesPanel(
                routes = uiState.data.savedRoutes,
                displayedRouteId = uiState.data.displayedRouteId,
                onSelect = { viewModel.displayRoute(it) },
                onDelete = { viewModel.deleteRoute(it) },
                onClose = { viewModel.toggleRoutesPanel() },
            )
        }

        if (uiState.data.showSaveLocationDialog) {
            SaveLocationDialog(
                onSave = { name ->
                    viewModel.saveLocation(name, "")
                    viewModel.dismissSaveLocationDialog()
                },
                onDismiss = { viewModel.dismissSaveLocationDialog() },
            )
        }

        if (uiState.data.isSelectingRegion) {
            RegionSelectionOverlay(viewModel = viewModel, data = uiState.data)
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
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val mapLibreMapRef = remember { mutableStateOf<MapLibreMap?>(null) }
    val overlaySources = remember { mutableStateOf<Map<String, GeoJsonSource>?>(null) }
    val lastAnimatedLocation = remember { mutableStateOf<LatLng?>(null) }

    remember {
        MapLibre.getInstance(context)
        true
    }

    DisposableEffect(Unit) {
        onDispose {
            mapViewRef.value?.onPause()
            mapViewRef.value?.onStop()
            mapViewRef.value?.onDestroy()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val mv = MapView(ctx)
            mapViewRef.value = mv
            mv.onCreate(null)
            mv.onStart()
            mv.onResume()
            mv.getMapAsync { map ->
                mapLibreMapRef.value = map
                map.uiSettings.isCompassEnabled = false
                map.uiSettings.isLogoEnabled = false
                map.uiSettings.isAttributionEnabled = false

                val tileSet = TileSet("tileset", "https://tile.openstreetmap.org/{z}/{x}/{y}.png")
                val styleBuilder = Style.Builder()
                    .withSource(RasterSource("osm-source", tileSet))
                    .withLayer(RasterLayer("osm-layer", "osm-source"))

                map.setStyle(styleBuilder) {
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

                    val posSource = GeoJsonSource(SOURCE_POSITION)
                    val savedPointsSource = GeoJsonSource(SOURCE_SAVED_POINTS)
                    val activeTrackSource = GeoJsonSource(SOURCE_ACTIVE_TRACK)
                    val trackbackSource = GeoJsonSource(SOURCE_TRACKBACK)
                    val displayedRouteSource = GeoJsonSource(SOURCE_DISPLAYED_ROUTE)

                    it.addSource(posSource)
                    it.addSource(savedPointsSource)
                    it.addSource(activeTrackSource)
                    it.addSource(trackbackSource)
                    it.addSource(displayedRouteSource)

                    it.addLayerAbove(CircleLayer(LAYER_POSITION_RING, SOURCE_POSITION)
                        .withProperties(
                            org.maplibre.android.style.layers.PropertyFactory.circleRadius(14f),
                            org.maplibre.android.style.layers.PropertyFactory.circleColor(Color.parseColor("#1A00FF41")),
                            org.maplibre.android.style.layers.PropertyFactory.circleOpacity(0.6f),
                        ), "osm-layer")
                    it.addLayerAbove(CircleLayer(LAYER_POSITION, SOURCE_POSITION)
                        .withProperties(
                            org.maplibre.android.style.layers.PropertyFactory.circleRadius(6f),
                            org.maplibre.android.style.layers.PropertyFactory.circleColor(Color.parseColor("#00FF41")),
                            org.maplibre.android.style.layers.PropertyFactory.circleOpacity(0.9f),
                        ), LAYER_POSITION_RING)
                    it.addLayerAbove(CircleLayer(LAYER_SAVED_POINTS, SOURCE_SAVED_POINTS)
                        .withProperties(
                            org.maplibre.android.style.layers.PropertyFactory.circleRadius(5f),
                            org.maplibre.android.style.layers.PropertyFactory.circleColor(Color.parseColor("#FFBA3F")),
                            org.maplibre.android.style.layers.PropertyFactory.circleOpacity(0.85f),
                        ), LAYER_POSITION)
                    it.addLayerAbove(LineLayer(LAYER_ACTIVE_TRACK, SOURCE_ACTIVE_TRACK)
                        .withProperties(
                            org.maplibre.android.style.layers.PropertyFactory.lineColor(Color.parseColor("#00FF41")),
                            org.maplibre.android.style.layers.PropertyFactory.lineWidth(3f),
                            org.maplibre.android.style.layers.PropertyFactory.lineOpacity(0.8f),
                        ), LAYER_SAVED_POINTS)
                    it.addLayerAbove(LineLayer(LAYER_TRACKBACK, SOURCE_TRACKBACK)
                        .withProperties(
                            org.maplibre.android.style.layers.PropertyFactory.lineColor(Color.parseColor("#00E639")),
                            org.maplibre.android.style.layers.PropertyFactory.lineWidth(2f),
                            org.maplibre.android.style.layers.PropertyFactory.lineOpacity(0.5f),
                            org.maplibre.android.style.layers.PropertyFactory.lineDasharray(arrayOf(0.5f, 1.5f)),
                        ), LAYER_ACTIVE_TRACK)
                    it.addLayerAbove(LineLayer(LAYER_DISPLAYED_ROUTE, SOURCE_DISPLAYED_ROUTE)
                        .withProperties(
                            org.maplibre.android.style.layers.PropertyFactory.lineColor(Color.parseColor("#00E639")),
                            org.maplibre.android.style.layers.PropertyFactory.lineWidth(2f),
                            org.maplibre.android.style.layers.PropertyFactory.lineOpacity(0.4f),
                        ), LAYER_TRACKBACK)

                    overlaySources.value = mapOf(
                        SOURCE_POSITION to posSource,
                        SOURCE_SAVED_POINTS to savedPointsSource,
                        SOURCE_ACTIVE_TRACK to activeTrackSource,
                        SOURCE_TRACKBACK to trackbackSource,
                        SOURCE_DISPLAYED_ROUTE to displayedRouteSource,
                    )

                    map.addOnCameraIdleListener {
                        try {
                            val bounds = map.projection.visibleRegion.latLngBounds
                            viewModel.updateCameraBounds(
                                north = bounds.latitudeNorth,
                                south = bounds.latitudeSouth,
                                east = bounds.longitudeEast,
                                west = bounds.longitudeWest,
                            )
                        } catch (_: Exception) {
                        }
                    }
                }
            }
            mv
        },
        update = {
            val lat = data.currentLatitude
            val lon = data.currentLongitude
            val currentLatLng = if (lat != null && lon != null) LatLng(lat, lon) else null

            if (data.isAutoCenter && currentLatLng != null && currentLatLng != lastAnimatedLocation.value) {
                lastAnimatedLocation.value = currentLatLng
                mapLibreMapRef.value?.animateCamera(
                    CameraUpdateFactory.newLatLng(currentLatLng),
                    1000
                )
            }

            val sources = overlaySources.value ?: return@AndroidView

            if (lat != null && lon != null) {
                val point = Point.fromLngLat(lon, lat)
                val feature = Feature.fromGeometry(point)
                sources[SOURCE_POSITION]?.setGeoJson(FeatureCollection.fromFeature(feature))
            } else {
                sources[SOURCE_POSITION]?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            }

            updateSavedPointsOverlay(sources[SOURCE_SAVED_POINTS], data.savedPoints)
            updateActiveTrackOverlay(sources[SOURCE_ACTIVE_TRACK], data.activeTrackPoints)
            updateTrackbackOverlay(sources[SOURCE_TRACKBACK], data.activeTrackPoints, data.isTrackback)
            updateDisplayedRouteOverlay(sources[SOURCE_DISPLAYED_ROUTE], data.displayedRoutePoints)
        }
    )
}

private fun updateSavedPointsOverlay(source: GeoJsonSource?, points: List<LocationSavedPointEntity>) {
    if (source == null) return
    if (points.isEmpty()) {
        source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        return
    }
    val features = points.map { pt ->
        Feature.fromGeometry(Point.fromLngLat(pt.longitude, pt.latitude))
    }
    source.setGeoJson(FeatureCollection.fromFeatures(features))
}

private fun updateActiveTrackOverlay(source: GeoJsonSource?, points: List<LocationSnapshotEntity>) {
    if (source == null) return
    if (points.size < 2) {
        source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        return
    }
    val coords = points.map { Point.fromLngLat(it.longitude, it.latitude) }
    val line = LineString.fromLngLats(coords)
    val feature = Feature.fromGeometry(line)
    source.setGeoJson(FeatureCollection.fromFeature(feature))
}

private fun updateTrackbackOverlay(source: GeoJsonSource?, points: List<LocationSnapshotEntity>, isTrackback: Boolean) {
    if (source == null) return
    if (!isTrackback || points.size < 2) {
        source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        return
    }
    val coords = points.reversed().map { Point.fromLngLat(it.longitude, it.latitude) }
    val line = LineString.fromLngLats(coords)
    val feature = Feature.fromGeometry(line)
    source.setGeoJson(FeatureCollection.fromFeature(feature))
}

private fun updateDisplayedRouteOverlay(source: GeoJsonSource?, points: List<LocationSnapshotEntity>) {
    if (source == null) return
    if (points.size < 2) {
        source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        return
    }
    val coords = points.map { Point.fromLngLat(it.longitude, it.latitude) }
    val line = LineString.fromLngLats(coords)
    val feature = Feature.fromGeometry(line)
    source.setGeoJson(FeatureCollection.fromFeature(feature))
}

@Composable
private fun CoordinatesOverlay(data: MapsData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 12.dp, end = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .background(BackgroundDark.copy(alpha = 0.85f), RoundedCornerShape(0.dp))
                .border(2.dp, PhosphorGreenDim, RoundedCornerShape(0.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = data.currentLocationText,
                    color = if (data.currentLocationText == "NO FIX") PhosphorGreenDim else PhosphorGreen,
                    fontFamily = SpaceGroteskSemiBold,
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
                            .background(PhosphorGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${data.activeTrackPoints.size}pts",
                        color = PhosphorGreenDim,
                        fontFamily = SpaceGroteskRegular,
                        fontSize = 10.sp,
                    )
                }
            }
            if (data.isTracking && data.activeTrackPoints.size >= 2) {
                val distance = calculateTrackDistance(data.activeTrackPoints)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatDistance(distance),
                    color = PhosphorGreenDim,
                    fontFamily = SpaceGroteskRegular,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

private fun calculateTrackDistance(points: List<LocationSnapshotEntity>): Double {
    var total = 0.0
    for (i in 1 until points.size) {
        val r = 6371000.0
        val dLat = Math.toRadians(points[i].latitude - points[i - 1].latitude)
        val dLon = Math.toRadians(points[i].longitude - points[i - 1].longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(points[i - 1].latitude)) *
            Math.cos(Math.toRadians(points[i].latitude)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        total += r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
    return total
}

private fun formatDistance(meters: Double): String {
    return if (meters < 1000) "%.0fm".format(meters) else "%.1fkm".format(meters / 1000)
}

@Composable
private fun MapControlsOverlay(
    data: MapsData,
    onAutoCenter: () -> Unit,
    onDownload: () -> Unit,
    onSavedPoints: () -> Unit,
    onRegions: () -> Unit,
    onRoutes: () -> Unit,
    onToggleTracking: () -> Unit,
    onSaveLocation: () -> Unit,
    onTrackback: () -> Unit,
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
                .background(BackgroundDark.copy(alpha = 0.7f), RoundedCornerShape(0.dp))
                .border(2.dp, PhosphorGreenDim, RoundedCornerShape(0.dp))
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MapControlButton(
                icon = {
                    Icon(
                        if (data.isAutoCenter) Icons.Filled.GpsFixed else Icons.Filled.GpsOff,
                        contentDescription = "Auto Center",
                        tint = if (data.isAutoCenter) PhosphorGreen else PhosphorGreenDim,
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
                        tint = TertiaryAmber,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = onRequestPermission,
            )
            MapControlButton(
                icon = {
                    Icon(
                        if (data.isTracking) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = if (data.isTracking) "Stop Tracking" else "Start Tracking",
                        tint = if (data.isTracking) TerminalDanger else PhosphorGreen,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = onToggleTracking,
            )
            if (data.currentLatitude != null) {
                MapControlButton(
                    icon = {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Save Location",
                            tint = TertiaryAmber,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    onClick = onSaveLocation,
                )
            }
            if (data.isTracking && data.activeTrackPoints.size >= 2) {
                MapControlButton(
                    icon = {
                        Icon(
                            Icons.Filled.Undo,
                            contentDescription = "Trackback",
                            tint = if (data.isTrackback) PhosphorGreen else PhosphorGreenDim,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    onClick = onTrackback,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp)
                .background(BackgroundDark.copy(alpha = 0.7f), RoundedCornerShape(0.dp))
                .border(2.dp, PhosphorGreenDim, RoundedCornerShape(0.dp))
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MapControlButton(
                icon = {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Download Region",
                        tint = PhosphorGreen,
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
                        tint = PhosphorGreen,
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
                        tint = PhosphorGreen,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = onRegions,
            )
            MapControlButton(
                icon = {
                    Icon(
                        Icons.Filled.Timeline,
                        contentDescription = "Routes",
                        tint = PhosphorGreen,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = onRoutes,
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
            .size(48.dp)
            .clip(RoundedCornerShape(0.dp))
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
            .background(BackgroundDark.copy(alpha = 0.95f), RoundedCornerShape(0.dp))
            .border(2.dp, PhosphorGreenDim, RoundedCornerShape(0.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "SAVED LOCATIONS (${points.size})",
                    color = PhosphorGreen,
                    fontFamily = SpaceGroteskBold,
                    fontSize = 14.sp,
                )
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = PhosphorGreenDim,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onClose),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (points.isEmpty()) {
                Text(
                    text = "No saved locations",
                    color = PhosphorGreenDim,
                    fontFamily = SpaceGroteskRegular,
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
                                    color = PhosphorGreen,
                                    fontFamily = SpaceGroteskSemiBold,
                                    fontSize = 12.sp,
                                )
                                Text(
                                    text = "%.6f, %.6f".format(point.latitude, point.longitude),
                                    color = PhosphorGreenDim,
                                    fontFamily = SpaceGroteskRegular,
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
private fun RoutesPanel(
    routes: List<TrackRouteEntity>,
    displayedRouteId: String?,
    onSelect: (String?) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 60.dp)
            .background(BackgroundDark.copy(alpha = 0.95f), RoundedCornerShape(0.dp))
            .border(2.dp, PhosphorGreenDim, RoundedCornerShape(0.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "SAVED ROUTES (${routes.size})",
                    color = PhosphorGreen,
                    fontFamily = SpaceGroteskBold,
                    fontSize = 14.sp,
                )
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = PhosphorGreenDim,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onClose),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (routes.isEmpty()) {
                Text(
                    text = "No saved routes. Start tracking to record a route.",
                    color = PhosphorGreenDim,
                    fontFamily = SpaceGroteskRegular,
                    fontSize = 12.sp,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(routes, key = { it.id }) { route ->
                        val isSelected = route.id == displayedRouteId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) PhosphorGreen.copy(alpha = 0.1f) else SurfaceContainerLow,
                                    RoundedCornerShape(0.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) PhosphorGreen else PhosphorGreenDim,
                                    RoundedCornerShape(0.dp)
                                )
                                .clickable {
                                    onSelect(if (isSelected) null else route.id)
                                }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = route.name.uppercase(),
                                    color = if (isSelected) PhosphorGreen else PhosphorGreenDim,
                                    fontFamily = SpaceGroteskSemiBold,
                                    fontSize = 12.sp,
                                )
                                Text(
                                    text = "${route.pointCount}pts | ${formatDistance(route.totalDistanceMeters)}",
                                    color = PhosphorGreenDim,
                                    fontFamily = SpaceGroteskRegular,
                                    fontSize = 10.sp,
                                )
                            }
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = TerminalDanger,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onDelete(route.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveLocationDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(BackgroundDark, RoundedCornerShape(0.dp))
                .border(2.dp, PhosphorGreen, RoundedCornerShape(0.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "SAVE CURRENT LOCATION",
                color = PhosphorGreen,
                fontFamily = SpaceGroteskBold,
                fontSize = 16.sp,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = {
                    Text(
                        "Location name",
                        color = PhosphorGreenDim,
                        fontFamily = SpaceGroteskRegular,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (name.isNotBlank()) onSave(name)
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PhosphorGreen,
                    unfocusedTextColor = PhosphorGreen,
                    focusedBorderColor = PhosphorGreen,
                    unfocusedBorderColor = PhosphorGreenDim,
                    cursorColor = PhosphorGreen,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .border(2.dp, PhosphorGreenDim, RoundedCornerShape(0.dp))
                        .background(SurfaceContainerLow, RoundedCornerShape(0.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "CANCEL",
                        color = PhosphorGreenDim,
                        fontFamily = SpaceGroteskSemiBold,
                        fontSize = 13.sp,
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            2.dp,
                            if (name.isNotBlank()) PhosphorGreen else PhosphorGreenDim,
                            RoundedCornerShape(0.dp)
                        )
                        .background(
                            if (name.isNotBlank()) PhosphorGreen.copy(alpha = 0.1f) else SurfaceContainerLow,
                            RoundedCornerShape(0.dp),
                        )
                        .clickable { if (name.isNotBlank()) onSave(name) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "SAVE",
                        color = if (name.isNotBlank()) PhosphorGreen else PhosphorGreenDim,
                        fontFamily = SpaceGroteskBold,
                        fontSize = 13.sp,
                    )
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
            .background(BackgroundDark.copy(alpha = 0.95f), RoundedCornerShape(0.dp))
            .border(2.dp, PhosphorGreenDim, RoundedCornerShape(0.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "OFFLINE REGIONS (${regions.size})",
                    color = PhosphorGreen,
                    fontFamily = SpaceGroteskBold,
                    fontSize = 14.sp,
                )
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = PhosphorGreenDim,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onClose),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (regions.isEmpty()) {
                Text(
                    text = "No offline regions. Tap + to download.",
                    color = PhosphorGreenDim,
                    fontFamily = SpaceGroteskRegular,
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
                                    color = PhosphorGreen,
                                    fontFamily = SpaceGroteskSemiBold,
                                    fontSize = 12.sp,
                                )
                                Text(
                                    text = "Z${region.minZoom}-${region.maxZoom} | ${region.tileCount} tiles | ${"%.1f".format(region.sizeBytes / 1_048_576.0)}MB",
                                    color = PhosphorGreenDim,
                                    fontFamily = SpaceGroteskRegular,
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
    data: MapsData,
) {
    var minZoom by remember { mutableStateOf(12) }
    var maxZoom by remember { mutableStateOf(15) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(BackgroundDark, RoundedCornerShape(0.dp))
                .border(2.dp, PhosphorGreen, RoundedCornerShape(0.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "DOWNLOAD OFFLINE MAP",
                color = PhosphorGreen,
                fontFamily = SpaceGroteskBold,
                fontSize = 16.sp,
            )

            Text(
                text = "Navigate to the area you want to download, then set zoom levels.",
                color = PhosphorGreenDim,
            fontFamily = SpaceGroteskBold,
                fontSize = 11.sp,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Min Zoom: $minZoom",
                    color = PhosphorGreen,
                    fontFamily = SpaceGroteskRegular,
                    fontSize = 13.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(10, 12, 14).forEach { z ->
                        Box(
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (minZoom == z) PhosphorGreen else PhosphorGreenDim,
                                    RoundedCornerShape(0.dp)
                                )
                                .background(
                                    if (minZoom == z) PhosphorGreen.copy(alpha = 0.1f) else SurfaceContainerLow,
                                    RoundedCornerShape(0.dp),
                                )
                                .clickable {
                                    minZoom = z
                                    viewModel.setZoomRange(z, maxZoom)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "$z",
                                color = if (minZoom == z) PhosphorGreen else PhosphorGreenDim,
                                fontFamily = SpaceGroteskRegular,
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
                    color = PhosphorGreen,
                    fontFamily = SpaceGroteskRegular,
                    fontSize = 13.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(14, 15, 16, 17).forEach { z ->
                        Box(
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (maxZoom == z) PhosphorGreen else PhosphorGreenDim,
                                    RoundedCornerShape(0.dp)
                                )
                                .background(
                                    if (maxZoom == z) PhosphorGreen.copy(alpha = 0.1f) else SurfaceContainerLow,
                                    RoundedCornerShape(0.dp),
                                )
                                .clickable {
                                    maxZoom = z
                                    viewModel.setZoomRange(minZoom, z)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "$z",
                                color = if (maxZoom == z) PhosphorGreen else PhosphorGreenDim,
                                fontFamily = SpaceGroteskRegular,
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
                        .border(2.dp, PhosphorGreenDim, RoundedCornerShape(0.dp))
                        .background(SurfaceContainerLow, RoundedCornerShape(0.dp))
                        .clickable { viewModel.cancelRegionSelection() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "CANCEL",
                        color = PhosphorGreenDim,
                        fontFamily = SpaceGroteskSemiBold,
                        fontSize = 13.sp,
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(2.dp, PhosphorGreen, RoundedCornerShape(0.dp))
                        .background(PhosphorGreen.copy(alpha = 0.1f), RoundedCornerShape(0.dp))
                        .clickable {
                            viewModel.startDownload(
                                regionName = "Region",
                                north = data.cameraNorth,
                                south = data.cameraSouth,
                                east = data.cameraEast,
                                west = data.cameraWest,
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "USE CURRENT VIEW",
                        color = PhosphorGreen,
                        fontFamily = SpaceGroteskBold,
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
            .background(BackgroundDark.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(BackgroundDark, RoundedCornerShape(0.dp))
                .border(2.dp, TertiaryAmber, RoundedCornerShape(0.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "DOWNLOADING MAP TILES",
                color = TertiaryAmber,
                fontFamily = SpaceGroteskBold,
                fontSize = 14.sp,
            )
            if (progress != null) {
                val pct = if (progress.total > 0) progress.downloaded * 100 / progress.total else 0
                val filledBlocks = pct / 5
                val emptyBlocks = 20 - filledBlocks
                val bar = "[" + "\u2588".repeat(filledBlocks) + "\u2591".repeat(emptyBlocks) + "]"
                Text(
                    text = bar,
                    color = PhosphorGreen,
                    fontFamily = SpaceGroteskRegular,
                    fontSize = 14.sp,
                )
                Text(
                    text = "$pct% | ${progress.downloaded} / ${progress.total} tiles",
                    color = PhosphorGreenDim,
                    fontFamily = SpaceGroteskRegular,
                    fontSize = 12.sp,
                )
                val sizeMB = "%.1f".format(progress.bytesDownloaded / 1_048_576.0)
                val totalMB = "%.1f".format(progress.estimatedTotalBytes / 1_048_576.0)
                Text(
                    text = "$sizeMB / $totalMB MB",
                    color = PhosphorGreenDim,
                    fontFamily = SpaceGroteskRegular,
                    fontSize = 11.sp,
                )
            }
        }
    }
}
