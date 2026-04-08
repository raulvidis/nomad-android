package com.nomad.android.data.repository

import com.nomad.android.data.local.entity.NoteEntity
import org.junit.Assert.*
import org.junit.Test

class NoteRepositoryTest {

    @Test
    fun noteEntityMapsCorrectly() {
        val entity = NoteEntity(
            id = 1,
            title = "Test",
            content = "Content",
            createdAt = 1000L,
            updatedAt = 2000L
        )
        assertEquals(1L, entity.id)
        assertEquals("Test", entity.title)
        assertEquals("Content", entity.content)
        assertEquals(1000L, entity.createdAt)
        assertEquals(2000L, entity.updatedAt)
    }

    @Test
    fun noteEntityDefaultId() {
        val entity = NoteEntity(
            title = "New",
            content = "",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        assertEquals(0L, entity.id)
    }
}
