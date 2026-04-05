package com.nomad.android.data.repository

import android.content.Context
import com.nomad.android.data.Result
import com.nomad.android.data.content.ContentPackManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapsRepository @Inject constructor(
    private val context: Context,
    private val contentPackManager: ContentPackManager
) {
    private val tilesDir by lazy {
        File(context.filesDir, "mapTiles").also { it.mkdirs() }
    }

    fun getAvailableLayers(): Flow<Result<List<MapLayer>>> = flow {
        val layers = listOf(
            MapLayer("basemap", "Basemap", true),
            MapLayer("poi", "Points of Interest", false),
            MapLayer("topo", "Topographic", false),
            MapLayer("emergency", "Emergency", false)
        )
        emit(Result.success(layers))
    }.catch { emit(Result.error("Failed to load map layers", it)) }

    fun downloadTiles(regionId: String, url: String): Flow<Float> = flow {
        emit(0f)
        emit(1f)
    }.catch { throw it }

    fun hasOfflineTiles(): Boolean =
        contentPackManager.isPackDownloaded("map_region") ||
            contentPackManager.isPackDownloaded("map_world")

    fun getDownloadedRegionName(): String? = when {
        contentPackManager.isPackDownloaded("map_world") -> "WORLD"
        contentPackManager.isPackDownloaded("map_region") -> "REGION"
        else -> null
    }

    fun getTilesDirectory(): File = tilesDir

    data class MapLayer(
        val id: String,
        val name: String,
        val isEnabled: Boolean
    )
}
