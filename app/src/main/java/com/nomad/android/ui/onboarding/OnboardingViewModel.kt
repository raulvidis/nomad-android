package com.nomad.android.ui.onboarding

import android.app.ActivityManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nomad.android.data.ai.AIEngine
import com.nomad.android.data.ai.LiteRTLMEngine
import com.nomad.android.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HardwareInfo(
    val totalRamMB: Long,
    val hasAICore: Boolean,
    val hasNPU: Boolean,
    val hasGPU: Boolean,
    val availableStorageMB: Long
)

data class OnboardingData(
    val currentStep: Int = 0,
    val totalSteps: Int = 5,
    val hardwareInfo: HardwareInfo? = null,
    val selectedModel: String = "",
    val downloadProgress: Float = 0f,
    val isComplete: Boolean = false
)

data class OnboardingUiState(
    val data: OnboardingData = OnboardingData(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val aiEngine: AIEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState(isLoading = true))
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    val isOnboardingComplete: Flow<Boolean> = settingsRepository.isOnboardingComplete

    init {
        scanHardware()
    }

    private fun scanHardware() {
        viewModelScope.launch {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            val totalRamMB = memoryInfo.totalMem / (1024 * 1024)

            val recommendedModel = LiteRTLMEngine.recommendedVariant(totalRamMB)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    data = OnboardingData(
                        currentStep = 0,
                        hardwareInfo = HardwareInfo(
                            totalRamMB = totalRamMB,
                            hasAICore = false,
                            hasNPU = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q,
                            hasGPU = true,
                            availableStorageMB = android.os.StatFs(context.filesDir.path).availableBytes / (1024 * 1024)
                        ),
                        selectedModel = recommendedModel.displayName
                    )
                )
            }
        }
    }

    fun nextStep() {
        val current = _uiState.value.data
        if (current.currentStep < current.totalSteps - 1) {
            _uiState.update {
                it.copy(data = current.copy(currentStep = current.currentStep + 1))
            }

            // Simulate download progress on step 3
            if (current.currentStep + 1 == 3) {
                simulateDownload()
            }
        }
    }

    fun selectModel(modelName: String) {
        _uiState.update {
            it.copy(data = it.data.copy(selectedModel = modelName))
        }
    }

    private fun simulateDownload() {
        viewModelScope.launch {
            for (i in 0..100 step 5) {
                _uiState.update {
                    it.copy(data = it.data.copy(downloadProgress = i / 100f))
                }
                delay(100)
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.completeOnboarding()
            _uiState.update {
                it.copy(data = it.data.copy(isComplete = true))
            }
        }
    }
}
