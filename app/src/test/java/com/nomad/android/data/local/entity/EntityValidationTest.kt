package com.nomad.android.data.local.entity

import org.junit.Assert.*
import org.junit.Test

class EntityValidationTest {

    @Test
    fun `ContentPackEntity has all required fields`() {
        val entity = ContentPackEntity(
            id = "test_pack",
            name = "Test Pack",
            type = "survival",
            sizeBytes = 1024L,
            status = "downloaded",
            downloadedAt = 1000L,
            version = "1.0",
            description = "Test description"
        )
        assertEquals("test_pack", entity.id)
        assertEquals("Test Pack", entity.name)
        assertEquals("survival", entity.type)
        assertEquals(1024L, entity.sizeBytes)
        assertEquals("downloaded", entity.status)
        assertEquals(1000L, entity.downloadedAt)
        assertEquals("1.0", entity.version)
        assertEquals("Test description", entity.description)
    }

    @Test
    fun `ContentPackEntity allows null downloadedAt`() {
        val entity = ContentPackEntity(
            id = "test",
            name = "Test",
            type = "test",
            sizeBytes = 0L,
            status = "available",
            downloadedAt = null,
            version = "1.0",
            description = "Test"
        )
        assertNull(entity.downloadedAt)
    }

    @Test
    fun `ChatMessageEntity has correct fields`() {
        val entity = ChatMessageEntity(
            id = 1L,
            sessionId = "session123",
            role = "user",
            content = "Hello",
            timestamp = System.currentTimeMillis()
        )
        assertEquals(1L, entity.id)
        assertEquals("session123", entity.sessionId)
        assertEquals("user", entity.role)
        assertEquals("Hello", entity.content)
    }

    @Test
    fun `ChatSessionEntity has correct fields`() {
        val now = System.currentTimeMillis()
        val entity = ChatSessionEntity(
            id = "session123",
            title = "Test Chat",
            createdAt = now,
            updatedAt = now
        )
        assertEquals("session123", entity.id)
        assertEquals("Test Chat", entity.title)
    }

    @Test
    fun `SearchHistoryEntity has correct fields`() {
        val entity = SearchHistoryEntity(
            id = 1L,
            query = "water purification",
            source = "wikipedia",
            timestamp = System.currentTimeMillis()
        )
        assertEquals("water purification", entity.query)
        assertEquals("wikipedia", entity.source)
    }

    @Test
    fun `SettingsEntity has correct fields`() {
        val entity = SettingsEntity(
            key = "onboarding_complete",
            value = "true"
        )
        assertEquals("onboarding_complete", entity.key)
        assertEquals("true", entity.value)
    }
}
