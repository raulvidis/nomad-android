package com.nomad.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nomad.android.data.local.entity.LocationSavedPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationSavedPointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: LocationSavedPointEntity)

    @Query("SELECT * FROM location_saved_points ORDER BY timestamp DESC")
    fun getAll(): Flow<List<LocationSavedPointEntity>>

    @Query("DELETE FROM location_saved_points WHERE id = :id")
    suspend fun deleteById(id: String)
}
