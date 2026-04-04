package com.nomad.android.data.ai

import kotlinx.coroutines.flow.Flow

interface AIEngine {
    suspend fun generate(prompt: String, context: List<String>): String
    fun generateStream(prompt: String, context: List<String>): Flow<String>
    suspend fun isAvailable(): Boolean
    fun getModelName(): String
    fun getDeviceInfo(): DeviceInfo
    suspend fun loadModel(): Result<Unit>
    fun unloadModel()
}

data class DeviceInfo(
    val totalRamMB: Long,
    val availableRamMB: Long,
    val hasNPU: Boolean,
    val hasGPU: Boolean
)

enum class AIEngineType(val displayName: String) {
    LITERTLM_E2B("LiteRT-LM (Gemma 4 E2B)"),
    LITERTLM_1B("LiteRT-LM (Gemma 3 1B)"),
    FALLBACK("Fallback (Rule-Based)"),
    NONE("No AI Engine Available")
}

data class AIEngineStatus(
    val engineType: AIEngineType,
    val isReady: Boolean,
    val modelName: String,
    val ramRequired: String,
    val modelSize: String
)
