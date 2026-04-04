package com.nomad.android.data.repository

import app.cash.turbine.test
import com.nomad.android.data.Result
import com.nomad.android.data.local.dao.ChatMessageDao
import com.nomad.android.data.local.entity.ChatMessageEntity
import com.nomad.android.data.local.entity.ChatSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getMessagesBySession returns messages from DAO`() = runTest {
        val messages = listOf(
            ChatMessageEntity(id = 1, sessionId = "s1", role = "user", content = "Hello", timestamp = 1000),
            ChatMessageEntity(id = 2, sessionId = "s1", role = "assistant", content = "Hi", timestamp = 2000)
        )
        val dao = FakeChatMessageDao(messages = messages)
        val repo = ChatRepository(dao)

        repo.getMessagesBySession("s1").test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            assertEquals(2, result.getOrNull()?.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getRecentSessions returns sessions from DAO`() = runTest {
        val sessions = listOf(
            ChatSessionEntity(id = "s1", title = "Session 1", createdAt = 1000, updatedAt = 2000)
        )
        val dao = FakeChatMessageDao(sessions = sessions)
        val repo = ChatRepository(dao)

        repo.getRecentSessions().test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()?.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `insertMessage succeeds`() = runTest {
        val dao = FakeChatMessageDao()
        val repo = ChatRepository(dao)
        val message = ChatMessageEntity(sessionId = "s1", role = "user", content = "test", timestamp = 1000)

        val result = repo.insertMessage(message)
        assertTrue(result.isSuccess)
        assertTrue(dao.insertedMessages.contains(message))
    }

    @Test
    fun `insertSession succeeds`() = runTest {
        val dao = FakeChatMessageDao()
        val repo = ChatRepository(dao)
        val session = ChatSessionEntity(id = "s1", title = "Test", createdAt = 1000, updatedAt = 2000)

        val result = repo.insertSession(session)
        assertTrue(result.isSuccess)
        assertEquals(session, dao.insertedSession)
    }

    @Test
    fun `deleteSession succeeds`() = runTest {
        val dao = FakeChatMessageDao()
        val repo = ChatRepository(dao)
        val session = ChatSessionEntity(id = "s1", title = "Test", createdAt = 1000, updatedAt = 2000)

        val result = repo.deleteSession(session)
        assertTrue(result.isSuccess)
        assertTrue(dao.deletedSessions.contains(session))
    }

    @Test
    fun `getSessionById returns session`() = runTest {
        val session = ChatSessionEntity(id = "s1", title = "Test", createdAt = 1000, updatedAt = 2000)
        val dao = FakeChatMessageDao(sessions = listOf(session))
        val repo = ChatRepository(dao)

        val result = repo.getSessionById("s1")
        assertTrue(result.isSuccess)
        assertEquals(session, result.getOrNull())
    }

    @Test
    fun `insertMessage returns error when DAO fails`() = runTest {
        val dao = FakeChatMessageDao(shouldFail = true)
        val repo = ChatRepository(dao)
        val message = ChatMessageEntity(sessionId = "s1", role = "user", content = "test", timestamp = 1000)

        val result = repo.insertMessage(message)
        assertTrue(result.isError)
    }

    @Test
    fun `insertSession returns error when DAO fails`() = runTest {
        val dao = FakeChatMessageDao(shouldFail = true)
        val repo = ChatRepository(dao)
        val session = ChatSessionEntity(id = "s1", title = "Test", createdAt = 1000, updatedAt = 2000)

        val result = repo.insertSession(session)
        assertTrue(result.isError)
    }

    @Test
    fun `getSessionById returns error when DAO fails`() = runTest {
        val dao = FakeChatMessageDao(shouldFail = true)
        val repo = ChatRepository(dao)

        val result = repo.getSessionById("s1")
        assertTrue(result.isError)
    }
}

class FakeChatMessageDao(
    private val messages: List<ChatMessageEntity> = emptyList(),
    private val sessions: List<ChatSessionEntity> = emptyList(),
    private val shouldFail: Boolean = false
) : ChatMessageDao {

    val insertedMessages = mutableListOf<ChatMessageEntity>()
    var insertedSession: ChatSessionEntity? = null
    val deletedSessions = mutableListOf<ChatSessionEntity>()

    override fun getBySession(sessionId: String) =
        if (shouldFail) throw RuntimeException("DB error")
        else flowOf(messages.filter { it.sessionId == sessionId })

    override suspend fun insertMessage(message: ChatMessageEntity) {
        if (shouldFail) throw RuntimeException("DB error")
        insertedMessages.add(message)
    }

    override suspend fun insertSession(session: ChatSessionEntity) {
        if (shouldFail) throw RuntimeException("DB error")
        insertedSession = session
    }

    override suspend fun updateSession(session: ChatSessionEntity) {
        if (shouldFail) throw RuntimeException("DB error")
    }

    override suspend fun deleteSession(session: ChatSessionEntity) {
        if (shouldFail) throw RuntimeException("DB error")
        deletedSessions.add(session)
    }

    override suspend fun deleteSessionById(sessionId: String) {
        if (shouldFail) throw RuntimeException("DB error")
    }

    override fun getRecentSessions(limit: Int) =
        if (shouldFail) throw RuntimeException("DB error")
        else flowOf(sessions.take(limit))

    override suspend fun getSessionById(sessionId: String): ChatSessionEntity? {
        if (shouldFail) throw RuntimeException("DB error")
        return sessions.find { it.id == sessionId }
    }
}
