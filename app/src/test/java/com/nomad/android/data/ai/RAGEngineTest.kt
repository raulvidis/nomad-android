package com.nomad.android.data.ai

import org.junit.Assert.*
import org.junit.Test

class RAGEngineTest {

    // Test chunkText via reflection since it's private
    private val engineClass = RAGEngine::class.java

    @Test
    fun `chunkText with short text returns single chunk`() {
        val text = "Hello world this is short"
        val chunks = invokeChunkText(text)
        assertEquals(1, chunks.size)
        assertEquals(text, chunks[0])
    }

    @Test
    fun `chunkText with empty string returns single empty chunk`() {
        val chunks = invokeChunkText("")
        assertEquals(1, chunks.size)
    }

    @Test
    fun `chunkText respects chunk size`() {
        val words = (1..600).map { "word$it" }
        val text = words.joinToString(" ")
        val chunks = invokeChunkText(text)

        assertTrue(chunks.size > 1, "Expected multiple chunks for 600 words")
        chunks.forEach { chunk ->
            val chunkWords = chunk.split(Regex("\\s+"))
            assertTrue(
                chunkWords.size <= RAGEngine.CHUNK_SIZE + RAGEngine.CHUNK_OVERLAP,
                "Chunk too large: ${chunkWords.size} words"
            )
        }
    }

    @Test
    fun `chunkText has overlap between consecutive chunks`() {
        val words = (1..700).map { "word$it" }
        val text = words.joinToString(" ")
        val chunks = invokeChunkText(text)

        if (chunks.size >= 2) {
            val lastWordsOfFirst = chunks[0].split(Regex("\\s+")).takeLast(10).toSet()
            val firstWordsOfSecond = chunks[1].split(Regex("\\s+")).take(50).toSet()
            val overlap = lastWordsOfFirst.intersect(firstWordsOfSecond)
            assertTrue(overlap.isNotEmpty(), "Expected overlap between consecutive chunks")
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
        val engine = createEngineForTesting()
        val method = engineClass.getDeclaredMethod("serializeVector", FloatArray::class.java)
        method.isAccessible = true
        val original = FloatArray(384) { it.toFloat() * 0.1f }

        val serialized = method.invoke(engine, original) as ByteArray
        val deserializedMethod = engineClass.getDeclaredMethod("deserializeVector", ByteArray::class.java)
        deserializedMethod.isAccessible = true
        val result = deserializedMethod.invoke(engine, serialized) as FloatArray

        assertArrayEquals(original, result, 0.001f)
    }

    @Test
    fun `cosineSimilarity with identical vectors returns 1`() {
        val engine = createEngineForTesting()
        val method = engineClass.getDeclaredMethod("cosineSimilarity", FloatArray::class.java, FloatArray::class.java)
        method.isAccessible = true
        val vec = floatArrayOf(1f, 0f, 0f, 0f)

        val similarity = method.invoke(engine, vec, vec) as Float
        assertEquals(1.0f, similarity, 0.001f)
    }

    @Test
    fun `cosineSimilarity with orthogonal vectors returns 0`() {
        val engine = createEngineForTesting()
        val method = engineClass.getDeclaredMethod("cosineSimilarity", FloatArray::class.java, FloatArray::class.java)
        method.isAccessible = true
        val a = floatArrayOf(1f, 0f)
        val b = floatArrayOf(0f, 1f)

        val similarity = method.invoke(engine, a, b) as Float
        assertEquals(0.0f, similarity, 0.001f)
    }

    @Test
    fun `cosineSimilarity with zero vectors returns 0`() {
        val engine = createEngineForTesting()
        val method = engineClass.getDeclaredMethod("cosineSimilarity", FloatArray::class.java, FloatArray::class.java)
        method.isAccessible = true
        val a = floatArrayOf(0f, 0f)
        val b = floatArrayOf(1f, 1f)

        val similarity = method.invoke(engine, a, b) as Float
        assertEquals(0.0f, similarity, 0.001f)
    }

    private fun invokeChunkText(text: String): List<String> {
        val engine = createEngineForTesting()
        val method = engineClass.getDeclaredMethod("chunkText", String::class.java)
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(engine, text) as List<String>
    }

    private fun createEngineForTesting(): Any {
        // RAGEngine constructor needs Context and AIEngine, but we only test private methods
        // Use reflection to create a minimal instance
        val constructor = engineClass.getDeclaredConstructor(
            android.content.Context::class.java,
            com.nomad.android.data.ai.AIEngine::class.java
        )
        constructor.isAccessible = true
        return constructor.newInstance(null, null)
    }
}
