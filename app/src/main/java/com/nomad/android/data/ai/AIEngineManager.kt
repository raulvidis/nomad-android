package com.nomad.android.data.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Manages the active AI engine and supports runtime model switching.
 * Injected as a singleton — consumers use this instead of AIEngine directly.
 */
class AIEngineManager(
    private val context: Context,
    private val deviceTotalRamMB: Long,
    initialVariant: LiteRTLMEngine.ModelVariant
) : AIEngine {

    private val modelsDir = File(context.filesDir, "models")

    private var currentEngine: LiteRTLMEngine = LiteRTLMEngine(context, initialVariant, deviceTotalRamMB)
    private val _activeVariant = MutableStateFlow(initialVariant)
    val activeVariant: StateFlow<LiteRTLMEngine.ModelVariant> = _activeVariant.asStateFlow()

    /** Returns all model variants that have been downloaded. */
    fun getDownloadedVariants(): List<LiteRTLMEngine.ModelVariant> {
        return LiteRTLMEngine.ModelVariant.entries.filter { variant ->
            File(modelsDir, variant.fileName).let { it.exists() && it.length() > 1_000_000 }
        }
    }

    /** Switch to a different model variant. Unloads the current model first. */
    fun switchModel(variant: LiteRTLMEngine.ModelVariant) {
        if (variant == _activeVariant.value) return
        Log.i(TAG, "Switching model from ${_activeVariant.value.displayName} to ${variant.displayName}")
        currentEngine.unloadModel()
        currentEngine = LiteRTLMEngine(context, variant, deviceTotalRamMB)
        _activeVariant.value = variant
    }

    // Delegate all AIEngine methods to the current engine

    override suspend fun generate(prompt: String, context: List<String>, imagePath: String?): String =
        currentEngine.generate(prompt, context, imagePath)

    override fun generateStream(prompt: String, context: List<String>, imagePath: String?): Flow<String> =
        currentEngine.generateStream(prompt, context, imagePath)

    override suspend fun isAvailable(): Boolean = currentEngine.isAvailable()

    override fun getModelName(): String = currentEngine.getModelName()

    override fun getDeviceInfo(): DeviceInfo = currentEngine.getDeviceInfo()

    override suspend fun loadModel(): com.nomad.android.data.Result<Unit> = currentEngine.loadModel()

    override fun unloadModel() = currentEngine.unloadModel()

    fun getModelSizeMB(): Int = _activeVariant.value.sizeMB

    companion object {
        private const val TAG = "AIEngineManager"
    }
}
