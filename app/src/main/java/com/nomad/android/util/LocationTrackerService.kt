package com.nomad.android.util

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.nomad.android.data.local.entity.LocationSnapshotEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

interface LocationSnapshotDb {
    suspend fun saveSnapshot(snapshot: LocationSnapshotEntity)
}

class LocationTrackerService(
    private val context: Application,
    private val snapshotDb: LocationSnapshotDb? = null
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    @Volatile
    var activeRouteId: String? = null

    private var fusedClient: FusedLocationProviderClient? = null
    private var useFallback = false
    private var fallbackLocationManager: LocationManager? = null
    private var fallbackListener: LocationListener? = null

    // One-shot timeout plumbing: the one-shot listeners registered in
    // requestSingleUpdate / requestFallbackSingleUpdate unregister themselves
    // only when a location fix arrives. If no fix ever arrives (indoors,
    // location off, missing provider), the callback lives forever. A 20-second
    // timeout forces cleanup so the listener doesn't leak until process death.
    private val mainHandler = Handler(Looper.getMainLooper())
    private var oneShotPlayServicesCallback: LocationCallback? = null
    private var oneShotFallbackListener: LocationListener? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                _currentLocation.value = location
                if (_isTracking.value) {
                    saveSnapshot(location, isTracking = true)
                }
            }
        }
    }

    init {
        try {
            fusedClient = LocationServices.getFusedLocationProviderClient(context)
        } catch (e: Exception) {
            useFallback = true
            fallbackLocationManager =
                context.getSystemService(android.content.Context.LOCATION_SERVICE) as? LocationManager
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (_isTracking.value) return
        _isTracking.value = true

        if (!useFallback) {
            fusedClient?.let { client ->
                val request = LocationRequest.Builder(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    TimeUnit.SECONDS.toMillis(60)
                ).build()

                client.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            }
        } else {
            startFallbackTracking()
        }
    }

    fun stopTracking() {
        _isTracking.value = false
        fusedClient?.removeLocationUpdates(locationCallback)
        stopFallbackTracking()
    }

    @SuppressLint("MissingPermission")
    fun requestSingleUpdate() {
        if (!useFallback) {
            fusedClient?.let { client ->
                val request = LocationRequest.Builder(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    TimeUnit.SECONDS.toMillis(10)
                ).setMaxUpdates(1).build()

                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        result.lastLocation?.let { location ->
                            _currentLocation.value = location
                        }
                        client.removeLocationUpdates(this)
                        mainHandler.removeCallbacksAndMessages(null)
                        oneShotPlayServicesCallback = null
                    }
                }
                oneShotPlayServicesCallback = callback
                client.requestLocationUpdates(request, callback, Looper.getMainLooper())

                // Force-unregister if no fix arrives within 20 seconds.
                // Without this, the callback leaks until process death.
                mainHandler.postDelayed({
                    oneShotPlayServicesCallback?.let { cb ->
                        client.removeLocationUpdates(cb)
                        oneShotPlayServicesCallback = null
                    }
                }, TimeUnit.SECONDS.toMillis(20))
            }
        } else {
            requestFallbackSingleUpdate()
        }
    }

    private fun saveSnapshot(location: Location, isTracking: Boolean) {
        val snapshot = LocationSnapshotEntity(
            id = UUID.randomUUID().toString(),
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            accuracy = location.accuracy,
            timestamp = System.currentTimeMillis(),
            isTracking = isTracking,
            routeId = activeRouteId
        )
        snapshotDb?.let { db ->
            scope.launch(Dispatchers.IO) {
                try {
                    db.saveSnapshot(snapshot)
                } catch (e: Exception) {
                    Log.e("LocationTracker", "Failed to save location snapshot", e)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startFallbackTracking() {
        fallbackLocationManager?.let { lm ->
            val provider = when {
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                    LocationManager.GPS_PROVIDER
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                    LocationManager.NETWORK_PROVIDER
                else -> return
            }

            val listener = LocationListener { location ->
                _currentLocation.value = location
                if (_isTracking.value) {
                    saveSnapshot(location, isTracking = true)
                }
            }
            fallbackListener = listener

            try {
                lm.requestLocationUpdates(
                    provider,
                    60_000L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            } catch (e: Exception) {
                Log.w(TAG, "Fallback location updates request failed", e)
            }
        }
    }

    private fun stopFallbackTracking() {
        fallbackListener?.let { listener ->
            fallbackLocationManager?.removeUpdates(listener)
            fallbackListener = null
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestFallbackSingleUpdate() {
        fallbackLocationManager?.let { lm ->
            val provider = when {
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                    LocationManager.GPS_PROVIDER
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                    LocationManager.NETWORK_PROVIDER
                else -> return
            }
            try {
                // Request a genuine fresh fix via a one-shot listener rather than
                // returning getLastKnownLocation(), which may be minutes or hours
                // stale. The listener publishes the first received location to
                // _currentLocation and unregisters itself immediately, mirroring
                // the Play Services single-update path.
                val oneShot = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        _currentLocation.value = location
                        lm.removeUpdates(this)
                        oneShotFallbackListener = null
                        mainHandler.removeCallbacksAndMessages(null)
                    }
                }
                oneShotFallbackListener = oneShot
                lm.requestLocationUpdates(
                    provider,
                    0L,
                    0f,
                    oneShot,
                    Looper.getMainLooper()
                )
                // Force-unregister if no fix arrives within 20 seconds.
                // Without this, the listener leaks until process death.
                mainHandler.postDelayed({
                    oneShotFallbackListener?.let { listener ->
                        lm.removeUpdates(listener)
                        oneShotFallbackListener = null
                    }
                }, TimeUnit.SECONDS.toMillis(20))
            } catch (e: Exception) {
                Log.w(TAG, "Fallback single location request failed", e)
            }
        }
    }

    /**
     * Cancel the internal CoroutineScope. Call during DI teardown or tests
     * to prevent leaked coroutines from outliving the tracker.
     */
    fun destroy() {
        scope.cancel()
        stopTracking()
        // Clean up any pending one-shot timeouts and force-unregister leaked
        // callbacks so they don't outlive the tracker.
        mainHandler.removeCallbacksAndMessages(null)
        oneShotPlayServicesCallback?.let { fusedClient?.removeLocationUpdates(it) }
        oneShotPlayServicesCallback = null
        oneShotFallbackListener?.let { fallbackLocationManager?.removeUpdates(it) }
        oneShotFallbackListener = null
    }

    companion object {
        private const val TAG = "LocationTracker"
    }
}
