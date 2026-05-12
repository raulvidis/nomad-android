package com.nomad.android.data.content

/**
 * Mock ZIM archive search results used when a ZIM file is present
 * but libkiwix JNI is not yet wired up.
 * Extracted from KiwixManager for testability.
 */
object ZimMockSearch {

    private val mockArticles = listOf(
        ZimSearchResult("A/Water", "Water", "Water is essential for survival. The human body can survive only 3 days without water..."),
        ZimSearchResult("A/Water_purification", "Water purification", "Water purification is the process of removing undesirable chemicals..."),
        ZimSearchResult("A/Solar_still", "Solar still", "A solar still is a device that uses solar energy to distill water..."),
    )

    /**
     * Search mock articles by title and snippet (case-insensitive substring match).
     * When no matches are found, returns a single "no exact matches" result.
     */
    fun search(query: String): List<ZimSearchResult> =
        mockArticles
            .filter { it.title.contains(query, ignoreCase = true) || it.snippet.contains(query, ignoreCase = true) }
            .ifEmpty { listOf(ZimSearchResult("A/Search", "Search: $query", "No exact matches found. Try broader terms.")) }
}
