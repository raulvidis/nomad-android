package com.nomad.android.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the streaming parsers ported from opendroid: separating model reasoning
 * from the answer ([ThinkingParser]) and salvaging improvised XML tool calls
 * ([ToolCallSalvage]). Robolectric for the `org.json` usage in the salvage parser.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class ChatStreamParsingTest {

    // --- ThinkingParser ---

    @Test
    fun `explicit think block splits into thinking and answer`() {
        val (thinking, answer) = ThinkingParser.split("<think>weighing options</think>The answer is water.")
        assertEquals("weighing options", thinking)
        assertEquals("The answer is water.", answer)
    }

    @Test
    fun `unclosed think block keeps everything as thinking with empty answer`() {
        val (thinking, answer) = ThinkingParser.split("<think>still reasoning")
        assertEquals("still reasoning", thinking)
        assertEquals("", answer)
    }

    @Test
    fun `implicit close tag (MiniCPM5 template) routes prefix to thinking`() {
        // Template injected <think> before generation, so the model emits only </think>.
        val (thinking, answer) = ThinkingParser.split("reasoning here</think>Boil the water.")
        assertEquals("reasoning here", thinking)
        assertEquals("Boil the water.", answer)
    }

    @Test
    fun `plain answer with no thinking tags is all answer`() {
        val (thinking, answer) = ThinkingParser.split("Just an answer.")
        assertEquals("", thinking)
        assertEquals("Just an answer.", answer)
    }

    // --- ToolCallSalvage ---

    @Test
    fun `salvage extracts an improvised function call and strips it from text`() {
        val raw = "Let me check.<function name=\"search_notes\"><param name=\"query\">camp</param></function>"
        val (stripped, calls) = ToolCallSalvage.parse(raw)
        assertEquals(1, calls.size)
        assertEquals("search_notes", calls[0].name)
        assertTrue(calls[0].arguments.contains("camp"))
        assertEquals("Let me check.", stripped)
    }

    @Test
    fun `salvage returns no calls for plain text`() {
        val (stripped, calls) = ToolCallSalvage.parse("nothing to see")
        assertTrue(calls.isEmpty())
        assertEquals("nothing to see", stripped)
    }
}
