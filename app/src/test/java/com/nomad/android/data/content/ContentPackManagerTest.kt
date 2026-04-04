package com.nomad.android.data.content

import org.junit.Assert.*
import org.junit.Test

class ContentPackManagerTest {

    @Test
    fun `formatSize formats GB correctly`() {
        assertEquals("1.0 GB", ContentPackManager(null, null).formatSize(1_073_741_824L))
    }

    @Test
    fun `formatSize formats MB correctly`() {
        assertEquals("512.0 MB", ContentPackManager(null, null).formatSize(512L * 1024 * 1024))
    }

    @Test
    fun `formatSize formats bytes correctly`() {
        assertEquals("500 bytes", ContentPackManager(null, null).formatSize(500L))
    }

    @Test
    fun `formatSize formats large GB`() {
        assertEquals("32.0 GB", ContentPackManager(null, null).formatSize(32L * 1024 * 1024 * 1024))
    }

    @Test
    fun `PackStatus enum has all values`() {
        val values = PackStatus.values()
        assertEquals(4, values.size)
        assertTrue(values.contains(PackStatus.AVAILABLE))
        assertTrue(values.contains(PackStatus.DOWNLOADING))
        assertTrue(values.contains(PackStatus.DOWNLOADED))
        assertTrue(values.contains(PackStatus.ERROR))
    }

    @Test
    fun `ContentPack data class holds values`() {
        val pack = ContentPack(
            id = "test",
            name = "Test Pack",
            type = "wikipedia",
            sizeBytes = 1024L,
            description = "A test pack",
            status = PackStatus.AVAILABLE
        )
        assertEquals("test", pack.id)
        assertEquals("Test Pack", pack.name)
        assertEquals("wikipedia", pack.type)
        assertEquals(1024L, pack.sizeBytes)
        assertEquals(PackStatus.AVAILABLE, pack.status)
    }

    @Test
    fun `ContentPack data class copy works`() {
        val pack = ContentPack("id", "name", "type", 0L, "desc", PackStatus.AVAILABLE)
        val downloaded = pack.copy(status = PackStatus.DOWNLOADED)
        assertEquals(PackStatus.DOWNLOADED, downloaded.status)
        assertEquals(PackStatus.AVAILABLE, pack.status)
    }
}
