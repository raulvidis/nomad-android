package com.nomad.android.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nomad.android.data.local.NomadDatabase
import com.nomad.android.data.local.entity.NoteEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteDaoTest {

    private lateinit var database: NomadDatabase
    private lateinit var noteDao: NoteDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NomadDatabase::class.java,
        ).allowMainThreadQueries().build()
        noteDao = database.noteDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetNote() = runTest {
        val id = noteDao.insert(
            NoteEntity(title = "Test Note", content = "Hello world", createdAt = 1000L, updatedAt = 1000L)
        )
        val note = noteDao.getById(id)
        assertNotNull(note)
        assertEquals("Test Note", note!!.title)
        assertEquals("Hello world", note.content)
    }

    @Test
    fun updateNote() = runTest {
        val id = noteDao.insert(
            NoteEntity(title = "Original", content = "Content", createdAt = 1000L, updatedAt = 1000L)
        )
        val note = noteDao.getById(id)!!
        noteDao.update(note.copy(title = "Updated", updatedAt = 2000L))
        val updated = noteDao.getById(id)
        assertEquals("Updated", updated!!.title)
        assertEquals(2000L, updated.updatedAt)
    }

    @Test
    fun deleteNote() = runTest {
        val id = noteDao.insert(
            NoteEntity(title = "To Delete", content = "Bye", createdAt = 1000L, updatedAt = 1000L)
        )
        noteDao.delete(id)
        assertNull(noteDao.getById(id))
    }

    @Test
    fun searchNotesFindsTitle() = runTest {
        noteDao.insert(NoteEntity(title = "Survival Guide", content = "Water is essential", createdAt = 1000L, updatedAt = 1000L))
        noteDao.insert(NoteEntity(title = "Cooking Tips", content = "Boil water first", createdAt = 2000L, updatedAt = 2000L))

        val results = noteDao.search("Survival")
        val list = results.collect { }
        
        kotlinx.coroutines.test.TestScope().runTest {
            val collected = mutableListOf<List<NoteEntity>>()
            noteDao.search("Survival").collect { collected.add(it) }
            assertTrue(collected.first().size == 1)
            assertEquals("Survival Guide", collected.first()[0].title)
        }
    }

    @Test
    fun searchNotesFindsContent() = runTest {
        noteDao.insert(NoteEntity(title = "Guide A", content = "Find clean water sources", createdAt = 1000L, updatedAt = 1000L))
        noteDao.insert(NoteEntity(title = "Guide B", content = "Build shelter", createdAt = 2000L, updatedAt = 2000L))

        runTest {
            val collected = mutableListOf<List<NoteEntity>>()
            noteDao.search("water").collect { collected.add(it) }
            assertTrue(collected.first().size == 1)
            assertEquals("Guide A", collected.first()[0].title)
        }
    }
}
