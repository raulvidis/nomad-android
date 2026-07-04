package com.nomad.android.data.repository

import android.content.Context
import android.util.Log
import com.nomad.android.data.Result
import com.nomad.android.data.local.dao.ContentPackDao
import com.nomad.android.data.local.entity.ContentPackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentPackRepository @Inject constructor(
    private val contentPackDao: ContentPackDao,
    private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val contentPacksDir by lazy {
        File(context.filesDir, "contentPacks").also { it.mkdirs() }
    }

    private companion object {
        private const val TAG = "ContentPackRepository"
    }

    /**
     * Validates that the resolved path stays within [contentPacksDir].
     * Returns null if the name contains a path traversal attempt (e.g. "../").
     */
    private fun sanitizePath(name: String): File? {
        return try {
            val baseDir = contentPacksDir.canonicalFile
            val resolved = File(baseDir, name).canonicalFile
            if (resolved.path.startsWith(baseDir.canonicalPath + File.separator) ||
                resolved == baseDir
            ) {
                resolved
            } else {
                Log.w(TAG, "Path traversal attempt blocked: $name")
                null
            }
        } catch (e: IOException) {
            Log.w(TAG, "Invalid path: $name", e)
            null
        }
    }

    fun getAllPacks(): Flow<Result<List<ContentPackEntity>>> =
        contentPackDao.getAll()
            .map { Result.success(it) }
            .catch { emit(Result.error("Failed to load content packs", it)) }

    fun getPacksByType(type: String): Flow<Result<List<ContentPackEntity>>> =
        contentPackDao.getByType(type)
            .map { Result.success(it) }
            .catch { emit(Result.error("Failed to load packs by type", it)) }

    fun getPacksByStatus(status: String): Flow<Result<List<ContentPackEntity>>> =
        contentPackDao.getByStatus(status)
            .map { Result.success(it) }
            .catch { emit(Result.error("Failed to load packs by status", it)) }

    suspend fun insertPack(pack: ContentPackEntity): Result<Unit> =
        Result.runCatching { contentPackDao.insert(pack) }

    suspend fun updatePack(pack: ContentPackEntity): Result<Unit> =
        Result.runCatching { contentPackDao.update(pack) }

    suspend fun deletePack(pack: ContentPackEntity): Result<Unit> =
        Result.runCatching {
            contentPackDao.delete(pack)
            val file = sanitizePath(pack.id)
            if (file != null && file.exists()) file.delete()
        }

    suspend fun deletePackById(id: String): Result<Unit> =
        Result.runCatching {
            contentPackDao.deleteById(id)
            val file = sanitizePath(id)
            if (file != null && file.exists()) file.delete()
        }

    fun downloadPack(pack: ContentPackEntity, url: String): Flow<Float> = flow {
        emit(0f)
        val finalFile = sanitizePath(pack.id)
            ?: throw RuntimeException("Invalid pack id: ${pack.id}")
        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()
        response.use { resp ->
            if (!resp.isSuccessful) {
                throw RuntimeException("Download failed: ${resp.code}")
            }

            val body = resp.body ?: throw RuntimeException("Empty response body")
            val totalBytes = body.contentLength()
            val tmpFile = File(contentPacksDir, "${pack.id}.tmp")
            var downloadedBytes = 0L

            try {
                body.byteStream().use { input ->
                    FileOutputStream(tmpFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            if (totalBytes > 0) {
                                emit(downloadedBytes.toFloat() / totalBytes.toFloat())
                            }
                        }
                    }
                }
                if (!tmpFile.renameTo(finalFile)) {
                    throw RuntimeException("Failed to move temp file to ${finalFile.name}")
                }
            } catch (e: Exception) {
                tmpFile.delete()
                throw e
            }
        }
        emit(1f)
    }.flowOn(Dispatchers.IO)

    fun getPackFile(packId: String): File? {
        val file = sanitizePath(packId) ?: return null
        return if (file.exists()) file else null
    }
}
