package com.nomad.android.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeBaseTest {

    private val base = KnowledgeBase(
        listOf(
            KnowledgeEntry("1", "Survival", "Fire Starting", "Gather tinder, kindling, and fuel. Use a ferro rod or bow drill. Shield from wind."),
            KnowledgeEntry("2", "First Aid", "CPR Basics", "Push hard and fast in the center of the chest at 100-120 compressions per minute."),
            KnowledgeEntry("3", "Navigation", "Cardinal Directions", "North South East West. Sun rises in the east and sets in the west."),
        ),
    )

    @Test
    fun `categories are sorted with All first`() {
        assertEquals(listOf("All", "First Aid", "Navigation", "Survival"), base.categories)
    }

    @Test
    fun `size returns entry count`() {
        assertEquals(3, base.size())
    }

    @Test
    fun `search for fire returns the Fire Starting entry`() {
        val hits = base.search("how do I start a fire")
        assertEquals(1, hits.size)
        assertEquals("Fire Starting", hits[0].title)
    }

    @Test
    fun `search ignores stopwords and short tokens`() {
        // "how"/"the" are stopwords, "do"/"I" are too short -> empty token set
        assertTrue(base.search("how the do I").isEmpty())
    }

    @Test
    fun `search returns empty for gibberish`() {
        assertTrue(base.search("zzzzqqqqxxx").isEmpty())
    }

    @Test
    fun `search respects topK`() {
        val big = KnowledgeBase(
            base.entries + KnowledgeEntry("4", "Survival", "Signal Fires", "Fire can be used as a distress signal."),
        )
        assertEquals(1, big.search("fire", topK = 1).size)
    }

    @Test
    fun `categoryFilter narrows results`() {
        // "compressions" only appears in the CPR (First Aid) entry
        assertEquals("CPR Basics", base.search("compressions").first().title)
        assertTrue(base.search("compressions", categoryFilter = "Survival").isEmpty())
    }

    @Test
    fun `All filter is treated as no filter`() {
        assertEquals(base.search("fire"), base.search("fire", categoryFilter = "All"))
    }

    @Test
    fun `retrieveContext returns empty string when nothing matches`() {
        assertEquals("", base.retrieveContext("zzzzqqqqxxx"))
    }

    @Test
    fun `retrieveContext formats entries with category and title`() {
        val ctx = base.retrieveContext("start a fire")
        assertTrue(ctx.startsWith("Relevant"))
        assertTrue(ctx.contains("Fire Starting"))
        assertTrue(ctx.contains("[Survival]"))
    }

    @Test
    fun `ties are broken alphabetically by title`() {
        val tied = KnowledgeBase(
            listOf(
                KnowledgeEntry("a", "Survival", "Zebra", "water fire shelter"),
                KnowledgeEntry("b", "Survival", "Apple", "water fire shelter"),
            ),
        )
        val hits = tied.search("water")
        assertEquals("Apple", hits[0].title)
        assertEquals("Zebra", hits[1].title)
    }

    @Test
    fun `empty base has only the All category`() {
        val empty = KnowledgeBase(emptyList())
        assertEquals(listOf("All"), empty.categories)
        assertTrue(empty.search("anything").isEmpty())
        assertEquals("", empty.retrieveContext("anything"))
    }
}
