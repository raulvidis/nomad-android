package com.nomad.android.data.ai

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class FallbackEngineTest {

    private val engine = FallbackEngine()

    @Test
    fun `generate returns CPR info when prompt contains cpr`() = runTest {
        val response = engine.generate("How do I perform cpr?", emptyList())
        assertTrue(response.contains("CPR"))
        assertTrue(response.contains("chest"))
        assertTrue(response.contains("[FALLBACK MODE"))
    }

    @Test
    fun `generate returns water purification info when prompt contains water`() = runTest {
        val response = engine.generate("How to purify water?", emptyList())
        assertTrue(response.contains("purify") || response.contains("Boil"))
    }

    @Test
    fun `generate returns fire starting info when prompt contains fire`() = runTest {
        val response = engine.generate("How to start a fire?", emptyList())
        assertTrue(response.contains("fire"))
    }

    @Test
    fun `generate returns fallback message for unknown topics`() = runTest {
        val response = engine.generate("What is quantum computing?", emptyList())
        assertTrue(response.contains("FALLBACK MODE"))
        assertTrue(response.contains("don't have specific information"))
    }

    @Test
    fun `generate returns multiple topics when prompt matches multiple keywords`() = runTest {
        val response = engine.generate("I need first aid for someone bleeding", emptyList())
        assertTrue(response.contains("FALLBACK MODE"))
        assertTrue(response.contains("bleeding") || response.contains("first aid"))
    }

    @Test
    fun `isAvailable always returns true`() = runTest {
        assertTrue(engine.isAvailable())
    }

    @Test
    fun `getModelName returns fallback name`() {
        assertEquals("Fallback (Rule-Based)", engine.getModelName())
    }

    @Test
    fun `loadModel always succeeds`() = runTest {
        val result = engine.loadModel()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `generateStream emits all chunks`() = runTest {
        val flow = engine.generateStream("How to start a fire?", emptyList())
        val results = flow.toList()
        assertTrue(results.isNotEmpty())
        val fullResponse = results.joinToString("")
        assertTrue(fullResponse.contains("fire"))
    }

    @Test
    fun `generate is case insensitive`() = runTest {
        val response = engine.generate("HOW TO PERFORM CPR?", emptyList())
        assertTrue(response.contains("CPR"))
    }

    @Test
    fun `loadModel returns custom Result type`() = runTest {
        val result = engine.loadModel()
        assertTrue(result is com.nomad.android.data.Result.Success)
    }

    // --- Additional tests for exact keyword matching ---

    @Test
    fun `generate matches keyword navigation exactly`() = runTest {
        // CRITICAL: "navigation" must appear as a substring in the prompt
        val response = engine.generate("Help with navigation in the wilderness", emptyList())
        assertTrue(response.contains("FALLBACK MODE"))
        assertTrue(response.contains("Using offline knowledge base"))
        assertTrue(response.contains("Navigation"))
    }

    @Test
    fun `generate matches keyword bleeding`() = runTest {
        val response = engine.generate("How to stop severe bleeding", emptyList())
        assertTrue(response.contains("FALLBACK MODE"))
        assertTrue(response.contains("bleeding"))
    }

    @Test
    fun `generate matches keyword shelter`() = runTest {
        val response = engine.generate("Build emergency shelter", emptyList())
        assertTrue(response.contains("FALLBACK MODE"))
        assertTrue(response.contains("shelter") || response.contains("Shelter"))
    }

    @Test
    fun `generate matches keyword knot`() = runTest {
        val response = engine.generate("Tie a knot for securing rope", emptyList())
        assertTrue(response.contains("FALLBACK MODE"))
        assertTrue(response.contains("knot") || response.contains("Knot"))
    }

    @Test
    fun `generate matches keyword plant`() = runTest {
        val response = engine.generate("Identify edible plant species", emptyList())
        assertTrue(response.contains("FALLBACK MODE"))
        assertTrue(response.contains("plant") || response.contains("Plant"))
    }

    @Test
    fun `generate matches keyword sos`() = runTest {
        val response = engine.generate("How to send sos signal", emptyList())
        assertTrue(response.contains("FALLBACK MODE"))
        assertTrue(response.contains("SOS") || response.contains("sos"))
    }

    @Test
    fun `generate matches keyword first aid`() = runTest {
        val response = engine.generate("Basic first aid procedures", emptyList())
        assertTrue(response.contains("FALLBACK MODE"))
        assertTrue(response.contains("first aid") || response.contains("First aid"))
    }

    @Test
    fun `generate returns image fallback when imagePath provided`() = runTest {
        val response = engine.generate("cpr", emptyList(), imagePath = "/some/image.jpg")
        assertTrue(response.contains("FALLBACK MODE"))
        assertTrue(response.contains("Image analysis requires"))
    }

    @Test
    fun `generate no-match response suggests available topics`() = runTest {
        val response = engine.generate("quantum physics equations", emptyList())
        assertTrue(response.contains("CPR"))
        assertTrue(response.contains("navigation"))
        assertTrue(response.contains("knots"))
    }

    @Test
    fun `getDeviceInfo returns zeros and false`() {
        val info = engine.getDeviceInfo()
        assertEquals(0L, info.totalRamMB)
        assertEquals(0L, info.availableRamMB)
        assertFalse(info.hasNPU)
        assertFalse(info.hasGPU)
    }

    @Test
    fun `unloadModel does nothing`() = runTest {
        engine.unloadModel()
    }
}
