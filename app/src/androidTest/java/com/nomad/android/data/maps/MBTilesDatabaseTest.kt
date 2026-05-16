package com.nomad.android.data.maps

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class MBTilesDatabaseTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createDb(): MBTilesDatabase {
        val dbFile = File(tempFolder.newFolder(), "test.mbtiles")
        val db = MBTilesDatabase(dbFile.absolutePath)
        db.open()
        return db
    }

    @Test
    fun writeAndReadTileRoundTrip() {
        val db = createDb()
        val tileData = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
        db.insertTile(10, 5, 3, tileData)
        val read = db.getTile(10, 5, 3)
        assertArrayEquals(tileData, read)
        db.close()
    }

    @Test
    fun getTileReturnsNullForMissingTile() {
        val db = createDb()
        assertNull(db.getTile(0, 0, 0))
        db.close()
    }

    @Test
    fun metadataRoundTrip() {
        val db = createDb()
        db.setMetadata("name", "Test Region")
        db.setMetadata("bounds", "-2.36,48.85,2.36,48.87")
        assertEquals("Test Region", db.getMetadata("name"))
        assertEquals("-2.36,48.85,2.36,48.87", db.getMetadata("bounds"))
        assertNull(db.getMetadata("nonexistent"))
        db.close()
    }

    @Test
    fun getTileCountReturnsCorrectCount() {
        val db = createDb()
        assertEquals(0, db.getTileCount())
        db.insertTile(10, 5, 3, byteArrayOf(1))
        db.insertTile(10, 6, 3, byteArrayOf(2))
        db.insertTile(10, 7, 3, byteArrayOf(3))
        assertEquals(3, db.getTileCount())
        db.close()
    }

    @Test
    fun overwriteExistingTile() {
        val db = createDb()
        db.insertTile(10, 5, 3, byteArrayOf(1))
        db.insertTile(10, 5, 3, byteArrayOf(2))
        assertEquals(1, db.getTileCount())
        assertArrayEquals(byteArrayOf(2), db.getTile(10, 5, 3))
        db.close()
    }
}
