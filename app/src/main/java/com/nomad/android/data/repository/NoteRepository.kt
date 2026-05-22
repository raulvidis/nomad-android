package com.nomad.android.data.repository

import com.nomad.android.data.Result
import com.nomad.android.data.local.dao.NoteDao
import com.nomad.android.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao
) {
    fun getAllNotes(): Flow<Result<List<NoteEntity>>> =
        noteDao.getAll()
            .map { Result.success(it) }
            .catch { emit(Result.error("Failed to load notes", it)) }

    suspend fun getNote(id: Long): Result<NoteEntity> =
        Result.runCatching {
            noteDao.getById(id) ?: throw NoSuchElementException("Note not found")
        }

    suspend fun saveNote(title: String, content: String, existingId: Long? = null): Result<Long> =
        Result.runCatching {
            val now = System.currentTimeMillis()
            if (existingId != null && existingId > 0) {
                val existing = noteDao.getById(existingId)
                    ?: throw NoSuchElementException("Note $existingId not found — may have been deleted")
                noteDao.update(existing.copy(title = title, content = content, updatedAt = now))
                existingId
            } else {
                noteDao.insert(NoteEntity(title = title, content = content, createdAt = now, updatedAt = now))
            }
        }

    suspend fun deleteNote(id: Long): Result<Unit> =
        Result.runCatching { noteDao.delete(id) }

    fun searchNotes(query: String): Flow<Result<List<NoteEntity>>> =
        noteDao.search(query)
            .map { Result.success(it) }
            .catch { emit(Result.error("Search failed", it)) }
}
