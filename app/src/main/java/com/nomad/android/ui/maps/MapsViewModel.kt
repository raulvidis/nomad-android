package com.nomad.android.ui.maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nomad.android.data.Result
import com.nomad.android.data.repository.MapsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapsData(
    val layers: List<MapsRepository.MapLayer> = emptyList(),
    val hasOfflineTiles: Boolean = false,
    val isMapInitialized: Boolean = false
)

data class MapsUiState(
    val data: MapsData = MapsData(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MapsViewModel @Inject constructor(
    private val mapsRepository: MapsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapsUiState(isLoading = true))
    val uiState: StateFlow<MapsUiState> = _uiState.asStateFlow()

    init {
        loadMapData()
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
                                isMapInitialized = true
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
}
