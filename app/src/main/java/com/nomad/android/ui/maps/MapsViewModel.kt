package com.nomad.android.ui.maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nomad.android.data.Result
import com.nomad.android.data.local.entity.LocationSavedPointEntity
import com.nomad.android.data.local.entity.LocationSnapshotEntity
import com.nomad.android.data.repository.LocationRepository
import com.nomad.android.data.repository.MapsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapsData(
    val layers: List<MapsRepository.MapLayer> = emptyList(),
    val hasOfflineTiles: Boolean = false,
    val isMapInitialized: Boolean = false,
    val regionName: String? = null,
    val currentLocationText: String = "NO FIX",
    val currentLatitude: Double? = null,
    val currentLongitude: Double? = null,
    val isTracking: Boolean = false,
    val trackingSnapshots: List<LocationSnapshotEntity> = emptyList(),
    val savedPoints: List<LocationSavedPointEntity> = emptyList(),
    val snapshotCount: Int = 0,
    val hasLocationPermission: Boolean = false
)

data class MapsUiState(
    val data: MapsData = MapsData(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MapsViewModel @Inject constructor(
    private val mapsRepository: MapsRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapsUiState(isLoading = true))
    val uiState: StateFlow<MapsUiState> = _uiState.asStateFlow()

    private val _locationPermissionGranted = MutableStateFlow(false)

    init {
        loadMapData()
        observeLocation()
    }

    private fun observeLocation() {
        viewModelScope.launch {
            combine(
                locationRepository.currentLocation,
                locationRepository.isTracking,
                locationRepository.savedPoints,
                locationRepository.recentSnapshots
            ) { location, isTracking, savedPoints, snapshots ->
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
                            trackingSnapshots = snapshots.takeLast(50),
                            snapshotCount = snapshots.size
                        )
                    )
                }
            }.collect {}
        }
    }

    fun loadMapData() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val result = mapsRepository.getAvailableLayers().first()
            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            data = MapsData(
                                layers = result.data,
                                hasOfflineTiles = mapsRepository.hasOfflineTiles(),
                                isMapInitialized = true,
                                regionName = mapsRepository.getDownloadedRegionName()
                            )
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
            }
        }
    }

    fun toggleLayer(layerId: String) {
        val currentLayers = _uiState.value.data.layers
        val updatedLayers = currentLayers.map { layer ->
            if (layer.id == layerId) layer.copy(isEnabled = !layer.isEnabled) else layer
        }
        _uiState.update { it.copy(data = it.data.copy(layers = updatedLayers)) }
    }

    fun setLocationPermissionGranted(granted: Boolean) {
        _locationPermissionGranted.value = granted
        _uiState.update { it.copy(data = it.data.copy(hasLocationPermission = granted)) }
        if (granted) {
            locationRepository.requestCurrentLocation()
        }
    }

    fun startTracking() {
        if (_locationPermissionGranted.value) {
            locationRepository.startTracking()
        }
    }

    fun stopTracking() {
        locationRepository.stopTracking()
    }

    fun saveLocation(name: String, notes: String) {
        viewModelScope.launch {
            locationRepository.saveCurrentLocation(name, notes)
        }
    }

    fun deleteSavedPoint(id: String) {
        viewModelScope.launch {
            locationRepository.deleteSavedPoint(id)
        }
    }

    fun requestCurrentLocation() {
        if (_locationPermissionGranted.value) {
            locationRepository.requestCurrentLocation()
        }
    }
}
