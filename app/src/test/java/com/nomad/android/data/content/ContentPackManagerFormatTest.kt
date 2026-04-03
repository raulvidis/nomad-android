package com.nomad.android.data.content

import org.junit.Assert.*
import org.junit.Test

class ContentPackManagerFormatTest {

    @Test
    fun `formatSize formats gigabytes`() {
        val manager = ContentPackManager(null, null)
        assertEquals("1.0 GB", manager.formatSize(1_073_741_824L))
        assertEquals("32.0 GB", manager.formatSize(32L * 1024 * 1024 * 1024))
        assertEquals("30.0 GB", manager.formatSize(32212254720L))
    }

    @Test
    fun `formatSize formats megabytes`() {
        val manager = ContentPackManager(null, null)
        assertEquals("1.0 MB", manager.formatSize(1_048_576L))
        assertEquals("500.0 MB", manager.formatSize(500L * 1024 * 1024))
    }

    @Test
    fun `formatSize formats bytes when under 1MB`() {
        val manager = ContentPackManager(null, null)
        assertEquals("0 bytes", manager.formatSize(0L))
        assertEquals("500 bytes", manager.formatSize(500L))
        assertEquals("1048575 bytes", manager.formatSize(1_048_575L))
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
    fun `ContentPack data class equality`() {
        val pack1 = ContentPack("id1", "Pack 1", "wikipedia", 1024L, "Test", PackStatus.AVAILABLE)
        val pack2 = ContentPack("id1", "Pack 1", "wikipedia", 1024L, "Test", PackStatus.AVAILABLE)
        assertEquals(pack1, pack2)
    }

    @Test
    fun `ContentPack data class copy preserves fields`() {
        val pack = ContentPack("id", "Pack", "type", 1024L, "Desc", PackStatus.AVAILABLE)
        val downloading = pack.copy(status = PackStatus.DOWNLOADING)
        assertEquals(PackStatus.DOWNLOADING, downloading.status)
        assertEquals("id", downloading.id)
        assertEquals("Pack", downloading.name)
    }
}
