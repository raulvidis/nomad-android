package com.nomad.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nomad.android.data.local.entity.ChatMessageEntity
import com.nomad.android.data.local.entity.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getBySession(sessionId: String): Flow<List<ChatMessageEntity>>

    @Insert
    suspend fun insert(message: ChatMessageEntity)

    @Query(
        """
        SELECT s.* FROM chat_sessions s
        INNER JOIN chat_messages m ON s.id = m.sessionId
        GROUP BY s.id
        ORDER BY MAX(m.timestamp) DESC
        LIMIT :limit
        """
    )
    fun getRecentSessions(limit: Int = 20): Flow<List<ChatSessionEntity>>
}
