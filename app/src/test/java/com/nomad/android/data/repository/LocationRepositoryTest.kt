package com.nomad.android.data.repository

import android.location.Location
import com.nomad.android.data.local.dao.LocationSavedPointDao
import com.nomad.android.data.local.dao.LocationSnapshotDao
import com.nomad.android.data.local.dao.TrackRouteDao
import com.nomad.android.data.local.entity.LocationSavedPointEntity
import com.nomad.android.data.local.entity.LocationSnapshotEntity
import com.nomad.android.data.local.entity.TrackRouteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var snapshotDao: FakeLocationSnapshotDao
    private lateinit var savedPointDao: FakeLocationSavedPointDao
    private lateinit var trackRouteDao: FakeTrackRouteDao
    private lateinit var tracker: FakeLocationTracker
    private lateinit var repository: LocationRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        snapshotDao = FakeLocationSnapshotDao()
        savedPointDao = FakeLocationSavedPointDao()
        trackRouteDao = FakeTrackRouteDao()
        tracker = FakeLocationTracker()
        repository = LocationRepository(snapshotDao, savedPointDao, trackRouteDao, tracker)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- startTracking ---

    @Test
    fun `startTracking returns success with routeId`() {
        tracker.activeRouteId = "route-123"
        val result = repository.startTracking()
        assertTrue(result.isSuccess)
        assertEquals("route-123", result.getOrNull())
        assertTrue(tracker.trackingStarted)
    }

    @Test
    fun `startTracking returns success with empty string when no active route`() {
        tracker.activeRouteId = null
        val result = repository.startTracking()
        assertTrue(result.isSuccess)
        assertEquals("", result.getOrNull())
    }

    @Test
    fun `startTracking returns error when tracker throws`() {
        tracker.shouldThrow = true
        val result = repository.startTracking()
        assertTrue(result.isError)
    }

    // --- stopTracking ---

    @Test
    fun `stopTracking returns success`() {
        val result = repository.stopTracking()
        assertTrue(result.isSuccess)
        assertTrue(tracker.trackingStopped)
    }

    @Test
    fun `stopTracking returns error when tracker throws`() {
        tracker.shouldThrow = true
        val result = repository.stopTracking()
        assertTrue(result.isError)
    }

    // --- requestCurrentLocation ---

    @Test
    fun `requestCurrentLocation returns success`() {
        val result = repository.requestCurrentLocation()
        assertTrue(result.isSuccess)
        assertTrue(tracker.singleUpdateRequested)
    }

    @Test
    fun `requestCurrentLocation returns error when tracker throws`() {
        tracker.shouldThrow = true
        val result = repository.requestCurrentLocation()
        assertTrue(result.isError)
    }

    // --- saveCurrentLocation ---

    @Test
    fun `saveCurrentLocation returns error when no current location`() = runTest {
        tracker.setLocation(null)
        val result = repository.saveCurrentLocation("Camp", "Nice spot")
        assertTrue(result.isError)
        assertEquals("No current location available", (result as com.nomad.android.data.Result.Error).message)
    }

    @Test
    fun `saveCurrentLocation saves point with correct coordinates`() = runTest {
        val loc = Location("test").apply {
            latitude = 40.7128
            longitude = -74.0060
            altitude = 10.5
        }
        tracker.setLocation(loc)

        val result = repository.saveCurrentLocation("Camp", "Nice spot")
        assertTrue(result.isSuccess)

        assertEquals(1, savedPointDao.insertedPoints.size)
        val saved = savedPointDao.insertedPoints.first()
        assertEquals("Camp", saved.name)
        assertEquals("Nice spot", saved.notes)
        assertEquals(40.7128, saved.latitude, 0.001)
        assertEquals(-74.0060, saved.longitude, 0.001)
        assertEquals(10.5, saved.altitude, 0.001)
        assertTrue(saved.id.isNotBlank())
        assertTrue(saved.timestamp > 0)
    }

    @Test
    fun `saveCurrentLocation returns error when DAO throws`() = runTest {
        val loc = Location("test").apply { latitude = 1.0; longitude = 2.0; altitude = 0.0 }
        tracker.setLocation(loc)
        savedPointDao.shouldFail = true

        val result = repository.saveCurrentLocation("Camp", "")
        assertTrue(result.isError)
    }

    // --- deleteSavedPoint ---

    @Test
    fun `deleteSavedPoint returns success`() = runTest {
        val result = repository.deleteSavedPoint("point-1")
        assertTrue(result.isSuccess)
        assertEquals("point-1", savedPointDao.deletedId)
    }

    @Test
    fun `deleteSavedPoint returns error when DAO throws`() = runTest {
        savedPointDao.shouldFail = true
        val result = repository.deleteSavedPoint("point-1")
        assertTrue(result.isError)
    }

    // --- cleanOldSnapshots ---

    @Test
    fun `cleanOldSnapshots deletes snapshots older than threshold`() = runTest {
        val result = repository.cleanOldSnapshots(daysOld = 7)
        assertTrue(result.isSuccess)
        assertNotNull(snapshotDao.deletedOlderThanCutoff)
        // Cutoff should be roughly now minus 7 days
        val expectedCutoff = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        // Allow 5 seconds tolerance since the call takes nonzero time
        assertTrue(Math.abs(snapshotDao.deletedOlderThanCutoff!! - expectedCutoff) < 5000)
    }

    @Test
    fun `cleanOldSnapshots defaults to 30 days`() = runTest {
        val result = repository.cleanOldSnapshots()
        assertTrue(result.isSuccess)
        val expectedCutoff = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        assertTrue(Math.abs(snapshotDao.deletedOlderThanCutoff!! - expectedCutoff) < 5000)
    }

    @Test
    fun `cleanOldSnapshots returns error when DAO throws`() = runTest {
        snapshotDao.shouldFail = true
        val result = repository.cleanOldSnapshots()
        assertTrue(result.isError)
    }

    // --- deleteRoute ---

    @Test
    fun `deleteRoute returns success`() = runTest {
        val result = repository.deleteRoute("route-1")
        assertTrue(result.isSuccess)
        assertEquals("route-1", trackRouteDao.deletedId)
    }

    @Test
    fun `deleteRoute returns error when DAO throws`() = runTest {
        trackRouteDao.shouldFail = true
        val result = repository.deleteRoute("route-1")
        assertTrue(result.isError)
    }

    // --- beginRoute ---

    @Test
    fun `beginRoute creates route with current location and sets active route`() = runTest {
        val loc = Location("test").apply { latitude = 51.5074; longitude = -0.1278 }
        tracker.setLocation(loc)

        val routeId = repository.beginRoute()
        assertTrue(routeId.isNotBlank())

        assertEquals(1, trackRouteDao.insertedRoutes.size)
        val route = trackRouteDao.insertedRoutes.first()
        assertEquals(routeId, route.id)
        assertEquals(51.5074, route.startLat, 0.001)
        assertEquals(-0.1278, route.startLon, 0.001)
        assertTrue(route.isActive)
        assertTrue(route.name.startsWith("Track "))
        assertEquals(routeId, tracker.activeRouteId)
    }

    @Test
    fun `beginRoute uses 0,0 when no location available`() = runTest {
        tracker.setLocation(null)

        val routeId = repository.beginRoute()
        val route = trackRouteDao.insertedRoutes.first()
        assertEquals(0.0, route.startLat, 0.001)
        assertEquals(0.0, route.startLon, 0.001)
    }

    // --- endRoute ---

    @Test
    fun `endRoute does nothing when no active route`() = runTest {
        tracker.activeRouteId = null
        repository.endRoute()
        assertNull(trackRouteDao.finalizedRouteId)
    }

    @Test
    fun `endRoute finalizes route with distance calculation`() = runTest {
        tracker.activeRouteId = "route-1"

        // Two points roughly 1 degree apart in latitude at the equator ≈ 111km
        snapshotDao.routePoints = listOf(
            LocationSnapshotEntity(
                id = "p1", latitude = 0.0, longitude = 0.0,
                altitude = 0.0, accuracy = 10f, timestamp = 1000,
                isTracking = true, routeId = "route-1"
            ),
            LocationSnapshotEntity(
                id = "p2", latitude = 1.0, longitude = 0.0,
                altitude = 0.0, accuracy = 10f, timestamp = 2000,
                isTracking = true, routeId = "route-1"
            )
        )

        val endLoc = Location("test").apply { latitude = 1.0; longitude = 0.0 }
        tracker.setLocation(endLoc)

        repository.endRoute()

        assertEquals("route-1", trackRouteDao.finalizedRouteId)
        assertEquals(1.0, trackRouteDao.finalizedEndLat!!, 0.001)
        assertEquals(0.0, trackRouteDao.finalizedEndLon!!, 0.001)
        assertEquals(2, trackRouteDao.finalizedPointCount)
        // 1 degree latitude ≈ 111,195 meters (haversine at equator)
        assertTrue("Expected ~111km, got ${trackRouteDao.finalizedDistance}m",
            trackRouteDao.finalizedDistance!! > 110_000)
        assertTrue("Expected ~111km, got ${trackRouteDao.finalizedDistance}m",
            trackRouteDao.finalizedDistance!! < 112_000)
        assertNull(tracker.activeRouteId)
    }

    @Test
    fun `endRoute falls back to last point when no current location`() = runTest {
        tracker.activeRouteId = "route-1"
        tracker.setLocation(null)

        snapshotDao.routePoints = listOf(
            LocationSnapshotEntity(
                id = "p1", latitude = 40.0, longitude = -74.0,
                altitude = 0.0, accuracy = 10f, timestamp = 1000,
                isTracking = true, routeId = "route-1"
            )
        )

        repository.endRoute()

        assertEquals(40.0, trackRouteDao.finalizedEndLat!!, 0.001)
        assertEquals(-74.0, trackRouteDao.finalizedEndLon!!, 0.001)
        assertEquals(1, trackRouteDao.finalizedPointCount)
        assertEquals(0.0, trackRouteDao.finalizedDistance!!, 0.001)
    }

    @Test
    fun `endRoute calculates distance correctly for multi-point route`() = runTest {
        tracker.activeRouteId = "route-1"
        tracker.setLocation(null)

        // Three points along the equator: 0°, 1°, 2° longitude
        // Each degree ≈ 111,195m, total ≈ 222,390m
        snapshotDao.routePoints = listOf(
            LocationSnapshotEntity(
                id = "p1", latitude = 0.0, longitude = 0.0,
                altitude = 0.0, accuracy = 10f, timestamp = 1000,
                isTracking = true, routeId = "route-1"
            ),
            LocationSnapshotEntity(
                id = "p2", latitude = 0.0, longitude = 1.0,
                altitude = 0.0, accuracy = 10f, timestamp = 2000,
                isTracking = true, routeId = "route-1"
            ),
            LocationSnapshotEntity(
                id = "p3", latitude = 0.0, longitude = 2.0,
                altitude = 0.0, accuracy = 10f, timestamp = 3000,
                isTracking = true, routeId = "route-1"
            )
        )

        repository.endRoute()

        assertEquals(3, trackRouteDao.finalizedPointCount)
        val dist = trackRouteDao.finalizedDistance!!
        assertTrue("Expected ~222km, got ${dist}m", dist > 221_000)
        assertTrue("Expected ~222km, got ${dist}m", dist < 224_000)
    }

    @Test
    fun `endRoute clears active route id`() = runTest {
        tracker.activeRouteId = "route-1"
        snapshotDao.routePoints = emptyList()
        tracker.setLocation(null)

        repository.endRoute()
        assertNull(tracker.activeRouteId)
    }

    // --- getRoutePoints ---

    @Test
    fun `getRoutePoints returns points from DAO`() = runTest {
        val points = listOf(
            LocationSnapshotEntity(
                id = "p1", latitude = 1.0, longitude = 2.0,
                altitude = 0.0, accuracy = 5f, timestamp = 1000,
                isTracking = true, routeId = "r1"
            )
        )
        snapshotDao.routePoints = points

        val result = repository.getRoutePoints("r1")
        assertEquals(1, result.size)
        assertEquals("p1", result.first().id)
    }

    // --- Fake implementations ---

    class FakeLocationTracker : LocationTracker {
        private val _currentLocation = MutableStateFlow<Location?>(null)
        override val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

        private val _isTracking = MutableStateFlow(false)
        override val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

        override var activeRouteId: String? = null

        var shouldThrow = false
        var trackingStarted = false
        var trackingStopped = false
        var singleUpdateRequested = false

        fun setLocation(location: Location?) {
            _currentLocation.value = location
        }

        override fun startTracking() {
            if (shouldThrow) throw RuntimeException("Tracker error")
            trackingStarted = true
            _isTracking.value = true
        }

        override fun stopTracking() {
            if (shouldThrow) throw RuntimeException("Tracker error")
            trackingStopped = true
            _isTracking.value = false
        }

        override fun requestSingleUpdate() {
            if (shouldThrow) throw RuntimeException("Tracker error")
            singleUpdateRequested = true
        }
    }

    class FakeLocationSnapshotDao(
        var routePoints: List<LocationSnapshotEntity> = emptyList(),
        var shouldFail: Boolean = false
    ) : LocationSnapshotDao {

        var deletedOlderThanCutoff: Long? = null

        override suspend fun insert(snapshot: LocationSnapshotEntity) {}
        override suspend fun insertAll(snapshots: List<LocationSnapshotEntity>) {}
        override fun getRecent(limit: Int) = flowOf(emptyList<LocationSnapshotEntity>())
        override fun getLatest() = flowOf(null as LocationSnapshotEntity?)
        override suspend fun getTrackingSnapshots(sinceMillis: Long) = emptyList<LocationSnapshotEntity>()

        override suspend fun deleteOlderThan(beforeMillis: Long) {
            if (shouldFail) throw RuntimeException("DB error")
            deletedOlderThanCutoff = beforeMillis
        }

        override suspend fun count() = 0
        override fun observeCount() = flowOf(0)

        override suspend fun getByRouteId(routeId: String): List<LocationSnapshotEntity> {
            if (shouldFail) throw RuntimeException("DB error")
            return routePoints
        }

        override fun observeByRouteId(routeId: String) = flowOf(routePoints)
    }

    class FakeLocationSavedPointDao(
        var shouldFail: Boolean = false
    ) : LocationSavedPointDao {

        val insertedPoints = mutableListOf<LocationSavedPointEntity>()
        var deletedId: String? = null

        override suspend fun insert(point: LocationSavedPointEntity) {
            if (shouldFail) throw RuntimeException("DB error")
            insertedPoints.add(point)
        }

        override fun getAll() = flowOf(emptyList<LocationSavedPointEntity>())

        override suspend fun deleteById(id: String) {
            if (shouldFail) throw RuntimeException("DB error")
            deletedId = id
        }
    }

    class FakeTrackRouteDao(
        var shouldFail: Boolean = false
    ) : TrackRouteDao {

        val insertedRoutes = mutableListOf<TrackRouteEntity>()
        var deletedId: String? = null
        var finalizedRouteId: String? = null
        var finalizedEndLat: Double? = null
        var finalizedEndLon: Double? = null
        var finalizedPointCount: Int = 0
        var finalizedDistance: Double? = null

        override suspend fun insert(route: TrackRouteEntity) {
            if (shouldFail) throw RuntimeException("DB error")
            insertedRoutes.add(route)
        }

        override fun getAll() = flowOf(emptyList<TrackRouteEntity>())
        override suspend fun getActiveRoute(): TrackRouteEntity? = null
        override suspend fun getById(id: String): TrackRouteEntity? = null

        override suspend fun finalizeRoute(
            id: String, endLat: Double, endLon: Double, pointCount: Int, totalDistanceMeters: Double
        ) {
            if (shouldFail) throw RuntimeException("DB error")
            finalizedRouteId = id
            finalizedEndLat = endLat
            finalizedEndLon = endLon
            finalizedPointCount = pointCount
            finalizedDistance = totalDistanceMeters
        }

        override suspend fun deleteById(id: String) {
            if (shouldFail) throw RuntimeException("DB error")
            deletedId = id
        }
    }
}
