# Navbar Fix + Offline Maps Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the navbar text overflow bug and add full-screen offline maps with live position tracking to the Maps screen.

**Architecture:** The navbar fix is a small change to `TerminalBottomNav` — only show labels for the selected tab. The offline maps feature replaces the text-only Maps screen with a full-screen MapLibre `MapView`, backed by a new `OfflineTileManager` that downloads and stores OSM raster tiles as MBTiles (SQLite). Floating overlay panels in terminal style provide GPS coords, tracking controls, and region download UI.

**Tech Stack:** MapLibre GL Android SDK 11.8.4, OkHttp 4.12.0, Google Play Services Location 21.3.0, Room/SQLite for MBTiles storage, Jetpack Compose for overlays.

---

## Task 1: Fix Navbar Text Overflow

**Files:**
- Modify: `app/src/main/java/com/nomad/android/ui/theme/TerminalEffects.kt:266-287`

- [ ] **Step 1: Update `TerminalBottomNav` to show label only for selected tab**

In `TerminalEffects.kt`, replace the `Column` content inside the `Box` (lines 266-287) with:

```kotlin
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                        tint = if (selected) TerminalGreen else TerminalGreenDim,
                        modifier = Modifier.size(24.dp),
                    )
                    if (selected) {
                        Text(
                            text = tab.label.uppercase(),
                            maxLines = 1,
                            fontFamily = androidx.compose.ui.text.font.FontFamily(
                                androidx.compose.ui.text.font.Font(
                                    R.font.jetbrains_mono_medium,
                                    FontWeight.Medium,
                                ),
                            ),
                            fontSize = 10.sp,
                            color = TerminalGreen,
                        )
                    }
                }
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/nomad/android/ui/theme/TerminalEffects.kt
git commit -m "fix: show navbar labels only for selected tab to prevent text overflow"
```

---

## Task 2: Create TileCalculator

**Files:**
- Create: `app/src/main/java/com/nomad/android/data/maps/TileCalculator.kt`
- Create: `app/src/test/java/com/nomad/android/data/maps/TileCalculatorTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.nomad.android.data.maps

import org.junit.Assert.*
import org.junit.Test

class TileCalculatorTest {

    @Test
    fun `latLngToTile at zoom 0 returns 0,0`() {
        val (x, y) = TileCalculator.latLngToTile(0.0, 0.0, 0)
        assertEquals(0, x)
        assertEquals(0, y)
    }

    @Test
    fun `latLngToTile at zoom 1 for known location`() {
        val (x, y) = TileCalculator.latLngToTile(48.8566, 2.3522, 1)
        assertEquals(1, x)
        assertEquals(1, y)
    }

    @Test
    fun `latLngToTile equator prime_meridian zoom 2`() {
        val (x, y) = TileCalculator.latLngToTile(0.0, 0.0, 2)
        assertEquals(2, x)
        assertEquals(2, y)
    }

    @Test
    fun `getTilesForBounds returns correct count for small area`() {
        val tiles = TileCalculator.getTilesForBounds(
            north = 48.87,
            south = 48.85,
            east = 2.36,
            west = 2.34,
            minZoom = 14,
            maxZoom = 14
        )
        assertFalse(tiles.isEmpty())
        assertTrue(tiles.size > 1)
    }

    @Test
    fun `getTilesForBounds across zoom levels increases tile count`() {
        val tilesZ14 = TileCalculator.getTilesForBounds(
            north = 48.87, south = 48.85, east = 2.36, west = 2.34,
            minZoom = 14, maxZoom = 14
        )
        val tilesZ14to15 = TileCalculator.getTilesForBounds(
            north = 48.87, south = 48.85, east = 2.36, west = 2.34,
            minZoom = 14, maxZoom = 15
        )
        assertTrue(tilesZ14to15.size > tilesZ14.size)
    }

    @Test
    fun `estimateSizeBytes is reasonable`() {
        val size = TileCalculator.estimateSizeBytes(100)
        assertEquals(100 * 15_000L, size)
    }

    @Test
    fun `latLngToTile clamps latitude at 85 degrees`() {
        val (x1, y1) = TileCalculator.latLngToTile(89.0, 0.0, 10)
        val (x2, y2) = TileCalculator.latLngToTile(85.0511, 0.0, 10)
        assertEquals(y1, y2)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.nomad.android.data.maps.TileCalculatorTest"`
Expected: FAIL — `TileCalculator` class does not exist

- [ ] **Step 3: Implement TileCalculator**

```kotlin
package com.nomad.android.data.maps

import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.max
import kotlin.math.pow

object TileCalculator {

    data class TileCoord(val x: Int, val y: Int, val z: Int)

    fun latLngToTile(lat: Double, lng: Double, zoom: Int): Pair<Int, Int> {
        val clampedLat = lat.coerceIn(-85.0511, 85.0511)
        val n = 2.0.pow(zoom.toDouble())
        val x = floor((lng + 180.0) / 360.0 * n).toInt().coerceIn(0, n.toInt() - 1)
        val latRad = Math.toRadians(clampedLat)
        val y = floor((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / Math.PI) / 2.0 * n).toInt()
            .coerceIn(0, n.toInt() - 1)
        return x to y
    }

    fun getTilesForBounds(
        north: Double,
        south: Double,
        east: Double,
        west: Double,
        minZoom: Int,
        maxZoom: Int
    ): List<TileCoord> {
        val tiles = mutableListOf<TileCoord>()
        for (z in minZoom..maxZoom) {
            val (minX, minY) = latLngToTile(north, west, z)
            val (maxX, maxY) = latLngToTile(south, east, z)
            for (x in minOf(minX, maxX)..maxOf(minX, maxX)) {
                for (y in minOf(minY, maxY)..maxOf(minY, maxY)) {
                    tiles.add(TileCoord(x, y, z))
                }
            }
        }
        return tiles
    }

    fun estimateSizeBytes(tileCount: Int, avgTileBytes: Int = 15_000): Long {
        return tileCount.toLong() * avgTileBytes
    }

    fun tileUrl(x: Int, y: Int, z: Int): String =
        "https://tile.openstreetmap.org/$z/$x/$y.png"
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.nomad.android.data.maps.TileCalculatorTest"`
Expected: All 7 tests PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nomad/android/data/maps/TileCalculator.kt app/src/test/java/com/nomad/android/data/maps/TileCalculatorTest.kt
git commit -m "feat: add TileCalculator for OSM tile coordinate math"
```

---

## Task 3: Create MBTilesWriter and MBTilesReader

**Files:**
- Create: `app/src/main/java/com/nomad/android/data/maps/MBTilesDatabase.kt`
- Create: `app/src/test/java/com/nomad/android/data/maps/MBTilesDatabaseTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.nomad.android.data.maps

import org.junit.Assert.*
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.Rule

class MBTilesDatabaseTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `write and read a tile round-trip`() {
        val dbFile = tempFolder.newFile("test.mbtiles")
        val db = MBTilesDatabase(dbFile.absolutePath)
        db.open()

        val tileData = byteArrayOf(0x89, 0x50, 0x4E, 0x47)
        db.insertTile(10, 5, 3, tileData)

        val read = db.getTile(10, 5, 3)
        assertArrayEquals(tileData, read)

        db.close()
    }

    @Test
    fun `getTile returns null for missing tile`() {
        val dbFile = tempFolder.newFile("test2.mbtiles")
        val db = MBTilesDatabase(dbFile.absolutePath)
        db.open()

        assertNull(db.getTile(0, 0, 0))

        db.close()
    }

    @Test
    fun `setMetadata and getMetadata round-trip`() {
        val dbFile = tempFolder.newFile("test3.mbtiles")
        val db = MBTilesDatabase(dbFile.absolutePath)
        db.open()

        db.setMetadata("name", "Test Region")
        db.setMetadata("bounds", "-2.36,48.85,2.36,48.87")

        assertEquals("Test Region", db.getMetadata("name"))
        assertEquals("-2.36,48.85,2.36,48.87", db.getMetadata("bounds"))
        assertNull(db.getMetadata("nonexistent"))

        db.close()
    }

    @Test
    fun `getTileCount returns correct count`() {
        val dbFile = tempFolder.newFile("test4.mbtiles")
        val db = MBTilesDatabase(dbFile.absolutePath)
        db.open()

        assertEquals(0, db.getTileCount())

        db.insertTile(10, 5, 3, byteArrayOf(1))
        db.insertTile(10, 6, 3, byteArrayOf(2))
        db.insertTile(10, 7, 3, byteArrayOf(3))

        assertEquals(3, db.getTileCount())

        db.close()
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.nomad.android.data.maps.MBTilesDatabaseTest"`
Expected: FAIL — `MBTilesDatabase` class does not exist

- [ ] **Step 3: Implement MBTilesDatabase**

```kotlin
package com.nomad.android.data.maps

import android.database.sqlite.SQLiteDatabase
import java.io.File

class MBTilesDatabase(private val path: String) {

    private var db: SQLiteDatabase? = null

    fun open() {
        val file = File(path)
        file.parentFile?.mkdirs()
        db = SQLiteDatabase.openOrCreateDatabase(file, null).apply {
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS metadata (
                    name TEXT PRIMARY KEY,
                    value TEXT
                )
                """.trimIndent()
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS tiles (
                    zoom_level INTEGER,
                    tile_column INTEGER,
                    tile_row INTEGER,
                    tile_data BLOB,
                    PRIMARY KEY (zoom_level, tile_column, tile_row)
                )
                """.trimIndent()
            )
        }
    }

    fun close() {
        db?.close()
        db = null
    }

    fun insertTile(z: Int, x: Int, y: Int, data: ByteArray) {
        val tmsY = (1 shl z) - 1 - y
        db?.compileStatement(
            "INSERT OR REPLACE INTO tiles (zoom_level, tile_column, tile_row, tile_data) VALUES (?, ?, ?, ?)"
        )?.use { stmt ->
            stmt.bindLong(1, z.toLong())
            stmt.bindLong(2, x.toLong())
            stmt.bindLong(3, tmsY.toLong())
            stmt.bindBlob(4, data)
            stmt.executeInsert()
        }
    }

    fun getTile(z: Int, x: Int, y: Int): ByteArray? {
        val tmsY = (1 shl z) - 1 - y
        val cursor = db?.rawQuery(
            "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?",
            arrayOf(z.toString(), x.toString(), tmsY.toString())
        )
        return cursor?.use {
            if (it.moveToFirst()) it.getBlob(0) else null
        }
    }

    fun setMetadata(name: String, value: String) {
        db?.execSQL(
            "INSERT OR REPLACE INTO metadata (name, value) VALUES (?, ?)",
            arrayOf(name, value)
        )
    }

    fun getMetadata(name: String): String? {
        val cursor = db?.rawQuery(
            "SELECT value FROM metadata WHERE name = ?",
            arrayOf(name)
        )
        return cursor?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }

    fun getTileCount(): Int {
        val cursor = db?.rawQuery("SELECT COUNT(*) FROM tiles", null)
        return cursor?.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        } ?: 0
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.nomad.android.data.maps.MBTilesDatabaseTest"`
Expected: All 4 tests PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nomad/android/data/maps/MBTilesDatabase.kt app/src/test/java/com/nomad/android/data/maps/MBTilesDatabaseTest.kt
git commit -m "feat: add MBTilesDatabase for offline tile storage in MBTiles format"
```

---

## Task 4: Create OfflineTileManager

**Files:**
- Create: `app/src/main/java/com/nomad/android/data/maps/OfflineTileManager.kt`
- Create: `app/src/test/java/com/nomad/android/data/maps/OfflineTileManagerTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.nomad.android.data.maps

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OfflineTileManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createManager(): OfflineTileManager {
        val dir = tempFolder.newFolder("mapTiles")
        return OfflineTileManager(tilesDir = dir, tileCalculator = TileCalculator)
    }

    @Test
    fun `getDownloadedRegions returns empty initially`() = runTest {
        val manager = createManager()
        assertTrue(manager.getDownloadedRegions().isEmpty())
    }

    @Test
    fun `deleteRegion removes a downloaded region`() = runTest {
        val manager = createManager()
        val tiles = listOf(TileCalculator.TileCoord(0, 0, 0))
        val regionId = manager.createRegion("test", 48.0, -48.0, 2.0, -2.0, 0, 0)
        assertNotNull(regionId)
        assertEquals(1, manager.getDownloadedRegions().size)
        manager.deleteRegion(regionId!!)
        assertTrue(manager.getDownloadedRegions().isEmpty())
    }

    @Test
    fun `hasTilesForBounds returns false when no tiles`() = runTest {
        val manager = createManager()
        assertFalse(manager.hasTilesForBounds(48.0, -48.0, 2.0, -2.0, 0))
    }

    @Test
    fun `getStoredTileCount returns zero initially`() = runTest {
        val manager = createManager()
        val regionId = manager.createRegion("test", 48.0, -48.0, 2.0, -2.0, 0, 0)
        assertEquals(0, manager.getStoredTileCount(regionId!!))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.nomad.android.data.maps.OfflineTileManagerTest"`
Expected: FAIL — `OfflineTileManager` class does not exist

- [ ] **Step 3: Implement OfflineTileManager**

```kotlin
package com.nomad.android.data.maps

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.UUID

data class OfflineRegion(
    val id: String,
    val name: String,
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double,
    val minZoom: Int,
    val maxZoom: Int,
    val tileCount: Int,
    val sizeBytes: Long,
    val downloadedAt: Long
)

data class DownloadProgress(
    val downloaded: Int,
    val total: Int,
    val bytesDownloaded: Long,
    val estimatedTotalBytes: Long,
    val isComplete: Boolean,
    val error: String? = null
)

class OfflineTileManager(
    private val tilesDir: File,
    private val tileCalculator: TileCalculator = TileCalculator,
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    private val databases = mutableMapOf<String, MBTilesDatabase>()

    init {
        tilesDir.mkdirs()
    }

    fun createRegion(
        name: String,
        north: Double,
        south: Double,
        east: Double,
        west: Double,
        minZoom: Int,
        maxZoom: Int
    ): String {
        val id = UUID.randomUUID().toString().take(8)
        val dbFile = File(tilesDir, "$id.mbtiles")
        val db = MBTilesDatabase(dbFile.absolutePath)
        db.open()
        db.setMetadata("name", name)
        db.setMetadata("bounds", "$west,$south,$east,$north")
        db.setMetadata("minzoom", minZoom.toString())
        db.setMetadata("maxzoom", maxZoom.toString())
        db.setMetadata("id", id)
        db.setMetadata("created", System.currentTimeMillis().toString())
        databases[id] = db
        return id
    }

    fun downloadRegion(
        regionId: String,
        north: Double,
        south: Double,
        east: Double,
        west: Double,
        minZoom: Int,
        maxZoom: Int
    ): Flow<DownloadProgress> = flow {
        val db = databases[regionId] ?: run {
            val dbFile = File(tilesDir, "$regionId.mbtiles")
            if (!dbFile.exists()) {
                emit(DownloadProgress(0, 0, 0, 0, false, "Region not found"))
                return@flow
            }
            val newDb = MBTilesDatabase(dbFile.absolutePath)
            newDb.open()
            databases[regionId] = newDb
            newDb
        }

        val tiles = tileCalculator.getTilesForBounds(north, south, east, west, minZoom, maxZoom)
        val total = tiles.size
        var downloaded = 0
        var bytesDownloaded = 0L

        emit(DownloadProgress(0, total, 0, tileCalculator.estimateSizeBytes(total), false))

        for (tile in tiles) {
            val existing = db.getTile(tile.z, tile.x, tile.y)
            if (existing != null) {
                downloaded++
                bytesDownloaded += existing.size
                emit(DownloadProgress(downloaded, total, bytesDownloaded, tileCalculator.estimateSizeBytes(total), false))
                continue
            }

            try {
                val url = tileCalculator.tileUrl(tile.x, tile.y, tile.z)
                val request = Request.Builder().url(url)
                    .header("User-Agent", "NOMAD-Android/1.0")
                    .build()
                val response = httpClient.newCall(request).execute()
                val body = response.body?.bytes()
                if (response.isSuccessful && body != null) {
                    db.insertTile(tile.z, tile.x, tile.y, body)
                    bytesDownloaded += body.size
                }
                response.close()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to download tile ${tile.z}/${tile.x}/${tile.y}", e)
            }

            downloaded++
            emit(DownloadProgress(downloaded, total, bytesDownloaded, tileCalculator.estimateSizeBytes(total), false))
        }

        db.setMetadata("tilecount", downloaded.toString())
        db.setMetadata("sizebytes", bytesDownloaded.toString())
        emit(DownloadProgress(downloaded, total, bytesDownloaded, bytesDownloaded, true))
    }.flowOn(Dispatchers.IO)

    fun getDownloadedRegions(): List<OfflineRegion> {
        return tilesDir.listFiles { file -> file.extension == "mbtiles" }?.mapNotNull { file ->
            val db = databases[file.nameWithoutExtension] ?: run {
                val newDb = MBTilesDatabase(file.absolutePath)
                try {
                    newDb.open()
                } catch (_: Exception) {
                    return@mapNotNull null
                }
                databases[file.nameWithoutExtension] = newDb
                newDb
            }
            val name = db.getMetadata("name") ?: return@mapNotNull null
            val bounds = db.getMetadata("bounds") ?: return@mapNotNull null
            val parts = bounds.split(",")
            if (parts.size != 4) return@mapNotNull null
            OfflineRegion(
                id = db.getMetadata("id") ?: file.nameWithoutExtension,
                name = name,
                west = parts[0].toDoubleOrNull() ?: 0.0,
                south = parts[1].toDoubleOrNull() ?: 0.0,
                east = parts[2].toDoubleOrNull() ?: 0.0,
                north = parts[3].toDoubleOrNull() ?: 0.0,
                minZoom = db.getMetadata("minzoom")?.toIntOrNull() ?: 0,
                maxZoom = db.getMetadata("maxzoom")?.toIntOrNull() ?: 0,
                tileCount = db.getMetadata("tilecount")?.toIntOrNull() ?: db.getTileCount(),
                sizeBytes = db.getMetadata("sizebytes")?.toLongOrNull() ?: file.length(),
                downloadedAt = db.getMetadata("created")?.toLongOrNull() ?: 0L
            )
        } ?: emptyList()
    }

    fun getTile(z: Int, x: Int, y: Int): ByteArray? {
        for (db in databases.values) {
            val tile = db.getTile(z, x, y)
            if (tile != null) return tile
        }
        for (file in tilesDir.listFiles { f -> f.extension == "mbtiles" } ?: emptyArray()) {
            val db = databases[file.nameWithoutExtension] ?: continue
            val tile = db.getTile(z, x, y)
            if (tile != null) return tile
        }
        return null
    }

    fun deleteRegion(regionId: String) {
        databases[regionId]?.close()
        databases.remove(regionId)
        File(tilesDir, "$regionId.mbtiles").delete()
    }

    fun hasTilesForBounds(north: Double, south: Double, east: Double, west: Double, zoom: Int): Boolean {
        val regions = getDownloadedRegions()
        return regions.any { region ->
            south >= region.south && north <= region.north &&
                west >= region.west && east <= region.east &&
                zoom in region.minZoom..region.maxZoom
        }
    }

    fun getStoredTileCount(regionId: String): Int {
        return databases[regionId]?.getTileCount() ?: 0
    }

    companion object {
        private const val TAG = "OfflineTileManager"
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.nomad.android.data.maps.OfflineTileManagerTest"`
Expected: All 4 tests PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nomad/android/data/maps/OfflineTileManager.kt app/src/test/java/com/nomad/android/data/maps/OfflineTileManagerTest.kt
git commit -m "feat: add OfflineTileManager for downloading and storing offline map tiles"
```

---

## Task 5: Create Hilt module for OfflineTileManager

**Files:**
- Create: `app/src/main/java/com/nomad/android/di/MapsModule.kt`

- [ ] **Step 1: Create MapsModule**

```kotlin
package com.nomad.android.di

import android.content.Context
import com.nomad.android.data.maps.OfflineTileManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapsModule {

    @Provides
    @Singleton
    fun provideOfflineTileManager(@ApplicationContext context: Context): OfflineTileManager {
        val tilesDir = File(context.filesDir, "mapTiles")
        return OfflineTileManager(tilesDir = tilesDir)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/nomad/android/di/MapsModule.kt
git commit -m "feat: add Hilt MapsModule for OfflineTileManager"
```

---

## Task 6: Rewrite MapsScreen with full-screen MapLibre map

**Files:**
- Modify: `app/src/main/java/com/nomad/android/ui/maps/MapsScreen.kt`
- Modify: `app/src/main/java/com/nomad/android/ui/maps/MapsViewModel.kt`

- [ ] **Step 1: Update MapsViewModel with offline tile state**

Replace the entire `MapsViewModel.kt` with:

```kotlin
package com.nomad.android.ui.maps

import android.app.Application
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nomad.android.data.Result
import com.nomad.android.data.local.entity.LocationSavedPointEntity
import com.nomad.android.data.local.entity.LocationSnapshotEntity
import com.nomad.android.data.maps.DownloadProgress
import com.nomad.android.data.maps.OfflineRegion
import com.nomad.android.data.maps.OfflineTileManager
import com.nomad.android.data.maps.TileCalculator
import com.nomad.android.data.repository.LocationRepository
import com.nomad.android.data.repository.MapsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import javax.inject.Inject

data class MapsData(
    val isMapInitialized: Boolean = false,
    val currentLocationText: String = "NO FIX",
    val currentLatitude: Double? = null,
    val currentLongitude: Double? = null,
    val isTracking: Boolean = false,
    val savedPoints: List<LocationSavedPointEntity> = emptyList(),
    val snapshotCount: Int = 0,
    val hasLocationPermission: Boolean = false,
    val downloadedRegions: List<OfflineRegion> = emptyList(),
    val isDownloading: Boolean = false,
    val downloadProgress: DownloadProgress? = null,
    val isSelectingRegion: Boolean = false,
    val selectedBounds: LatLngBounds? = null,
    val selectedMinZoom: Int = 12,
    val selectedMaxZoom: Int = 15,
    val isAutoCenter: Boolean = true,
    val showSavedPanel: Boolean = false,
    val showRegionList: Boolean = false,
    val regionName: String? = null
)

data class MapsUiState(
    val data: MapsData = MapsData(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MapsViewModel @Inject constructor(
    private val mapsRepository: MapsRepository,
    private val locationRepository: LocationRepository,
    private val offlineTileManager: OfflineTileManager,
    application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapsUiState(isLoading = true))
    val uiState: StateFlow<MapsUiState> = _uiState.asStateFlow()

    private val _locationPermissionGranted = MutableStateFlow(false)

    init {
        val hasPermission = ContextCompat.checkSelfPermission(
            application,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            _locationPermissionGranted.value = true
            _uiState.update { it.copy(data = it.data.copy(hasLocationPermission = true)) }
            locationRepository.requestCurrentLocation()
        }

        loadMapData()
        observeLocation()
    }

    private fun observeLocation() {
        viewModelScope.launch {
            combine(
                locationRepository.currentLocation,
                locationRepository.isTracking,
                locationRepository.savedPoints,
                locationRepository.recentSnapshots
            ) { location, isTracking, savedPoints, snapshots ->
                val locText = location?.let {
                    "%.6f, %.6f".format(it.latitude, it.longitude)
                } ?: "NO FIX"

                _uiState.update { state ->
                    state.copy(
                        data = state.data.copy(
                            currentLocationText = locText,
                            currentLatitude = location?.latitude,
                            currentLongitude = location?.longitude,
                            isTracking = isTracking,
                            savedPoints = savedPoints,
                            snapshotCount = snapshots.size
                        )
                    )
                }
            }.collect {}
        }
    }

    fun loadMapData() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val regions = offlineTileManager.getDownloadedRegions()
            val regionName = mapsRepository.getDownloadedRegionName()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    data = it.data.copy(
                        isMapInitialized = true,
                        downloadedRegions = regions,
                        regionName = regionName
                    )
                )
            }
        }
    }

    fun setLocationPermissionGranted(granted: Boolean) {
        _locationPermissionGranted.value = granted
        _uiState.update { it.copy(data = it.data.copy(hasLocationPermission = granted)) }
        if (granted) locationRepository.requestCurrentLocation()
    }

    fun startTracking() {
        if (_locationPermissionGranted.value) locationRepository.startTracking()
    }

    fun stopTracking() {
        locationRepository.stopTracking()
    }

    fun saveLocation(name: String, notes: String) {
        viewModelScope.launch { locationRepository.saveCurrentLocation(name, notes) }
    }

    fun deleteSavedPoint(id: String) {
        viewModelScope.launch { locationRepository.deleteSavedPoint(id) }
    }

    fun requestCurrentLocation() {
        if (_locationPermissionGranted.value) locationRepository.requestCurrentLocation()
    }

    fun toggleAutoCenter() {
        _uiState.update { it.copy(data = it.data.copy(isAutoCenter = !it.data.isAutoCenter)) }
    }

    fun toggleSavedPanel() {
        _uiState.update { it.copy(data = it.data.copy(showSavedPanel = !it.data.showSavedPanel)) }
    }

    fun toggleRegionList() {
        _uiState.update { it.copy(data = it.data.copy(showRegionList = !it.data.showRegionList)) }
    }

    fun startRegionSelection() {
        _uiState.update { it.copy(data = it.data.copy(isSelectingRegion = true)) }
    }

    fun cancelRegionSelection() {
        _uiState.update { it.copy(data = it.data.copy(isSelectingRegion = false, selectedBounds = null)) }
    }

    fun setSelectedBounds(bounds: LatLngBounds) {
        _uiState.update { it.copy(data = it.data.copy(selectedBounds = bounds)) }
    }

    fun setZoomRange(min: Int, max: Int) {
        _uiState.update { it.copy(data = it.data.copy(selectedMinZoom = min, selectedMaxZoom = max)) }
    }

    fun startDownload(regionName: String) {
        val bounds = _uiState.value.data.selectedBounds ?: return
        val minZoom = _uiState.value.data.selectedMinZoom
        val maxZoom = _uiState.value.data.selectedMaxZoom

        val tiles = TileCalculator.getTilesForBounds(
            bounds.latNorth, bounds.latSouth, bounds.lonEast, bounds.lonWest,
            minZoom, maxZoom
        )
        val estimatedSize = TileCalculator.estimateSizeBytes(tiles.size)

        val id = offlineTileManager.createRegion(
            regionName.ifBlank { "Region" },
            bounds.latNorth, bounds.latSouth, bounds.lonEast, bounds.lonWest,
            minZoom, maxZoom
        )

        _uiState.update {
            it.copy(data = it.data.copy(isDownloading = true, isSelectingRegion = false))
        }

        viewModelScope.launch {
            offlineTileManager.downloadRegion(
                id, bounds.latNorth, bounds.latSouth, bounds.lonEast, bounds.lonWest,
                minZoom, maxZoom
            ).collect { progress ->
                _uiState.update {
                    it.copy(data = it.data.copy(downloadProgress = progress))
                }
            }
            val regions = offlineTileManager.getDownloadedRegions()
            _uiState.update {
                it.copy(
                    data = it.data.copy(
                        isDownloading = false,
                        downloadProgress = null,
                        downloadedRegions = regions
                    )
                )
            }
        }
    }

    fun deleteRegion(regionId: String) {
        offlineTileManager.deleteRegion(regionId)
        _uiState.update {
            it.copy(data = it.data.copy(
                downloadedRegions = offlineTileManager.getDownloadedRegions()
            ))
        }
    }

    fun getTileProvider(): OfflineTileProvider? {
        return OfflineTileProvider(offlineTileManager)
    }
}

class OfflineTileProvider(private val tileManager: OfflineTileManager) {
    fun getTile(z: Int, x: Int, y: Int): android.graphics.Bitmap? {
        val data = tileManager.getTile(z, x, y) ?: return null
        return BitmapFactory.decodeByteArray(data, 0, data.size)
    }
}
```

- [ ] **Step 2: Rewrite MapsScreen with full-screen map and overlays**

Replace the entire `MapsScreen.kt` with:

```kotlin
package com.nomad.android.ui.maps

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filledLayers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nomad.android.R
import com.nomad.android.data.local.entity.LocationSavedPointEntity
import com.nomad.android.ui.theme.TerminalAmber
import com.nomad.android.ui.theme.TerminalBg
import com.nomad.android.ui.theme.TerminalDanger
import com.nomad.android.ui.theme.TerminalGreen
import com.nomad.android.ui.theme.TerminalGreenDim
import com.nomad.android.ui.theme.TerminalSurface
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapView.OnMapChangedListener
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MapsScreen(
    viewModel: MapsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions.values.all { it }
        viewModel.setLocationPermissionGranted(granted)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.data.isMapInitialized) {
            MapViewContainer(
                data = uiState.data,
                viewModel = viewModel,
            )
        }

        CoordinatesOverlay(data = uiState.data)

        MapControlsOverlay(
            data = uiState.data,
            onZoomIn = {},
            onZoomOut = {},
            onAutoCenter = { viewModel.toggleAutoCenter() },
            onDownload = { viewModel.startRegionSelection() },
            onSavedPoints = { viewModel.toggleSavedPanel() },
            onRegions = { viewModel.toggleRegionList() },
            onRequestPermission = {
                locationPermissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            },
        )

        AnimatedVisibility(
            visible = uiState.data.showSavedPanel,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            SavedPointsPanel(
                points = uiState.data.savedPoints,
                onDelete = { viewModel.deleteSavedPoint(it) },
                onClose = { viewModel.toggleSavedPanel() },
            )
        }

        AnimatedVisibility(
            visible = uiState.data.showRegionList,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            RegionsPanel(
                regions = uiState.data.downloadedRegions,
                onDelete = { viewModel.deleteRegion(it) },
                onClose = { viewModel.toggleRegionList() },
            )
        }

        if (uiState.data.isSelectingRegion) {
            RegionSelectionOverlay(
                viewModel = viewModel,
            )
        }

        if (uiState.data.isDownloading) {
            DownloadProgressOverlay(progress = uiState.data.downloadProgress)
        }
    }
}

@Composable
private fun MapViewContainer(
    data: MapsData,
    viewModel: MapsViewModel,
) {
    val context = LocalContext.current
    val tileProvider = remember { viewModel.getTileProvider() }

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

    DisposableEffect(Unit) {
        MapLibre.getInstance(context)
        onDispose {
            mapView?.onDestroy()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val mv = MapView(ctx)
            mapView = mv
            mv.getMapAsync { map ->
                mapLibreMap = map
                map.uiSettings.isCompassEnabled = false
                map.uiSettings.isLogoEnabled = false
                map.uiSettings.isAttributionEnabled = false

                val styleBuilder = Style.Builder()

                val hasOffline = data.downloadedRegions.isNotEmpty()

                if (hasOffline && tileProvider != null) {
                    val tileSet = TileSet("tileset", "offline://tile/{z}/{x}/{y}.png")
                        .withMinZoom(0.0)
                        .withMaxZoom(19.0)
                    styleBuilder.withSource(RasterSource("offline-source", tileSet, 256))
                    styleBuilder.withLayer(RasterLayer("offline-layer", "offline-source"))
                } else {
                    val tileSet = TileSet("tileset", "https://tile.openstreetmap.org/{z}/{x}/{y}.png")
                        .withMinZoom(0.0)
                        .withMaxZoom(19.0)
                    styleBuilder.withSource(RasterSource("osm-source", tileSet, 256))
                    styleBuilder.withLayer(RasterLayer("osm-layer", "osm-source"))
                }

                map.setStyle(styleBuilder)

                val initialPos = if (data.currentLatitude != null && data.currentLongitude != null) {
                    CameraPosition.Builder()
                        .target(LatLng(data.currentLatitude, data.currentLongitude))
                        .zoom(12.0)
                        .build()
                } else {
                    CameraPosition.Builder()
                        .target(LatLng(48.8566, 2.3522))
                        .zoom(4.0)
                        .build()
                }
                map.cameraPosition = initialPos
            }
            mv.onCreate(null)
            mv.onStart()
            mv.onResume()
            mv
        },
        update = { mv ->
            val lat = data.currentLatitude
            val lon = data.currentLongitude
            if (data.isAutoCenter && lat != null && lon != null) {
                mapLibreMap?.animateCamera(
                    CameraUpdateFactory.newLatLng(LatLng(lat, lon)),
                    1000
                )
            }
        }
    )
}

@Composable
private fun CoordinatesOverlay(data: MapsData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 12.dp, end = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .background(TerminalBg.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                .border(1.dp, TerminalGreenDim, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = data.currentLocationText,
                color = if (data.currentLocationText == "NO FIX") TerminalGreenDim else TerminalGreen,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                ),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (data.isTracking) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(TerminalGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

@Composable
private fun MapControlsOverlay(
    data: MapsData,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onAutoCenter: () -> Unit,
    onDownload: () -> Unit,
    onSavedPoints: () -> Unit,
    onRegions: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 12.dp, bottom = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(TerminalBg.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .border(1.dp, TerminalGreenDim, RoundedCornerShape(8.dp))
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MapControlButton(
                icon = { Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom In", tint = TerminalGreen, modifier = Modifier.size(20.dp)) },
                onClick = onZoomIn,
            )
            MapControlButton(
                icon = { Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom Out", tint = TerminalGreen, modifier = Modifier.size(20.dp)) },
                onClick = onZoomOut,
            )
            MapControlButton(
                icon = {
                    Icon(
                        if (data.isAutoCenter) Icons.Filled.GpsFixed else Icons.Filled.GpsOff,
                        contentDescription = "Auto Center",
                        tint = if (data.isAutoCenter) TerminalGreen else TerminalGreenDim,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = onAutoCenter,
            )
            MapControlButton(
                icon = { Icon(Icons.Filled.MyLocation, contentDescription = "Permission", tint = TerminalAmber, modifier = Modifier.size(20.dp)) },
                onClick = onRequestPermission,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp)
                .background(TerminalBg.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .border(1.dp, TerminalGreenDim, RoundedCornerShape(8.dp))
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MapControlButton(
                icon = { Icon(Icons.Filled.Add, contentDescription = "Download Region", tint = TerminalGreen, modifier = Modifier.size(20.dp)) },
                onClick = onDownload,
            )
            MapControlButton(
                icon = { Icon(Icons.Filled.Bookmark, contentDescription = "Saved Points", tint = TerminalGreen, modifier = Modifier.size(20.dp)) },
                onClick = onSavedPoints,
            )
            MapControlButton(
                icon = { Icon(Icons.FilledLayers, contentDescription = "Regions", tint = TerminalGreen, modifier = Modifier.size(20.dp)) },
                onClick = onRegions,
            )
        }
    }
}

@Composable
private fun MapControlButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

@Composable
private fun SavedPointsPanel(
    points: List<LocationSavedPointEntity>,
    onDelete: (String) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 60.dp)
            .background(TerminalBg.copy(alpha = 0.95f), RoundedCornerShape(8.dp))
            .border(1.dp, TerminalGreenDim, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "SAVED LOCATIONS (${points.size})",
                    color = TerminalGreen,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
                    ),
                    fontSize = 14.sp,
                )
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = TerminalGreenDim,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onClose),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (points.isEmpty()) {
                Text(
                    text = "No saved locations",
                    color = TerminalGreenDim,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 12.sp,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(points, key = { it.id }) { point ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = point.name.uppercase(),
                                    color = TerminalGreen,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                                    ),
                                    fontSize = 12.sp,
                                )
                                Text(
                                    text = "%.6f, %.6f".format(point.latitude, point.longitude),
                                    color = TerminalGreenDim,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                                    ),
                                    fontSize = 10.sp,
                                )
                            }
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = TerminalDanger,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onDelete(point.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegionsPanel(
    regions: List<com.nomad.android.data.maps.OfflineRegion>,
    onDelete: (String) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 60.dp)
            .background(TerminalBg.copy(alpha = 0.95f), RoundedCornerShape(8.dp))
            .border(1.dp, TerminalGreenDim, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "OFFLINE REGIONS (${regions.size})",
                    color = TerminalGreen,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
                    ),
                    fontSize = 14.sp,
                )
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = TerminalGreenDim,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onClose),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (regions.isEmpty()) {
                Text(
                    text = "No offline regions. Tap + to download.",
                    color = TerminalGreenDim,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 12.sp,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(regions, key = { it.id }) { region ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = region.name.uppercase(),
                                    color = TerminalGreen,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                                    ),
                                    fontSize = 12.sp,
                                )
                                Text(
                                    text = "Z${region.minZoom}-${region.maxZoom} | ${region.tileCount} tiles | ${"%.1f".format(region.sizeBytes / 1_048_576.0)}MB",
                                    color = TerminalGreenDim,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                                    ),
                                    fontSize = 10.sp,
                                )
                            }
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = TerminalDanger,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onDelete(region.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegionSelectionOverlay(
    viewModel: MapsViewModel,
) {
    var regionName by remember { mutableStateOf("") }
    var minZoom by remember { mutableStateOf(12) }
    var maxZoom by remember { mutableStateOf(15) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(TerminalBg, RoundedCornerShape(8.dp))
                .border(1.dp, TerminalGreen, RoundedCornerShape(8.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "DOWNLOAD OFFLINE MAP",
                color = TerminalGreen,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
                ),
                fontSize = 16.sp,
            )

            Text(
                text = "Navigate to the area you want to download, then set zoom levels.",
                color = TerminalGreenDim,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                ),
                fontSize = 11.sp,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Min Zoom: $minZoom",
                    color = TerminalGreen,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 13.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(10, 12, 14).forEach { z ->
                        Box(
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (minZoom == z) TerminalGreen else TerminalGreenDim,
                                    RoundedCornerShape(4.dp)
                                )
                                .background(
                                    if (minZoom == z) TerminalGreen.copy(alpha = 0.15f) else TerminalSurface,
                                    RoundedCornerShape(4.dp),
                                )
                                .clickable { minZoom = z; viewModel.setZoomRange(z, maxZoom) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "$z",
                                color = if (minZoom == z) TerminalGreen else TerminalGreenDim,
                                fontFamily = androidx.compose.ui.text.font.FontFamily(
                                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                                ),
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Max Zoom: $maxZoom",
                    color = TerminalGreen,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 13.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(14, 15, 16, 17).forEach { z ->
                        Box(
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (maxZoom == z) TerminalGreen else TerminalGreenDim,
                                    RoundedCornerShape(4.dp)
                                )
                                .background(
                                    if (maxZoom == z) TerminalGreen.copy(alpha = 0.15f) else TerminalSurface,
                                    RoundedCornerShape(4.dp),
                                )
                                .clickable { maxZoom = z; viewModel.setZoomRange(minZoom, z) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "$z",
                                color = if (maxZoom == z) TerminalGreen else TerminalGreenDim,
                                fontFamily = androidx.compose.ui.text.font.FontFamily(
                                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                                ),
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .border(1.dp, TerminalGreenDim, RoundedCornerShape(4.dp))
                        .background(TerminalSurface, RoundedCornerShape(4.dp))
                        .clickable { viewModel.cancelRegionSelection() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "CANCEL",
                        color = TerminalGreenDim,
                        fontFamily = androidx.compose.ui.text.font.FontFamily(
                            androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                        ),
                        fontSize = 13.sp,
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, TerminalGreen, RoundedCornerShape(4.dp))
                        .background(TerminalGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .clickable { viewModel.startDownload(regionName) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "DOWNLOAD",
                        color = TerminalGreen,
                        fontFamily = androidx.compose.ui.text.font.FontFamily(
                            androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
                        ),
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadProgressOverlay(
    progress: com.nomad.android.data.maps.DownloadProgress?,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(TerminalBg, RoundedCornerShape(8.dp))
                .border(1.dp, TerminalAmber, RoundedCornerShape(8.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "DOWNLOADING MAP TILES",
                color = TerminalAmber,
                fontFamily = androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
                ),
                fontSize = 14.sp,
            )
            if (progress != null) {
                val pct = if (progress.total > 0) progress.downloaded * 100 / progress.total else 0
                val filledBlocks = pct / 5
                val emptyBlocks = 20 - filledBlocks
                val bar = "[" + "█".repeat(filledBlocks) + "░".repeat(emptyBlocks) + "]"
                Text(
                    text = bar,
                    color = TerminalGreen,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 14.sp,
                )
                Text(
                    text = "$pct% | ${progress.downloaded} / ${progress.total} tiles",
                    color = TerminalGreenDim,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 12.sp,
                )
                val sizeMB = "%.1f".format(progress.bytesDownloaded / 1_048_576.0)
                val totalMB = "%.1f".format(progress.estimatedTotalBytes / 1_048_576.0)
                Text(
                    text = "$sizeMB / $totalMB MB",
                    color = TerminalGreenDim,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                    ),
                    fontSize = 11.sp,
                )
            }
        }
    }
}
```

**IMPORTANT NOTE:** In `SavedPointsPanel`, the line with `height(androidx.compose.ui.unit.dp广场(200.dp)...` contains a compile error. The correct code should be:

```kotlin
                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nomad/android/ui/maps/MapsScreen.kt app/src/main/java/com/nomad/android/ui/maps/MapsViewModel.kt
git commit -m "feat: full-screen MapLibre map with offline tile support and live position"
```

---

## Task 7: Push and verify CI

- [ ] **Step 1: Push to remote**

```bash
git push
```

- [ ] **Step 2: Check CI status**

```bash
gh run list --limit 1
```

Wait for completion. All steps should pass: lint, unit tests, debug APK build.
