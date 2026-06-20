package com.nomad.android.data.ai

import org.json.JSONObject

/**
 * Splits a model's raw output into `(thinking, answer)`. Ported from opendroid.
 * Handles three formats:
 *  - Explicit: model emits both `<think>` and `</think>`.
 *  - Implicit: the chat template injects `<think>\n` before generation, so the
 *    model emits ONLY the closing `</think>` followed by the answer. **MiniCPM5's
 *    GGUF template does this** — without handling it the reasoning leaks into the
 *    answer bubble.
 *  - GPT-OSS / Harmony channel format (`<|channel|>…final…`).
 *
 * During streaming the buffer arrives incrementally; before `</think>` lands the
 * implicit case is indistinguishable from a plain answer, so text briefly shows in
 * the answer and snaps into the thinking section once the close tag arrives.
 */
object ThinkingParser {

    private val THINK_OPEN = Regex("<think[^>]*>")
    private val THINK_CLOSE = Regex("</think\\s*>")
    private val CHANNEL_FINAL = Regex("<\\|channel\\|>\\s*final")
    private val HARMONY_CHANNEL = Regex("<\\|channel\\|>\\s*\\w+")
    private val HARMONY_START = Regex("<\\|start\\|>\\s*\\w*")
    private val HARMONY_TOKEN = Regex("<\\|[a-zA-Z_]+\\|>")

    /** @return Pair(thinking, answer). */
    fun split(raw: String): Pair<String, String> {
        val openMatch = THINK_OPEN.find(raw)
        if (openMatch != null) {
            val openEnd = openMatch.range.last + 1
            val closeMatch = THINK_CLOSE.find(raw, openEnd)
            return if (closeMatch == null) {
                Pair(raw.substring(openEnd), "")
            } else {
                Pair(
                    raw.substring(openEnd, closeMatch.range.first),
                    raw.substring(closeMatch.range.last + 1).trimStart(),
                )
            }
        }

        val implicitClose = THINK_CLOSE.find(raw)
        if (implicitClose != null) {
            return Pair(
                raw.substring(0, implicitClose.range.first).trimStart(),
                raw.substring(implicitClose.range.last + 1).trimStart(),
            )
        }

        if (raw.contains("<|channel|>")) {
            val finalCh = CHANNEL_FINAL.find(raw)
            return if (finalCh == null) {
                Pair(stripHarmony(raw), "")
            } else {
                Pair(
                    stripHarmony(raw.substring(0, finalCh.range.first)),
                    stripHarmony(raw.substring(finalCh.range.first)),
                )
            }
        }
        return Pair("", raw)
    }

    private fun stripHarmony(s: String): String = s
        .replace(HARMONY_CHANNEL, "")
        .replace(HARMONY_START, "")
        .replace(HARMONY_TOKEN, "")
        .trim()

    /**
     * Remove tool-call markup from text destined for the answer bubble. Once a
     * native (`<tool_call>…`) or improvised (`<function…`) call begins, the rest of
     * the buffer is machinery, not prose — so we display only what precedes it.
     */
    fun stripToolMarkup(s: String): String {
        val cut = listOf("<tool_call", "<function")
            .mapNotNull { marker -> s.indexOf(marker).takeIf { it >= 0 } }
            .minOrNull()
        return (if (cut != null) s.substring(0, cut) else s).trim()
    }
}

/**
 * Best-effort parser for the `<function name="…"><param name="…">value</param>…
 * </function>` shape that weak models improvise when they don't follow the chat
 * template's native tool-call format. Returns the content with those blocks
 * stripped plus the extracted calls. Param values become JSON strings; the tools'
 * arg readers coerce as needed. Calls with a blank name are dropped. Ported from
 * opendroid as a hedge for MiniCPM5-1B's inconsistent tool-call formatting.
 */
object ToolCallSalvage {

    private val FN_REGEX = Regex(
        "<function\\s+name=[\"']([^\"']+)[\"']\\s*>(.*?)</function>",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val PARAM_REGEX = Regex(
        "<param\\s+name=[\"']([^\"']+)[\"']\\s*>(.*?)</param>",
        RegexOption.DOT_MATCHES_ALL,
    )

    fun parse(content: String): Pair<String, List<LlamaBridge.ParsedToolCall>> {
        val matches = FN_REGEX.findAll(content).toList()
        if (matches.isEmpty()) return content to emptyList()
        val calls = matches.mapIndexedNotNull { i, m ->
            val name = m.groupValues[1].trim()
            if (name.isEmpty()) return@mapIndexedNotNull null
            val args = JSONObject()
            for (p in PARAM_REGEX.findAll(m.groupValues[2])) {
                args.put(p.groupValues[1].trim(), p.groupValues[2].trim())
            }
            LlamaBridge.ParsedToolCall(id = "fn-$i", name = name, arguments = args.toString())
        }
        val stripped = content.replace(FN_REGEX, "").trim()
        return stripped to calls
    }
}
