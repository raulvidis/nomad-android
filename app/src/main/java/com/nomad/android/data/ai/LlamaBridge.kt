package com.nomad.android.data.ai

import android.util.Log
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Kotlin facade over the libnomad_llm.so JNI bridge. Single-instance:
 * llama.cpp keeps a global model+context, so we model this provider as
 * an object with thread-safe load/generate guards.
 */
object LlamaBridge {

    private const val TAG = "LlamaBridge"

    private val available: Boolean = try {
        System.loadLibrary("nomad_llm")
        nativeInit()
        true
    } catch (e: UnsatisfiedLinkError) {
        Log.e(TAG, "native library not available", e)
        false
    }

    private val busy = AtomicBoolean(false)
    @Volatile private var loadedPath: String? = null

    val isAvailable: Boolean get() = available
    val isModelLoaded: Boolean get() = available && nativeIsLoaded()

    /**
     * Validates the file's GGUF header against an architecture allowlist,
     * then hands it to llama_model_load_from_file. The validation step is
     * defense against models that crash the loader instead of returning
     * NULL — many published GGUFs misrepresent their architecture or
     * embed unsupported tokenizers.
     */
    suspend fun loadModel(file: File, contextSize: Int = 0): Result<Unit> = withContext(Dispatchers.IO) {
        if (!available) {
            return@withContext Result.failure(IllegalStateException(
                "Native library libnomad_llm.so failed to load on this device."))
        }
        if (!file.exists()) {
            return@withContext Result.failure(IllegalStateException("Model file not found: ${file.absolutePath}"))
        }
        if (file.length() < 1024) {
            return@withContext Result.failure(IllegalStateException("Model file is too small to be a valid GGUF."))
        }

        val header = GgufMetadata.read(file)
            ?: return@withContext Result.failure(IllegalStateException(
                "File doesn't parse as a GGUF (magic mismatch or truncated header)."))

        when (val v = GgufMetadata.verdict(header)) {
            is GgufMetadata.Verdict.Reject -> {
                Log.w(TAG, "rejecting ${file.name}: ${v.reason}")
                return@withContext Result.failure(IllegalStateException(v.reason))
            }
            GgufMetadata.Verdict.Ok -> Unit
        }

        if (!busy.compareAndSet(false, true)) {
            return@withContext Result.failure(IllegalStateException("Provider is busy."))
        }
        try {
            val ok = nativeLoadModel(file.absolutePath, contextSize)
            if (!ok) {
                val err = nativeGetLastError().ifBlank { "llama_model_load_from_file returned NULL" }
                return@withContext Result.failure(IllegalStateException(err))
            }
            loadedPath = file.absolutePath
            Log.i(TAG, "loaded ${file.name}")
            Result.success(Unit)
        } finally {
            busy.set(false)
        }
    }

    fun unload() {
        if (available) {
            nativeUnloadModel()
            loadedPath = null
        }
    }

    /** Reset the per-conversation state (clears KV cache and chat history). */
    fun resetConversation() {
        if (available && nativeIsLoaded()) nativeResetConversation()
    }

    /**
     * Set (or clear, with the empty string) the system prompt prepended
     * on every conversation render. Persistent across resetConversation
     * — only nativeUnloadModel / shutdown clears it.
     */
    fun setSystemPrompt(prompt: String) {
        if (available) nativeSetSystemPrompt(prompt)
    }

    /**
     * Submit a user prompt and stream the assistant's response token-by-token.
     * [onChunk] is called from a worker thread for each decoded UTF-8 chunk.
     * Returns true on normal end-of-generation; false on any failure.
     */
    suspend fun chat(
        prompt: String,
        onChunk: suspend (String) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!available)         return@withContext Result.failure(IllegalStateException("Native library unavailable"))
        if (!nativeIsLoaded())  return@withContext Result.failure(IllegalStateException("No model loaded"))
        if (!busy.compareAndSet(false, true)) {
            return@withContext Result.failure(IllegalStateException("Already generating"))
        }
        try {
            if (!nativeSubmitUserPrompt(prompt)) {
                val err = nativeGetLastError().ifBlank { "Failed to submit prompt" }
                return@withContext Result.failure(IllegalStateException(err))
            }
            while (true) {
                ensureActive()
                val chunk = nativeNextToken() ?: break
                if (chunk.isNotEmpty()) onChunk(chunk)
            }
            Result.success(Unit)
        } finally {
            busy.set(false)
        }
    }

    /**
     * Submit one model iteration in the tool-call flow.
     *
     * - If [prompt] is non-empty: append a user message + an inline
     *   "[attachments: ...]" suffix when [attachmentsJson] describes any.
     * - If [prompt] is empty: skip the user-append and re-render. This
     *   is how we continue after appendToolResult().
     *
     * After this returns true, stream the assistant tokens via [streamTokens]
     * until EOG, then call [finishTurnAndParse] to extract tool calls.
     */
    suspend fun submitTurn(
        prompt: String,
        attachmentsJson: String,
        toolsJson: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!available || !nativeIsLoaded()) {
            return@withContext Result.failure(IllegalStateException("No model loaded"))
        }
        if (!busy.compareAndSet(false, true)) {
            return@withContext Result.failure(IllegalStateException("Already generating"))
        }
        try {
            if (!nativeSubmitTurn(prompt, attachmentsJson, toolsJson)) {
                val err = nativeGetLastError().ifBlank { "Failed to submit turn" }
                Result.failure(IllegalStateException(err))
            } else {
                Result.success(Unit)
            }
        } finally {
            busy.set(false)
        }
    }

    /**
     * Stream tokens for the most recent submit. Calls [onChunk] for each
     * decoded UTF-8 chunk; returns when the model emits EOG.
     *
     * Unlike [chat], does NOT submit a prompt — submission is the
     * caller's responsibility via [submitTurn]. This split is what lets
     * the agent loop interleave submit / stream / finish / append-result.
     */
    suspend fun streamTokens(
        onChunk: suspend (String) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!available || !nativeIsLoaded()) {
            return@withContext Result.failure(IllegalStateException("No model loaded"))
        }
        if (!busy.compareAndSet(false, true)) {
            return@withContext Result.failure(IllegalStateException("Already streaming"))
        }
        try {
            while (true) {
                ensureActive()
                val chunk = nativeNextToken() ?: break
                if (chunk.isNotEmpty()) onChunk(chunk)
            }
            Result.success(Unit)
        } finally {
            busy.set(false)
        }
    }

    /** Result of parsing one assistant turn. */
    data class ParsedTurn(
        val content: String,
        val toolCalls: List<ParsedToolCall>,
    )

    data class ParsedToolCall(
        val id: String,
        val name: String,
        val arguments: String,
    )

    /**
     * Parse the just-completed assistant turn into prose content + a list
     * of structured tool calls (possibly empty). Replaces the raw
     * assistant message in the native history with the structured one,
     * so the next [submitTurn] renders it correctly.
     */
    fun finishTurnAndParse(): ParsedTurn {
        if (!available || !nativeIsLoaded()) return ParsedTurn("", emptyList())
        val raw = nativeFinishTurnAndParse()
        return try {
            val o = JSONObject(raw)
            val content = o.optString("content", "")
            val arr = o.optJSONArray("tool_calls")
            val calls = if (arr == null) emptyList() else List(arr.length()) { i ->
                val c = arr.getJSONObject(i)
                ParsedToolCall(
                    id = c.optString("id", i.toString()),
                    name = c.optString("name", ""),
                    arguments = c.optString("arguments", ""),
                )
            }
            ParsedTurn(content, calls)
        } catch (e: Exception) {
            Log.w(TAG, "finishTurnAndParse: bad JSON from native (${e.message}); raw=$raw")
            ParsedTurn(raw, emptyList())
        }
    }

    /**
     * Append a tool result message to the conversation, to be rendered
     * on the next [submitTurn] call. Safe to call multiple times in a
     * row (one per tool call).
     */
    fun appendToolResult(callId: String, name: String, content: String) {
        if (available && nativeIsLoaded()) nativeAppendToolResult(callId, name, content)
    }

    // ── Native bindings (implemented in cpp/llama-jni.cpp) ──────────────

    @JvmStatic private external fun nativeInit()
    @JvmStatic private external fun nativeShutdown()
    @JvmStatic private external fun nativeGetLastError(): String
    @JvmStatic private external fun nativeLoadModel(path: String, nCtx: Int): Boolean
    @JvmStatic private external fun nativeUnloadModel()
    @JvmStatic private external fun nativeIsLoaded(): Boolean
    @JvmStatic private external fun nativeSubmitUserPrompt(prompt: String): Boolean
    @JvmStatic private external fun nativeNextToken(): String?
    @JvmStatic private external fun nativeResetConversation()
    @JvmStatic private external fun nativeSetSystemPrompt(prompt: String)

    // Tool-calling additions (Phase 4 JNI surface).
    @JvmStatic private external fun nativeSubmitTurn(
        prompt: String,
        attachmentsJson: String,
        toolsJson: String,
    ): Boolean
    @JvmStatic private external fun nativeFinishTurnAndParse(): String
    @JvmStatic private external fun nativeAppendToolResult(
        callId: String,
        name: String,
        content: String,
    )
}
