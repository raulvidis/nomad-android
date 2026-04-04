package com.nomad.android.ui.emergency

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class EmergencyData(
    val dummy: Boolean = true
)

data class EmergencyUiState(
    val data: EmergencyData = EmergencyData(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EmergencyViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(EmergencyUiState())
    val uiState: StateFlow<EmergencyUiState> = _uiState.asStateFlow()
}
