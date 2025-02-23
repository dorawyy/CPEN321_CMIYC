package com.example.cmiyc.repositories

import com.example.cmiyc.api.ApiClient
import com.example.cmiyc.api.LocationUpdateRequest
import com.example.cmiyc.data.User
import com.mapbox.geojson.Point
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration.Companion.seconds

object UserRepository {
    private val api = ApiClient.apiService
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    // Queue for pending location updates
    private val locationUpdateQueue = ConcurrentLinkedQueue<LocationUpdateRequest>()

    // Flag to track if update job is running
    private var isUpdating = false

    init {
        startLocationUpdateWorker()
    }

    private fun startLocationUpdateWorker() {
        scope.launch {
            while (isActive) {
                if (isAuthenticated() && locationUpdateQueue.isNotEmpty()) {
                    processLocationUpdates()
                }
                delay(1.seconds)
            }
        }
    }

    private suspend fun processLocationUpdates() {
        if (isUpdating) return
        isUpdating = true

        try {
            val userId = getCurrentUserId()
            // Take the latest location update
            val latestUpdate = locationUpdateQueue.poll() ?: return

            try {
                val response = api.updateUserLocation(userId, latestUpdate)
                if (response.isSuccessful) {
                    // Update local user state
                    _currentUser.value = _currentUser.value?.copy(
                        currentLocation = Point.fromLngLat(
                            latestUpdate.longitude,
                            latestUpdate.latitude
                        ),
                        lastLocationUpdate = latestUpdate.timestamp
                    )

                    // Clear any older updates in the queue
                    locationUpdateQueue.clear()
                } else {
                    // If update failed, add it back to queue for retry
                    locationUpdateQueue.offer(latestUpdate)
                }
            } catch (e: Exception) {
                // If update failed, add it back to queue for retry
                locationUpdateQueue.offer(latestUpdate)
                throw e
            }
        } finally {
            isUpdating = false
        }
    }

    fun isAuthenticated(): Boolean {
        return _currentUser.value != null
    }

    fun getCurrentUserId(): String {
        return currentUser.value?.userId
            ?: throw Exception("User not authenticated")
    }

    fun setCurrentUser(credentials: User) {
        _currentUser.value = User(credentials.userId, credentials.email, credentials.displayName, credentials.photoUrl)
    }

    fun updateUserLocation(location: Point) {
        val request = LocationUpdateRequest(
            longitude = location.longitude(),
            latitude = location.latitude()
        )
        locationUpdateQueue.offer(request)
    }

    fun clearCurrentUser() {
        _currentUser.value = null
    }

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            try {
                clearCurrentUser()
            } catch (e: Exception) {
                throw e
            }
        }
    }

    fun cleanup() {
        scope.cancel()
    }
}