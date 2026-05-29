package com.nomad.android.data.ai

import org.junit.Assert.*
import org.junit.Test

class LlamaCppEngineTest {

    @Test
    fun `MINICPM5_1B variant has correct coordinates`() {
        val v = LlamaCppEngine.ModelVariant.MINICPM5_1B
        assertEquals("MiniCPM5 1B", v.displayName)
        assertEquals("MiniCPM5-1B-Q4_K_M.gguf", v.fileName)
        assertEquals(656, v.sizeMB)
        assertTrue(v.downloadUrl.startsWith("https://huggingface.co/openbmb/MiniCPM5-1B-GGUF/resolve/main/"))
        assertTrue(v.downloadUrl.endsWith("MiniCPM5-1B-Q4_K_M.gguf"))
    }

    @Test
    fun `recommendedVariant always returns the single model`() {
        assertEquals(LlamaCppEngine.ModelVariant.MINICPM5_1B, LlamaCppEngine.recommendedVariant(8192))
        assertEquals(LlamaCppEngine.ModelVariant.MINICPM5_1B, LlamaCppEngine.recommendedVariant(1024))
    }

    @Test
    fun `stripThinking removes think blocks and trims`() {
        val raw = "<think>let me reason</think>\n\nBoil the water for 1 minute."
        assertEquals("Boil the water for 1 minute.", LlamaCppEngine.stripThinking(raw))
    }

    @Test
    fun `stripThinking leaves plain text untouched`() {
        assertEquals("Just an answer.", LlamaCppEngine.stripThinking("Just an answer."))
    }

    @Test
    fun `stripThinking drops an unclosed think block`() {
        assertEquals("", LlamaCppEngine.stripThinking("<think>still reasoning and not done"))
    }

    @Test
    fun `buildPromptWithContext inlines history before the question`() {
        val out = LlamaCppEngine.buildPromptWithContext("How do I purify water?", listOf("User: hi", "AI: hello"))
        assertTrue(out.contains("User: hi"))
        assertTrue(out.contains("AI: hello"))
        assertTrue(out.trimEnd().endsWith("How do I purify water?"))
    }

    @Test
    fun `streamingClean suppresses output while a think block is open`() {
        val (delta, emitted) = LlamaCppEngine.streamingClean("<think>reasoning", 0)
        assertEquals("", delta)
        assertEquals(0, emitted)
    }

    @Test
    fun `streamingClean emits cleaned text past the tail guard once think closes`() {
        val acc = "<think>reasoning</think>The answer is to boil water thoroughly."
        val (delta, emitted) = LlamaCppEngine.streamingClean(acc, 0)
        assertTrue(delta.startsWith("The answer is to boil water"))
        assertTrue(emitted > 0)
        // Tail guard holds back the last 7 chars of the cleaned text.
        val cleaned = LlamaCppEngine.stripThinking(acc)
        assertEquals(cleaned.length - 7, emitted)
    }
}
