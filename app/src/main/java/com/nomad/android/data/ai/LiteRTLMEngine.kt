package com.nomad.android.data.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.nomad.android.data.Result
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.File

class LiteRTLMEngine(
    private val context: Context,
    private val modelVariant: ModelVariant,
    private val deviceTotalRamMB: Long = 0
) : AIEngine {

    enum class ModelVariant(
        val displayName: String,
        val fileName: String,
        val ramRequiredMB: Long,
        val sizeMB: Int,
        val downloadUrl: String
    ) {
        GEMMA4_E2B(
            displayName = "Gemma 4 E2B",
            fileName = "gemma-4-E2B-it-web.task",
            ramRequiredMB = 2048,
            sizeMB = 2004,
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it-web.task"
        )
    }

    private val modelDir by lazy { File(context.filesDir, "models").also { it.mkdirs() } }
    private var llmInference: LlmInference? = null
    private var isModelLoaded = false

    fun getModelFile(): File = File(modelDir, modelVariant.fileName)

    override suspend fun generate(prompt: String, context: List<String>): String = withContext(Dispatchers.IO) {
        if (!isModelLoaded) {
            val result = loadModel()
            if (result.isError) {
                return@withContext (result as Result.Error).message
            }
        }

        val inference = llmInference ?: return@withContext "AI Engine not initialized."
        val fullPrompt = buildPromptWithContext(prompt, context)

        try {
            inference.generateResponse(fullPrompt)
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed", e)
            "AI generation error: ${e.message}"
        }
    }

    override fun generateStream(prompt: String, context: List<String>): Flow<String> = callbackFlow {
        if (!isModelLoaded) {
            withContext(Dispatchers.IO) { loadModel() }.let { result ->
                if (result.isError) {
                    trySend((result as Result.Error).message)
                    close()
                    return@callbackFlow
                }
            }
        }

        val inference = llmInference
        if (inference == null) {
            trySend("AI Engine not initialized.")
            close()
            return@callbackFlow
        }

        val fullPrompt = buildPromptWithContext(prompt, context)

        try {
            inference.generateResponseAsync(fullPrompt) { partialResult, done ->
                if (partialResult.isNotEmpty()) {
                    trySend(partialResult)
                }
                if (done) {
                    close()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Streaming generation failed", e)
            trySend("AI generation error: ${e.message}")
            close()
        }

        awaitClose { }
    }

    override suspend fun isAvailable(): Boolean = getModelFile().let { it.exists() && it.length() > 1_000_000 }

    override fun getModelName(): String = modelVariant.displayName

    override fun getDeviceInfo(): DeviceInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return DeviceInfo(
            totalRamMB = if (deviceTotalRamMB > 0) deviceTotalRamMB else memoryInfo.totalMem / (1024 * 1024),
            availableRamMB = memoryInfo.availMem / (1024 * 1024),
            hasNPU = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
            hasGPU = true
        )
    }

    override suspend fun loadModel(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val modelFile = getModelFile()
            if (!modelFile.exists()) {
                return@withContext Result.error("Model not downloaded yet. Go to Settings and tap GET on Gemma 4 E2B.")
            }
            if (modelFile.length() < 1_000_000) {
                modelFile.delete()
                return@withContext Result.error("Model file is corrupt. Go to Settings, delete it, and download again.")
            }

            Log.i(TAG, "Loading model: ${modelFile.absolutePath} (${modelFile.length() / 1_048_576} MB)")

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(1024)
                .setMaxTopK(64)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            isModelLoaded = true
            Log.i(TAG, "Model loaded successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
            isModelLoaded = false
            llmInference = null
            Result.error("Failed to load AI model: ${e.message}", e)
        }
    }

    override fun unloadModel() {
        try {
            llmInference?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing LlmInference", e)
        }
        llmInference = null
        isModelLoaded = false
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
            append(prompt)
        }
    }

    companion object {
        private const val TAG = "LiteRTLMEngine"

        fun recommendedVariant(totalRamMB: Long): ModelVariant = ModelVariant.GEMMA4_E2B
    }
}
