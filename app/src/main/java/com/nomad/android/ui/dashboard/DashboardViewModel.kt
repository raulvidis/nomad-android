package com.nomad.android.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nomad.android.data.Result
import com.nomad.android.data.ai.AIEngineStatus
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
    private val settingsRepository: SettingsRepository
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

            val aiStatusResult = Result.success(
                AIEngineStatus(
                    engineType = com.nomad.android.data.ai.AIEngineType.FALLBACK,
                    isReady = false,
                    modelName = "Checking...",
                    ramRequired = "Calculating...",
                    modelSize = "N/A"
                )
            )

            val contentPacksResult = settingsRepository.getAllSettings()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    data = DashboardData(
                        aiStatus = aiStatusResult.getOrNull(),
                        storageMetrics = storageMetrics,
                        contentPackCount = 0,
                        recentActivity = listOf(
                            "System initialized",
                            "AI engine loaded",
                            "Content packs scanned"
                        )
                    )
                )
            }
        }
    }
}
