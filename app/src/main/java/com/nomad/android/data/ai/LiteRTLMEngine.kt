package com.nomad.android.data.ai

import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

class LiteRTLMEngine(
    private val context: Context,
    private val modelVariant: ModelVariant
) : AIEngine {

    enum class ModelVariant(
        val displayName: String,
        val fileName: String,
        val ramRequiredMB: Long,
        val sizeMB: Int
    ) {
        E2B("Gemma 4 E2B", "gemma4-e2b.bin", 6144, 3000),
        ONE_B("Gemma 3 1B", "gemma3-1b.bin", 2048, 1000)
    }

    private val modelDir by lazy { File(context.filesDir, "models").also { it.mkdirs() } }
    private var isModelLoaded = false

    private fun getModelFile(): File = File(modelDir, modelVariant.fileName)

    override suspend fun generate(prompt: String, context: List<String>): String {
        if (!isModelLoaded) {
            loadModel().getOrElse { return "AI Engine not loaded. Please try again." }
        }

        val fullPrompt = buildPromptWithContext(prompt, context)

        // TODO: Implement with LiteRT-LM / MediaPipe GenAI SDK
        // val options = LlmInference.LlmInferenceOptions.builder()
        //     .setModelPath(getModelFile().absolutePath)
        //     .setMaxTokens(1024)
        //     .setTemperature(0.7f)
        //     .build()
        // val inference = LlmInference.createFromContext(context, options)
        // return inference.generateResponse(fullPrompt)

        return "[LiteRT-LM] Response for: ${fullPrompt.take(50)}..."
    }

    override fun generateStream(prompt: String, context: List<String>): Flow<String> = flow {
        if (!isModelLoaded) {
            loadModel().getOrElse {
                emit("AI Engine not loaded.")
                return@flow
            }
        }

        val fullPrompt = buildPromptWithContext(prompt, context)

        // TODO: Implement streaming with LiteRT-LM
        // val options = LlmInference.LlmInferenceOptions.builder()
        //     .setModelPath(getModelFile().absolutePath)
        //     .build()
        // val inference = LlmInference.createFromContext(context, options)
        // inference.generateResponseStream(fullPrompt).collect { token -> emit(token) }

        val mockResponse = "[LiteRT-LM] Streaming response for: ${fullPrompt.take(50)}..."
        mockResponse.chunked(4).forEach { chunk ->
            emit(chunk)
            kotlinx.coroutines.delay(30)
        }
    }

    override suspend fun isAvailable(): Boolean = getModelFile().exists()

    override fun getModelName(): String = modelVariant.displayName

    override fun getDeviceInfo(): DeviceInfo {
        val runtime = Runtime.getRuntime()
        val totalRamMB = runtime.maxMemory() / (1024 * 1024)
        return DeviceInfo(
            totalRamMB = totalRamMB,
            availableRamMB = runtime.freeMemory() / (1024 * 1024),
            hasNPU = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
            hasGPU = true
        )
    }

    override suspend fun loadModel(): Result<Unit> {
        return try {
            if (!getModelFile().exists()) {
                return Result.failure(IllegalStateException("Model file not found: ${modelVariant.fileName}"))
            }
            // TODO: Actually load model into LiteRT-LM
            // val options = LlmInference.LlmInferenceOptions.builder()
            //     .setModelPath(getModelFile().absolutePath)
            //     .build()
            // inference = LlmInference.createFromContext(context, options)
            isModelLoaded = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun unloadModel() {
        isModelLoaded = false
        // TODO: inference?.close()
    }

    fun requiresDownload(): Boolean = !getModelFile().exists()
    fun getModelSizeMB(): Int = modelVariant.sizeMB

    private fun buildPromptWithContext(prompt: String, context: List<String>): String {
        return buildString {
            if (context.isNotEmpty()) {
                appendLine("Context:")
                context.forEachIndexed { i, ctx ->
                    appendLine("[${i + 1}] $ctx")
                }
                appendLine()
            }
            appendLine("Question: $prompt")
            appendLine("Answer:")
        }
    }

    companion object {
        fun recommendedVariant(totalRamMB: Long): ModelVariant {
            return if (totalRamMB >= 6144) ModelVariant.E2B else ModelVariant.ONE_B
        }
    }
}
