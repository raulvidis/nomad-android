package com.nomad.android.data.ai

import org.junit.Assert.*
import org.junit.Test

class RAGEngineTest {

    private val mockEngine = object : AIEngine {
        override suspend fun generate(prompt: String, context: List<String>): String = "mock"
        override fun generateStream(prompt: String, context: List<String>): kotlinx.coroutines.flow.Flow<String> =
            kotlinx.coroutines.flow.flow { emit("mock") }
        override suspend fun isAvailable(): Boolean = true
        override fun getModelName(): String = "mock"
        override fun getDeviceInfo(): DeviceInfo = DeviceInfo(0, 0, false, false)
        override suspend fun loadModel(): Result<Unit> = Result.success(Unit)
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
                chunkWords.size <= RAGEngine.CHUNK_SIZE + RAGEngine.CHUNK_OVERLAP,
            )
        }
    }

    @Test
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
}
