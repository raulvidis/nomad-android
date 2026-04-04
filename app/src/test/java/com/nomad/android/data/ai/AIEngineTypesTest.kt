package com.nomad.android.data.ai

import org.junit.Assert.*
import org.junit.Test

class AIEngineTypesTest {

    @Test
    fun `AIEngineType enum has all values`() {
        val values = AIEngineType.values()
        assertEquals(4, values.size)
        assertTrue(values.contains(AIEngineType.LITERTLM_E2B))
        assertTrue(values.contains(AIEngineType.LITERTLM_1B))
        assertTrue(values.contains(AIEngineType.FALLBACK))
        assertTrue(values.contains(AIEngineType.NONE))
    }

    @Test
    fun `AIEngineStatus data class holds values`() {
        val status = AIEngineStatus(
            engineType = AIEngineType.LITERTLM_E2B,
            isReady = true,
            modelName = "Gemma 4 E2B",
            ramRequired = "6144MB total",
            modelSize = "3000 MB"
        )
        assertEquals(AIEngineType.LITERTLM_E2B, status.engineType)
        assertTrue(status.isReady)
        assertEquals("Gemma 4 E2B", status.modelName)
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
    fun `LiteRTLMEngine ModelVariant E2B has correct properties`() {
        val e2b = LiteRTLMEngine.ModelVariant.E2B
        assertEquals("Gemma 4 E2B", e2b.displayName)
        assertEquals("gemma4-e2b.bin", e2b.fileName)
        assertEquals(6144L, e2b.ramRequiredMB)
        assertEquals(3000, e2b.sizeMB)
    }

    @Test
    fun `LiteRTLMEngine ModelVariant ONE_B has correct properties`() {
        val oneB = LiteRTLMEngine.ModelVariant.ONE_B
        assertEquals("Gemma 3 1B", oneB.displayName)
        assertEquals("gemma3-1b.bin", oneB.fileName)
        assertEquals(2048L, oneB.ramRequiredMB)
        assertEquals(1000, oneB.sizeMB)
    }

    @Test
    fun `recommendedVariant selects E2B for 6GB+ RAM`() {
        assertEquals(LiteRTLMEngine.ModelVariant.E2B, LiteRTLMEngine.recommendedVariant(6144))
        assertEquals(LiteRTLMEngine.ModelVariant.E2B, LiteRTLMEngine.recommendedVariant(8192))
    }

    @Test
    fun `recommendedVariant selects 1B for 2GB-6GB RAM`() {
        assertEquals(LiteRTLMEngine.ModelVariant.ONE_B, LiteRTLMEngine.recommendedVariant(4096))
        assertEquals(LiteRTLMEngine.ModelVariant.ONE_B, LiteRTLMEngine.recommendedVariant(2048))
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
