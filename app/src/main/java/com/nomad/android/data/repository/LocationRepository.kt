package com.nomad.android.data.repository

import com.nomad.android.data.Result
import com.nomad.android.data.local.dao.LocationSavedPointDao
import com.nomad.android.data.local.dao.LocationSnapshotDao
import com.nomad.android.data.local.entity.LocationSavedPointEntity
import com.nomad.android.data.local.entity.LocationSnapshotEntity
import com.nomad.android.util.LocationTrackerService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Singleton

@Singleton
class LocationRepository(
    private val snapshotDao: LocationSnapshotDao,
    private val savedPointDao: LocationSavedPointDao,
    private val trackerService: LocationTrackerService
) {

    val recentSnapshots: Flow<List<LocationSnapshotEntity>> =
        snapshotDao.getRecent(limit = 100)

    val savedPoints: Flow<List<LocationSavedPointEntity>> =
        savedPointDao.getAll()

    val currentLocation: StateFlow<android.location.Location?> =
        trackerService.currentLocation

    val isTracking: StateFlow<Boolean> =
        trackerService.isTracking

    val trackingCount: Flow<Int> = snapshotDao.observeCount()

    fun startTracking(): Result<Unit> {
        return try {
            trackerService.startTracking()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error("Failed to start tracking", e)
        }
    }

    fun stopTracking(): Result<Unit> {
        return try {
            trackerService.stopTracking()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error("Failed to stop tracking", e)
        }
    }

    fun requestCurrentLocation(): Result<Unit> {
        return try {
            trackerService.requestSingleUpdate()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error("Failed to request location", e)
        }
    }

    suspend fun saveCurrentLocation(name: String, notes: String): Result<Unit> {
        return try {
            val location = trackerService.currentLocation.value
                ?: return Result.error("No current location available")
            val point = LocationSavedPointEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                timestamp = System.currentTimeMillis(),
                notes = notes
            )
            savedPointDao.insert(point)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error("Failed to save location", e)
        }
    }

    suspend fun deleteSavedPoint(id: String): Result<Unit> {
        return Result.runCatching { savedPointDao.deleteById(id) }
    }

    suspend fun cleanOldSnapshots(daysOld: Int = 30): Result<Unit> {
        return Result.runCatching {
            val cutoff = System.currentTimeMillis() - (daysOld.toLong() * 24 * 60 * 60 * 1000)
            snapshotDao.deleteOlderThan(cutoff)
        }
    }
}
