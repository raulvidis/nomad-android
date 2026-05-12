package com.nomad.android.data.content

import org.junit.Assert.*
import org.junit.Test

class BundledContentSearchTest {

    // --- Title matching ---

    @Test
    fun `search matches by title case-insensitively`() {
        val results = BundledContentSearch.search("cpr")
        assertEquals(1, results.size)
        assertEquals("CPR Basics", results[0].title)
    }

    @Test
    fun `search matches by title with exact case`() {
        val results = BundledContentSearch.search("Water Purification")
        assertEquals(1, results.size)
        assertEquals("Water Purification", results[0].title)
    }

    @Test
    fun `search matches partial title`() {
        val results = BundledContentSearch.search("fire")
        // "fire" matches "Fire Starting" (title) and "SOS Signals" (content contains "fires")
        assertTrue(results.size >= 1)
        val titles = results.map { it.title }
        assertTrue(titles.contains("Fire Starting"))
    }

    // --- Content matching ---

    @Test
    fun `search matches by content body`() {
        val results = BundledContentSearch.search("dandelion")
        assertEquals(1, results.size)
        assertEquals("Edible Plants", results[0].title)
    }

    @Test
    fun `search matches multiple entries when query appears in several`() {
        val results = BundledContentSearch.search("water")
        // "Water Purification" title + "SOS Signals" mentions water, "First Aid" doesn't
        // "Water Purification" has "water" in title, "Edible Plants" mentions purification?
        // Let's just verify we get at least the water-related ones
        assertTrue(results.size >= 2)
        val titles = results.map { it.title }
        assertTrue(titles.contains("Water Purification"))
    }

    // --- Result format ---

    @Test
    fun `search results have A-slash prefix in path`() {
        val results = BundledContentSearch.search("CPR")
        assertEquals("A/CPR Basics", results[0].path)
    }

    @Test
    fun `search results snippet is capped at 100 chars plus ellipsis`() {
        val results = BundledContentSearch.search("CPR")
        val snippet = results[0].snippet
        // The content is long; snippet should be content.take(100) + "..."
        assertTrue(snippet.endsWith("..."))
        // 100 chars of content + 3 for "..."
        assertTrue(snippet.length <= 103)
    }

    // --- No matches ---

    @Test
    fun `search returns no-matches result for unknown query`() {
        val results = BundledContentSearch.search("quantum computing")
        assertEquals(1, results.size)
        assertEquals("A/Search", results[0].path)
        assertTrue(results[0].snippet.contains("No matches"))
        assertTrue(results[0].snippet.contains("Settings"))
    }

    @Test
    fun `search no-matches result includes original query`() {
        val results = BundledContentSearch.search("xyz123abc")
        assertEquals("Search: xyz123abc", results[0].title)
    }

    // --- Entries integrity ---

    @Test
    fun `bundled knowledge has exactly 9 entries`() {
        assertEquals(9, BundledContentSearch.entries.size)
    }

    @Test
    fun `all bundled entries have non-blank title and content`() {
        for (entry in BundledContentSearch.entries) {
            assertTrue("Entry '${entry.title}' has blank title", entry.title.isNotBlank())
            assertTrue("Entry '${entry.title}' has blank content", entry.content.isNotBlank())
        }
    }

    @Test
    fun `all bundled entries have unique titles`() {
        val titles = BundledContentSearch.entries.map { it.title }
        assertEquals(titles.size, titles.toSet().size)
    }

    // --- Edge cases ---

    @Test
    fun `search with empty string matches all entries`() {
        val results = BundledContentSearch.search("")
        assertEquals(BundledContentSearch.entries.size, results.size)
    }

    @Test
    fun `search matches shelter building via title substring`() {
        val results = BundledContentSearch.search("shelter")
        assertEquals(1, results.size)
        assertEquals("Shelter Building", results[0].title)
    }

    @Test
    fun `search matches navigation entry`() {
        val results = BundledContentSearch.search("polaris")
        assertEquals(1, results.size)
        assertEquals("Navigation", results[0].title)
    }

    @Test
    fun `search matches knots entry`() {
        val results = BundledContentSearch.search("bowline")
        assertEquals(1, results.size)
        assertEquals("Knots", results[0].title)
    }
}
