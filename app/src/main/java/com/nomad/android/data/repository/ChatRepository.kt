package com.nomad.android.data.repository

import com.nomad.android.data.Result
import com.nomad.android.data.local.dao.ChatMessageDao
import com.nomad.android.data.local.entity.ChatMessageEntity
import com.nomad.android.data.local.entity.ChatSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatMessageDao: ChatMessageDao
) {
    fun getMessagesBySession(sessionId: String): Flow<Result<List<ChatMessageEntity>>> =
        chatMessageDao.getBySession(sessionId)
            .map { Result.success(it) }
            .catch { emit(Result.error("Failed to load messages", it)) }

    fun getRecentSessions(limit: Int = 20): Flow<Result<List<ChatSessionEntity>>> =
        chatMessageDao.getRecentSessions(limit)
            .map { Result.success(it) }
            .catch { emit(Result.error("Failed to load sessions", it)) }

    suspend fun getSessionById(sessionId: String): Result<ChatSessionEntity?> =
        Result.runCatching { chatMessageDao.getSessionById(sessionId) }

    suspend fun insertMessage(message: ChatMessageEntity): Result<Unit> =
        Result.runCatching { chatMessageDao.insertMessage(message) }

    suspend fun insertSession(session: ChatSessionEntity): Result<Unit> =
        Result.runCatching { chatMessageDao.insertSession(session) }

    suspend fun updateSession(session: ChatSessionEntity): Result<Unit> =
        Result.runCatching { chatMessageDao.updateSession(session) }

    suspend fun deleteSession(session: ChatSessionEntity): Result<Unit> =
        Result.runCatching { chatMessageDao.deleteSession(session) }

    suspend fun deleteSessionById(sessionId: String): Result<Unit> =
        Result.runCatching { chatMessageDao.deleteSessionById(sessionId) }

    suspend fun deleteAllSessions(): Result<Unit> =
        Result.runCatching {
            chatMessageDao.deleteAllInTransaction()
        }

    suspend fun deleteMessagesBySession(sessionId: String): Result<Unit> =
        Result.runCatching { chatMessageDao.deleteMessagesBySession(sessionId) }
}
