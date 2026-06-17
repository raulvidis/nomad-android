package com.nomad.android.data.ai

/**
 * A single source of truth for searchable offline knowledge that the chat (and
 * other surfaces) query to augment LLM answers with retrieved facts.
 *
 * Pure Kotlin — no Android dependencies — so retrieval logic can be unit tested
 * in isolation. Entries are supplied by the DI layer, typically parsed from the
 * bundled `survival_content.json` resource by [com.nomad.android.data.content.KnowledgePackLoader].
 */
class KnowledgeBase(entries: List<KnowledgeEntry>) {

    private val entries: List<KnowledgeEntry> = entries

    /**
     * Distinct categories present in the base, sorted alphabetically, with "All"
     * prepended. Used to drive UI filter chips and unfiltered retrieval.
     */
    val categories: List<String> =
        listOf("All") + entries.map { it.category }.distinct().sorted()

    fun size(): Int = entries.size

    /**
     * Rank entries by bag-of-words token overlap with [query].
     *
     * Tokens are lowercased, stripped of non-alphanumeric characters, and reduced
     * to non-stopword tokens longer than 2 characters — so filler words like
     * "how"/"the"/"what" don't inflate scores and degrade relevance. Entries with
     * no overlap are dropped; the rest are sorted by descending score (ties broken
     * alphabetically by title) and truncated to [topK].
     *
     * [categoryFilter] narrows the candidate set beforehand; "All", blank, or null
     * disables filtering.
     */
    fun search(
        query: String,
        topK: Int = DEFAULT_TOP_K,
        categoryFilter: String? = null,
    ): List<KnowledgeEntry> {
        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) return emptyList()

        val candidates = if (categoryFilter.isNullOrBlank() || categoryFilter.equals("All", ignoreCase = true)) {
            entries
        } else {
            entries.filter { it.category.equals(categoryFilter, ignoreCase = true) }
        }

        return candidates
            .map { entry -> entry to queryTokens.intersect(tokenize("${entry.title} ${entry.content}")).size }
            .filter { it.second > 0 }
            .sortedWith(
                compareByDescending<Pair<KnowledgeEntry, Int>> { it.second }
                    .thenBy { it.first.title.lowercase() },
            )
            .take(topK)
            .map { it.first }
    }

    /**
     * Retrieve the top entries for [query] and format them as a context block
     * suitable for prepending to an LLM prompt. Returns "" when nothing matches,
     * so callers can treat knowledge-base context as purely additive.
     */
    fun retrieveContext(
        query: String,
        topK: Int = DEFAULT_TOP_K,
        categoryFilter: String? = null,
    ): String {
        val hits = search(query, topK, categoryFilter)
        if (hits.isEmpty()) return ""
        return buildString {
            appendLine("Relevant offline knowledge base entries (ground your answer in these when relevant):")
            hits.forEach { entry ->
                appendLine("- [${entry.category}] ${entry.title}: ${entry.content}")
            }
        }
    }

    companion object {
        const val DEFAULT_TOP_K = 3

        private val STOPWORDS = setOf(
            "the", "and", "for", "are", "but", "not", "you", "all", "can", "her",
            "was", "one", "our", "out", "has", "had", "from", "this", "that", "with",
            "they", "will", "would", "there", "their", "what", "about", "which",
            "when", "who", "how", "why", "into", "your", "its", "over", "than",
            "them", "were", "been", "more", "some", "such", "also", "any", "just",
            "should", "tell", "give", "need", "help", "explain", "about",
        )

        private fun tokenize(text: String): Set<String> =
            text.lowercase().split(Regex("\\s+"))
                .map { it.replace(Regex("[^a-z0-9]"), "") }
                .filter { it.length > 2 && it !in STOPWORDS }
                .toSet()
    }
}

/**
 * A single retrievable knowledge entry. [source] identifies provenance
 * (e.g. "bundled" for the shipped JSON pack, or an archive name for ZIM content).
 */
data class KnowledgeEntry(
    val id: String,
    val category: String,
    val title: String,
    val content: String,
    val source: String = "bundled",
)
