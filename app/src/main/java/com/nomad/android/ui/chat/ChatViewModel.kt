package com.nomad.android.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nomad.android.data.Result
import com.nomad.android.data.ai.AIEngine
import com.nomad.android.data.local.entity.ChatMessageEntity
import com.nomad.android.data.local.entity.ChatSessionEntity
import com.nomad.android.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatSession(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class ChatData(
    val currentSessionId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val sessions: List<ChatSession> = emptyList(),
    val isStreaming: Boolean = false,
    val contextFilters: List<String> = listOf("Wikipedia", "Survival", "First Aid", "All"),
    val selectedFilter: String = "All"
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

    init {
        loadRecentSessions()
    }

    fun loadRecentSessions() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            chatRepository.getRecentSessions().collect { result ->
                when (result) {
                    is Result.Success -> {
                        val sessions = result.data.map { entity ->
                            ChatSession(
                                id = entity.id,
                                title = entity.title,
                                createdAt = entity.createdAt,
                                updatedAt = entity.updatedAt
                            )
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                data = it.data.copy(sessions = sessions)
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
                        sessions = listOf(session) + it.data.sessions
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
                                    messages = messages
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
            // Set currentSessionId immediately to prevent duplicate session creation
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

        _uiState.update {
            it.copy(
                data = it.data.copy(
                    messages = it.data.messages + userMessage,
                    isStreaming = true
                )
            )
        }

        viewModelScope.launch {
            // Save user message
            chatRepository.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = "user",
                    content = content,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Get AI response
            try {
                val response = aiEngine.generate(content, emptyList())
                val assistantMessage = ChatMessage(
                    sessionId = sessionId,
                    role = "assistant",
                    content = response
                )

                // Save assistant message
                chatRepository.insertMessage(
                    ChatMessageEntity(
                        sessionId = sessionId,
                        role = "assistant",
                        content = response,
                        timestamp = System.currentTimeMillis()
                    )
                )

                _uiState.update {
                    it.copy(
                        data = it.data.copy(
                            messages = it.data.messages + assistantMessage,
                            isStreaming = false
                        )
                    )
                }
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

    fun selectFilter(filter: String) {
        _uiState.update { it.copy(data = it.data.copy(selectedFilter = filter)) }
    }
}
