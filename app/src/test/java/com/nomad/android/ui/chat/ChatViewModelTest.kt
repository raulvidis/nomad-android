package com.nomad.android.ui.chat

import com.nomad.android.data.Result
import com.nomad.android.data.ai.AIEngine
import com.nomad.android.data.ai.ChatAgent
import com.nomad.android.data.ai.KnowledgeBase
import com.nomad.android.data.ai.KnowledgeEntry
import com.nomad.android.data.local.entity.ChatSessionEntity
import com.nomad.android.data.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial context filters mirror knowledge base categories without a stale Wikipedia chip`() {
        // Regression: ChatViewModel used to append `+ listOf("Wikipedia")` to the
        // filter chips. The bundled knowledge base has no "Wikipedia" category, so a
        // user who tapped that chip received zero grounding candidates for every
        // message — silently disabling knowledge-base grounding with no feedback.
        // contextFilters must equal knowledgeBase.categories exactly.
        val knowledgeBase = KnowledgeBase(
            listOf(
                KnowledgeEntry("1", "Survival", "Fire Starting", "tinder kindling fuel ferro rod bow drill"),
                KnowledgeEntry("2", "First Aid", "CPR Basics", "chest compressions rate depth"),
                KnowledgeEntry("3", "Navigation", "Cardinal Directions", "north south east west sun stars"),
            ),
        )
        val chatRepository = mock<ChatRepository> {
            on { getRecentSessions(any()) }.thenReturn(flowOf(Result.Success(emptyList<ChatSessionEntity>())))
        }
        val aiEngine = mock<AIEngine>()
        val chatAgent = mock<ChatAgent>()

        val vm = ChatViewModel(chatRepository, aiEngine, knowledgeBase, chatAgent)

        runTest(mainDispatcher) {
            advanceUntilIdle()
        }

        val filters = vm.uiState.value.data.contextFilters
        assertEquals(knowledgeBase.categories, filters)
        assertFalse("Stale 'Wikipedia' filter must not appear in context filters", filters.contains("Wikipedia"))
    }
}
