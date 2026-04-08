package com.nomad.android.data.ai

import android.app.ActivityManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import android.util.Log
import com.nomad.android.data.Result
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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
        "<end_of_session>",
        "```xml", "```",
        "<channel>", "</channel>",
        "<tool_response>", "</tool_response>",
        "</think>",
        "<turn", "</turn", "turn\u25B7",
    )

    private val controlTokenPattern = Regex(
        """</?(?:start_of_turn|end_of_turn|end_of_session|eos|turn|bos|tool_response|channel|thought)\s*[^>]*>|turn\u25B7|<turn\s*\u25B7?>|Thinking Process:?"""
    )

    private val thinkingBlockPattern = Regex(
        """<channel>thought.*?</channel>""", RegexOption.DOT_MATCHES_ALL
    )

    // Only strip known AI control tags - not generic HTML-like content
    private val residualTagPattern = Regex(
        """</?(?:start_of_turn|end_of_turn|end_of_session|eos|turn|bos|tool_response|channel|thought|system)\s*[^>]*>|turn\u25B7"""
    )

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

    override suspend fun generate(prompt: String, context: List<String>, imagePath: String?): String = withContext(Dispatchers.IO) {
        if (!isModelLoaded) {
            val result = loadModel()
            if (result.isError) {
                val modelFile = getModelFile()
                val fileSizeMB = if (modelFile.exists()) modelFile.length() / 1_048_576 else 0
                val expectedMB = modelVariant.sizeMB
                return@withContext if (!modelFile.exists()) {
                    "No AI model downloaded. Go to Settings and download a model to enable AI chat."
                } else if (fileSizeMB < expectedMB * 0.9) {
                    "Model file incomplete (${fileSizeMB}MB of ${expectedMB}MB). Delete and re-download in Settings."
                } else {
                    "Failed to load model: ${(result as Result.Error).message}"
                }
            }
        }

        val inference = llmInference ?: return@withContext "Model not loaded. Try restarting the app."
        val fullPrompt = buildPromptWithContext(prompt, context, imagePath)

        try {
            val raw = inference.generateResponse(fullPrompt)
            cleanFullResponse(raw)
        } catch (e: Throwable) {
            Log.e(TAG, "Generation failed", e)
            "AI generation error. The model may be corrupt — try re-downloading in Settings."
        }
    }

    override fun generateStream(prompt: String, context: List<String>, imagePath: String?): Flow<String> = callbackFlow {
        if (!isModelLoaded) {
            withContext(Dispatchers.IO) { loadModel() }.let { result ->
                if (result.isError) {
                    trySend("No AI model downloaded. Go to Settings and download a model to enable AI chat.")
                    close()
                    return@callbackFlow
                }
            }
        }

        val inference = llmInference
        if (inference == null) {
            trySend("No AI model downloaded. Go to Settings and download a model to enable AI chat.")
            close()
            return@callbackFlow
        }

        val fullPrompt = withContext(Dispatchers.IO) { buildPromptWithContext(prompt, context, imagePath) }
        var shouldStop = false
        var emittedToken = false
        val pendingBuffer = StringBuilder()

        try {
            inference.generateResponseAsync(fullPrompt) { partialResult, done ->
                if (shouldStop) {
                    if (!isClosedForSend) close()
                    return@generateResponseAsync
                }

                if (done) {
                    // Flush any remaining buffered content
                    if (pendingBuffer.isNotEmpty()) {
                        val cleaned = cleanToken(pendingBuffer.toString())
                        if (!cleaned.isNullOrEmpty()) trySend(cleaned)
                        pendingBuffer.clear()
                    }
                    if (!isClosedForSend) close()
                    return@generateResponseAsync
                }

                pendingBuffer.append(partialResult)
                val buffered = pendingBuffer.toString()

                // Check if buffer contains a complete stop token
                if (stopTokens.any { buffered.contains(it) }) {
                    shouldStop = true
                    val beforeStop = buffered
                        .split(Regex("""</?(?:end_of_turn|eos|tool_response|channel|turn)\s*[^>]*>|turn\u25B7"""))
                        .firstOrNull()
                    val cleaned = beforeStop?.let { cleanToken(it) }
                    if (!cleaned.isNullOrEmpty()) trySend(cleaned)
                    pendingBuffer.clear()
                    if (!isClosedForSend) close()
                    return@generateResponseAsync
                }

                // If buffer might contain a partial stop token (starts with '<'),
                // hold it until more data arrives
                if (buffered.contains("<") && !buffered.contains(">")) {
                    return@generateResponseAsync
                }

                // No partial tag detected — flush the buffer
                val cleaned = cleanToken(buffered)
                pendingBuffer.clear()
                if (cleaned != null) {
                    emittedToken = true
                    trySend(cleaned)
                }
            }
        } catch (e: Throwable) {
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

    private fun buildPromptWithContext(prompt: String, context: List<String>, imagePath: String? = null): String {
        return buildString {
            append("<start_of_turn>user\n")
            appendLine(SYSTEM_PROMPT)
            appendLine()
            if (context.isNotEmpty()) {
                appendLine("Conversation history:")
                context.forEach { appendLine(it) }
                appendLine()
            }
            if (imagePath != null) {
                val imageBase64 = encodeImageToBase64(imagePath)
                if (imageBase64 != null) {
                    appendLine("[User attached an image]")
                    append("<image>data:image/jpeg;base64,")
                    append(imageBase64)
                    appendLine("</image>")
                }
            }
            append(prompt)
            append("<end_of_turn>\n")
            append("<start_of_turn>model\n")
        }
    }

    private fun encodeImageToBase64(imagePath: String): String? {
        return try {
            val file = File(imagePath)
            if (!file.exists()) return null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(imagePath, options)
            val scale = maxOf(1, maxOf(options.outWidth, options.outHeight) / 512)
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = scale }
            val bitmap = BitmapFactory.decodeFile(imagePath, decodeOptions) ?: return null
            val baos = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, baos)
            bitmap.recycle()
            val encoded = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            baos.close()
            if (encoded.length > 500_000) {
                Log.w(TAG, "Image base64 too large (${encoded.length} chars), skipping")
                return null
            }
            encoded
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to encode image", e)
            null
        }
    }

    companion object {
        private const val TAG = "LiteRTLMEngine"

        private const val SYSTEM_PROMPT = """You are NOMAD, an offline survival assistant. You give clear, concise, and practical answers about survival, first aid, navigation, emergency preparedness, and general knowledge. Keep answers direct and actionable. Do not include any XML tags, control tokens, or internal reasoning in your responses."""

        fun recommendedVariant(totalRamMB: Long): ModelVariant = ModelVariant.GEMMA4_E2B
    }

}
