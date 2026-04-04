package com.nomad.android.data.repository

import android.content.Context
import com.nomad.android.data.Result
import com.nomad.android.data.local.dao.ContentPackDao
import com.nomad.android.data.local.entity.ContentPackEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
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
            val file = File(contentPacksDir, pack.id)
            if (file.exists()) file.delete()
        }

    suspend fun deletePackById(id: String): Result<Unit> =
        Result.runCatching {
            contentPackDao.deleteById(id)
            val file = File(contentPacksDir, id)
            if (file.exists()) file.delete()
        }

    fun downloadPack(pack: ContentPackEntity, url: String): Flow<Float> = flow {
        emit(0f)
        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw RuntimeException("Download failed: ${response.code}")
        }

        val body = response.body ?: throw RuntimeException("Empty response body")
        val totalBytes = body.contentLength()
        val file = File(contentPacksDir, pack.id)
        var downloadedBytes = 0L

        body.byteStream().use { input ->
            FileOutputStream(file).use { output ->
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
        emit(1f)
    }.catch { throw it }

    fun getPackFile(packId: String): File? {
        val file = File(contentPacksDir, packId)
        return if (file.exists()) file else null
    }
}
