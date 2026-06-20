package com.nomad.android.data.ai.tools

import com.nomad.android.data.Result
import com.nomad.android.data.repository.NoteRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lets the model search the user's saved notes. Read-only; wraps
 * [NoteRepository.searchNotes] (title/content LIKE match). Returns a formatted list
 * of matches, a "no matches" message, or an error if the lookup fails.
 */
class SearchNotesTool(
    private val noteRepository: NoteRepository,
) : ChatTool {

    override val name: String = "search_notes"

    override val description: String =
        "Search the user's own saved notes by keyword (matches note titles and bodies). " +
            "Call this when the user refers to something they wrote down or asks what their notes say."

    override val parameters: JSONObject = JSONObject().apply {
        put("type", "object")
        put(
            "properties",
            JSONObject().apply {
                put(
                    "query",
                    JSONObject().apply {
                        put("type", "string")
                        put("description", "Keyword(s) to match against note titles and contents.")
                    },
                )
            },
        )
        put("required", JSONArray().apply { put("query") })
    }

    override suspend fun execute(args: JSONObject): ToolResult {
        val query = args.optString("query").trim()
        if (query.isEmpty()) return ToolResult.Err("missing required argument: query")

        return when (val result = noteRepository.searchNotes(query).first()) {
            is Result.Success -> {
                val notes = result.data
                if (notes.isEmpty()) {
                    ToolResult.Ok("No matching notes.")
                } else {
                    val text = notes.joinToString("\n\n") { note ->
                        "${note.title}: ${note.content.take(SNIPPET_LEN)}"
                    }
                    ToolResult.Ok(text)
                }
            }
            is Result.Error -> ToolResult.Err(result.message)
        }
    }

    private companion object {
        const val SNIPPET_LEN = 300
    }
}
