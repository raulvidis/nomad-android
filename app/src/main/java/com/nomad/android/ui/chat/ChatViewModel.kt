package com.nomad.android.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nomad.android.data.Result
import com.nomad.android.data.ai.AIEngine
import com.nomad.android.data.ai.KnowledgeBase
import com.nomad.android.data.local.entity.ChatMessageEntity
import com.nomad.android.data.local.entity.ChatSessionEntity
import com.nomad.android.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import android.util.Log
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatMessage(
    val id: Long = 0,
    val sessionId: String,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null
)

data class QueuedMessage(
    val content: String,
    val imagePath: String? = null
)

data class ChatSession(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)

enum class ThinkingPower(val label: String, val maxTokens: Int, val topK: Int) {
    LOW("Fast", 256, 10),
    MEDIUM("Balanced", 512, 20),
    HIGH("Deep", 1024, 40)
}

data class ChatData(
    val currentSessionId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val sessions: List<ChatSession> = emptyList(),
    val isStreaming: Boolean = false,
    val contextFilters: List<String> = listOf("All", "Survival", "First Aid", "Wikipedia"),
    val selectedFilter: String = "All",
    val thinkingPower: ThinkingPower = ThinkingPower.LOW,
    val contextTokenCount: Int = 0,
    val messageQueue: List<QueuedMessage> = emptyList(),
    val pendingImagePath: String? = null
)

data class ChatUiState(
    val data: ChatData = ChatData(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val aiEngine: AIEngine,
    private val knowledgeBase: KnowledgeBase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatUiState(
            isLoading = true,
            data = ChatData(
                contextFilters = (knowledgeBase.categories + listOf("Wikipedia")).distinct(),
            ),
        ),
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    private var streamingJob: Job? = null
    private var sessionsCollectionJob: Job? = null
    private var messagesCollectionJob: Job? = null
    private var streamingMessageId: Long? = null

    init {
        loadRecentSessions()
    }

    fun loadRecentSessions() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        sessionsCollectionJob?.cancel()
        sessionsCollectionJob = viewModelScope.launch {
            chatRepository.getRecentSessions().collect { result ->
                val sessions = when (result) {
                    is Result.Success -> result.data.map { entity ->
                        ChatSession(
                            id = entity.id,
                            title = entity.title,
                            createdAt = entity.createdAt,
                            updatedAt = entity.updatedAt
                        )
                    }
                    is Result.Error -> emptyList()
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        data = it.data.copy(sessions = sessions)
                    )
                }
            }
        }
    }

    fun newSession() {
        streamingJob?.cancel()
        streamingJob = null
        messagesCollectionJob?.cancel()
        messagesCollectionJob = null
        val sessionId = UUID.randomUUID().toString()
        val session = ChatSession(
            id = sessionId,
            title = "New Session",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            chatRepository.insertSession(
                ChatSessionEntity(
                    id = session.id,
                    title = session.title,
                    createdAt = session.createdAt,
                    updatedAt = session.updatedAt
                )
            ).let { result ->
                if (result is Result.Error) {
                    Log.e("ChatViewModel", "Failed to insert session: ${result.message}")
                }
            }
            _uiState.update {
                it.copy(
                    data = it.data.copy(
                        currentSessionId = sessionId,
                        messages = emptyList(),
                        sessions = listOf(session) + it.data.sessions,
                        contextTokenCount = 0
                    )
                )
            }
        }
    }

    fun loadSession(sessionId: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        messagesCollectionJob?.cancel()
        messagesCollectionJob = viewModelScope.launch {
            chatRepository.getMessagesBySession(sessionId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val messages = result.data.map { entity ->
                            ChatMessage(
                                id = entity.id,
                                sessionId = entity.sessionId,
                                role = entity.role,
                                content = entity.content,
                                timestamp = entity.timestamp,
                                imageUri = entity.imageUri
                            )
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                data = it.data.copy(
                                    currentSessionId = sessionId,
                                    messages = messages,
                                    contextTokenCount = estimateTokenCount(messages)
                                )
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, error = result.message)
                        }
                    }
                }
            }
        }
    }

    fun sendMessage(content: String, imagePath: String? = null) {
        if (content.length > 10000) {
            _uiState.update { it.copy(error = "Message too long (${content.length} chars, max 10000)") }
            return
        }
        val state = _uiState.value
        val image = imagePath ?: state.data.pendingImagePath

        if (state.data.isStreaming) {
            _uiState.update {
                it.copy(
                    data = it.data.copy(
                        messageQueue = it.data.messageQueue + QueuedMessage(content, image)
                    )
                )
            }
            return
        }

        val sessionId = state.data.currentSessionId
        if (sessionId != null) {
            _uiState.update { it.copy(data = it.data.copy(pendingImagePath = null)) }
            sendUserMessage(sessionId, content, image)
        } else {
            val newId = UUID.randomUUID().toString()
            val session = ChatSession(
                id = newId,
                title = content.take(50),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            _uiState.update {
                it.copy(
                    data = it.data.copy(
                        pendingImagePath = null,
                        currentSessionId = newId,
                        sessions = listOf(session) + it.data.sessions
                    )
                )
            }
            viewModelScope.launch {
                chatRepository.insertSession(
                    ChatSessionEntity(
                        id = session.id,
                        title = session.title,
                        createdAt = session.createdAt,
                        updatedAt = session.updatedAt
                    )
                ).let { result ->
                    if (result is Result.Error) {
                        Log.e("ChatViewModel", "Failed to insert session: ${result.message}")
                    }
                }
                sendUserMessage(newId, content, image)
            }
        }
    }

    fun setPendingImage(path: String?) {
        _uiState.update { it.copy(data = it.data.copy(pendingImagePath = path)) }
    }

    fun removeFromQueue(index: Int) {
        _uiState.update {
            if (index < 0 || index >= it.data.messageQueue.size) return@update it
            it.copy(
                data = it.data.copy(
                    messageQueue = it.data.messageQueue.toMutableList().apply { removeAt(index) }
                )
            )
        }
    }

    private fun sendUserMessage(sessionId: String, content: String, imagePath: String? = null) {
        // Cancel the DB message collector so the streaming placeholder (id < 0) isn't
        // clobbered when the user-message insert re-emits Room rows. The collector
        // is re-established by loadSession() when the chat is re-entered.
        messagesCollectionJob?.cancel()
        messagesCollectionJob = null

        val userMessage = ChatMessage(
            sessionId = sessionId,
            role = "user",
            content = content,
            imageUri = imagePath
        )

        val streamId = -System.nanoTime()
        val streamingMessage = ChatMessage(
            id = streamId,
            sessionId = sessionId,
            role = "assistant",
            content = ""
        )
        streamingMessageId = streamId

        _uiState.update {
            it.copy(
                data = it.data.copy(
                    messages = it.data.messages + userMessage + streamingMessage,
                    isStreaming = true
                )
            )
        }

        streamingJob = viewModelScope.launch {
            val currentSessionId = sessionId
            val currentStreamingMessage = streamingMessage

            val currentSession = _uiState.value.data.sessions.find { it.id == sessionId }
            if (currentSession != null && currentSession.title == "New Session") {
                val title = content.take(50)
                val updatedSession = currentSession.copy(title = title)
                chatRepository.updateSession(
                    ChatSessionEntity(
                        id = updatedSession.id,
                        title = updatedSession.title,
                        createdAt = currentSession.createdAt,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                _uiState.update { state ->
                    state.copy(
                        data = state.data.copy(
                            sessions = state.data.sessions.map { s ->
                                if (s.id == sessionId) updatedSession else s
                            }
                        )
                    )
                }
            }

            chatRepository.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = "user",
                    content = content,
                    timestamp = System.currentTimeMillis(),
                    imageUri = imagePath
                )
            ).let { result ->
                if (result is Result.Error) {
                    Log.e("ChatViewModel", "Failed to insert message: ${result.message}")
                }
            }

            val responseBuilder = StringBuilder()
            try {
                val conversationContext = buildContext()
                val knowledgeContext = knowledgeBase.retrieveContext(
                    content,
                    categoryFilter = _uiState.value.data.selectedFilter,
                )
                val fullContext = if (knowledgeContext.isNotEmpty()) {
                    listOf(knowledgeContext) + conversationContext
                } else {
                    conversationContext
                }

                aiEngine.generateStream(content, fullContext, imagePath).collect { token ->
                    if (!isActive) return@collect
                    responseBuilder.append(token)
                    val currentResponse = responseBuilder.toString()
                    _uiState.update { state ->
                        val messages = state.data.messages.toMutableList()
                        val idx = messages.indexOfFirst { it.id == streamingMessageId }
                        if (idx >= 0) messages[idx] = currentStreamingMessage.copy(content = currentResponse)
                        state.copy(data = state.data.copy(messages = messages))
                    }
                }

                val finalResponse = responseBuilder.toString()
                    .replace(Regex("""\n{3,}"""), "\n\n")
                    .trim()

                _uiState.update { state ->
                    val messages = state.data.messages.toMutableList()
                    val idx = messages.indexOfFirst { it.id == streamingMessageId }
                    if (idx >= 0) messages[idx] = currentStreamingMessage.copy(content = finalResponse)
                    state.copy(
                        data = state.data.copy(
                            messages = messages,
                            isStreaming = false,
                            contextTokenCount = estimateTokenCount(messages)
                        )
                    )
                }
                streamingMessageId = null

                chatRepository.insertMessage(
                    ChatMessageEntity(
                        sessionId = sessionId,
                        role = "assistant",
                        content = finalResponse,
                        timestamp = System.currentTimeMillis()
                    )
                ).let { result ->
                    if (result is Result.Error) {
                        Log.e("ChatViewModel", "Failed to insert message: ${result.message}")
                    }
                }

                autoCompactIfNeeded()
            } catch (e: Throwable) {
                val partialResponse = responseBuilder.toString()
                val finalPartial = if (partialResponse.isNotBlank()) partialResponse else "Error: ${e.message}"
                _uiState.update {
                    val messages = it.data.messages.toMutableList()
                    val idx = messages.indexOfFirst { msg -> msg.id == streamingMessageId }
                    if (idx >= 0) messages[idx] = currentStreamingMessage.copy(content = finalPartial)
                    it.copy(
                        error = "AI Engine error: ${e.message}",
                        data = it.data.copy(
                            isStreaming = false,
                            messages = messages
                        )
                    )
                }
                streamingMessageId = null
                return@launch
            }

            processQueue(currentSessionId)
        }
    }

    private fun processQueue(sessionId: String) {
        val queue = _uiState.value.data.messageQueue
        if (queue.isEmpty()) return

        val next = queue.first()
        _uiState.update {
            it.copy(data = it.data.copy(messageQueue = it.data.messageQueue.drop(1)))
        }
        sendUserMessage(sessionId, next.content, next.imagePath)
    }

    fun setThinkingPower(power: ThinkingPower) {
        _uiState.update { it.copy(data = it.data.copy(thinkingPower = power)) }
    }

    fun compactContext() {
        val messages = _uiState.value.data.messages
        if (messages.size <= 4) return
        val sessionId = _uiState.value.data.currentSessionId

        val compacted = messages.take(1) + ChatMessage(
            sessionId = messages.first().sessionId,
            role = "assistant",
            content = "[${messages.size - 4} earlier messages compacted]",
            timestamp = messages[messages.size / 2].timestamp
        ) + messages.takeLast(3)

        viewModelScope.launch {
            // Persist to DB first — mirrors autoCompactIfNeeded() pattern
            if (sessionId != null) {
                val result = chatRepository.replaceMessagesForSession(
                    sessionId,
                    compacted.map { msg ->
                        ChatMessageEntity(
                            sessionId = msg.sessionId,
                            role = msg.role,
                            content = msg.content,
                            timestamp = msg.timestamp,
                            imageUri = msg.imageUri
                        )
                    }
                )
                if (result is Result.Error) {
                    Log.e("ChatViewModel", "compactContext: DB write failed, skipping UI update — ${result.message}")
                    return@launch
                }
            }

            // Update UI only after DB write succeeds
            _uiState.update {
                it.copy(
                    data = it.data.copy(
                        messages = compacted,
                        contextTokenCount = estimateTokenCount(compacted)
                    )
                )
            }
        }
    }

    /**
     * Selects messages that fit within the effective token budget
     * (after reserving KB space). Returns lines in chronological order.
     */
    private fun selectMessagesInBudget(messages: List<ChatMessage>): List<String> {
        val selected = mutableListOf<String>()
        var tokenBudget = (MAX_CONTEXT_TOKENS * (1 - KNOWLEDGE_BUDGET_FRACTION)).toInt()
        val history = messages.dropLast(2) // exclude current user msg + empty assistant placeholder
        for (msg in history.reversed()) {
            val line = "${msg.role}: ${msg.content}"
            val tokens = estimateTokenCount(line)
            if (tokenBudget - tokens < 0) break
            tokenBudget -= tokens
            selected.add(0, line)
        }
        return selected
    }

    private fun buildContext(): List<String> {
        return selectMessagesInBudget(_uiState.value.data.messages)
    }

    private fun estimateTokenCount(messages: List<ChatMessage>): Int {
        val selected = selectMessagesInBudget(messages)
        var total = SYSTEM_PROMPT_TOKENS + selected.sumOf { estimateTokenCount(it) }
        val lastUserMsg = messages.lastOrNull { it.role == "user" }
        if (lastUserMsg != null) {
            total += estimateTokenCount(lastUserMsg.content)
        }
        return total
    }

    private fun estimateTokenCount(text: String): Int {
        // MiniCPM5-1B tokenizer averages ~4 chars per token for English
        return (text.length / 4.0).toInt().coerceAtLeast(1)
    }

    private suspend fun autoCompactIfNeeded() {
        val tokens = _uiState.value.data.contextTokenCount
        if (tokens > AUTO_COMPACT_THRESHOLD) {
            val messages = _uiState.value.data.messages
            if (messages.size <= 4) return
            val sessionId = _uiState.value.data.currentSessionId

            val compacted = messages.take(1) + ChatMessage(
                sessionId = messages.first().sessionId,
                role = "assistant",
                content = "[${messages.size - 4} earlier messages compacted]",
                timestamp = messages[messages.size / 2].timestamp
            ) + messages.takeLast(3)

            // Write to DB first — if this fails, leave UI consistent with DB
            if (sessionId != null) {
                val result = chatRepository.replaceMessagesForSession(
                    sessionId,
                    compacted.map { msg ->
                        ChatMessageEntity(
                            sessionId = msg.sessionId,
                            role = msg.role,
                            content = msg.content,
                            timestamp = msg.timestamp,
                            imageUri = msg.imageUri
                        )
                    }
                )
                if (result is Result.Error) {
                    Log.e("ChatViewModel", "autoCompact: DB write failed, skipping UI update — ${result.message}")
                    return
                }
            }

            // Update UI only after DB write succeeds
            _uiState.update {
                it.copy(
                    data = it.data.copy(
                        messages = compacted,
                        contextTokenCount = estimateTokenCount(compacted)
                    )
                )
            }
        }
    }

    fun deleteAllSessions() {
        streamingJob?.cancel()
        streamingJob = null
        streamingMessageId = null
        viewModelScope.launch {
            chatRepository.deleteAllSessions()
            _uiState.update {
                it.copy(
                    data = it.data.copy(
                        currentSessionId = null,
                        messages = emptyList(),
                        sessions = emptyList(),
                        contextTokenCount = 0
                    )
                )
            }
        }
    }

    companion object {
        // MiniCPM5-1B: 4096 context (LlamaCppEngine.DEFAULT_CTX), reserve ~512 for generation
        private const val MAX_CONTEXT_TOKENS = 3_500
        private const val AUTO_COMPACT_THRESHOLD = 3_000
        // Approximate tokens for the system prompt + turn formatting overhead
        private const val SYSTEM_PROMPT_TOKENS = 80
        private const val KNOWLEDGE_BUDGET_FRACTION = 0.15
    }

    fun selectFilter(filter: String) {
        _uiState.update { it.copy(data = it.data.copy(selectedFilter = filter)) }
    }

    fun deleteSession(sessionId: String) {
        streamingJob?.cancel()
        streamingJob = null
        streamingMessageId = null
        viewModelScope.launch {
            chatRepository.deleteSessionById(sessionId)
            loadRecentSessions()
            if (_uiState.value.data.currentSessionId == sessionId) {
                _uiState.update {
                    it.copy(
                        data = it.data.copy(
                            currentSessionId = null,
                            messages = emptyList(),
                            contextTokenCount = 0
                        )
                    )
                }
            }
        }
    }

    fun resetStuckState() {
        streamingJob?.cancel()
        streamingJob = null
        streamingMessageId = null
        _uiState.update {
            it.copy(
                data = it.data.copy(isStreaming = false),
                error = null
            )
        }
    }
}
