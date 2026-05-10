package com.nomad.android.data.repository

import com.nomad.android.data.local.dao.LocationSavedPointDao
import com.nomad.android.data.local.dao.LocationSnapshotDao
import com.nomad.android.data.local.dao.TrackRouteDao
import com.nomad.android.data.local.entity.LocationSavedPointEntity
import com.nomad.android.data.local.entity.LocationSnapshotEntity
import com.nomad.android.data.local.entity.TrackRouteEntity
import com.nomad.android.util.LocationTrackerService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class LocationRepositoryTest {

    private val snapshotDao: LocationSnapshotDao = mock()
    private val savedPointDao: LocationSavedPointDao = mock()
    private val trackRouteDao: TrackRouteDao = mock()
    private val trackerService: LocationTrackerService = mock()

    private lateinit var repository: LocationRepository

    @Before
    fun setUp() {
        repository = LocationRepository(
            snapshotDao = snapshotDao,
            savedPointDao = savedPointDao,
            trackRouteDao = trackRouteDao,
            trackerService = trackerService
        )
    }

    // --- recentSnapshots ---

    @Test
    fun `recentSnapshots returns snapshots from DAO`() = runTest {
        val snapshots = listOf(
            LocationSnapshotEntity("s1", 44.43, 26.10, 100.0, 5f, 1000L, true, null)
        )
        whenever(snapshotDao.getRecent(limit = 100)).thenReturn(flow { emit(snapshots) })

        val result = repository.recentSnapshots.first()

        assertEquals(1, result.size)
        assertEquals("s1", result[0].id)
    }

    // --- savedPoints ---

    @Test
    fun `savedPoints returns points from DAO`() = runTest {
        val points = listOf(
            LocationSavedPointEntity("p1", "Home", 44.43, 26.10, 100.0, 1000L, "Base camp")
        )
        whenever(savedPointDao.getAll()).thenReturn(flow { emit(points) })

        val result = repository.savedPoints.first()

        assertEquals(1, result.size)
        assertEquals("Home", result[0].name)
    }

    // --- currentLocation ---

    @Test
    fun `currentLocation delegates to tracker service`() = runTest {
        val location = mock<android.location.Location> {
            on { latitude } doReturn 44.43
            on { longitude } doReturn 26.10
        }
        whenever(trackerService.currentLocation).thenReturn(MutableStateFlow(location))

        val result = repository.currentLocation.value

        assertNotNull(result)
        assertEquals(44.43, result!!.latitude, 0.001)
        assertEquals(26.10, result.longitude, 0.001)
    }

    @Test
    fun `currentLocation returns null when no location available`() = runTest {
        whenever(trackerService.currentLocation).thenReturn(MutableStateFlow(null))

        val result = repository.currentLocation.value

        assertNull(result)
    }

    // --- isTracking ---

    @Test
    fun `isTracking delegates to tracker service`() = runTest {
        whenever(trackerService.isTracking).thenReturn(MutableStateFlow(true))

        val result = repository.isTracking.value

        assertTrue(result)
    }

    @Test
    fun `isTracking returns false when not tracking`() = runTest {
        whenever(trackerService.isTracking).thenReturn(MutableStateFlow(false))

        val result = repository.isTracking.value

        assertFalse(result)
    }

    // --- startTracking ---

    @Test
    fun `startTracking returns success with route ID`() = runTest {
        whenever(trackerService.activeRouteId).thenReturn("route-123")
        whenever(trackerService.startTracking()).then { }

        val result = repository.startTracking()

        assertTrue(result.isSuccess)
        assertEquals("route-123", result.getOrNull())
    }

    @Test
    fun `startTracking catches exception and returns error`() {
        whenever(trackerService.startTracking()).thenThrow(RuntimeException("Location permission denied"))

        val result = repository.startTracking()

        assertTrue(result.isError)
    }

    // --- stopTracking ---

    @Test
    fun `stopTracking returns success`() {
        whenever(trackerService.stopTracking()).then { }

        val result = repository.stopTracking()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `stopTracking catches exception and returns error`() {
        whenever(trackerService.stopTracking()).thenThrow(RuntimeException("Stop failed"))

        val result = repository.stopTracking()

        assertTrue(result.isError)
    }

    // --- requestCurrentLocation ---

    @Test
    fun `requestCurrentLocation returns success`() {
        whenever(trackerService.requestSingleUpdate()).then { }

        val result = repository.requestCurrentLocation()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `requestCurrentLocation catches exception and returns error`() {
        whenever(trackerService.requestSingleUpdate()).thenThrow(RuntimeException("No GPS"))

        val result = repository.requestCurrentLocation()

        assertTrue(result.isError)
    }

    // --- saveCurrentLocation ---

    @Test
    fun `saveCurrentLocation returns error when no location available`() = runTest {
        whenever(trackerService.currentLocation).thenReturn(MutableStateFlow(null))

        val result = repository.saveCurrentLocation("Test", "Notes")

        assertTrue(result.isError)
    }

    @Test
    fun `saveCurrentLocation saves point with current location`() = runTest {
        val location = mock<android.location.Location> {
            on { latitude } doReturn 44.43
            on { longitude } doReturn 26.10
            on { altitude } doReturn 100.0
        }
        whenever(trackerService.currentLocation).thenReturn(MutableStateFlow(location))
        whenever(savedPointDao.insert(any())).then { }

        val result = repository.saveCurrentLocation("Camp", "Base camp")

        assertTrue(result.isSuccess)
        argumentCaptor<LocationSavedPointEntity>().apply {
            verify(savedPointDao).insert(capture())
            assertEquals("Camp", firstValue.name)
            assertEquals(44.43, firstValue.latitude, 0.001)
            assertEquals(26.10, firstValue.longitude, 0.001)
            assertEquals("Base camp", firstValue.notes)
        }
    }

    @Test
    fun `saveCurrentLocation returns error when DAO throws`() = runTest {
        val location = mock<android.location.Location> {
            on { latitude } doReturn 44.43
            on { longitude } doReturn 26.10
            on { altitude } doReturn 100.0
        }
        whenever(trackerService.currentLocation).thenReturn(MutableStateFlow(location))
        whenever(savedPointDao.insert(any())).thenThrow(RuntimeException("DB full"))

        val result = repository.saveCurrentLocation("Camp", "Notes")

        assertTrue(result.isError)
    }

    // --- deleteSavedPoint ---

    @Test
    fun `deleteSavedPoint returns success`() = runTest {
        whenever(savedPointDao.deleteById("p1")).then { }

        val result = repository.deleteSavedPoint("p1")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `deleteSavedPoint returns error when DAO throws`() = runTest {
        whenever(savedPointDao.deleteById("p1")).thenThrow(RuntimeException("Delete failed"))

        val result = repository.deleteSavedPoint("p1")

        assertTrue(result.isError)
    }

    // --- cleanOldSnapshots ---

    @Test
    fun `cleanOldSnapshots returns success`() = runTest {
        whenever(snapshotDao.deleteOlderThan(any())).then { }

        val result = repository.cleanOldSnapshots(30)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `cleanOldSnapshots returns error when DAO throws`() = runTest {
        whenever(snapshotDao.deleteOlderThan(any())).thenThrow(RuntimeException("Cleanup failed"))

        val result = repository.cleanOldSnapshots(30)

        assertTrue(result.isError)
    }

    // --- savedRoutes ---

    @Test
    fun `savedRoutes returns routes from DAO`() = runTest {
        val routes = listOf(
            TrackRouteEntity("r1", "Morning Run", 44.43, 26.10, null, null, 0, 0.0, 1000L, true)
        )
        whenever(trackRouteDao.getAll()).thenReturn(flow { emit(routes) })

        val result = repository.savedRoutes.first()

        assertEquals(1, result.size)
        assertEquals("Morning Run", result[0].name)
    }

    // --- getRoutePoints ---

    @Test
    fun `getRoutePoints returns points from DAO`() = runTest {
        val points = listOf(
            LocationSnapshotEntity("s1", 44.43, 26.10, 100.0, 5f, 1000L, true, "route-1")
        )
        whenever(snapshotDao.getByRouteId("route-1")).thenReturn(points)

        val result = repository.getRoutePoints("route-1")

        assertEquals(1, result.size)
        assertEquals("route-1", result[0].routeId)
    }

    // --- observeRoutePoints ---

    @Test
    fun `observeRoutePoints returns flow from DAO`() = runTest {
        val points = listOf(
            LocationSnapshotEntity("s1", 44.43, 26.10, 100.0, 5f, 1000L, true, "route-1")
        )
        whenever(snapshotDao.observeByRouteId("route-1")).thenReturn(flow { emit(points) })

        val result = repository.observeRoutePoints("route-1").first()

        assertEquals(1, result.size)
    }

    // --- trackingCount ---

    @Test
    fun `trackingCount returns count from DAO`() = runTest {
        whenever(snapshotDao.observeCount()).thenReturn(flow { emit(42) })

        val result = repository.trackingCount.first()

        assertEquals(42, result)
    }

    // --- deleteRoute ---

    @Test
    fun `deleteRoute returns success`() = runTest {
        whenever(trackRouteDao.deleteById("r1")).then { }

        val result = repository.deleteRoute("r1")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `deleteRoute returns error when DAO throws`() = runTest {
        whenever(trackRouteDao.deleteById("r1")).thenThrow(RuntimeException("Delete failed"))

        val result = repository.deleteRoute("r1")

        assertTrue(result.isError)
    }

    // --- beginRoute ---

    @Test
    fun `beginRoute creates route with current location`() = runTest {
        val location = mock<android.location.Location> {
            on { latitude } doReturn 44.43
            on { longitude } doReturn 26.10
        }
        whenever(trackerService.currentLocation).thenReturn(MutableStateFlow(location))
        whenever(trackRouteDao.insert(any())).then { }

        val routeId = repository.beginRoute()

        assertNotNull(routeId)
        assertTrue(routeId.isNotEmpty())
        argumentCaptor<TrackRouteEntity>().apply {
            verify(trackRouteDao).insert(capture())
            assertEquals(44.43, firstValue.startLat, 0.001)
            assertEquals(26.10, firstValue.startLon, 0.001)
            assertTrue(firstValue.isActive)
        }
        verify(trackerService).activeRouteId = routeId
    }

    @Test
    fun `beginRoute uses zero coordinates when no location`() = runTest {
        whenever(trackerService.currentLocation).thenReturn(MutableStateFlow(null))
        whenever(trackRouteDao.insert(any())).then { }

        repository.beginRoute()

        argumentCaptor<TrackRouteEntity>().apply {
            verify(trackRouteDao).insert(capture())
            assertEquals(0.0, firstValue.startLat, 0.001)
            assertEquals(0.0, firstValue.startLon, 0.001)
        }
    }

    // --- endRoute ---

    @Test
    fun `endRoute finalizes route with points and location`() = runTest {
        val routeId = "route-123"
        whenever(trackerService.activeRouteId).thenReturn(routeId)
        val location = mock<android.location.Location> {
            on { latitude } doReturn 44.50
            on { longitude } doReturn 26.20
        }
        whenever(trackerService.currentLocation).thenReturn(MutableStateFlow(location))

        val points = listOf(
            LocationSnapshotEntity("s1", 44.43, 26.10, 100.0, 5f, 1000L, true, routeId),
            LocationSnapshotEntity("s2", 44.44, 26.11, 100.0, 5f, 2000L, true, routeId),
            LocationSnapshotEntity("s3", 44.45, 26.12, 100.0, 5f, 3000L, true, routeId)
        )
        whenever(snapshotDao.getByRouteId(routeId)).thenReturn(points)
        whenever(trackRouteDao.finalizeRoute(any(), any(), any(), any(), any())).then { }

        repository.endRoute()

        verify(trackRouteDao).finalizeRoute(
            eq(routeId),
            eq(44.50),
            eq(26.20),
            eq(3),
            anyDouble()
        )
        verify(trackerService).activeRouteId = null
    }

    @Test
    fun `endRoute does nothing when no active route`() = runTest {
        whenever(trackerService.activeRouteId).thenReturn(null)

        repository.endRoute()

        verifyNoInteractions(snapshotDao)
        verifyNoInteractions(trackRouteDao)
    }
}
