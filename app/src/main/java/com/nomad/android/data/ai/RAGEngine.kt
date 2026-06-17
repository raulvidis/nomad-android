package com.nomad.android.data.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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
    private val aiEngine: AIEngine,
    private val knowledgeBase: KnowledgeBase? = null,
) {
    companion object {
        const val CHUNK_SIZE = 512
        const val CHUNK_OVERLAP = 128
        const val DEFAULT_TOP_K = 5
        private val STOPWORDS = setOf(
            "the", "and", "for", "are", "but", "not", "you", "all", "can", "her",
            "was", "one", "our", "out", "has", "had", "from", "this", "that", "with",
            "they", "will", "would", "there", "their", "what", "about", "which",
            "when", "who", "how", "why", "into", "your", "its", "over", "than",
            "them", "were", "been", "more", "some", "such", "also", "any", "just"
        )
    }

    fun chunkText(text: String): List<String> {
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

    fun query(question: String, topK: Int = DEFAULT_TOP_K, documents: List<String>): Flow<String> = flow {
        val contextChunks = search(question, topK, documents)
        val ragPrompt = buildRAGPrompt(question, contextChunks)
        aiEngine.generateStream(ragPrompt, emptyList()).collect { emit(it) }
    }

    suspend fun querySync(question: String, topK: Int = DEFAULT_TOP_K, documents: List<String>): String {
        val contextChunks = search(question, topK, documents)
        val ragPrompt = buildRAGPrompt(question, contextChunks)
        return aiEngine.generate(ragPrompt, emptyList())
    }

    /**
     * Normalize text into a set of searchable tokens: lowercase, strip
     * non-alphanumeric characters, then drop stopwords and tokens of length <= 2.
     * Mirrors the length>2 rule used in ChatViewModel so common words ("the",
     * "how", "what") and trailing punctuation ("water?") don't inflate overlap
     * scores and degrade retrieval relevance.
     */
    private fun tokenize(text: String): Set<String> {
        return text.lowercase().split(Regex("\\s+"))
            .map { it.replace(Regex("[^a-z0-9]"), "") }
            .filter { it.length > 2 && it !in STOPWORDS }
            .toSet()
    }

    private fun search(query: String, topK: Int, documents: List<String>): List<RAGChunk> {
        // When no raw documents are supplied but a KnowledgeBase is attached, defer
        // retrieval to it so RAG queries run over the canonical bundled knowledge.
        if (documents.isEmpty() && knowledgeBase != null) {
            return knowledgeBase.search(query, topK).mapIndexed { index, entry ->
                RAGChunk(
                    id = index.toLong(),
                    source = entry.source.ifBlank { "local://knowledge" },
                    title = entry.title,
                    chunkText = entry.content,
                    chunkIndex = index,
                )
            }
        }

        val queryWords = tokenize(query)
        val scored = documents.mapIndexed { index, doc ->
            val docWords = tokenize(doc)
            val overlap = queryWords.intersect(docWords).size
            index to overlap
        }.filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(topK)

        return scored.map { (index, _) ->
            RAGChunk(
                id = index.toLong(),
                source = "local://doc$index",
                title = "Document $index",
                chunkText = documents[index],
                chunkIndex = 0
            )
        }
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

            Based on the context above, provide a clear and helpful answer. If the context doesn't contain the answer, say so clearly.
        """.trimIndent()
    }

}
