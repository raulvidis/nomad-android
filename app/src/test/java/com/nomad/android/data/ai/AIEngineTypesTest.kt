package com.nomad.android.data.ai

import org.junit.Assert.*
import org.junit.Test

class AIEngineTypesTest {

    @Test
    fun `AIEngineType enum has all values`() {
        val values = AIEngineType.values()
        assertEquals(3, values.size)
        assertTrue(values.contains(AIEngineType.LLAMACPP_MINICPM5))
        assertTrue(values.contains(AIEngineType.FALLBACK))
        assertTrue(values.contains(AIEngineType.NONE))
    }

    @Test
    fun `AIEngineStatus data class holds values`() {
        val status = AIEngineStatus(
            engineType = AIEngineType.LLAMACPP_MINICPM5,
            isReady = true,
            modelName = "MiniCPM5 1B",
            ramRequired = "6144MB total",
            modelSize = "656 MB"
        )
        assertEquals(AIEngineType.LLAMACPP_MINICPM5, status.engineType)
        assertTrue(status.isReady)
        assertEquals("MiniCPM5 1B", status.modelName)
    }

    @Test
    fun `DeviceInfo data class holds values`() {
        val info = DeviceInfo(
            totalRamMB = 8192,
            availableRamMB = 4096,
            hasNPU = true,
            hasGPU = true
        )
        assertEquals(8192L, info.totalRamMB)
        assertEquals(4096L, info.availableRamMB)
        assertTrue(info.hasNPU)
        assertTrue(info.hasGPU)
    }

    @Test
    fun `RAGChunk data class holds values`() {
        val chunk = RAGChunk(1L, "local://doc0", "Document 0", "Boil water...", 0)
        assertEquals(1L, chunk.id)
        assertEquals("local://doc0", chunk.source)
        assertEquals("Document 0", chunk.title)
        assertEquals("Boil water...", chunk.chunkText)
    }

    @Test
    fun `RAGQuery data class has default empty sources`() {
        val query = RAGQuery("question", emptyList())
        assertEquals("question", query.question)
        assertTrue(query.sources.isEmpty())
    }
}
