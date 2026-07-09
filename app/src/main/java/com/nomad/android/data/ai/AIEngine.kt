package com.nomad.android.data.ai

import kotlinx.coroutines.flow.Flow

interface AIEngine {
    suspend fun generate(prompt: String, context: List<String>, imagePath: String? = null): String
    fun generateStream(prompt: String, context: List<String>, imagePath: String? = null): Flow<String>
    suspend fun isAvailable(): Boolean
    fun getModelName(): String
    fun getDeviceInfo(): DeviceInfo
    suspend fun loadModel(): com.nomad.android.data.Result<Unit>
    suspend fun unloadModel()
}

data class DeviceInfo(
    val totalRamMB: Long,
    val availableRamMB: Long,
    val hasNPU: Boolean,
    val hasGPU: Boolean
)

enum class AIEngineType(val displayName: String) {
    LLAMACPP_MINICPM5("llama.cpp (MiniCPM5 1B)"),
    LLAMACPP_QWEN3_5("llama.cpp (Qwen3.5 0.8B)"),
    LLAMACPP_GEMMA4("llama.cpp (Gemma-4 E2B)"),
    LLAMACPP_LFM2_5("llama.cpp (LFM2.5 230M)"),
    FALLBACK("Fallback (Rule-Based)"),
    NONE("No AI Engine Available");

    companion object {
        fun fromVariant(variant: LlamaCppEngine.ModelVariant): AIEngineType = when (variant) {
            LlamaCppEngine.ModelVariant.MINICPM5_1B -> LLAMACPP_MINICPM5
            LlamaCppEngine.ModelVariant.QWEN3_5_0_8B -> LLAMACPP_QWEN3_5
            LlamaCppEngine.ModelVariant.GEMMA4_E2B -> LLAMACPP_GEMMA4
            LlamaCppEngine.ModelVariant.LFM2_5_230M -> LLAMACPP_LFM2_5
        }
    }
}

data class AIEngineStatus(
    val engineType: AIEngineType,
    val isReady: Boolean,
    val modelName: String,
    val ramRequired: String,
    val modelSize: String
)
