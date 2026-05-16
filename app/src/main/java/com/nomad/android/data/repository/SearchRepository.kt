package com.nomad.android.data.repository

import com.nomad.android.data.Result
import com.nomad.android.data.local.dao.SearchHistoryDao
import com.nomad.android.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao
) {
    fun getAllHistory(): Flow<Result<List<SearchHistoryEntity>>> =
        searchHistoryDao.getAll()
            .map { Result.success(it) }
            .catch { emit(Result.error("Failed to load search history", it)) }

    suspend fun insertHistoryEntry(entry: SearchHistoryEntity): Result<Unit> =
        Result.runCatching { searchHistoryDao.insert(entry) }

    suspend fun deleteOlderThan(cutoffTimestamp: Long): Result<Unit> =
        Result.runCatching { searchHistoryDao.deleteOlderThan(cutoffTimestamp) }
}
