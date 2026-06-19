package com.nomad.android.ui.chat

import com.nomad.android.data.ai.AgentEvent
import com.nomad.android.data.ai.tools.ToolResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-function coverage for mapping [AgentEvent]s onto the chat message list.
 * No ViewModel, DB, or dispatchers needed — the reducer is the testable core of
 * the streaming/tool-call UI wiring.
 */
class ChatTurnReducerTest {

    private val sessionId = "s1"

    private fun ids(): () -> Long {
        var n = 0L
        return { --n } // -1, -2, -3 … (transient negative ids)
    }

    private fun reduceAll(events: List<AgentEvent>): List<ChatMessage> {
        val nextId = ids()
        var messages = emptyList<ChatMessage>()
        var state = ChatTurnReducer.State()
        for (e in events) {
            val (m, s) = ChatTurnReducer.reduce(messages, state, e, sessionId, nextId)
            messages = m
            state = s
        }
        return messages
    }

    @Test
    fun `TurnStarted appends a streaming assistant bubble`() {
        val messages = reduceAll(listOf(AgentEvent.TurnStarted))
        assertEquals(1, messages.size)
        assertEquals("assistant", messages[0].role)
        assertTrue(messages[0].isStreaming)
    }

    @Test
    fun `AnswerDelta updates the current assistant content`() {
        val messages = reduceAll(
            listOf(AgentEvent.TurnStarted, AgentEvent.AnswerDelta("Boil the water.")),
        )
        assertEquals("Boil the water.", messages.single().content)
    }

    @Test
    fun `ThinkingDelta records reasoning and auto-expands it`() {
        val messages = reduceAll(
            listOf(AgentEvent.TurnStarted, AgentEvent.ThinkingDelta("considering options")),
        )
        assertEquals("considering options", messages.single().thinkingText)
        assertTrue(messages.single().isThinkingExpanded)
    }

    @Test
    fun `tool call appears as a card and resolves to Ok with duration`() {
        val messages = reduceAll(
            listOf(
                AgentEvent.TurnStarted,
                AgentEvent.AnswerDelta("checking"),
                AgentEvent.ToolCallStarted("c1", "search_knowledge_base", "{\"query\":\"burns\"}"),
                AgentEvent.ToolCallFinished("c1", "search_knowledge_base", "{}", ToolResult.Ok("Cool the burn."), 42),
            ),
        )
        // [assistant bubble, tool card]
        assertEquals(2, messages.size)
        val card = messages.first { it.kind == MessageKind.ToolCall }
        assertEquals("search_knowledge_base", card.toolCall?.name)
        assertEquals(ToolStatus.Ok, card.toolCall?.status)
        assertEquals(42L, card.toolCall?.durationMs)
        assertTrue(card.toolCall?.resultText?.contains("Cool the burn") == true)
    }

    @Test
    fun `a second TurnStarted opens a new bubble and stops streaming the previous`() {
        val messages = reduceAll(
            listOf(
                AgentEvent.TurnStarted,
                AgentEvent.AnswerDelta("let me look"),
                AgentEvent.ToolCallStarted("c1", "search_notes", "{}"),
                AgentEvent.ToolCallFinished("c1", "search_notes", "{}", ToolResult.Ok("note"), 5),
                AgentEvent.TurnStarted,
            ),
        )
        val assistants = messages.filter { it.kind == MessageKind.Normal && it.role == "assistant" }
        assertEquals(2, assistants.size)
        assertFalse(assistants[0].isStreaming)
        assertTrue(assistants[1].isStreaming)
    }

    @Test
    fun `Finished sets the final answer and stops streaming`() {
        val messages = reduceAll(
            listOf(
                AgentEvent.TurnStarted,
                AgentEvent.ThinkingDelta("reasoning"),
                AgentEvent.AnswerDelta("partial"),
                AgentEvent.Finished("Cool the burn under running water.", "reasoning"),
            ),
        )
        val msg = messages.single()
        assertEquals("Cool the burn under running water.", msg.content)
        assertFalse(msg.isStreaming)
        assertFalse(msg.isThinkingExpanded)
    }
}
