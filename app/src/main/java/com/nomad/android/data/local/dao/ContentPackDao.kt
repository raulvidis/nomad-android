package com.nomad.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nomad.android.data.local.entity.ContentPackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentPackDao {

    @Query("SELECT * FROM content_packs")
    fun getAll(): Flow<List<ContentPackEntity>>

    @Query("SELECT * FROM content_packs WHERE type = :type")
    fun getByType(type: String): Flow<List<ContentPackEntity>>

    @Query("SELECT * FROM content_packs WHERE status = :status")
    fun getByStatus(status: String): Flow<List<ContentPackEntity>>

    @Insert
    suspend fun insert(contentPack: ContentPackEntity)

    @Insert
    suspend fun insertAll(contentPacks: List<ContentPackEntity>)

    @Update
    suspend fun update(contentPack: ContentPackEntity)

    @Delete
    suspend fun delete(contentPack: ContentPackEntity)
}
