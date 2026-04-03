package com.nomad.android.data.ai

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONArray

data class RAGChunk(
    val id: Long,
    val source: String,
    val title: String,
    val chunkText: String,
    val chunkIndex: Int
)

data class RAGQuery(
    val question: String,
    val contextChunks: List<RAGChunk>,
    val sources: List<String> = emptyList()
)

class RAGEngine(
    private val context: Context,
    private val aiEngine: AIEngine
) {
    companion object {
        const val CHUNK_SIZE = 512
        const val CHUNK_OVERLAP = 128
        const val DEFAULT_TOP_K = 5
        const val EMBEDDING_DIMENSION = 384
    }

    suspend fun initVectorStore(db: SQLiteDatabase) = withContext(Dispatchers.IO) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS rag_chunks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                source TEXT NOT NULL,
                title TEXT NOT NULL,
                chunk_text TEXT NOT NULL,
                chunk_index INTEGER NOT NULL,
                created_at INTEGER DEFAULT (strftime('%s','now'))
            )
        """)
        // TODO: Enable when sqlite-vec .so is loaded
        // db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS rag_embeddings USING vec0(embedding float[$EMBEDDING_DIMENSION])")
    }

    suspend fun indexContent(source: String, title: String, text: String, db: SQLiteDatabase): Int = withContext(Dispatchers.IO) {
        val chunks = chunkText(text)
        var indexed = 0
        db.beginTransaction()
        try {
            chunks.forEachIndexed { index, chunk ->
                db.execSQL(
                    "INSERT INTO rag_chunks (source, title, chunk_text, chunk_index) VALUES (?, ?, ?, ?)",
                    arrayOf(source, title, chunk, index)
                )
                // TODO: Generate embedding and insert into rag_embeddings
                // val embedding = embed(chunk)
                // db.execSQL("INSERT INTO rag_embeddings (rowid, embedding) VALUES (last_insert_rowid(), ?)", arrayOf(serializeVector(embedding)))
                indexed++
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return indexed
    }

    suspend fun query(question: String, topK: Int = DEFAULT_TOP_K, sources: List<String>? = null): Flow<String> = withContext(Dispatchers.IO) {
        val contextChunks = mockSearch(question, topK)
        val ragPrompt = buildRAGPrompt(question, contextChunks)
        return@withContext aiEngine.generateStream(ragPrompt, systemPrompt())
    }

    suspend fun querySync(question: String, topK: Int = DEFAULT_TOP_K): Result<String> {
        val contextChunks = mockSearch(question, topK)
        val ragPrompt = buildRAGPrompt(question, contextChunks)
        return aiEngine.generate(ragPrompt, systemPrompt())
    }

    private fun chunkText(text: String): List<String> {
        val words = text.split(Regex("\\s+"))
        if (words.size <= CHUNK_SIZE) return listOf(text)

        val chunks = mutableListOf<String>()
        var i = 0
        while (i < words.size) {
            val end = minOf(i + CHUNK_SIZE, words.size)
            chunks.add(words.subList(i, end).joinToString(" "))
            i += CHUNK_SIZE - CHUNK_OVERLAP
        }
        return chunks
    }

    private fun buildRAGPrompt(question: String, chunks: List<RAGChunk>): String {
        val contextBlock = chunks.joinToString("\n\n---\n\n") { chunk ->
            "[Source: ${chunk.source} | ${chunk.title}]\n${chunk.chunkText}"
        }
        return """
            CONTEXT FROM OFFLINE KNOWLEDGE BASE:
            $contextBlock

            ---
            USER QUESTION: $question

            Based on the context above, provide a clear and helpful answer. If the context doesn't contain relevant information, say so and provide general guidance.
        """.trimIndent()
    }

    private fun mockSearch(query: String, topK: Int): List<RAGChunk> {
        return (1..minOf(topK, 3)).map { i ->
            RAGChunk(
                id = i.toLong(),
                source = "local://doc$i",
                title = "Document $i",
                chunkText = "Relevant content from document $i related to: $query",
                chunkIndex = 0
            )
        }
    }

    private fun systemPrompt(): String {
        return """You are NOMAD's offline AI assistant. Answer questions using only the provided context from the local knowledge base. Be concise and accurate. If the context doesn't contain the answer, say so clearly."""
    }

    private fun serializeVector(vector: FloatArray): ByteArray {
        val buffer = java.nio.ByteBuffer.allocate(vector.size * 4)
        buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN)
        vector.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    private fun deserializeVector(bytes: ByteArray): FloatArray {
        val buffer = java.nio.ByteBuffer.wrap(bytes)
        buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN)
        val vector = FloatArray(bytes.size / 4)
        for (i in vector.indices) {
            vector[i] = buffer.getFloat()
        }
        return vector
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        return if (normA == 0f || normB == 0f) 0f else dotProduct / (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB))
    }
}
