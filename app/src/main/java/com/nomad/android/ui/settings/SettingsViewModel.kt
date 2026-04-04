package com.nomad.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nomad.android.data.Result
import com.nomad.android.data.ai.AIEngineStatus
import com.nomad.android.data.repository.MapsRepository
import com.nomad.android.data.repository.SettingsRepository
import com.nomad.android.data.repository.SettingsRepository.Companion.THEME_AMBER
import com.nomad.android.data.repository.SettingsRepository.Companion.THEME_BLUE
import com.nomad.android.data.repository.SettingsRepository.Companion.THEME_CRT_GREEN
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContentPackInfo(
    val id: String,
    val name: String,
    val type: String,
    val size: String,
    val isDownloaded: Boolean
)

data class SettingsData(
    val aiStatus: AIEngineStatus? = null,
    val contentPacks: List<ContentPackInfo> = emptyList(),
    val storageMetrics: SettingsRepository.StorageMetrics? = null,
    val currentTheme: String = THEME_CRT_GREEN
)

data class SettingsUiState(
    val data: SettingsData = SettingsData(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val aiEngineStatus: AIEngineStatus
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = true))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val availableThemes = listOf(
        ThemeOption(THEME_CRT_GREEN, "CRT Green", "Classic PipBoy green phosphor"),
        ThemeOption(THEME_AMBER, "Amber Phosphor", "Warm amber display"),
        ThemeOption(THEME_BLUE, "Blue Screen", "Cool blue terminal")
    )

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val storageMetrics = settingsRepository.getStorageMetrics()

            settingsRepository.getTheme().collect { theme ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        data = SettingsData(
                            aiStatus = aiEngineStatus,
                            storageMetrics = storageMetrics,
                            currentTheme = theme,
                            contentPacks = listOf(
                                ContentPackInfo("essentials", "Survival Essentials", "guide", "15 MB", false),
                                ContentPackInfo("wiki_mini", "Wikipedia Mini", "knowledge", "120 MB", false),
                                ContentPackInfo("maps_local", "Local Maps", "maps", "50 MB", false)
                            )
                        )
                    )
                }
            }
        }
    }

    fun setTheme(themeName: String) {
        viewModelScope.launch {
            settingsRepository.setTheme(themeName)
            _uiState.update { it.copy(data = it.data.copy(currentTheme = themeName)) }
        }
    }
}

data class ThemeOption(
    val id: String,
    val name: String,
    val description: String
)
