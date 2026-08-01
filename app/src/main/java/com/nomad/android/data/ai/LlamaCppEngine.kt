package com.nomad.android.data.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.nomad.android.data.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * [AIEngine] backed by the vendored llama.cpp (`libnomad_llm.so`) via [LlamaBridge].
 * GGUF-only; offers several downloadable Q4_K_M models (see [ModelVariant]).
 * Default/recommended = OpenBMB MiniCPM5-1B.
 *
 * Spec 1 renders each turn as: system role via [LlamaBridge.setSystemPrompt] +
 * a single user message with the conversation history inlined (matching NOMAD's
 * existing single-turn pattern). The native conversation is reset every turn, so
 * prior turns must be inlined. True stateful multi-turn / tool-calling is Spec 2.
 */
class LlamaCppEngine(
    private val context: Context,
    private val modelVariant: ModelVariant,
    private val deviceTotalRamMB: Long = 0,
) : AIEngine {

    enum class ModelVariant(
        val displayName: String,
        val fileName: String,
        val ramRequiredMB: Long,
        val sizeMB: Int,
        val downloadUrl: String,
    ) {
        MINICPM5_1B(
            displayName = "MiniCPM5 1B",
            fileName = "MiniCPM5-1B-Q4_K_M.gguf",
            ramRequiredMB = 2048,
            sizeMB = 656,
            downloadUrl = "https://huggingface.co/openbmb/MiniCPM5-1B-GGUF/resolve/main/MiniCPM5-1B-Q4_K_M.gguf",
        ),
        QWEN3_5_0_8B(
            displayName = "Qwen3.5 0.8B",
            fileName = "Qwen3.5-0.8B-Q4_K_M.gguf",
            ramRequiredMB = 2048,
            sizeMB = 508,
            downloadUrl = "https://huggingface.co/unsloth/Qwen3.5-0.8B-GGUF/resolve/main/Qwen3.5-0.8B-Q4_K_M.gguf",
        ),
        // Gemma-4 E2B is a multimodal (vision) model; we run its language tower
        // text-only (no mmproj). Larger RAM footprint than the 1B models.
        GEMMA4_E2B(
            displayName = "Gemma-4 E2B",
            fileName = "gemma-4-E2B-it-Q4_K_M.gguf",
            ramRequiredMB = 4608,
            sizeMB = 2963,
            downloadUrl = "https://huggingface.co/unsloth/gemma-4-E2B-it-GGUF/resolve/main/gemma-4-E2B-it-Q4_K_M.gguf",
        ),
        // Liquid Foundation Model — hybrid (LIV conv + GQA), instruction-tuned,
        // text-only. Smallest model in the catalogue; lightest RAM footprint.
        LFM2_5_230M(
            displayName = "LFM2.5 230M",
            fileName = "LFM2.5-230M-Q4_K_M.gguf",
            ramRequiredMB = 1024,
            sizeMB = 146,
            downloadUrl = "https://huggingface.co/LiquidAI/LFM2.5-230M-GGUF/resolve/main/LFM2.5-230M-Q4_K_M.gguf",
        )
    }

    private val modelDir by lazy { File(context.filesDir, "models").also { it.mkdirs() } }
    private val inferenceMutex = Mutex()

    fun getModelFile(): File = File(modelDir, modelVariant.fileName)

    override suspend fun generate(prompt: String, context: List<String>, imagePath: String?): String =
        withContext(Dispatchers.IO) {
            inferenceMutex.withLock {
                val ensured = ensureLoaded()
                if (ensured is Result.Error) return@withContext ensured.message
                LlamaBridge.setSystemPrompt(SYSTEM_PROMPT)
                LlamaBridge.resetConversation()
                val sb = StringBuilder()
                val res = LlamaBridge.chat(buildPromptWithContext(prompt, context)) { sb.append(it) }
                if (res.isFailure) {
                    "AI generation error: ${res.exceptionOrNull()?.message}. Try sending your message again."
                } else {
                    stripThinking(sb.toString())
                }
            }
        }

    override fun generateStream(prompt: String, context: List<String>, imagePath: String?): Flow<String> =
        callbackFlow {
            // Suspend until inference mutex is available — queued, not silently dropped.
            inferenceMutex.lock()
            try {
                val ensured = ensureLoaded()
                if (ensured is Result.Error) {
                    trySend(ensured.message)
                } else {
                    LlamaBridge.setSystemPrompt(SYSTEM_PROMPT)
                    LlamaBridge.resetConversation()
                    val acc = StringBuilder()
                    var emitted = 0
                    val res = LlamaBridge.chat(buildPromptWithContext(prompt, context)) { chunk ->
                        acc.append(chunk)
                        val (delta, newEmitted) = streamingClean(acc.toString(), emitted)
                        emitted = newEmitted
                        if (delta.isNotEmpty()) trySend(delta)
                    }
                    // Flush any text held back by the tail guard (or never emitted
                    // because a think block was still open mid-stream).
                    val finalCleaned = stripThinking(acc.toString())
                    if (emitted < finalCleaned.length) trySend(finalCleaned.substring(emitted))
                    if (res.isFailure) trySend("AI generation error: ${res.exceptionOrNull()?.message}")
                }
            } finally {
                // Unlock in finally to guarantee release even on cancellation.
                // close() is implicit — callbackFlow closes the channel when the
                // builder body returns (including via this finally).
                inferenceMutex.unlock()
            }
            awaitClose { /* mutex already released in finally above */ }
        }

    override suspend fun isAvailable(): Boolean =
        LlamaBridge.isAvailable && getModelFile().let { it.exists() && it.length() > 1_000_000 }

    override fun getModelName(): String = modelVariant.displayName

    override fun getDeviceInfo(): DeviceInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return DeviceInfo(
            totalRamMB = if (deviceTotalRamMB > 0) deviceTotalRamMB else mi.totalMem / (1024 * 1024),
            availableRamMB = mi.availMem / (1024 * 1024),
            hasNPU = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
            hasGPU = false,
        )
    }

    override suspend fun loadModel(): Result<Unit> = withContext(Dispatchers.IO) { ensureLoaded() }

    override suspend fun unloadModel() {
        inferenceMutex.withLock { LlamaBridge.unload() }
    }

    fun requiresDownload(): Boolean = !getModelFile().exists()
    fun getModelSizeMB(): Int = modelVariant.sizeMB

    private suspend fun ensureLoaded(): Result<Unit> {
        if (!LlamaBridge.isAvailable) {
            return Result.error("Native AI engine failed to load on this device.")
        }
        if (LlamaBridge.isModelLoaded) return Result.success(Unit)
        val file = getModelFile()
        if (!file.exists()) {
            return Result.error(
                "No AI model downloaded. Go to Settings and download ${modelVariant.displayName} to enable AI chat."
            )
        }
        val minBytes = (modelVariant.sizeMB * 0.9 * 1_048_576).toLong()
        if (file.length() < minBytes) {
            return Result.error("Model file incomplete. Delete and re-download in Settings.")
        }
        val r = LlamaBridge.loadModel(file, DEFAULT_CTX)
        return if (r.isSuccess) Result.success(Unit)
        else Result.error("Failed to load model: ${r.exceptionOrNull()?.message}")
    }

    companion object {
        private const val DEFAULT_CTX = 4096

        // Tool-free variant of ChatAgent.SYSTEM_PROMPT for the direct (non-agent)
        // generate path — same answering rules, no tool descriptions.
        val SYSTEM_PROMPT: String = """
            You are NOMAD, an offline survival assistant running on the user's phone with no internet. The user may be in a real emergency — be calm, direct, and practical.

            Answering rules:
            - Lead with the single most important action or fact, then supporting detail.
            - Use short numbered steps for procedures. Keep answers under ~150 words unless asked for more.
            - In life-threatening situations, state what to do RIGHT NOW first; cautions after.
            - Never invent facts, dosages, or plant/mushroom identifications. If unsure, say so and give the safest option.
            - Do not overthink. If you reason internally, keep it to 2-3 short sentences, then answer immediately.
        """.trimIndent()

        fun recommendedVariant(totalRamMB: Long): ModelVariant {
            // Pick the largest model that fits comfortably in ~80% of device RAM.
            // 80% leaves headroom for the OS, llama.cpp runtime overhead, and the
            // app's own memory. Fall back to the smallest variant if nothing fits.
            val usableRamMB = (totalRamMB * 0.8).toLong()
            return ModelVariant.entries
                .sortedByDescending { it.ramRequiredMB }
                .firstOrNull { it.ramRequiredMB <= usableRamMB }
                ?: ModelVariant.entries.minByOrNull { it.ramRequiredMB }!!
        }

        /** Remove `<think>...</think>` reasoning blocks and trim leading whitespace. */
        fun stripThinking(s: String): String {
            val out = StringBuilder()
            var i = 0
            while (i < s.length) {
                val open = s.indexOf("<think>", i)
                if (open < 0) {
                    out.append(s, i, s.length)
                    break
                }
                out.append(s, i, open)
                val close = s.indexOf("</think>", open)
                if (close < 0) {
                    // Unclosed block (mid-stream): drop everything from here on.
                    break
                }
                i = close + 8
            }
            return out.toString().trimStart('\n', '\r', ' ', '\t')
        }

        /**
         * Incremental cleaner for streaming. Given the full raw accumulation [acc]
         * and how many cleaned chars were [alreadyEmitted], returns the next chunk
         * to emit plus the new emitted count.
         *
         * - Emits nothing while inside an open `<think>` block.
         * - Holds back a 7-char tail so a partial `</think>`/`<think>` tag spanning
         *   chunk boundaries isn't leaked; the final flush emits the remainder.
         */
        fun streamingClean(acc: String, alreadyEmitted: Int): Pair<String, Int> {
            val opens = countOccurrences(acc, "<think>")
            val closes = countOccurrences(acc, "</think>")
            if (opens > closes) return "" to alreadyEmitted
            val cleaned = stripThinking(acc)
            val safeLen = (cleaned.length - 7).coerceAtLeast(0)
            if (safeLen <= alreadyEmitted) return "" to alreadyEmitted
            return cleaned.substring(alreadyEmitted, safeLen) to safeLen
        }

        private fun countOccurrences(haystack: String, needle: String): Int {
            var count = 0
            var idx = haystack.indexOf(needle)
            while (idx >= 0) {
                count++
                idx = haystack.indexOf(needle, idx + needle.length)
            }
            return count
        }

        /** Build a single user-turn prompt with the conversation history inlined. */
        fun buildPromptWithContext(prompt: String, ctx: List<String>): String =
            buildString {
                if (ctx.isNotEmpty()) {
                    appendLine("Conversation history:")
                    ctx.forEach { appendLine(it) }
                    appendLine()
                }
                append(prompt)
            }
    }
}
