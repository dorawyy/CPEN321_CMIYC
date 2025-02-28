package com.example.cmiyc.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.example.cmiyc.repositories.UserRepository
import com.google.android.gms.location.*
import com.mapbox.geojson.Point
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

class LocationManager @Inject constructor(
    private val context: Context,
    private val userRepository: UserRepository
) {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val scope = CoroutineScope(Dispatchers.IO) // Changed to IO dispatcher for background work

    private val _currentLocation = MutableStateFlow<Point?>(null)
    val currentLocation: StateFlow<Point?> = _currentLocation

    private var locationCallback: LocationCallback? = null
    private var locationHandlerThread: HandlerThread? = null
    private var locationHandler: Handler? = null

    private val UPDATE_INTERVAL_IN_MILLISECONDS = 5000L
    private val FASTEST_INTERVAL = 5000L
    private val MIN_ACCURACY = 30f

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        // Create a background thread for location updates
        locationHandlerThread = HandlerThread("LocationHandlerThread")
        locationHandlerThread?.start()
        locationHandler = locationHandlerThread?.looper?.let { Handler(it) }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL_IN_MILLISECONDS
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_INTERVAL)
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    if (location.hasAccuracy() && location.accuracy <= MIN_ACCURACY) {
                        val point = Point.fromLngLat(location.longitude, location.latitude)
                        _currentLocation.value = point

                        scope.launch {
                            try {
                                userRepository.updateUserLocation(point)
                            } catch (e: Exception) {
                                println("Error updating location: ${e.message}")
                            }
                        }
                    }
                }
            }
        }

        try {
            // Get last known location first
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        val point = Point.fromLngLat(it.longitude, it.latitude)
                        _currentLocation.value = point
                        scope.launch {
                            userRepository.updateUserLocation(point)
                        }
                    }
                }

            // Start location updates on the background thread
            locationCallback?.let { callback ->
                locationHandler?.let { handler ->
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        callback,
                        handler.looper
                    ).addOnSuccessListener {
                        println("Location updates successfully requested")
                    }.addOnFailureListener { e ->
                        println("Failed to request location updates: ${e.message}")
                    }
                } ?: println("Location handler is null")
            } ?: println("Location callback is null")
        } catch (e: Exception) {
            println("Exception requesting location updates: ${e.message}")
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
        locationHandlerThread?.quitSafely()
        locationHandlerThread = null
        locationHandler = null
    }

    fun getCurrentLocation() {
        if (_currentLocation.value == null) {
            startLocationUpdates()
        }
    }
}