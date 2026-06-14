package com.nomad.android.ui.maps

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nomad.android.data.maps.DownloadProgress
import com.nomad.android.data.maps.OfflineRegion
import com.nomad.android.data.maps.OfflineTileManager
import com.nomad.android.data.maps.TileCalculator
import com.nomad.android.data.repository.LocationRepository
import com.nomad.android.data.repository.MapsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.collections.emptyList

data class MapsData(
    val isMapInitialized: Boolean = false,
    val currentLocationText: String = "NO FIX",
    val currentLatitude: Double? = null,
    val currentLongitude: Double? = null,
    val isTracking: Boolean = false,
    val savedPoints: List<com.nomad.android.data.local.entity.LocationSavedPointEntity> = emptyList(),
    val snapshotCount: Int = 0,
    val hasLocationPermission: Boolean = false,
    val downloadedRegions: List<OfflineRegion> = emptyList(),
    val isDownloading: Boolean = false,
    val downloadProgress: DownloadProgress? = null,
    val isSelectingRegion: Boolean = false,
    val selectedMinZoom: Int = 12,
    val selectedMaxZoom: Int = 15,
    val isAutoCenter: Boolean = true,
    val showSavedPanel: Boolean = false,
    val showRegionList: Boolean = false,
    val showRoutesPanel: Boolean = false,
    val regionName: String? = null,
    val cameraNorth: Double = 48.87,
    val cameraSouth: Double = 48.85,
    val cameraEast: Double = 2.36,
    val cameraWest: Double = 2.34,
    val activeRouteId: String? = null,
    val activeTrackPoints: List<com.nomad.android.data.local.entity.LocationSnapshotEntity> = emptyList(),
    val savedRoutes: List<com.nomad.android.data.local.entity.TrackRouteEntity> = emptyList(),
    val displayedRouteId: String? = null,
    val displayedRoutePoints: List<com.nomad.android.data.local.entity.LocationSnapshotEntity> = emptyList(),
    val isTrackback: Boolean = false,
    val showSaveLocationDialog: Boolean = false,
)

data class MapsUiState(
    val data: MapsData = MapsData(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MapsViewModel @Inject constructor(
    private val mapsRepository: MapsRepository,
    private val locationRepository: LocationRepository,
    private val offlineTileManager: OfflineTileManager,
    application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapsUiState(isLoading = true))
    val uiState: StateFlow<MapsUiState> = _uiState.asStateFlow()

    private val _locationPermissionGranted = MutableStateFlow(false)

    init {
        val hasPermission = ContextCompat.checkSelfPermission(
            application,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            _locationPermissionGranted.value = true
            _uiState.update { it.copy(data = it.data.copy(hasLocationPermission = true)) }
            locationRepository.requestCurrentLocation()
        }
        loadMapData()
        observeLocation()
    }

    private fun observeLocation() {
        viewModelScope.launch {
            combine(
                locationRepository.currentLocation,
                locationRepository.isTracking,
                locationRepository.savedPoints,
                locationRepository.recentSnapshots,
                locationRepository.savedRoutes
            ) { location, isTracking, savedPoints, recentSnapshots, savedRoutes ->
                val locText = location?.let {
                    "%.6f, %.6f".format(it.latitude, it.longitude)
                } ?: "NO FIX"
                _uiState.update { state ->
                    state.copy(
                        data = state.data.copy(
                            currentLocationText = locText,
                            currentLatitude = location?.latitude,
                            currentLongitude = location?.longitude,
                            isTracking = isTracking,
                            savedPoints = savedPoints,
                            snapshotCount = recentSnapshots.size,
                            savedRoutes = savedRoutes,
                        )
                    )
                }
            }.collect {}
        }
    }

    fun loadMapData() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val regions = offlineTileManager.getDownloadedRegions()
            val regionName = mapsRepository.getDownloadedRegionName()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    data = it.data.copy(
                        isMapInitialized = true,
                        downloadedRegions = regions,
                        regionName = regionName
                    )
                )
            }
        }
    }

    fun setLocationPermissionGranted(granted: Boolean) {
        _locationPermissionGranted.value = granted
        _uiState.update { it.copy(data = it.data.copy(hasLocationPermission = granted)) }
        if (granted) locationRepository.requestCurrentLocation()
    }

    fun startTracking() {
        if (!_locationPermissionGranted.value) return
        viewModelScope.launch {
            val result = locationRepository.beginRoute()
            val routeId = result.getOrNull() ?: return@launch
            locationRepository.startTracking()
            _uiState.update { it.copy(data = it.data.copy(activeRouteId = routeId)) }
            observeActiveTrack(routeId)
        }
    }

    private fun observeActiveTrack(routeId: String) {
        viewModelScope.launch {
            locationRepository.observeRoutePoints(routeId).collect { points ->
                _uiState.update { it.copy(data = it.data.copy(activeTrackPoints = points)) }
            }
        }
    }

    fun stopTracking() {
        viewModelScope.launch {
            locationRepository.endRoute()
            locationRepository.stopTracking()
            _uiState.update {
                it.copy(data = it.data.copy(
                    isTracking = false,
                    activeRouteId = null,
                    activeTrackPoints = emptyList(),
                    isTrackback = false
                ))
            }
        }
    }

    fun saveLocation(name: String, notes: String) {
        viewModelScope.launch { locationRepository.saveCurrentLocation(name, notes) }
    }

    fun deleteSavedPoint(id: String) {
        viewModelScope.launch { locationRepository.deleteSavedPoint(id) }
    }

    fun requestCurrentLocation() {
        if (_locationPermissionGranted.value) locationRepository.requestCurrentLocation()
    }

    fun toggleAutoCenter() {
        _uiState.update { it.copy(data = it.data.copy(isAutoCenter = !it.data.isAutoCenter)) }
    }

    fun toggleSavedPanel() {
        _uiState.update { it.copy(data = it.data.copy(showSavedPanel = !it.data.showSavedPanel)) }
    }

    fun toggleRegionList() {
        _uiState.update { it.copy(data = it.data.copy(showRegionList = !it.data.showRegionList)) }
    }

    fun toggleRoutesPanel() {
        _uiState.update { it.copy(data = it.data.copy(showRoutesPanel = !it.data.showRoutesPanel)) }
    }

    fun toggleTrackback() {
        _uiState.update { it.copy(data = it.data.copy(isTrackback = !it.data.isTrackback)) }
    }

    fun showSaveLocationDialog() {
        _uiState.update { it.copy(data = it.data.copy(showSaveLocationDialog = true)) }
    }

    fun dismissSaveLocationDialog() {
        _uiState.update { it.copy(data = it.data.copy(showSaveLocationDialog = false)) }
    }

    fun displayRoute(routeId: String?) {
        if (routeId == null) {
            _uiState.update { it.copy(data = it.data.copy(displayedRouteId = null, displayedRoutePoints = emptyList())) }
            return
        }
        viewModelScope.launch {
            val points = locationRepository.getRoutePoints(routeId)
            _uiState.update { it.copy(data = it.data.copy(displayedRouteId = routeId, displayedRoutePoints = points)) }
        }
    }

    fun deleteRoute(id: String) {
        viewModelScope.launch { locationRepository.deleteRoute(id) }
    }

    fun startRegionSelection() {
        _uiState.update { it.copy(data = it.data.copy(isSelectingRegion = true)) }
    }

    fun cancelRegionSelection() {
        _uiState.update { it.copy(data = it.data.copy(isSelectingRegion = false)) }
    }

    fun setZoomRange(min: Int, max: Int) {
        _uiState.update { it.copy(data = it.data.copy(selectedMinZoom = min, selectedMaxZoom = max)) }
    }

    fun updateCameraBounds(north: Double, south: Double, east: Double, west: Double) {
        _uiState.update {
            it.copy(data = it.data.copy(
                cameraNorth = north,
                cameraSouth = south,
                cameraEast = east,
                cameraWest = west
            ))
        }
    }

    fun startDownload(
        regionName: String,
        north: Double,
        south: Double,
        east: Double,
        west: Double
    ) {
        val minZoom = _uiState.value.data.selectedMinZoom
        val maxZoom = _uiState.value.data.selectedMaxZoom

        val tiles = TileCalculator.getTilesForBounds(north, south, east, west, minZoom, maxZoom)
        val estimatedSize = TileCalculator.estimateSizeBytes(tiles.size)

        val id = offlineTileManager.createRegion(
            regionName.ifBlank { "Region" },
            north, south, east, west,
            minZoom, maxZoom
        )

        _uiState.update {
            it.copy(data = it.data.copy(isDownloading = true, isSelectingRegion = false))
        }

        viewModelScope.launch {
            var downloadError: String? = null
            offlineTileManager.downloadRegion(
                id, north, south, east, west,
                minZoom, maxZoom
            ).collect { progress ->
                _uiState.update {
                    it.copy(data = it.data.copy(downloadProgress = progress))
                }
                if (progress.isComplete && progress.error != null) {
                    downloadError = progress.error
                }
            }
            val regions = offlineTileManager.getDownloadedRegions()
            _uiState.update {
                it.copy(
                    data = it.data.copy(
                        isDownloading = false,
                        downloadProgress = null,
                        downloadedRegions = regions
                    ),
                    error = downloadError
                )
            }
        }
    }

    fun deleteRegion(regionId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                offlineTileManager.deleteRegion(regionId)
            }
            val regions = withContext(Dispatchers.IO) {
                offlineTileManager.getDownloadedRegions()
            }
            _uiState.update {
                it.copy(data = it.data.copy(downloadedRegions = regions))
            }
        }
    }
}
