package com.nomad.android.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nomad.android.data.Result
import com.nomad.android.data.ai.AIEngine
import com.nomad.android.data.local.entity.ChatMessageEntity
import com.nomad.android.data.local.entity.ChatSessionEntity
import com.nomad.android.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatMessage(
    val id: Long = 0,
    val sessionId: String,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatSession(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)

enum class ThinkingPower(val label: String, val maxTokens: Int, val topK: Int) {
    LOW("Low", 64, 5),
    MEDIUM("Med", 256, 10),
    HIGH("High", 512, 10)
}

data class ChatData(
    val currentSessionId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val sessions: List<ChatSession> = emptyList(),
    val isStreaming: Boolean = false,
    val contextFilters: List<String> = listOf("Wikipedia", "Survival", "First Aid", "All"),
    val selectedFilter: String = "All",
    val thinkingPower: ThinkingPower = ThinkingPower.LOW,
    val contextTokenCount: Int = 0
)

data class ChatUiState(
    val data: ChatData = ChatData(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val aiEngine: AIEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(isLoading = true))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    private var streamingJob: Job? = null

    init {
        loadRecentSessions()
    }

    fun loadRecentSessions() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
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
            )
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

        viewModelScope.launch {
            chatRepository.getMessagesBySession(sessionId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val messages = result.data.map { entity ->
                            ChatMessage(
                                id = entity.id,
                                sessionId = entity.sessionId,
                                role = entity.role,
                                content = entity.content,
                                timestamp = entity.timestamp
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

    fun sendMessage(content: String) {
        if (_uiState.value.data.isStreaming) return

        val sessionId = _uiState.value.data.currentSessionId
        if (sessionId != null) {
            sendUserMessage(sessionId, content)
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
                )
                sendUserMessage(newId, content)
            }
        }
    }

    private fun sendUserMessage(sessionId: String, content: String) {
        val userMessage = ChatMessage(
            sessionId = sessionId,
            role = "user",
            content = content
        )

        val streamingMessage = ChatMessage(
            sessionId = sessionId,
            role = "assistant",
            content = ""
        )

        _uiState.update {
            it.copy(
                data = it.data.copy(
                    messages = it.data.messages + userMessage + streamingMessage,
                    isStreaming = true
                )
            )
        }

        streamingJob = viewModelScope.launch {
            chatRepository.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = "user",
                    content = content,
                    timestamp = System.currentTimeMillis()
                )
            )

            val responseBuilder = StringBuilder()
            try {
                aiEngine.generateStream(content, buildContext()).collect { token ->
                    responseBuilder.append(token)
                    val currentResponse = responseBuilder.toString()
                    _uiState.update { state ->
                        val messages = state.data.messages.toMutableList()
                        messages[messages.lastIndex] = streamingMessage.copy(content = currentResponse)
                        state.copy(data = state.data.copy(messages = messages))
                    }
                }

                val finalResponse = responseBuilder.toString().trim()

                // Update the final message
                _uiState.update { state ->
                    val messages = state.data.messages.toMutableList()
                    messages[messages.lastIndex] = streamingMessage.copy(content = finalResponse)
                    state.copy(
                        data = state.data.copy(
                            messages = messages,
                            isStreaming = false,
                            contextTokenCount = estimateTokenCount(messages)
                        )
                    )
                }

                chatRepository.insertMessage(
                    ChatMessageEntity(
                        sessionId = sessionId,
                        role = "assistant",
                        content = finalResponse,
                        timestamp = System.currentTimeMillis()
                    )
                )

                autoCompactIfNeeded()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "AI Engine error: ${e.message}",
                        data = it.data.copy(isStreaming = false)
                    )
                }
            }
        }
    }

    fun setThinkingPower(power: ThinkingPower) {
        _uiState.update { it.copy(data = it.data.copy(thinkingPower = power)) }
    }

    fun compactContext() {
        val messages = _uiState.value.data.messages
        if (messages.size <= 4) return

        // Keep first 2 messages (initial context) and last 4 (recent conversation)
        val compacted = messages.take(2) + ChatMessage(
            sessionId = messages.first().sessionId,
            role = "assistant",
            content = "[${messages.size - 6} earlier messages compacted]",
            timestamp = messages[messages.size / 2].timestamp
        ) + messages.takeLast(4)

        _uiState.update {
            it.copy(
                data = it.data.copy(
                    messages = compacted,
                    contextTokenCount = estimateTokenCount(compacted)
                )
            )
        }
    }

    private fun buildContext(): List<String> {
        val messages = _uiState.value.data.messages
        val power = _uiState.value.data.thinkingPower
        val maxContextTokens = when (power) {
            ThinkingPower.LOW -> 8_000
            ThinkingPower.MEDIUM -> 32_000
            ThinkingPower.HIGH -> 100_000
        }

        // Walk backwards from recent messages, accumulating tokens until budget is hit
        val history = messages.dropLast(2) // exclude current user msg + empty assistant placeholder
        val selected = mutableListOf<String>()
        var tokenBudget = maxContextTokens

        for (msg in history.reversed()) {
            val line = "${msg.role}: ${msg.content}"
            val tokens = estimateTokenCount(line)
            if (tokenBudget - tokens < 0) break
            tokenBudget -= tokens
            selected.add(0, line)
        }
        return selected
    }

    private fun estimateTokenCount(messages: List<ChatMessage>): Int {
        return messages.sumOf { estimateTokenCount(it.content) }
    }

    private fun estimateTokenCount(text: String): Int {
        // Gemma tokenizer averages ~3.5 chars per token for English
        return (text.length / 3.5).toInt().coerceAtLeast(1)
    }

    private fun autoCompactIfNeeded() {
        val tokens = _uiState.value.data.contextTokenCount
        if (tokens > AUTO_COMPACT_THRESHOLD) {
            compactContext()
        }
    }

    companion object {
        private const val AUTO_COMPACT_THRESHOLD = 100_000
    }

    fun selectFilter(filter: String) {
        _uiState.update { it.copy(data = it.data.copy(selectedFilter = filter)) }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatRepository.deleteSessionById(sessionId)
            loadRecentSessions()
            // If currently viewing this session, clear it
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
}
