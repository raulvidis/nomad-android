package com.nomad.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nomad.android.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<SearchHistoryEntity>>

    @Insert
    suspend fun insert(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)
}
