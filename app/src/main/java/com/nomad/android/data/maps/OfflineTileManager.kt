package com.nomad.android.data.maps

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.LinkedHashMap
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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
    private val lock = ReentrantLock()
    private val databases = object : LinkedHashMap<String, MBTilesDatabase>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, MBTilesDatabase>?): Boolean {
            if (size > MAX_OPEN_DATABASES) {
                eldest?.value?.close()
                return true
            }
            return false
        }
    }

    init {
        tilesDir.mkdirs()
    }

    private fun getOrOpenDatabase(regionId: String): MBTilesDatabase? {
        return lock.withLock {
            databases[regionId] ?: run {
                val dbFile = File(tilesDir, "$regionId.mbtiles")
                if (!dbFile.exists()) {
                    null
                } else {
                    val newDb = MBTilesDatabase(dbFile.absolutePath)
                    newDb.open()
                    databases[regionId] = newDb
                    newDb
                }
            }
        }
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
        lock.withLock { databases[id] = db }
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
        val db = getOrOpenDatabase(regionId)
        if (db == null) {
            emit(DownloadProgress(0, 0, 0, 0, false, "Region not found"))
            return@flow
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

            var failed = 0
            try {
                val url = tileCalculator.tileUrl(tile.x, tile.y, tile.z)
                val request = Request.Builder().url(url)
                    .header("User-Agent", "NOMAD-Android/1.0")
                    .build()
                val response = httpClient.newCall(request).execute()
                response.use {
                    val body = it.body?.bytes()
                    if (it.isSuccessful && body != null) {
                        db.insertTile(tile.z, tile.x, tile.y, body)
                        bytesDownloaded += body.size
                        downloaded++
                    } else {
                        failed++
                    }
                }
            } catch (e: Exception) {
                failed++
                Log.w(TAG, "Failed to download tile ${tile.z}/${tile.x}/${tile.y}", e)
            }
            emit(DownloadProgress(downloaded, total, bytesDownloaded, tileCalculator.estimateSizeBytes(total), false))
        }

        db.setMetadata("tilecount", downloaded.toString())
        db.setMetadata("sizebytes", bytesDownloaded.toString())
        emit(DownloadProgress(downloaded, total, bytesDownloaded, bytesDownloaded, true))
    }.flowOn(Dispatchers.IO)

    fun getDownloadedRegions(): List<OfflineRegion> {
        return tilesDir.listFiles { file -> file.extension == "mbtiles" }?.mapNotNull { file ->
            val db = getOrOpenDatabase(file.nameWithoutExtension) ?: return@mapNotNull null
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
        val dbs = lock.withLock { databases.values.toList() }
        for (db in dbs) {
            try {
                val tile = db.getTile(z, x, y)
                if (tile != null) return tile
            } catch (_: Exception) {
                // Database may have been closed by concurrent deleteRegion
            }
        }
        return null
    }

    fun deleteRegion(regionId: String) {
        lock.withLock {
            databases[regionId]?.close()
            databases.remove(regionId)
        }
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
        return lock.withLock { databases[regionId] }?.getTileCount() ?: 0
    }

    fun closeAll() {
        lock.withLock {
            databases.values.forEach { it.close() }
            databases.clear()
        }
    }

    companion object {
        private const val TAG = "OfflineTileManager"
        private const val MAX_OPEN_DATABASES = 5
    }
}
