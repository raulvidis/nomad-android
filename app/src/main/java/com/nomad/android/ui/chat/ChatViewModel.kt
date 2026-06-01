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
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(isLoading = true))
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
                val knowledgeContext = retrieveKnowledgeContext(content)
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

        val compacted = messages.take(1) + ChatMessage(
            sessionId = messages.first().sessionId,
            role = "assistant",
            content = "[${messages.size - 4} earlier messages compacted]",
            timestamp = messages[messages.size / 2].timestamp
        ) + messages.takeLast(3)

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
        val data = _uiState.value.data
        val messages = data.messages
        val maxContextTokens = MAX_CONTEXT_TOKENS

        val selected = mutableListOf<String>()
        var tokenBudget = maxContextTokens

        // Reserve budget for knowledge base context
        val knowledgeBudget = (maxContextTokens * 0.15).toInt() // 15% for KB
        tokenBudget -= knowledgeBudget

        // Walk backwards from recent messages, accumulating tokens until budget is hit
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

    private fun retrieveKnowledgeContext(query: String): String {
        val filter = _uiState.value.data.selectedFilter
        val knowledgeEntries = KNOWLEDGE_BASE.filter { (category, _, _) ->
            filter == "All" || category.equals(filter, ignoreCase = true)
        }

        val queryWords = query.lowercase().split(Regex("\\s+")).filter { it.length > 2 }.toSet()
        val relevant = knowledgeEntries
            .map { (_, title, content) ->
                val text = "$title $content".lowercase()
                val score = queryWords.count { text.contains(it) }
                Triple(title, content, score)
            }
            .filter { it.third > 0 }
            .sortedByDescending { it.third }
            .take(3)

        if (relevant.isEmpty()) return ""

        return buildString {
            appendLine("Relevant knowledge base entries:")
            relevant.forEach { (title, content, _) ->
                appendLine("- $title: $content")
            }
        }
    }

    private fun estimateTokenCount(messages: List<ChatMessage>): Int {
        var total = SYSTEM_PROMPT_TOKENS
        val maxContextTokens = MAX_CONTEXT_TOKENS
        val knowledgeBudget = (maxContextTokens * 0.15).toInt()
        var tokenBudget = maxContextTokens - knowledgeBudget
        val history = messages.dropLast(2)
        for (msg in history.reversed()) {
            val line = "${msg.role}: ${msg.content}"
            val tokens = (line.length / 4.0).toInt().coerceAtLeast(1)
            if (tokenBudget - tokens < 0) break
            tokenBudget -= tokens
            total += tokens
        }
        val lastUserMsg = messages.lastOrNull { it.role == "user" }
        if (lastUserMsg != null) {
            total += (lastUserMsg.content.length / 4.0).toInt().coerceAtLeast(1)
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

            // Write to DB first — if this fails, UI stays consistent with DB
            if (sessionId != null) {
                chatRepository.replaceMessagesForSession(
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

        // Built-in knowledge base entries: (category, title, content)
        private val KNOWLEDGE_BASE = listOf(
            Triple("Survival", "CPR Basics", "To perform CPR: Check responsiveness, call emergency services, push hard and fast in the center of the chest at 100-120 compressions per minute, give rescue breaths if trained. Continue until help arrives or an AED is available."),
            Triple("Survival", "Water Purification", "Boil water for at least 1 minute (3 minutes above 6,500 ft). Use purification tablets or chlorine dioxide drops. Solar disinfection: clear bottle in direct sunlight for 6 hours. Filter through cloth first to remove sediment."),
            Triple("Survival", "Fire Starting", "Gather tinder (dry leaves, bark), kindling (small sticks), and fuel (logs). Create a teepee or log cabin fire lay. Use matches, lighter, ferro rod, or bow-drill friction method. Shield from wind. Never leave unattended."),
            Triple("Survival", "Shelter Building", "Find natural windbreaks (rock faces, fallen trees). Build lean-to with branches at 45 degrees. Insulate ground with leaves/pine needles. Keep shelter small to retain body heat. Ensure ventilation if using fire nearby."),
            Triple("Survival", "Navigation Without Tools", "Sun rises in east, sets in west. North Star (Polaris) indicates north in the northern hemisphere. Moss often grows thicker on north side of trees. Follow waterways downstream toward civilization. Leave trail markers."),
            Triple("Survival", "Edible Plants", "Only eat plants you can positively identify. Safe: dandelion (all parts), clover, cattail (roots/shoots), pine needles (tea), chickweed. Avoid: milky sap, umbrella-shaped flowers, almond scent, three-leaved growth, white/yellow berries."),
            Triple("Survival", "SOS Signals", "Three of anything: fires, whistle blasts, mirror flashes. Ground-to-air: use contrasting materials, minimum 10ft letters. Mirror signal toward aircraft. At night, use bright fires. Universal distress: SOS (... --- ...) in Morse code."),
            Triple("First Aid", "Bleeding Control", "Apply direct pressure with clean cloth. Elevate wound above heart. Apply pressure bandage. For severe limb bleeding, apply tourniquet 2-3 inches above wound. Do not remove soaked cloths—add more on top."),
            Triple("First Aid", "Burns Treatment", "Cool burn immediately with cool (not cold) running water for 10-20 minutes. Cover with sterile non-stick dressing. Do not apply ice, butter, or toothpaste. Seek medical help for burns larger than palm size or on face/joints/genitals."),
            Triple("First Aid", "Shock Treatment", "Lay person down with legs elevated 12 inches. Keep warm with blankets. Do not give food or water. Monitor breathing. If unconscious but breathing, place in recovery position. Call emergency services immediately."),
            Triple("First Aid", "Fracture Splinting", "Immobilize the joint above and below the fracture. Use rigid materials (sticks, boards) padded with cloth. Secure with bandages but don't restrict circulation. Check pulse below splint regularly."),
            Triple("First Aid", "Hypothermia", "Move to shelter. Remove wet clothing. Warm gradually with blankets, skin-to-skin contact. Give warm sweet drinks if conscious. Do not rub limbs, apply direct heat, or give alcohol. Severe cases: call emergency services."),
            Triple("Wikipedia", "Morse Code", "International communication system using dots and dashes. SOS = ... --- ... Key letters: E=. T=- A=.- I=.. M=-- N=-. Useful for emergency signaling with light, sound, or tapping."),
            Triple("Wikipedia", "Cardinal Directions", "North, South, East, West. The sun rises in the east and sets in the west. Compass points to magnetic north. True north differs from magnetic north by the declination angle."),
            Triple("Wikipedia", "Dehydration", "Occurs when body loses more fluid than it takes in. Symptoms: thirst, dark urine, dizziness, fatigue. Prevention: drink water regularly, more in heat/exertion. Treatment: oral rehydration salts, sip water slowly."),
            Triple("Wikipedia", "Wilderness First Aid", "The practice of medicine in environments where definitive care is delayed. Priorities: scene safety, primary survey (ABCs), secondary survey. Evacuation decisions based on mechanism of injury and patient condition."),
        )
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
