package com.nomad.android.data.repository

import com.nomad.android.data.Result
import com.nomad.android.data.local.dao.LocationSavedPointDao
import com.nomad.android.data.local.dao.LocationSnapshotDao
import com.nomad.android.data.local.dao.TrackRouteDao
import com.nomad.android.data.local.entity.LocationSavedPointEntity
import com.nomad.android.data.local.entity.LocationSnapshotEntity
import com.nomad.android.data.local.entity.TrackRouteEntity
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
    private val trackRouteDao: TrackRouteDao,
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

    val savedRoutes: Flow<List<TrackRouteEntity>> = trackRouteDao.getAll()

    suspend fun getRoutePoints(routeId: String): List<LocationSnapshotEntity> =
        snapshotDao.getByRouteId(routeId)

    fun observeRoutePoints(routeId: String): Flow<List<LocationSnapshotEntity>> =
        snapshotDao.observeByRouteId(routeId)

    fun startTracking(): Result<String> {
        return try {
            trackerService.startTracking()
            Result.success(trackerService.activeRouteId ?: "")
        } catch (e: Exception) {
            Result.error("Failed to start tracking", e)
        }
    }

    suspend fun beginRoute(): String {
        val routeId = UUID.randomUUID().toString()
        val location = trackerService.currentLocation.value
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
        val route = TrackRouteEntity(
            id = routeId,
            name = "Track ${dateFormat.format(System.currentTimeMillis())}",
            startLat = location?.latitude ?: 0.0,
            startLon = location?.longitude ?: 0.0,
            createdAt = System.currentTimeMillis(),
            isActive = true
        )
        trackRouteDao.insert(route)
        trackerService.activeRouteId = routeId
        return routeId
    }

    suspend fun endRoute() {
        val routeId = trackerService.activeRouteId ?: return
        val location = trackerService.currentLocation.value
        val points = snapshotDao.getByRouteId(routeId)
        val count = points.size
        var totalDist = 0.0
        for (i in 1 until points.size) {
            totalDist += haversine(
                points[i - 1].latitude, points[i - 1].longitude,
                points[i].latitude, points[i].longitude
            )
        }
        trackRouteDao.finalizeRoute(
            id = routeId,
            endLat = location?.latitude ?: points.lastOrNull()?.latitude ?: 0.0,
            endLon = location?.longitude ?: points.lastOrNull()?.longitude ?: 0.0,
            pointCount = count,
            totalDistanceMeters = totalDist
        )
        trackerService.activeRouteId = null
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

    suspend fun deleteRoute(id: String): Result<Unit> {
        return Result.runCatching { trackRouteDao.deleteById(id) }
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}
