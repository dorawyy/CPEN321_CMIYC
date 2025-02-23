package com.example.cmiyc.location

import android.annotation.SuppressLint
import android.content.Context
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
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _currentLocation = MutableStateFlow<Point?>(null)
    val currentLocation: StateFlow<Point?> = _currentLocation

    private var locationCallback: LocationCallback? = null

    private val UPDATE_INTERVAL_IN_MILLISECONDS = 30000L // 30 seconds
    private val FASTEST_INTERVAL = 20000L // 20 seconds
    private val MIN_ACCURACY = 30f // 30 meters

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
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
                println("Location update received: ${locationResult.lastLocation}")
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

            // Start location updates
            locationCallback?.let {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    it,
                    Looper.getMainLooper()
                ).addOnSuccessListener {
                    println("Location updates successfully requested")
                }.addOnFailureListener { e ->
                    println("Failed to request location updates: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("Exception requesting location updates: ${e.message}")
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    fun getCurrentLocation() {
        if (_currentLocation.value == null) {
            startLocationUpdates()
        }
    }
}