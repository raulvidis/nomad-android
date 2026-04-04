package com.nomad.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nomad.android.data.Result
import com.nomad.android.data.ai.AIEngineStatus
import com.nomad.android.data.content.ContentPackManager
import com.nomad.android.data.content.PackStatus
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
    val isDownloaded: Boolean,
    val downloadProgress: Float = 0f,
    val isDownloading: Boolean = false
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
    private val aiEngineStatus: AIEngineStatus,
    private val contentPackManager: ContentPackManager
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
                contentPackManager.getAvailablePacks().collect { packs ->
                    val contentPacks = packs.map { pack ->
                        ContentPackInfo(
                            id = pack.id,
                            name = pack.name,
                            type = pack.type,
                            size = contentPackManager.formatSize(pack.sizeBytes),
                            isDownloaded = pack.status == PackStatus.DOWNLOADED,
                            isDownloading = pack.status == PackStatus.DOWNLOADING
                        )
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            data = SettingsData(
                                aiStatus = aiEngineStatus,
                                storageMetrics = storageMetrics,
                                currentTheme = theme,
                                contentPacks = contentPacks
                            )
                        )
                    }
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

    fun downloadPack(packId: String) {
        val pack = _uiState.value.data.contentPacks.find { it.id == packId } ?: return
        if (pack.isDownloaded || pack.isDownloading) return

        _uiState.update {
            it.copy(data = it.data.copy(
                contentPacks = it.data.contentPacks.map { p ->
                    if (p.id == packId) p.copy(isDownloading = true, downloadProgress = 0f) else p
                }
            ))
        }

        viewModelScope.launch {
            try {
                contentPackManager.downloadPack(packId).collect { progress ->
                    _uiState.update {
                        it.copy(data = it.data.copy(
                            contentPacks = it.data.contentPacks.map { p ->
                                if (p.id == packId) p.copy(downloadProgress = progress) else p
                            }
                        ))
                    }
                }
                // Reload to get updated pack status
                loadSettings()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(data = it.data.copy(
                        contentPacks = it.data.contentPacks.map { p ->
                            if (p.id == packId) p.copy(isDownloading = false, downloadProgress = 0f) else p
                        }
                    ))
                }
            }
        }
    }

    fun deletePack(packId: String) {
        viewModelScope.launch {
            contentPackManager.deletePack(packId)
            loadSettings()
        }
    }
}

data class ThemeOption(
    val id: String,
    val name: String,
    val description: String
)
