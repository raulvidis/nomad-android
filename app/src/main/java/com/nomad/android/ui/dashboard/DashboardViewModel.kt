package com.nomad.android.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nomad.android.data.Result
import com.nomad.android.data.ai.AIEngineStatus
import com.nomad.android.data.ai.AIEngineType
import com.nomad.android.data.repository.ContentPackRepository
import com.nomad.android.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardData(
    val aiStatus: AIEngineStatus? = null,
    val storageMetrics: SettingsRepository.StorageMetrics? = null,
    val contentPackCount: Int = 0,
    val recentActivity: List<String> = emptyList()
)

data class DashboardUiState(
    val data: DashboardData = DashboardData(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val contentPackRepository: ContentPackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun refreshStatus() {
        loadDashboard()
    }

    private fun loadDashboard() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val storageMetrics = settingsRepository.getStorageMetrics()

            val aiStatus = AIEngineStatus(
                engineType = AIEngineType.FALLBACK,
                isReady = false,
                modelName = "Checking...",
                ramRequired = "Calculating...",
                modelSize = "N/A"
            )

            // Collect content pack count from the database
            var packCount = 0
            contentPackRepository.getAllPacks().collect { result ->
                if (result is Result.Success) {
                    packCount = result.data.size
                }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    data = DashboardData(
                        aiStatus = aiStatus,
                        storageMetrics = storageMetrics,
                        contentPackCount = packCount,
                        recentActivity = listOf(
                            "System initialized",
                            "AI engine loaded",
                            "${packCount} content packs scanned"
                        )
                    )
                )
            }
        }
    }
}
