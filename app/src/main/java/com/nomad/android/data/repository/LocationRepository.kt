package com.nomad.android.data.repository

import com.nomad.android.data.local.dao.LocationSavedPointDao
import com.nomad.android.data.local.dao.LocationSnapshotDao
import com.nomad.android.data.local.entity.LocationSavedPointEntity
import com.nomad.android.util.LocationTrackerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.UUID
import javax.inject.Singleton

@Singleton
class LocationRepository(
    private val snapshotDao: LocationSnapshotDao,
    private val savedPointDao: LocationSavedPointDao,
    private val trackerService: LocationTrackerService
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val recentSnapshots: Flow<List<com.nomad.android.data.local.entity.LocationSnapshotEntity>> =
        snapshotDao.getRecent(limit = 100)

    val savedPoints: Flow<List<LocationSavedPointEntity>> =
        savedPointDao.getAll()

    val currentLocation: StateFlow<android.location.Location?> =
        trackerService.currentLocation

    val isTracking: StateFlow<Boolean> =
        trackerService.isTracking

    val trackingCount: Flow<Int> = snapshotDao.getRecent(limit = Int.MAX_VALUE).map { it.size }

    fun startTracking() {
        trackerService.startTracking()
    }

    fun stopTracking() {
        trackerService.stopTracking()
    }

    fun requestCurrentLocation() {
        trackerService.requestSingleUpdate()
    }

    suspend fun saveCurrentLocation(name: String, notes: String) {
        val location = trackerService.currentLocation.value ?: return
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
    }

    suspend fun deleteSavedPoint(id: String) {
        savedPointDao.deleteById(id)
    }

    suspend fun cleanOldSnapshots(daysOld: Int = 30) {
        val cutoff = System.currentTimeMillis() - (daysOld.toLong() * 24 * 60 * 60 * 1000)
        snapshotDao.deleteOlderThan(cutoff)
    }
}
