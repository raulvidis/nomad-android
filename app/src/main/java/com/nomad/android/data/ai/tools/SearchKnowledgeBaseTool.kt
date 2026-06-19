package com.nomad.android.data.ai.tools

import com.nomad.android.data.ai.KnowledgeBase
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lets the model search the bundled offline survival knowledge base. Read-only;
 * wraps [KnowledgeBase.search]. Returns a formatted block of the top matches, or a
 * plain "no matches" message (never an error) so the model can react gracefully.
 */
class SearchKnowledgeBaseTool(
    private val knowledgeBase: KnowledgeBase,
) : ChatTool {

    override val name: String = "search_knowledge_base"

    override val description: String =
        "Search the offline survival knowledge base (first aid, water, fire, shelter, " +
            "navigation, emergency procedures, and bundled reference content). Call this " +
            "when the user's question needs stored factual/survival knowledge."

    override val parameters: JSONObject = JSONObject().apply {
        put("type", "object")
        put(
            "properties",
            JSONObject().apply {
                put(
                    "query",
                    JSONObject().apply {
                        put("type", "string")
                        put("description", "What to look up, e.g. 'how to treat a burn'.")
                    },
                )
                put(
                    "category",
                    JSONObject().apply {
                        put("type", "string")
                        put("description", "Optional category filter, e.g. 'First Aid'. Omit to search all.")
                    },
                )
            },
        )
        put("required", JSONArray().apply { put("query") })
    }

    override suspend fun execute(args: JSONObject): ToolResult {
        val query = args.optString("query").trim()
        if (query.isEmpty()) return ToolResult.Err("missing required argument: query")
        val category = args.optString("category").ifBlank { null }

        val hits = knowledgeBase.search(query, categoryFilter = category)
        if (hits.isEmpty()) return ToolResult.Ok("No matching knowledge-base entries.")

        val text = hits.joinToString("\n\n") { entry ->
            "[${entry.title} | ${entry.source}]\n${entry.content}"
        }
        return ToolResult.Ok(text)
    }
}
