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
            fileName = "gemma-4-E2B-it.litertlm",
            ramRequiredMB = 2048,
            sizeMB = 2643,
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
        )
    }

    private val modelDir by lazy { File(context.filesDir, "models").also { it.mkdirs() } }
    private var llmInference: LlmInference? = null
    private var isModelLoaded = false

    private val stopTokens = setOf(
        "<end_of_turn>", "<eos>",
        "</end_of_turn>", "</start_of_turn>",
        "```xml", "```",
        "<channel>", "</channel>",
        "<tool_response>", "</tool_response>",
        "<turn", "</turn", "turn\u25B7",
    )

    private val controlTokenPattern = Regex(
        """</?(?:start_of_turn|end_of_turn|eos|turn|bos|tool_response|channel|thought)\s*[^>]*>|turn\u25B7|<turn\s*\u25B7?>|Thinking Process:?\s*"""
    )

    private val thinkingBlockPattern = Regex(
        """<channel>thought.*?</channel>""", RegexOption.DOT_MATCHES_ALL
    )

    private val residualTagPattern = Regex("""<[a-z_/][^>]{0,30}>""")

    fun getModelFile(): File = File(modelDir, modelVariant.fileName)

    private fun cleanToken(token: String): String? {
        if (token.isBlank()) return null

        // Drop tokens that are entirely thinking/reasoning control blocks
        if (token.contains("<channel>") || token.contains("Thinking Process")) return null

        var cleaned = controlTokenPattern.replace(token, "")
        // Strip any remaining angle-bracket tags that look like control tokens
        cleaned = residualTagPattern.replace(cleaned, "")
        // Strip unicode play symbol used as turn marker
        cleaned = cleaned.replace("\u25B7", "")
        cleaned = cleaned.trim()

        return cleaned.ifEmpty { null }
    }

    override suspend fun generate(prompt: String, context: List<String>): String = withContext(Dispatchers.IO) {
        if (!isModelLoaded) {
            val result = loadModel()
            if (result.isError) {
                val modelFile = getModelFile()
                val fileSizeMB = if (modelFile.exists()) modelFile.length() / 1_048_576 else 0
                val expectedMB = modelVariant.sizeMB
                return@withContext if (!modelFile.exists()) {
                    "Model not downloaded. Go to Settings and download ${modelVariant.displayName}."
                } else if (fileSizeMB < expectedMB * 0.9) {
                    "Model file incomplete (${fileSizeMB}MB of ${expectedMB}MB). Delete and re-download in Settings."
                } else {
                    "Failed to load model: ${(result as Result.Error).message}"
                }
            }
        }

        val inference = llmInference ?: return@withContext "Model not loaded. Try restarting the app."
        val fullPrompt = buildPromptWithContext(prompt, context)

        try {
            val raw = inference.generateResponse(fullPrompt)
            cleanFullResponse(raw)
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed", e)
            "AI generation error. The model may be corrupt — try re-downloading in Settings."
        }
    }

    override fun generateStream(prompt: String, context: List<String>): Flow<String> = callbackFlow {
        if (!isModelLoaded) {
            withContext(Dispatchers.IO) { loadModel() }.let { result ->
                if (result.isError) {
                    trySend("Model not ready. Download ${modelVariant.displayName} in Settings to enable AI chat.")
                    close()
                    return@callbackFlow
                }
            }
        }

        val inference = llmInference
        if (inference == null) {
            trySend("Model not ready. Download ${modelVariant.displayName} in Settings to enable AI chat.")
            close()
            return@callbackFlow
        }

        val fullPrompt = buildPromptWithContext(prompt, context)
        var shouldStop = false
        var emittedToken = false

        try {
            inference.generateResponseAsync(fullPrompt) { partialResult, done ->
                if (shouldStop) {
                    if (!isClosedForSend) close()
                    return@generateResponseAsync
                }

                if (done) {
                    if (!isClosedForSend) close()
                    return@generateResponseAsync
                }

                if (stopTokens.any { partialResult.contains(it) }) {
                    shouldStop = true
                    val beforeStop = partialResult
                        .split(Regex("""</?(?:end_of_turn|eos|tool_response|channel|turn)\s*[^>]*>|turn\u25B7"""))
                        .firstOrNull()
                    val cleaned = beforeStop?.let { cleanToken(it) }
                    if (!cleaned.isNullOrEmpty()) trySend(cleaned)
                    if (!isClosedForSend) close()
                    return@generateResponseAsync
                }

                val cleaned = cleanToken(partialResult)
                if (cleaned != null) {
                    emittedToken = true
                    trySend(cleaned)
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
                return@withContext Result.error("Model not downloaded yet. Go to Settings and tap GET on ${modelVariant.displayName}.")
            }
            if (modelFile.length() < 1_000_000) {
                modelFile.delete()
                return@withContext Result.error("Model file is corrupt. Go to Settings, delete it, and download again.")
            }

            Log.i(TAG, "Loading model: ${modelFile.absolutePath} (${modelFile.length() / 1_048_576} MB)")

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(1024)
                .setMaxTopK(40)
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

    private fun cleanFullResponse(raw: String): String {
        // Remove thinking blocks entirely
        var cleaned = thinkingBlockPattern.replace(raw, "")
        // Remove all control tokens
        cleaned = controlTokenPattern.replace(cleaned, "")
        // Remove any remaining angle-bracket control tags
        cleaned = residualTagPattern.replace(cleaned, "")
        cleaned = cleaned.replace("\u25B7", "")
        // Collapse excessive whitespace/newlines
        cleaned = cleaned.replace(Regex("""\n{3,}"""), "\n\n")
        return cleaned.trim()
    }

    private fun buildPromptWithContext(prompt: String, context: List<String>): String {
        return buildString {
            append("<start_of_turn>user\n")
            appendLine(SYSTEM_PROMPT)
            appendLine()
            if (context.isNotEmpty()) {
                appendLine("Conversation history:")
                context.forEach { appendLine(it) }
                appendLine()
            }
            append(prompt)
            append("<end_of_turn>\n")
            append("<start_of_turn>model\n")
        }
    }

    companion object {
        private const val TAG = "LiteRTLMEngine"

        private const val SYSTEM_PROMPT = """You are NOMAD, an offline survival assistant. You give clear, concise, and practical answers about survival, first aid, navigation, emergency preparedness, and general knowledge. Keep answers direct and actionable. Do not include any XML tags, control tokens, or internal reasoning in your responses."""

        fun recommendedVariant(totalRamMB: Long): ModelVariant = ModelVariant.GEMMA4_E2B
    }

}
