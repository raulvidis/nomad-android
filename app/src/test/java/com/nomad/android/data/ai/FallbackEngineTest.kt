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
}
