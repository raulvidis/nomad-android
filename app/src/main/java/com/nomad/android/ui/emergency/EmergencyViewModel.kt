package com.nomad.android.ui.emergency

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class EmergencyContact(
    val name: String,
    val number: String,
    val description: String
)

data class EmergencyData(
    val isSOSActive: Boolean = false,
    val checklistItems: Map<String, Boolean> = mapOf(
        "Stay calm" to false,
        "Check surroundings for danger" to false,
        "Call emergency services" to false,
        "Administer first aid if needed" to false,
        "Signal for help" to false
    ),
    val contacts: List<EmergencyContact> = listOf(
        EmergencyContact("Emergency Services", "911", "Police, Fire, Medical"),
        EmergencyContact("Poison Control", "1-800-222-1222", "Toxic exposure"),
        EmergencyContact("Search & Rescue", "1-800-555-HELP", "Lost/missing persons")
    )
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

    fun toggleSOS() {
        _uiState.update {
            it.copy(data = it.data.copy(isSOSActive = !it.data.isSOSActive))
        }
    }

    fun acknowledgeChecklistItem(item: String) {
        val updated = _uiState.value.data.checklistItems.toMutableMap()
        updated[item] = !(updated[item] ?: false)
        _uiState.update {
            it.copy(data = it.data.copy(checklistItems = updated.toMap()))
        }
    }
}
