package com.nomad.android.data.repository

import android.content.Context
import android.os.StatFs
import com.nomad.android.data.Result
import com.nomad.android.data.local.dao.ContentPackDao
import com.nomad.android.data.local.dao.SettingsDao
import com.nomad.android.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val settingsDao: SettingsDao,
    private val contentPackDao: ContentPackDao,
    private val context: Context
) {
    fun getSetting(key: String): Flow<Result<String?>> =
        settingsDao.getAll()
            .map { list -> Result.success(list.find { it.key == key }?.value) as Result<String?> }
            .catch { emit(Result.error("Failed to read setting", it)) }

    suspend fun setSetting(key: String, value: String): Result<Unit> =
        Result.runCatching { settingsDao.set(SettingsEntity(key = key, value = value)) }

    fun getAllSettings(): Flow<Result<List<SettingsEntity>>> =
        settingsDao.getAll()
            .map { Result.success(it) }
            .catch { emit(Result.error("Failed to load settings", it)) }

    // Onboarding persistence
    val isOnboardingComplete: Flow<Boolean> =
        settingsDao.getAll()
            .map { list -> list.find { it.key == KEY_ONBOARDING_COMPLETE }?.value == "true" }
            .catch { emit(false) }

    suspend fun completeOnboarding(): Result<Unit> =
        setSetting(KEY_ONBOARDING_COMPLETE, "true")

    // Storage metrics
    fun getStorageMetrics(): StorageMetrics {
        val stat = StatFs(context.filesDir.path)
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        val usedBytes = totalBytes - availableBytes
        return StorageMetrics(
            totalBytes = totalBytes,
            usedBytes = usedBytes,
            availableBytes = availableBytes,
            usedPercent = if (totalBytes > 0) ((usedBytes.toFloat() / totalBytes.toFloat()) * 100).toInt() else 0
        )
    }

    companion object {
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        const val KEY_THEME = "theme"
    }

    data class StorageMetrics(
        val totalBytes: Long,
        val usedBytes: Long,
        val availableBytes: Long,
        val usedPercent: Int
    )
}
