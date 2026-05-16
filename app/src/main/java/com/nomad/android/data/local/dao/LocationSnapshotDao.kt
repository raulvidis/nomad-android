package com.nomad.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nomad.android.data.local.entity.LocationSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationSnapshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: LocationSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(snapshots: List<LocationSnapshotEntity>)

    @Query("SELECT * FROM location_snapshots ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int = 100): Flow<List<LocationSnapshotEntity>>

    @Query("SELECT * FROM location_snapshots ORDER BY timestamp DESC LIMIT 1")
    fun getLatest(): Flow<LocationSnapshotEntity?>

    @Query("SELECT * FROM location_snapshots WHERE isTracking = 1 AND timestamp >= :sinceMillis ORDER BY timestamp ASC")
    suspend fun getTrackingSnapshots(sinceMillis: Long): List<LocationSnapshotEntity>

    @Query("DELETE FROM location_snapshots WHERE timestamp < :beforeMillis")
    suspend fun deleteOlderThan(beforeMillis: Long)

    @Query("SELECT COUNT(*) FROM location_snapshots")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM location_snapshots")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM location_snapshots WHERE routeId = :routeId ORDER BY timestamp ASC")
    suspend fun getByRouteId(routeId: String): List<LocationSnapshotEntity>

    @Query("SELECT * FROM location_snapshots WHERE routeId = :routeId ORDER BY timestamp ASC")
    fun observeByRouteId(routeId: String): Flow<List<LocationSnapshotEntity>>
}
