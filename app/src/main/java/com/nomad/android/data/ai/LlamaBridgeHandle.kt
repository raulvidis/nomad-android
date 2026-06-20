package com.nomad.android.data.ai

import javax.inject.Inject

/**
 * Thin seam over the [LlamaBridge] singleton so the agent loop can be driven by a
 * fake in unit tests. Mirrors exactly the bridge methods [ChatAgent] needs; the
 * default implementation delegates straight through.
 */
interface LlamaBridgeHandle {
    val isModelLoaded: Boolean
    fun resetConversation()
    fun setSystemPrompt(prompt: String)
    suspend fun submitTurn(prompt: String, attachmentsJson: String, toolsJson: String): Result<Unit>
    suspend fun streamTokens(onChunk: suspend (String) -> Unit): Result<Unit>
    fun finishTurnAndParse(): LlamaBridge.ParsedTurn
    fun appendToolResult(callId: String, name: String, content: String)
}

class DefaultLlamaBridgeHandle @Inject constructor() : LlamaBridgeHandle {
    override val isModelLoaded: Boolean get() = LlamaBridge.isModelLoaded
    override fun resetConversation() = LlamaBridge.resetConversation()
    override fun setSystemPrompt(prompt: String) = LlamaBridge.setSystemPrompt(prompt)
    override suspend fun submitTurn(prompt: String, attachmentsJson: String, toolsJson: String) =
        LlamaBridge.submitTurn(prompt, attachmentsJson, toolsJson)
    override suspend fun streamTokens(onChunk: suspend (String) -> Unit) =
        LlamaBridge.streamTokens(onChunk)
    override fun finishTurnAndParse(): LlamaBridge.ParsedTurn = LlamaBridge.finishTurnAndParse()
    override fun appendToolResult(callId: String, name: String, content: String) =
        LlamaBridge.appendToolResult(callId, name, content)
}
