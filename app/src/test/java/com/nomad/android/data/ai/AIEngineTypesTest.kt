package com.nomad.android.data.ai

import org.junit.Assert.*
import org.junit.Test

class AIEngineTypesTest {

    @Test
    fun `AIEngineType enum has all values`() {
        val values = AIEngineType.values()
        assertEquals(5, values.size)
        assertTrue(values.contains(AIEngineType.LITERTLM_E2B))
        assertTrue(values.contains(AIEngineType.LITERTLM_QWEN35_2B))
        assertTrue(values.contains(AIEngineType.LITERTLM_QWEN35_08B))
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
    fun `LiteRTLMEngine ModelVariant GEMMA4_E2B has correct properties`() {
        val gemma4 = LiteRTLMEngine.ModelVariant.GEMMA4_E2B
        assertEquals("Gemma 4 E2B", gemma4.displayName)
        assertEquals("gemma-4-E2B-it.litertlm", gemma4.fileName)
        assertEquals(2048L, gemma4.ramRequiredMB)
        assertEquals(2643, gemma4.sizeMB)
        assertTrue(gemma4.downloadUrl.startsWith("https://"))
        assertTrue(gemma4.downloadUrl.contains("huggingface"))
    }

    @Test
    fun `recommendedVariant selects Gemma4 for high RAM`() {
        assertEquals(LiteRTLMEngine.ModelVariant.GEMMA4_E2B, LiteRTLMEngine.recommendedVariant(8192))
        assertEquals(LiteRTLMEngine.ModelVariant.GEMMA4_E2B, LiteRTLMEngine.recommendedVariant(4096))
        assertEquals(LiteRTLMEngine.ModelVariant.GEMMA4_E2B, LiteRTLMEngine.recommendedVariant(2048))
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
