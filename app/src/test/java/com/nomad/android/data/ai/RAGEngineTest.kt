package com.nomad.android.data.ai

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
        override fun unloadModel() {}
    }

    private val engine = RAGEngine(mockEngine)

    @Test
    fun `chunkText with short text returns single chunk`() {
        val text = "Hello world this is short"
        val chunks = engine.chunkText(text)
        assertEquals(1, chunks.size)
        assertEquals(text, chunks[0])
    }

    @Test
    fun `chunkText with empty string returns single empty chunk`() {
        val chunks = engine.chunkText("")
        assertEquals(1, chunks.size)
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

    @Test
    fun `EMBEDDING_DIMENSION is 384`() {
        assertEquals(384, RAGEngine.EMBEDDING_DIMENSION)
    }

    @Test
    fun `serializeVector and deserializeVector round-trip`() {
        val original = FloatArray(384) { it.toFloat() * 0.1f }
        val serialized = engine.serializeVector(original)
        val result = engine.deserializeVector(serialized)

        assertArrayEquals(original, result, 0.001f)
    }

    @Test
    fun `cosineSimilarity with identical vectors returns 1`() {
        val vec = floatArrayOf(1f, 0f, 0f, 0f)
        val similarity = engine.cosineSimilarity(vec, vec)
        assertEquals(1.0f, similarity, 0.001f)
    }

    @Test
    fun `cosineSimilarity with orthogonal vectors returns 0`() {
        val a = floatArrayOf(1f, 0f)
        val b = floatArrayOf(0f, 1f)
        val similarity = engine.cosineSimilarity(a, b)
        assertEquals(0.0f, similarity, 0.001f)
    }

    @Test
    fun `cosineSimilarity with zero vectors returns 0`() {
        val a = floatArrayOf(0f, 0f)
        val b = floatArrayOf(1f, 1f)
        val similarity = engine.cosineSimilarity(a, b)
        assertEquals(0.0f, similarity, 0.001f)
    }

    @Test
    fun `chunkText overlaps chunks at boundaries`() {
        // Create text with exactly CHUNK_SIZE + 1 words to force 2 chunks
        val words = (1..513).map { "word$it" }
        val text = words.joinToString(" ")
        val chunks = engine.chunkText(text)

        assertEquals(2, chunks.size)

        // First chunk should have words 1-512
        val firstChunkWords = chunks[0].split(Regex("\\s+"))
        assertEquals(512, firstChunkWords.size)

        // Second chunk should start at word (512 - 128 + 1) = word385
        // because i advances by CHUNK_SIZE - CHUNK_OVERLAP = 384
        val secondChunkWords = chunks[1].split(Regex("\\s+"))
        assertTrue(secondChunkWords.size <= RAGEngine.CHUNK_SIZE)

        // Verify overlap: last 128 words of chunk 0 should be first 128 words of chunk 1
        val overlapFromFirst = firstChunkWords.takeLast(RAGEngine.CHUNK_OVERLAP)
        val overlapFromSecond = secondChunkWords.take(RAGEngine.CHUNK_OVERLAP)
        assertEquals(
            "Chunks should overlap by ${RAGEngine.CHUNK_OVERLAP} words",
            overlapFromFirst,
            overlapFromSecond
        )
    }

    @Test
    fun `chunkText produces correct number of chunks for large text`() {
        val words = (1..1500).map { "word$it" }
        val text = words.joinToString(" ")
        val chunks = engine.chunkText(text)

        // Step size = 512 - 128 = 384. Coverage: 384 * (n-1) + 512 >= 1500
        // (n-1) >= (1500 - 512) / 384 = 2.57, so n >= 4
        assertTrue(chunks.size >= 4)
        assertTrue(chunks.size <= 5)

        // All words should be covered
        val coveredWords = chunks.flatMap { it.split(Regex("\\s+")) }.toSet()
        val originalWords = words.toSet()
        assertTrue(
            "All original words should appear in chunks",
            coveredWords.containsAll(originalWords)
        )
    }

    @Test
    fun `chunkText handles text with extra whitespace`() {
        val text = "hello   world   test   data"
        val chunks = engine.chunkText(text)
        assertEquals(1, chunks.size)
        // Should normalize whitespace
        assertFalse(chunks[0].contains("  "))
    }

    @Test
    fun `chunkText with single word returns single chunk`() {
        val chunks = engine.chunkText("hello")
        assertEquals(1, chunks.size)
        assertEquals("hello", chunks[0])
    }

    @Test
    fun `cosineSimilarity with opposite vectors returns -1`() {
        val a = floatArrayOf(1f, 0f)
        val b = floatArrayOf(-1f, 0f)
        val similarity = engine.cosineSimilarity(a, b)
        assertEquals(-1.0f, similarity, 0.001f)
    }

    @Test
    fun `cosineSimilarity with parallel vectors returns 1`() {
        val a = floatArrayOf(1f, 2f, 3f)
        val b = floatArrayOf(2f, 4f, 6f) // 2x a
        val similarity = engine.cosineSimilarity(a, b)
        assertEquals(1.0f, similarity, 0.001f)
    }
}
