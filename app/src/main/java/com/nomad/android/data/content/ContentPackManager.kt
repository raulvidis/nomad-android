package com.nomad.android.data.content

import android.content.Context
import android.util.Log
import com.nomad.android.DownloadService
import com.nomad.android.data.ai.LiteRTLMEngine
import com.nomad.android.data.local.dao.ContentPackDao
import com.nomad.android.data.local.entity.ContentPackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class ContentPack(
    val id: String,
    val name: String,
    val type: String,
    val sizeBytes: Long,
    val description: String,
    val status: PackStatus,
    val downloadUrl: String? = null
)

enum class PackStatus { AVAILABLE, DOWNLOADING, DOWNLOADED, ERROR }

class ContentPackManager(
    private val context: Context,
    private val contentPackDao: ContentPackDao,
    private val okHttpClient: OkHttpClient
) {
    private val downloadDir by lazy {
        File(context.filesDir, "contentPacks").also { it.mkdirs() }
    }
    private val modelsDir by lazy {
        File(context.filesDir, "models").also { it.mkdirs() }
    }

    // Track active downloads so UI can survive process recreation
    private val _activeDownloads = MutableStateFlow<Map<String, Float>>(emptyMap())
    val activeDownloads: StateFlow<Map<String, Float>> = _activeDownloads.asStateFlow()

    fun getAvailablePacks(): Flow<List<ContentPack>> = flow {
        // Clean up old misnamed model files from previous versions
        cleanupOldModelFiles()

        val bundled = getBundledPacks()
        val downloadedContentIds = downloadDir.listFiles()?.map { it.name } ?: emptyList()
        val downloadedModelFiles = modelsDir.listFiles()?.map { it.name } ?: emptyList()

        val packs = bundled.map { pack ->
            val isDownloaded = when (pack.type) {
                "ai_model" -> {
                    // Check if the actual model file exists in models dir
                    val variant = getModelVariantForPack(pack.id)
                    variant != null && downloadedModelFiles.contains(variant.fileName)
                }
                else -> downloadedContentIds.contains(pack.id)
            }
            pack.copy(status = if (isDownloaded) PackStatus.DOWNLOADED else pack.status)
        }
        emit(packs)
    }.flowOn(Dispatchers.IO)

    private fun getBundledPacks(): List<ContentPack> {
        val modelPacks = LiteRTLMEngine.ModelVariant.entries.map { variant ->
            ContentPack(
                id = modelVariantToPackId(variant),
                name = variant.displayName,
                type = "ai_model",
                sizeBytes = variant.sizeMB.toLong() * 1_048_576,
                description = "On-device LLM (${formatSize(variant.sizeMB.toLong() * 1_048_576)} download)",
                status = PackStatus.AVAILABLE,
                downloadUrl = variant.downloadUrl
            )
        }

        return listOf(
            ContentPack("essentials", "Essentials Pack", "survival", 524288000L, "First aid, survival guides, SOS protocols", PackStatus.DOWNLOADED),
        ) + modelPacks + listOf(
            ContentPack("wiki_mini", "Wikipedia Mini", "wikipedia", 2147483648L, "Top 10,000 articles (no images)", PackStatus.AVAILABLE),
            ContentPack("wiki_full", "Wikipedia Full", "wikipedia", 32212254720L, "All articles (no images)", PackStatus.AVAILABLE),
            ContentPack("map_region", "Map - Region", "maps", 1073741824L, "Single region offline map", PackStatus.AVAILABLE),
            ContentPack("map_world", "Map - World", "maps", 128849018880L, "Global basemap + POIs", PackStatus.AVAILABLE),
            ContentPack("books", "Classic Books Pack", "books", 2147483648L, "Project Gutenberg top 1000", PackStatus.AVAILABLE),
        )
    }

    private fun modelVariantToPackId(variant: LiteRTLMEngine.ModelVariant): String = when (variant) {
        LiteRTLMEngine.ModelVariant.GEMMA4_E2B -> "ai_gemma4"
    }

    private fun getModelVariantForPack(packId: String): LiteRTLMEngine.ModelVariant? = when (packId) {
        "ai_gemma4" -> LiteRTLMEngine.ModelVariant.GEMMA4_E2B
        else -> null
    }

    fun downloadPack(packId: String): Flow<Float> = flow {
        _activeDownloads.update { it + (packId to 0f) }
        emit(0f)

        val hasOtherDownloads = _activeDownloads.value.size > 1
        if (!hasOtherDownloads) {
            DownloadService.start(context)
        }

        try {
            val pack = getBundledPacks().find { it.id == packId }
                ?: throw IllegalArgumentException("Unknown pack: $packId")

            if (pack.type == "ai_model" && pack.downloadUrl != null) {
                // Real download from HuggingFace
                val variant = getModelVariantForPack(packId)
                    ?: throw IllegalArgumentException("Unknown model variant for pack: $packId")
                val destFile = File(modelsDir, variant.fileName)

                if (destFile.exists() && destFile.length() > 0) {
                    emit(1f)
                } else {
                    downloadFile(pack.downloadUrl, destFile) { progress ->
                        _activeDownloads.update { it + (packId to progress) }
                        emit(progress)
                    }
                }
            } else {
                // Simulated download for non-AI packs (no real CDN yet)
                val destFile = File(downloadDir, packId)
                if (destFile.exists()) {
                    emit(1f)
                    return@flow
                }
                val steps = 25
                val delayPerStep = when {
                    pack.sizeBytes > 10_000_000_000L -> 300L
                    pack.sizeBytes > 1_000_000_000L -> 200L
                    else -> 120L
                }
                for (i in 1..steps) {
                    delay(delayPerStep)
                    val progress = i.toFloat() / steps
                    _activeDownloads.update { it + (packId to progress) }
                    emit(progress)
                }
                destFile.createNewFile()
            }

            // Persist to Room
            contentPackDao.insert(
                ContentPackEntity(
                    id = pack.id,
                    name = pack.name,
                    type = pack.type,
                    sizeBytes = pack.sizeBytes,
                    status = "downloaded",
                    downloadedAt = System.currentTimeMillis(),
                    version = "1.0",
                    description = pack.description
                )
            )

            emit(1f)
        } finally {
            _activeDownloads.update { it - packId }
            if (_activeDownloads.value.isEmpty()) {
                DownloadService.stop(context)
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun downloadFile(
        url: String,
        destFile: File,
        onProgress: suspend (Float) -> Unit
    ) {
        val tmpFile = File(destFile.parentFile, "${destFile.name}.tmp")

        try {
            Log.i(TAG, "Downloading $url to ${destFile.absolutePath}")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "NOMAD-Android/1.0")
                .build()

            val response = okHttpClient.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    throw RuntimeException("Download failed: HTTP ${resp.code}")
                }

                val body = resp.body ?: throw RuntimeException("Empty response body")
                val totalBytes = body.contentLength()
                var downloadedBytes = 0L

                body.byteStream().use { input ->
                    FileOutputStream(tmpFile).use { output ->
                        val buffer = ByteArray(65536)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            if (totalBytes > 0) {
                                onProgress(downloadedBytes.toFloat() / totalBytes.toFloat())
                            }
                        }
                    }
                }
            }

            // Validate the downloaded file is not an HTML error page
            if (tmpFile.length() < 1_000_000) {
                val header = ByteArray(100)
                FileInputStream(tmpFile).use { it.read(header) }
                val headerStr = header.toString(Charsets.UTF_8)
                if (headerStr.contains("<html", ignoreCase = true) || headerStr.contains("<!DOCTYPE", ignoreCase = true)) {
                    tmpFile.delete()
                    throw RuntimeException("Download returned an HTML error page instead of the model file")
                }
            }

            // Atomic rename
            if (!tmpFile.renameTo(destFile)) {
                throw RuntimeException("Failed to rename temp file to ${destFile.name}")
            }
            Log.i(TAG, "Download complete: ${destFile.absolutePath} (${destFile.length()} bytes)")

        } catch (e: Exception) {
            tmpFile.delete()
            throw e
        }
    }

    suspend fun deletePack(packId: String) {
        // Delete model file if AI pack
        val variant = getModelVariantForPack(packId)
        if (variant != null) {
            val modelFile = File(modelsDir, variant.fileName)
            if (modelFile.exists()) modelFile.delete()
        }
        // Delete content pack file
        val file = File(downloadDir, packId)
        if (file.exists()) file.delete()
        // Remove from database
        contentPackDao.deleteById(packId)
    }

    fun isPackDownloaded(packId: String): Boolean {
        val variant = getModelVariantForPack(packId)
        if (variant != null) {
            return File(modelsDir, variant.fileName).exists()
        }
        return File(downloadDir, packId).exists()
    }

    fun getDownloadedPackSize(packId: String): Long {
        val variant = getModelVariantForPack(packId)
        if (variant != null) {
            val file = File(modelsDir, variant.fileName)
            return if (file.exists()) file.length() else 0L
        }
        val file = File(downloadDir, packId)
        return if (file.exists()) file.length() else 0L
    }

    fun formatSize(bytes: Long): String = when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.0f MB".format(bytes / 1_048_576.0)
        else -> "$bytes bytes"
    }

    private fun cleanupOldModelFiles() {
        // Remove model files from previous versions with wrong filenames
        val validFileNames = LiteRTLMEngine.ModelVariant.entries.map { it.fileName }.toSet()
        modelsDir.listFiles()?.forEach { file ->
            if (file.name !in validFileNames && !file.name.endsWith(".tmp")) {
                Log.i(TAG, "Cleaning up old model file: ${file.name}")
                file.delete()
            }
        }
    }

    companion object {
        private const val TAG = "ContentPackManager"
    }
}
