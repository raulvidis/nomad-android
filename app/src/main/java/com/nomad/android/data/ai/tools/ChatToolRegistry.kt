package com.nomad.android.data.ai.tools

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

/**
 * Holds the set of tools the chat model may call and bridges them to llama.cpp.
 *
 * [describeAsJson] renders the tools as the OpenAI-compatible function array that
 * [com.nomad.android.data.ai.LlamaBridge.submitTurn] forwards to `common_chat`.
 * [execute] runs a parsed call defensively: unknown tool, bad JSON, timeout, and
 * thrown exceptions all map to [ToolResult.Err] so the agent loop never crashes.
 */
class ChatToolRegistry(
    private val tools: List<ChatTool>,
) {
    private val byName: Map<String, ChatTool> = tools.associateBy { it.name }

    val toolNames: Set<String> get() = byName.keys

    fun get(name: String): ChatTool? = byName[name]

    /** OpenAI-style `[{"type":"function","function":{name,description,parameters}}]`. */
    fun describeAsJson(): String {
        val arr = JSONArray()
        for (tool in tools) {
            arr.put(
                JSONObject().apply {
                    put("type", "function")
                    put(
                        "function",
                        JSONObject().apply {
                            put("name", tool.name)
                            put("description", tool.description)
                            put("parameters", tool.parameters)
                        },
                    )
                },
            )
        }
        return arr.toString()
    }

    suspend fun execute(name: String, argsJson: String): ToolResult {
        val tool = byName[name] ?: return ToolResult.Err("unknown tool: $name")
        val args = try {
            if (argsJson.isBlank()) JSONObject() else JSONObject(argsJson)
        } catch (_: Exception) {
            return ToolResult.Err("invalid JSON arguments")
        }
        return try {
            withTimeout(TIMEOUT_MS) { tool.execute(args) }
        } catch (_: TimeoutCancellationException) {
            ToolResult.Err("tool timed out after ${TIMEOUT_MS / 1000}s")
        } catch (e: Exception) {
            ToolResult.Err("${e.javaClass.simpleName}: ${e.message ?: "unknown error"}")
        }
    }

    companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
