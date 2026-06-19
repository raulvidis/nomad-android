package com.nomad.android.data.ai

import app.cash.turbine.test
import com.nomad.android.data.ai.tools.ChatToolRegistry
import com.nomad.android.data.ai.tools.SearchKnowledgeBaseTool
import com.nomad.android.data.ai.tools.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Drives [ChatAgent] with a scripted fake bridge: iteration 1 emits a tool call,
 * iteration 2 emits the final answer. Asserts the agent (a) streams the answer,
 * (b) runs the tool and feeds its result back to the bridge, and (c) finishes with
 * the model's final answer. Robolectric for `org.json` used by the registry.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class ChatAgentTest {

    private val knowledgeBase = KnowledgeBase(
        listOf(
            KnowledgeEntry("1", "First Aid", "Treating burns", "Cool the burn under running water.", "bundled"),
        ),
    )
    private val registry = ChatToolRegistry(listOf(SearchKnowledgeBaseTool(knowledgeBase)))

    private class FakeBridge(
        private val turns: List<Turn>,
        override val isModelLoaded: Boolean = true,
    ) : LlamaBridgeHandle {
        data class Turn(val tokens: List<String>, val parsed: LlamaBridge.ParsedTurn)

        var iter = 0
        val appended = mutableListOf<Triple<String, String, String>>()

        override fun resetConversation() {}
        override fun setSystemPrompt(prompt: String) {}
        override suspend fun submitTurn(prompt: String, attachmentsJson: String, toolsJson: String) =
            Result.success(Unit)

        override suspend fun streamTokens(onChunk: suspend (String) -> Unit): Result<Unit> {
            turns[iter].tokens.forEach { onChunk(it) }
            return Result.success(Unit)
        }

        override fun finishTurnAndParse(): LlamaBridge.ParsedTurn = turns[iter++].parsed
        override fun appendToolResult(callId: String, name: String, content: String) {
            appended.add(Triple(callId, name, content))
        }
    }

    @Test
    fun `agent calls the tool then finishes with the model answer`() = runTest {
        val bridge = FakeBridge(
            listOf(
                FakeBridge.Turn(
                    tokens = listOf("Let me check the knowledge base."),
                    parsed = LlamaBridge.ParsedTurn(
                        content = "",
                        toolCalls = listOf(
                            LlamaBridge.ParsedToolCall("c1", "search_knowledge_base", "{\"query\":\"burns\"}"),
                        ),
                    ),
                ),
                FakeBridge.Turn(
                    tokens = listOf("Cool the burn under running water."),
                    parsed = LlamaBridge.ParsedTurn("Cool the burn under running water.", emptyList()),
                ),
            ),
        )
        val agent = ChatAgent(bridge, registry)

        agent.run("how do I treat a burn?").test {
            assertTrue(awaitItem() is AgentEvent.TurnStarted)
            assertEquals("Let me check the knowledge base.", (awaitItem() as AgentEvent.AnswerDelta).answer)

            val started = awaitItem() as AgentEvent.ToolCallStarted
            assertEquals("search_knowledge_base", started.name)

            val finishedCall = awaitItem() as AgentEvent.ToolCallFinished
            assertTrue(finishedCall.result is ToolResult.Ok)
            assertTrue((finishedCall.result as ToolResult.Ok).text.contains("Cool the burn"))

            assertTrue(awaitItem() is AgentEvent.TurnStarted)
            assertEquals("Cool the burn under running water.", (awaitItem() as AgentEvent.AnswerDelta).answer)

            val finished = awaitItem() as AgentEvent.Finished
            assertEquals("Cool the burn under running water.", finished.answer)
            awaitComplete()
        }

        assertEquals(1, bridge.appended.size)
        assertEquals("search_knowledge_base", bridge.appended[0].second)
        assertTrue(bridge.appended[0].third.contains("Cool the burn"))
    }

    @Test
    fun `agent reports model unavailable when no model is loaded`() = runTest {
        val bridge = FakeBridge(emptyList(), isModelLoaded = false)
        val agent = ChatAgent(bridge, registry)
        agent.run("hello").test {
            assertTrue(awaitItem() is AgentEvent.ModelUnavailable)
            awaitComplete()
        }
    }
}
