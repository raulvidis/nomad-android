package com.nomad.android.data.content

import android.content.Context
import com.nomad.android.data.local.dao.ContentPackDao
import com.nomad.android.data.local.entity.ContentPackEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class ContentPack(
    val id: String,
    val name: String,
    val type: String,
    val sizeBytes: Long,
    val description: String,
    val status: PackStatus
)

enum class PackStatus { AVAILABLE, DOWNLOADING, DOWNLOADED, ERROR }

class ContentPackManager(
    private val context: Context,
    private val contentPackDao: ContentPackDao
) {
    fun getAvailablePacks(): Flow<List<ContentPack>> = flow {
        // TODO: Fetch from remote manifest
        emit(getBundledPacks())
    }

    private fun getBundledPacks(): List<ContentPack> = listOf(
        ContentPack("essentials", "Essentials Pack", "survival", 524288000L, "First aid, survival guides, SOS protocols", PackStatus.DOWNLOADED),
        ContentPack("wiki_mini", "Wikipedia Mini", "wikipedia", 2147483648L, "Top 10,000 articles (no images)", PackStatus.AVAILABLE),
        ContentPack("wiki_full", "Wikipedia Full", "wikipedia", 32212254720L, "All articles (no images)", PackStatus.AVAILABLE),
        ContentPack("map_region", "Map - Region", "maps", 1073741824L, "Single region offline map", PackStatus.AVAILABLE),
        ContentPack("map_world", "Map - World", "maps", 128849018880L, "Global basemap + POIs", PackStatus.AVAILABLE),
        ContentPack("ai_e2b", "AI Model - Gemma 4 E2B", "ai_model", 3145728000L, "On-device AI (3GB, requires 6GB RAM)", PackStatus.AVAILABLE),
        ContentPack("ai_1b", "AI Model - Gemma 3 1B", "ai_model", 1048576000L, "Lightweight AI (1GB, requires 4GB RAM)", PackStatus.AVAILABLE),
        ContentPack("books", "Classic Books Pack", "books", 2147483648L, "Project Gutenberg top 1000", PackStatus.AVAILABLE),
    )

    suspend fun downloadPack(packId: String): Flow<Float> = flow {
        // TODO: Implement with WorkManager
        emit(0f)
        emit(1f)
    }

    suspend fun deletePack(packId: String) {
        contentPackDao.deleteById(packId)
    }

    fun formatSize(bytes: Long): String = when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        else -> "$bytes bytes"
    }
}
