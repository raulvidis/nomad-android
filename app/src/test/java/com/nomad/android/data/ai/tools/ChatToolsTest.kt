package com.nomad.android.data.ai.tools

import com.nomad.android.data.ai.KnowledgeBase
import com.nomad.android.data.ai.KnowledgeEntry
import com.nomad.android.data.local.dao.NoteDao
import com.nomad.android.data.local.entity.NoteEntity
import com.nomad.android.data.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * TDD coverage for the model-callable tool layer (`data/ai/tools/`): the registry
 * that advertises tools to llama.cpp as an OpenAI-style schema and runs them, plus
 * the two read-only tools that expose the knowledge base and the user's notes.
 *
 * Robolectric because the tool schemas and argument parsing use `org.json`, which
 * is stubbed (throws) under plain JUnit.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class ChatToolsTest {

    private val knowledgeBase = KnowledgeBase(
        listOf(
            KnowledgeEntry("1", "First Aid", "Treating burns", "Cool the burn under running water for 20 minutes.", "bundled"),
            KnowledgeEntry("2", "Water", "Purifying water", "Boil water for at least one minute to make it safe.", "bundled"),
        ),
    )

    private fun notesRepoReturning(vararg notes: NoteEntity): NoteRepository {
        val dao = object : NoteDao {
            override fun getAll(): Flow<List<NoteEntity>> = flowOf(notes.toList())
            override suspend fun getById(id: Long): NoteEntity? = notes.firstOrNull { it.id == id }
            override suspend fun insert(note: NoteEntity): Long = 0
            override suspend fun update(note: NoteEntity) {}
            override suspend fun delete(id: Long) {}
            override fun search(query: String): Flow<List<NoteEntity>> = flowOf(notes.toList())
        }
        return NoteRepository(dao)
    }

    // --- ToolResult ---

    @Test
    fun `ToolResult Err renders ERROR prefix for the model`() {
        assertEquals("ERROR: boom", ToolResult.Err("boom").toModelString())
        assertEquals("hello", ToolResult.Ok("hello").toModelString())
    }

    // --- ChatToolRegistry.describeAsJson ---

    @Test
    fun `describeAsJson emits an OpenAI function envelope for every tool`() {
        val registry = ChatToolRegistry(
            listOf(
                SearchKnowledgeBaseTool(knowledgeBase),
                SearchNotesTool(notesRepoReturning()),
            ),
        )

        val arr = JSONArray(registry.describeAsJson())
        assertEquals(2, arr.length())

        val names = (0 until arr.length()).map { i ->
            val fn = arr.getJSONObject(i)
            assertEquals("function", fn.getString("type"))
            val function = fn.getJSONObject("function")
            assertTrue(function.has("description"))
            assertEquals("object", function.getJSONObject("parameters").getString("type"))
            function.getString("name")
        }
        assertTrue(names.containsAll(listOf("search_knowledge_base", "search_notes")))
    }

    // --- ChatToolRegistry.execute ---

    @Test
    fun `execute returns Err for an unknown tool`() = runTest {
        val registry = ChatToolRegistry(emptyList())
        val result = registry.execute("does_not_exist", "{}")
        assertTrue(result is ToolResult.Err)
    }

    @Test
    fun `execute returns Err for malformed JSON arguments`() = runTest {
        val registry = ChatToolRegistry(listOf(SearchKnowledgeBaseTool(knowledgeBase)))
        val result = registry.execute("search_knowledge_base", "{not json")
        assertTrue(result is ToolResult.Err)
    }

    // --- SearchKnowledgeBaseTool ---

    @Test
    fun `search_knowledge_base returns matching entry content`() = runTest {
        val tool = SearchKnowledgeBaseTool(knowledgeBase)
        val result = tool.execute(JSONObject().put("query", "burns"))
        assertTrue(result is ToolResult.Ok)
        assertTrue((result as ToolResult.Ok).text.contains("Cool the burn"))
    }

    @Test
    fun `search_knowledge_base reports no matches without failing`() = runTest {
        val tool = SearchKnowledgeBaseTool(knowledgeBase)
        val result = tool.execute(JSONObject().put("query", "helicopter avionics"))
        assertTrue(result is ToolResult.Ok)
        assertTrue((result as ToolResult.Ok).text.contains("No matching", ignoreCase = true))
    }

    @Test
    fun `search_knowledge_base requires a query argument`() = runTest {
        val tool = SearchKnowledgeBaseTool(knowledgeBase)
        val result = tool.execute(JSONObject())
        assertTrue(result is ToolResult.Err)
    }

    // --- SearchNotesTool ---

    @Test
    fun `search_notes returns matching note title and snippet`() = runTest {
        val repo = notesRepoReturning(
            NoteEntity(id = 1, title = "Camp location", content = "Ridge above the river bend.", createdAt = 0, updatedAt = 0),
        )
        val tool = SearchNotesTool(repo)
        val result = tool.execute(JSONObject().put("query", "camp"))
        assertTrue(result is ToolResult.Ok)
        val text = (result as ToolResult.Ok).text
        assertTrue(text.contains("Camp location"))
        assertTrue(text.contains("Ridge above the river"))
    }

    @Test
    fun `search_notes reports no matches without failing`() = runTest {
        val tool = SearchNotesTool(notesRepoReturning())
        val result = tool.execute(JSONObject().put("query", "anything"))
        assertTrue(result is ToolResult.Ok)
        assertTrue((result as ToolResult.Ok).text.contains("No matching", ignoreCase = true))
    }
}
