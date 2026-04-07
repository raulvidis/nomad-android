package com.nomad.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nomad.android.data.Result
import com.nomad.android.data.ai.AIEngineStatus
import com.nomad.android.data.content.ContentPackManager
import com.nomad.android.data.content.PackStatus
import com.nomad.android.data.repository.SettingsRepository
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
    val storageMetrics: SettingsRepository.StorageMetrics? = null
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

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val storageMetrics = settingsRepository.getStorageMetrics()
            val packs = contentPackManager.getAvailablePacks().first()

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
                        contentPacks = contentPacks
                    )
                )
            }
        }
    }

    fun downloadPack(packId: String) {
        val pack = _uiState.value.data.contentPacks.find { it.id == packId } ?: return
        if (pack.isDownloaded || pack.isDownloading) return

        _uiState.update { it.copy(error = null) }

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
                // Mark as downloaded without full reload (avoids loading flash)
                _uiState.update {
                    it.copy(data = it.data.copy(
                        contentPacks = it.data.contentPacks.map { p ->
                            if (p.id == packId) p.copy(
                                isDownloading = false,
                                isDownloaded = true,
                                downloadProgress = 1f
                            ) else p
                        },
                        storageMetrics = settingsRepository.getStorageMetrics()
                    ))
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Download failed: ${e.message}",
                        data = it.data.copy(
                            contentPacks = it.data.contentPacks.map { p ->
                                if (p.id == packId) p.copy(isDownloading = false, downloadProgress = 0f) else p
                            }
                        )
                    )
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

