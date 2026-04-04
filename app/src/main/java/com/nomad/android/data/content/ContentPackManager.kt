package com.nomad.android.data.content

import android.content.Context
import com.nomad.android.data.local.dao.ContentPackDao
import com.nomad.android.data.local.entity.ContentPackEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

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
    private val downloadDir by lazy {
        File(context.filesDir, "contentPacks").also { it.mkdirs() }
    }

    fun getAvailablePacks(): Flow<List<ContentPack>> = flow {
        val bundled = getBundledPacks()
        val downloadedIds = downloadDir.listFiles()?.map { it.name } ?: emptyList()

        val packs = bundled.map { pack ->
            pack.copy(
                status = if (downloadedIds.contains(pack.id)) PackStatus.DOWNLOADED else pack.status
            )
        }
        emit(packs)
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

    fun downloadPack(packId: String): Flow<Float> = flow {
        emit(0f)

        val pack = getBundledPacks().find { it.id == packId }
            ?: throw IllegalArgumentException("Unknown pack: $packId")

        val destFile = File(downloadDir, packId)
        if (destFile.exists()) {
            emit(1f)
            return@flow
        }

        // Simulate download progress for now (replace with OkHttp + WorkManager when backend is ready)
        val steps = 20
        for (i in 1..steps) {
            delay(100)
            emit(i.toFloat() / steps)
        }

        // Create placeholder file to mark as downloaded
        destFile.createNewFile()
        emit(1f)
    }

    suspend fun deletePack(packId: String) {
        val file = File(downloadDir, packId)
        if (file.exists()) file.delete()
        contentPackDao.deleteById(packId)
    }

    fun isPackDownloaded(packId: String): Boolean = File(downloadDir, packId).exists()

    fun getDownloadedPackSize(packId: String): Long {
        val file = File(downloadDir, packId)
        return if (file.exists()) file.length() else 0L
    }

    fun formatSize(bytes: Long): String = when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        else -> "$bytes bytes"
    }
}
