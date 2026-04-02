package com.nomad.android.data.ai

import kotlinx.coroutines.flow.Flow

interface AIEngine {
    suspend fun generate(prompt: String, system: String? = null): Result<String>
    fun generateStream(prompt: String, system: String? = null): Flow<String>
    fun isAvailable(): Boolean
    fun getModelName(): String
    fun getDeviceInfo(): String
}

enum class AIEngineType(val displayName: String) {
    AICORE("AICore (Gemini Nano 4)"),
    LITERTLM_E2B("LiteRT-LM (Gemma 4 E2B)"),
    LITERTLM_1B("LiteRT-LM (Gemma 3 1B)"),
    NONE("No AI Engine Available")
}

data class AIEngineStatus(
    val engineType: AIEngineType,
    val isReady: Boolean,
    val modelName: String,
    val ramRequired: String,
    val modelSize: String
)
