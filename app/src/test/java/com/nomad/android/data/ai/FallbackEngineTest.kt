package com.nomad.android.data.ai

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class FallbackEngineTest {

    private val engine = FallbackEngine()

    @Test
    fun `generate returns CPR info when prompt contains cpr`() = runTest {
        val response = engine.generate("How do I perform CPR?", emptyList())
        assertTrue(response.contains("CPR"))
        assertTrue(response.contains("chest"))
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

    @Test
    fun `generate with imagePath returns model-required message`() = runTest {
        val response = engine.generate("How to start a fire?", emptyList(), imagePath = "/some/image.jpg")
        assertTrue(response.contains("FALLBACK MODE"))
        assertTrue(response.contains("Image analysis requires the AI model"))
        assertFalse(response.contains("fire")) // Should NOT return survival info for image prompts
    }

    @Test
    fun `generate with imagePath ignores matching keywords`() = runTest {
        val response = engine.generate("Help with CPR for bleeding", emptyList(), imagePath = "/photo.png")
        assertTrue(response.contains("Image analysis requires the AI model"))
        assertFalse(response.contains("chest"))
        assertFalse(response.contains("bleeding"))
    }

    @Test
    fun `getDeviceInfo returns all zeros and false`() {
        val info = engine.getDeviceInfo()
        assertEquals(0L, info.totalRamMB)
        assertEquals(0L, info.availableRamMB)
        assertFalse(info.hasNPU)
        assertFalse(info.hasGPU)
    }

    @Test
    fun `unloadModel does not crash`() {
        engine.unloadModel() // Should be a no-op
    }

    @Test
    fun `generate returns navigation info`() = runTest {
        val response = engine.generate("How do I navigate without a compass?", emptyList())
        assertTrue(response.contains("Navigation") || response.contains("navigation"))
        assertTrue(response.contains("North Star") || response.contains("north"))
    }

    @Test
    fun `generate returns shelter info`() = runTest {
        val response = engine.generate("How to build a shelter in the woods?", emptyList())
        assertTrue(response.contains("shelter") || response.contains("Shelter"))
    }

    @Test
    fun `generate returns knot info`() = runTest {
        val response = engine.generate("What knots should I know?", emptyList())
        assertTrue(response.contains("knot") || response.contains("Knot"))
        assertTrue(response.contains("Bowline") || response.contains("bowline"))
    }

    @Test
    fun `generate returns plant info`() = runTest {
        val response = engine.generate("Which plants are safe to eat?", emptyList())
        assertTrue(response.contains("plant") || response.contains("Plant"))
    }

    @Test
    fun `generate returns sos info`() = runTest {
        val response = engine.generate("How do I signal for help?", emptyList())
        assertTrue(response.contains("SOS") || response.contains("sos") || response.contains("signal"))
    }
}
