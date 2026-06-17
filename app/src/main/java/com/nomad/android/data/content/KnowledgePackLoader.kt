package com.nomad.android.data.content

import android.content.Context
import android.util.Log
import com.nomad.android.data.ai.KnowledgeEntry
import org.json.JSONObject

/**
 * Loads bundled offline knowledge from the `survival_content.json` raw resource
 * into [KnowledgeEntry] objects for the [com.nomad.android.data.ai.KnowledgeBase].
 *
 * Expected JSON shape: `{ "articles": [ { "id", "category", "title", "content" }, ... ] }`
 * — the same resource the Knowledge screen consumes, so the chat and the
 * knowledge browser share one canonical content pack.
 *
 * On any parse failure this returns an empty list so the chat degrades
 * gracefully (still works, just without knowledge-base augmentation).
 */
object KnowledgePackLoader {

    private const val TAG = "KnowledgePackLoader"
    private const val RESOURCE_NAME = "survival_content"
    private const val RESOURCE_TYPE = "raw"

    fun load(context: Context): List<KnowledgeEntry> {
        val json = readResource(context) ?: return emptyList()
        return parse(json)
    }

    fun parse(json: String): List<KnowledgeEntry> = try {
        val root = JSONObject(json)
        val articles = root.optJSONArray("articles") ?: return emptyList()
        buildList {
            for (i in 0 until articles.length()) {
                val obj = articles.optJSONObject(i) ?: continue
                val title = obj.optString("title").trim()
                if (title.isBlank()) continue
                add(
                    KnowledgeEntry(
                        id = obj.optString("id").ifBlank { "kb_$i" },
                        category = obj.optString("category").ifBlank { "Survival" },
                        title = title,
                        content = obj.optString("content").trim(),
                        source = obj.optString("source").ifBlank { "bundled" },
                    ),
                )
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to parse bundled knowledge JSON", e)
        emptyList()
    }

    private fun readResource(context: Context): String? = try {
        val resId = context.resources.getIdentifier(RESOURCE_NAME, RESOURCE_TYPE, context.packageName)
        if (resId == 0) {
            Log.w(TAG, "Bundled knowledge resource '$RESOURCE_NAME' not found")
            null
        } else {
            context.resources.openRawResource(resId).bufferedReader().use { it.readText() }
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to read bundled knowledge resource", e)
        null
    }
}
