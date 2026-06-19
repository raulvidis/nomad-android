package com.nomad.android.data.ai.tools

import org.json.JSONObject

/**
 * A tool the chat model can invoke mid-turn. Tools are advertised to llama.cpp as
 * an OpenAI-style function schema (see [ChatToolRegistry.describeAsJson]); when the
 * model emits a call, the registry parses the arguments and runs [execute].
 *
 * Nomad's tools are local and read-only (knowledge base, notes), so they run without
 * an approval prompt — see the design spec.
 */
interface ChatTool {
    /** Stable identifier the model uses to call the tool, e.g. `search_knowledge_base`. */
    val name: String

    /** Natural-language description sent to the model so it knows when to call the tool. */
    val description: String

    /** JSON Schema (`{"type":"object", "properties":{…}, "required":[…]}`) for the arguments. */
    val parameters: JSONObject

    /** Run the tool. Implementations must not throw; map failures to [ToolResult.Err]. */
    suspend fun execute(args: JSONObject): ToolResult
}

/**
 * Outcome of a tool call. [toModelString] is what gets fed back into the conversation
 * as the tool result for the model's next turn.
 */
sealed class ToolResult {
    data class Ok(val text: String) : ToolResult()
    data class Err(val message: String) : ToolResult()

    fun toModelString(): String = when (this) {
        is Ok -> text
        is Err -> "ERROR: $message"
    }
}
