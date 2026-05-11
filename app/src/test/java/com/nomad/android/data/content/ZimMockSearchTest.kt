package com.nomad.android.data.content

import org.junit.Assert.*
import org.junit.Test

class ZimMockSearchTest {

    @Test
    fun `search matches Water by title`() {
        val results = ZimMockSearch.search("Water")
        assertEquals(1, results.size)
        assertEquals("Water", results[0].title)
    }

    @Test
    fun `search matches Water purification by title substring`() {
        val results = ZimMockSearch.search("purification")
        assertEquals(1, results.size)
        assertEquals("Water purification", results[0].title)
    }

    @Test
    fun `search matches Solar still by title`() {
        val results = ZimMockSearch.search("solar")
        assertEquals(1, results.size)
        assertEquals("Solar still", results[0].title)
    }

    @Test
    fun `search matches multiple water-related articles`() {
        val results = ZimMockSearch.search("water")
        // "Water" matches title, "Water purification" matches title,
        // "Solar still" snippet mentions "distill water"
        assertTrue(results.size >= 2)
    }

    @Test
    fun `search returns no-matches result for unrelated query`() {
        val results = ZimMockSearch.search("quantum")
        assertEquals(1, results.size)
        assertEquals("A/Search", results[0].path)
        assertTrue(results[0].snippet.contains("No exact matches"))
    }

    @Test
    fun `search is case-insensitive`() {
        val lower = ZimMockSearch.search("water")
        val upper = ZimMockSearch.search("WATER")
        assertEquals(lower.size, upper.size)
        assertEquals(lower.map { it.title }, upper.map { it.title })
    }

    @Test
    fun `no-matches result includes original query in title`() {
        val results = ZimMockSearch.search("xyzzy")
        assertEquals("Search: xyzzy", results[0].title)
    }

    @Test
    fun `search results have correct path format`() {
        val results = ZimMockSearch.search("Water")
        assertTrue(results[0].path.startsWith("A/"))
    }

    @Test
    fun `empty query matches all mock articles`() {
        val results = ZimMockSearch.search("")
        // Empty string is contained in everything
        assertEquals(3, results.size)
    }
}
