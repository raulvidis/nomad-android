package com.nomad.android.data.ai

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Minimal GGUF header reader. We only need a few metadata values
 * (architecture, model name) to decide whether the file is safe to hand
 * to llama_model_load_from_file — many GGUFs in the wild misrepresent
 * their architecture, and feeding them to the loader segfaults instead
 * of returning NULL.
 *
 * GGUF spec: https://github.com/ggml-org/ggml/blob/master/docs/gguf.md
 *
 * Format (little-endian):
 *   magic   = 4 bytes ("GGUF")
 *   version = u32
 *   tensor_count   = u64
 *   metadata_count = u64
 *   then metadata_count KV pairs of:
 *     key   = u64 length + UTF-8 bytes
 *     type  = u32
 *     value = depends on type
 *
 * We stop scanning as soon as we have the keys we care about; on any
 * unknown / unsupported value type we abort the parse rather than risk
 * mis-aligning the stream and silently reading garbage.
 */
object GgufMetadata {

    private const val GGUF_MAGIC = 0x46554747 // "GGUF" little-endian

    private const val T_U8     = 0
    private const val T_I8     = 1
    private const val T_U16    = 2
    private const val T_I16    = 3
    private const val T_U32    = 4
    private const val T_I32    = 5
    private const val T_F32    = 6
    private const val T_BOOL   = 7
    private const val T_STRING = 8
    private const val T_ARRAY  = 9
    private const val T_U64    = 10
    private const val T_I64    = 11
    private const val T_F64    = 12

    data class Header(
        /** general.architecture — what code path llama.cpp uses to load this model. */
        val architecture: String?,
        /** general.name — used as a heuristic to catch known-broken models that lie about architecture. */
        val name: String?,
        /** tokenizer.ggml.model — bpe / gpt2 / llama / spm / etc. */
        val tokenizerModel: String?,
    )

    /** Returns null if the file doesn't parse as a GGUF or the header is malformed. */
    fun read(file: File): Header? = try {
        RandomAccessFile(file, "r").use { raf ->
            val ch = raf.channel
            // Map up to 1 MB — metadata is typically a few KB; bail if not enough.
            val mapSize = minOf(raf.length(), 1L * 1024 * 1024)
            if (mapSize < 24) return@use null
            val buf = ch.map(FileChannel.MapMode.READ_ONLY, 0, mapSize).order(ByteOrder.LITTLE_ENDIAN)

            val magic = buf.int
            if (magic != GGUF_MAGIC) return@use null
            val version = buf.int
            if (version < 1 || version > 3) return@use null

            /* tensor_count */ buf.long
            val metaCount = buf.long
            if (metaCount < 0 || metaCount > 10_000) return@use null

            var arch: String? = null
            var name: String? = null
            var tokModel: String? = null

            for (i in 0 until metaCount) {
                if (buf.remaining() < 12) return@use null
                val key = readString(buf) ?: return@use null
                if (!buf.hasRemaining()) return@use null
                val type = buf.int
                val ok = skipValueCapturing(buf, type, key) { k, v ->
                    when (k) {
                        "general.architecture" -> arch     = v
                        "general.name"         -> name     = v
                        "tokenizer.ggml.model" -> tokModel = v
                    }
                }
                if (!ok) return@use null
                if (arch != null && name != null && tokModel != null) break
            }
            Header(arch, name, tokModel)
        }
    } catch (_: Exception) {
        null
    }

    private fun readString(buf: java.nio.ByteBuffer): String? {
        if (buf.remaining() < 8) return null
        val len = buf.long
        if (len < 0 || len > Int.MAX_VALUE || len > buf.remaining()) return null
        val bytes = ByteArray(len.toInt())
        buf.get(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    /**
     * Advance past a metadata value of [type]. If the value is a string,
     * invoke [onString] with ([key], stringValue). Returns false if the
     * type is unknown or the buffer runs out.
     */
    private fun skipValueCapturing(
        buf: java.nio.ByteBuffer,
        type: Int,
        key: String,
        onString: (String, String) -> Unit,
    ): Boolean {
        return when (type) {
            T_U8, T_I8, T_BOOL                       -> { if (buf.remaining() < 1) false else { buf.get(); true } }
            T_U16, T_I16                             -> { if (buf.remaining() < 2) false else { buf.short; true } }
            T_U32, T_I32, T_F32                      -> { if (buf.remaining() < 4) false else { buf.int;   true } }
            T_U64, T_I64, T_F64                      -> { if (buf.remaining() < 8) false else { buf.long;  true } }
            T_STRING -> {
                val s = readString(buf) ?: return false
                onString(key, s); true
            }
            T_ARRAY -> {
                if (buf.remaining() < 12) return false
                val elemType = buf.int
                val count = buf.long
                if (count < 0 || count > 1_000_000) return false
                // We don't care about array contents — skip element by element.
                for (i in 0 until count) {
                    if (!skipValueCapturing(buf, elemType, key) { _, _ -> }) return false
                }
                true
            }
            else -> false
        }
    }

    sealed class Verdict {
        object Ok : Verdict()
        data class Reject(val reason: String) : Verdict()
    }

    /**
     * The header read alone is enough validation — if the file parses,
     * we hand it to llama_model_load_from_file and trust the loader to
     * return NULL cleanly on architectures it can't handle.
     *
     * (Earlier versions of this code maintained a Kotlin-side allowlist
     * of architecture names. It was always wrong: upstream llama.cpp
     * gains handlers faster than we update the list — Qwen3.5 ("qwen35")
     * and MiniCPM5 ("llama") were both rejected here when their handlers
     * actually exist in b9334. The crash class the allowlist was
     * supposed to prevent only materialized on the stale prebuilt
     * libllama.so we used to ship; with a current build-from-source
     * llama.cpp, the loader's own validation is sufficient.)
     */
    fun verdict(header: Header): Verdict {
        if (header.architecture.isNullOrBlank()) {
            return Verdict.Reject("GGUF has no general.architecture metadata.")
        }
        return Verdict.Ok
    }
}
