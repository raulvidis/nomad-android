package com.nomad.android.data.ai

import org.junit.Assert.*
import org.junit.Test

class AIEngineTypesTest {

    @Test
    fun `AIEngineType enum has all values`() {
        val values = AIEngineType.values()
        assertEquals(5, values.size)
        assertTrue(values.contains(AIEngineType.LLAMACPP_MINICPM5))
        assertTrue(values.contains(AIEngineType.LLAMACPP_QWEN3_5))
        assertTrue(values.contains(AIEngineType.LLAMACPP_GEMMA4))
        assertTrue(values.contains(AIEngineType.FALLBACK))
        assertTrue(values.contains(AIEngineType.NONE))
    }

    @Test
    fun `fromVariant maps each ModelVariant to correct AIEngineType`() {
        assertEquals(
            AIEngineType.LLAMACPP_MINICPM5,
            AIEngineType.fromVariant(LlamaCppEngine.ModelVariant.MINICPM5_1B)
        )
        assertEquals(
            AIEngineType.LLAMACPP_QWEN3_5,
            AIEngineType.fromVariant(LlamaCppEngine.ModelVariant.QWEN3_5_0_8B)
        )
        assertEquals(
            AIEngineType.LLAMACPP_GEMMA4,
            AIEngineType.fromVariant(LlamaCppEngine.ModelVariant.GEMMA4_E2B)
        )
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
}
