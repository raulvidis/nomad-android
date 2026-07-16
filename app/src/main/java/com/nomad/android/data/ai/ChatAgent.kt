package com.nomad.android.data.ai

import com.nomad.android.data.ai.tools.ChatToolRegistry
import com.nomad.android.data.ai.tools.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/** A typed event in a single chat turn's agent loop. Consumed by the ViewModel. */
sealed interface AgentEvent {
    /** A new assistant bubble begins streaming (start of each loop iteration). */
    data object TurnStarted : AgentEvent
    data class ThinkingDelta(val thinking: String) : AgentEvent
    data class AnswerDelta(val answer: String) : AgentEvent
    data class ToolCallStarted(val id: String, val name: String, val args: String) : AgentEvent
    data class ToolCallFinished(
        val id: String,
        val name: String,
        val args: String,
        val result: ToolResult,
        val durationMs: Long,
    ) : AgentEvent
    /** Terminal: the model's final answer for this turn. */
    data class Finished(val answer: String, val thinking: String, val cappedNote: String? = null) : AgentEvent
    data class Error(val message: String) : AgentEvent
    /** No model loaded — the caller should fall back to the rule-based engine. */
    data object ModelUnavailable : AgentEvent
}

/**
 * Model-driven chat turn: lets the LLM decide when to read the knowledge base or
 * notes via tool calls, instead of always-on RAG injection. Drives the existing
 * [LlamaBridgeHandle] primitives (submit → stream → parse → run tools → append →
 * loop), emitting [AgentEvent]s the ViewModel maps onto chat UI state.
 *
 * The native conversation history persists across turns within a session, so each
 * user message is one `run(...)`; tool results are appended to that history.
 */
@Singleton
class ChatAgent @Inject constructor(
    private val bridge: LlamaBridgeHandle,
    private val registry: ChatToolRegistry,
) {

    fun run(userText: String): Flow<AgentEvent> = channelFlow {
        if (!bridge.isModelLoaded) {
            send(AgentEvent.ModelUnavailable)
            return@channelFlow
        }

        // Each turn is self-contained: the ViewModel inlines prior history into the
        // prompt (matching NOMAD's existing pattern), so reset native state per turn.
        bridge.resetConversation()
        bridge.setSystemPrompt(SYSTEM_PROMPT)
        val toolsJson = registry.describeAsJson()
        var prompt = userText
        var iteration = 0

        while (iteration < MAX_ITERATIONS) {
            iteration++
            send(AgentEvent.TurnStarted)

            val submit = bridge.submitTurn(prompt, "", toolsJson)
            if (submit.isFailure) {
                send(AgentEvent.Error(submit.exceptionOrNull()?.message ?: "model submit failed"))
                return@channelFlow
            }

            val acc = StringBuilder()
            var prevThinking = ""
            var prevAnswer = ""
            val stream = bridge.streamTokens { chunk ->
                acc.append(chunk)
                val (thinking, answerRaw) = ThinkingParser.split(acc.toString())
                if (thinking.length != prevThinking.length) {
                    prevThinking = thinking
                    send(AgentEvent.ThinkingDelta(thinking))
                }
                val answer = ThinkingParser.stripToolMarkup(answerRaw)
                if (answer != prevAnswer) {
                    prevAnswer = answer
                    send(AgentEvent.AnswerDelta(answer))
                }
            }
            if (stream.isFailure) {
                send(AgentEvent.Error(stream.exceptionOrNull()?.message ?: "token stream failed"))
                return@channelFlow
            }

            val parsed = bridge.finishTurnAndParse()
            val (thinkingFinal, answerFinalRaw) = ThinkingParser.split(acc.toString())
            var toolCalls = parsed.toolCalls
            var answerFinal = ThinkingParser.stripToolMarkup(answerFinalRaw)

            // Hedge: if the native parser found no calls, salvage improvised <function> XML.
            if (toolCalls.isEmpty()) {
                val (stripped, salvaged) = ToolCallSalvage.parse(answerFinalRaw)
                if (salvaged.isNotEmpty()) {
                    toolCalls = salvaged
                    answerFinal = ThinkingParser.stripToolMarkup(stripped)
                }
            }

            if (toolCalls.isEmpty()) {
                val finalAnswer = answerFinal.ifBlank { ThinkingParser.stripToolMarkup(parsed.content) }
                send(AgentEvent.Finished(finalAnswer, thinkingFinal))
                return@channelFlow
            }

            for (call in toolCalls) {
                if (!isActive) return@channelFlow
                send(AgentEvent.ToolCallStarted(call.id, call.name, call.arguments))
                val start = System.currentTimeMillis()
                val result: ToolResult = registry.execute(call.name, call.arguments)
                val durationMs = System.currentTimeMillis() - start
                send(AgentEvent.ToolCallFinished(call.id, call.name, call.arguments, result, durationMs))
                bridge.appendToolResult(call.id, call.name, result.toModelString())
            }
            prompt = "" // continue from the appended tool results
        }

        send(
            AgentEvent.Finished(
                answer = "",
                thinking = "",
                cappedNote = "Stopped after $MAX_ITERATIONS tool iterations.",
            ),
        )
    }.flowOn(Dispatchers.Default) // keep JNI parse + tool execution off the main thread

    companion object {
        const val MAX_ITERATIONS = 5

        val SYSTEM_PROMPT: String = """
            You are NOMAD, an offline survival assistant running on the user's phone with no internet. The user may be in a real emergency — be calm, direct, and practical.

            Answering rules:
            - Lead with the single most important action or fact, then supporting detail.
            - Use short numbered steps for procedures. Keep answers under ~150 words unless asked for more.
            - In life-threatening situations, state what to do RIGHT NOW first; cautions after.
            - Never invent facts, dosages, or plant/mushroom identifications. If unsure, say so and give the safest option.
            - Do not overthink. If you reason internally, keep it to 2-3 short sentences, then answer immediately.

            Tools (call one only when stored knowledge or the user's notes would improve the answer, then answer from the result):
            - search_knowledge_base(query, category?) — offline survival knowledge base (first aid, water, fire, shelter, navigation, emergency procedures, reference content).
            - search_notes(query) — the user's own saved notes.
        """.trimIndent()
    }
}
