package com.nomad.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nomad.android.data.local.entity.TrackRouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackRouteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(route: TrackRouteEntity)

    @Query("SELECT * FROM track_routes ORDER BY createdAt DESC")
    fun getAll(): Flow<List<TrackRouteEntity>>

    @Query("SELECT * FROM track_routes WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveRoute(): TrackRouteEntity?

    @Query("SELECT * FROM track_routes WHERE id = :id")
    suspend fun getById(id: String): TrackRouteEntity?

    @Query("UPDATE track_routes SET isActive = 0, endLat = :endLat, endLon = :endLon, pointCount = :pointCount, totalDistanceMeters = :totalDistanceMeters WHERE id = :id")
    suspend fun finalizeRoute(id: String, endLat: Double, endLon: Double, pointCount: Int, totalDistanceMeters: Double)

    @Query("DELETE FROM track_routes WHERE id = :id")
    suspend fun deleteById(id: String)
}
