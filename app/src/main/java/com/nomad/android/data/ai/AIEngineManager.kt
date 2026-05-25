package com.nomad.android.data.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class AIEngineManager(
    private val context: Context,
    private val deviceTotalRamMB: Long,
    initialVariant: LiteRTLMEngine.ModelVariant,
    private val fallbackEngine: FallbackEngine
) : AIEngine {

    private val modelsDir = File(context.filesDir, "models")

    @Volatile
    private var currentEngine: LiteRTLMEngine = LiteRTLMEngine(context, initialVariant, deviceTotalRamMB)
    private val _activeVariant = MutableStateFlow(initialVariant)
    val activeVariant: StateFlow<LiteRTLMEngine.ModelVariant> = _activeVariant.asStateFlow()
    private val engineLock = Mutex()
    private val engineRwLock = ReentrantReadWriteLock()
    private val _engineStatus = MutableStateFlow(computeCurrentStatus())
    val engineStatus: StateFlow<AIEngineStatus> = _engineStatus.asStateFlow()

    fun getDownloadedVariants(): List<LiteRTLMEngine.ModelVariant> {
        return LiteRTLMEngine.ModelVariant.entries.filter { variant ->
            File(modelsDir, variant.fileName).let { it.exists() && it.length() > 1_000_000 }
        }
    }

    suspend fun switchModel(variant: LiteRTLMEngine.ModelVariant) {
        engineLock.withLock {
            engineRwLock.write {
                currentEngine.unloadModel()
                currentEngine = LiteRTLMEngine(context, variant, deviceTotalRamMB)
                _activeVariant.value = variant
                _engineStatus.value = computeCurrentStatus()
            }
        }
    }

    private suspend fun refreshEngineIfNeeded() {
        if (!currentEngine.getModelFile().exists()) {
            val downloaded = LiteRTLMEngine.ModelVariant.entries.firstOrNull { variant ->
                File(modelsDir, variant.fileName).let { it.exists() && it.length() > 1_000_000 }
            }
            if (downloaded != null) {
                switchModel(downloaded)
            }
        }
    }

    override suspend fun generate(prompt: String, context: List<String>, imagePath: String?): String {
        refreshEngineIfNeeded()
        val engine = engineLock.withLock { currentEngine }
        return if (engine.isAvailable()) {
            engine.generate(prompt, context, imagePath)
        } else {
            fallbackEngine.generate(prompt, context, imagePath)
        }
    }

    override fun generateStream(prompt: String, context: List<String>, imagePath: String?): Flow<String> = flow {
        val engine = engineLock.withLock { currentEngine }
        if (engine.isAvailable()) {
            emitAll(engine.generateStream(prompt, context, imagePath))
        } else {
            emitAll(fallbackEngine.generateStream(prompt, context, imagePath))
        }
    }

    override suspend fun isAvailable(): Boolean {
        val engine = engineLock.withLock { currentEngine }
        return engine.isAvailable()
    }

    override fun getModelName(): String = engineRwLock.read { currentEngine.getModelName() }

    override fun getDeviceInfo(): DeviceInfo = engineRwLock.read { currentEngine.getDeviceInfo() }

    override suspend fun loadModel(): com.nomad.android.data.Result<Unit> {
        val engine = engineLock.withLock { currentEngine }
        return engine.loadModel()
    }

    override suspend fun unloadModel() {
        val engine = engineLock.withLock { currentEngine }
        engine.unloadModel()
    }

    fun getModelSizeMB(): Int = _activeVariant.value.sizeMB

    private fun computeCurrentStatus(): AIEngineStatus {
        val deviceInfo = currentEngine.getDeviceInfo()
        val modelFile = File(modelsDir, _activeVariant.value.fileName)
        val isReady = modelFile.exists() && modelFile.length() > 1_000_000
        return AIEngineStatus(
            engineType = AIEngineType.LITERTLM_E2B,
            isReady = isReady,
            modelName = currentEngine.getModelName(),
            ramRequired = "${deviceInfo.totalRamMB}MB total",
            modelSize = "${_activeVariant.value.sizeMB} MB"
        )
    }

    companion object {
        private const val TAG = "AIEngineManager"
    }
}
