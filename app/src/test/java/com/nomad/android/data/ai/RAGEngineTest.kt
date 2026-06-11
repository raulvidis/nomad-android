package com.nomad.android.data.ai

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RAGEngineTest {

    private val mockEngine = object : AIEngine {
        override suspend fun generate(prompt: String, context: List<String>, imagePath: String?): String = "mock"
        override fun generateStream(prompt: String, context: List<String>, imagePath: String?): kotlinx.coroutines.flow.Flow<String> =
            kotlinx.coroutines.flow.flow { emit("mock") }
        override suspend fun isAvailable(): Boolean = true
        override fun getModelName(): String = "mock"
        override fun getDeviceInfo(): DeviceInfo = DeviceInfo(0, 0, false, false)
        override suspend fun loadModel(): com.nomad.android.data.Result<Unit> = com.nomad.android.data.Result.success(Unit)
        override suspend fun unloadModel() {}
    }

    private val engine = RAGEngine(mockEngine)

    @Test
    fun `chunkText with short text returns single chunk`() {
        val text = "Hello world this is short"
        val chunks = engine.chunkText(text)
        assertEquals(1, chunks.size)
        // For short text (<=512 words), chunkText returns the ORIGINAL text unchanged
        assertEquals(text, chunks[0])
    }

    @Test
    fun `chunkText with empty string returns single empty chunk`() {
        val chunks = engine.chunkText("")
        assertEquals(1, chunks.size)
        assertEquals("", chunks[0])
    }

    @Test
    fun `chunkText short text preserves original whitespace`() {
        // chunkText returns listOf(text) for short texts — original whitespace is preserved
        val text = "Hello  world   multiple    spaces"
        val chunks = engine.chunkText(text)
        assertEquals(1, chunks.size)
        assertEquals(text, chunks[0])
    }

    @Test
    fun `chunkText respects chunk size`() {
        val words = (1..600).map { "word$it" }
        val text = words.joinToString(" ")
        val chunks = engine.chunkText(text)

        assertTrue(chunks.size > 1)
        chunks.forEach { chunk ->
            val chunkWords = chunk.split(Regex("\\s+"))
            assertTrue(
                "Chunk has ${chunkWords.size} words, expected <= ${RAGEngine.CHUNK_SIZE}",
                chunkWords.size <= RAGEngine.CHUNK_SIZE,
            )
        }
    }

    @Test
    fun `chunkText with exactly 512 words returns single chunk`() {
        val words = (1..512).map { "word$it" }
        val text = words.joinToString(" ")
        val chunks = engine.chunkText(text)
        assertEquals(1, chunks.size)
        assertEquals(text, chunks[0])
    }

    @Test
    fun `chunkText with 513 words returns multiple chunks`() {
        val words = (1..513).map { "word$it" }
        val text = words.joinToString(" ")
        val chunks = engine.chunkText(text)
        assertTrue("Expected multiple chunks for 513 words", chunks.size > 1)
    }

    @Test
    fun `CHUNK_SIZE is 512`() {
        assertEquals(512, RAGEngine.CHUNK_SIZE)
    }

    @Test
    fun `CHUNK_OVERLAP is 128`() {
        assertEquals(128, RAGEngine.CHUNK_OVERLAP)
    }

    @Test
    fun `DEFAULT_TOP_K is 5`() {
        assertEquals(5, RAGEngine.DEFAULT_TOP_K)
    }

    // --- query and querySync ---

    @Test
    fun `querySync returns AI engine response with matching documents`() = runTest {
        val result = engine.querySync(
            question = "fire safety",
            documents = listOf("Fire safety is important", "Cooking tips for chefs")
        )
        // The mock engine returns "mock"
        assertEquals("mock", result)
    }

    @Test
    fun `querySync with no matching documents still returns response`() = runTest {
        val result = engine.querySync(
            question = "xyzabc",
            documents = listOf("Fire safety is important", "Cooking tips")
        )
        assertEquals("mock", result)
    }

    @Test
    fun `query returns streaming response`() = runTest {
        val flow = engine.query(
            question = "fire",
            documents = listOf("Fire safety is important")
        )
        val results = flow.toList()
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun `query with empty documents returns response`() = runTest {
        val result = engine.querySync(
            question = "anything",
            documents = emptyList()
        )
        assertEquals("mock", result)
    }
}
