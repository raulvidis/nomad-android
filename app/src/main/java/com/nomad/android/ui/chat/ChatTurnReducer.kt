package com.nomad.android.ui.chat

import com.nomad.android.data.ai.AgentEvent
import com.nomad.android.data.ai.tools.ToolResult

/**
 * Pure reduction of a chat turn's [AgentEvent] stream onto the message list. Kept
 * free of ViewModel/DB/coroutine concerns so the streaming + tool-card UI mapping
 * is unit-testable in isolation. Side effects (DB persistence, isStreaming flag,
 * queue processing) stay in [ChatViewModel].
 *
 * [AgentEvent.Finished], [AgentEvent.Error], and [AgentEvent.ModelUnavailable] are
 * handled by the ViewModel; [reduce] only transforms list/UI state for the events
 * that directly shape the visible turn.
 */
object ChatTurnReducer {

    data class State(
        val currentAssistantId: Long? = null,
        val toolCardIds: Map<String, Long> = emptyMap(),
    )

    fun reduce(
        messages: List<ChatMessage>,
        state: State,
        event: AgentEvent,
        sessionId: String,
        nextId: () -> Long,
    ): Pair<List<ChatMessage>, State> = when (event) {

        is AgentEvent.TurnStarted -> {
            // Stop streaming any prior assistant bubble, then open a fresh one.
            val stopped = messages.map { if (it.id == state.currentAssistantId) it.copy(isStreaming = false) else it }
            val id = nextId()
            val bubble = ChatMessage(
                id = id,
                sessionId = sessionId,
                role = "assistant",
                content = "",
                isStreaming = true,
            )
            (stopped + bubble) to state.copy(currentAssistantId = id)
        }

        // Collapsed by default — the user can expand a live thinking block by tapping it.
        is AgentEvent.ThinkingDelta -> updateCurrent(messages, state) {
            it.copy(thinkingText = event.thinking)
        } to state

        is AgentEvent.AnswerDelta -> updateCurrent(messages, state) {
            it.copy(content = event.answer)
        } to state

        is AgentEvent.ToolCallStarted -> {
            val id = nextId()
            val card = ChatMessage(
                id = id,
                sessionId = sessionId,
                role = "assistant",
                content = "",
                kind = MessageKind.ToolCall,
                toolCall = ToolCallUi(
                    callId = event.id,
                    name = event.name,
                    args = event.args,
                    status = ToolStatus.Running,
                ),
            )
            (messages + card) to state.copy(toolCardIds = state.toolCardIds + (event.id to id))
        }

        is AgentEvent.ToolCallFinished -> {
            val cardId = state.toolCardIds[event.id]
            val updated = messages.map { msg ->
                if (msg.id == cardId && msg.toolCall != null) {
                    msg.copy(
                        toolCall = msg.toolCall.copy(
                            status = if (event.result is ToolResult.Ok) ToolStatus.Ok else ToolStatus.Err,
                            resultText = event.result.toModelString(),
                            durationMs = event.durationMs,
                        ),
                    )
                } else {
                    msg
                }
            }
            updated to state
        }

        is AgentEvent.Finished -> updateCurrent(messages, state) {
            it.copy(
                content = event.answer.ifBlank { it.content },
                thinkingText = event.thinking.ifBlank { it.thinkingText },
                isStreaming = false,
                isThinkingExpanded = false,
            )
        } to state

        is AgentEvent.Error, AgentEvent.ModelUnavailable -> messages to state
    }

    private inline fun updateCurrent(
        messages: List<ChatMessage>,
        state: State,
        transform: (ChatMessage) -> ChatMessage,
    ): List<ChatMessage> =
        messages.map { if (it.id == state.currentAssistantId) transform(it) else it }
}
