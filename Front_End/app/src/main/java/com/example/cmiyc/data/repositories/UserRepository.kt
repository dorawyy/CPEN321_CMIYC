package com.example.cmiyc.repositories

import com.example.cmiyc.api.ApiClient
import com.example.cmiyc.api.dto.*
import com.example.cmiyc.data.Log
import com.example.cmiyc.data.User
import com.mapbox.geojson.Point
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration.Companion.seconds

object UserRepository {
    private val api = ApiClient.apiService
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _logs = MutableStateFlow<List<Log>>(emptyList())
    val logs: StateFlow<List<Log>> = _logs

    // Queue for pending location updates
    private val locationUpdateQueue = ConcurrentLinkedQueue<LocationDTO>()

    // Flag to track if update job is running
    private var isUpdating = false

    // Error tracking for backoff
    private var consecutiveFailures = 0
    private val maxRetryDelay = 60.seconds

    init {
        startLocationUpdateWorker()
    }

    private fun startLocationUpdateWorker() {
        scope.launch {
            while (isActive) {
                if (isAuthenticated() && locationUpdateQueue.isNotEmpty()) {
                    processLocationUpdates()
                }
                // Exponential backoff for retry delay
                val delay = if (consecutiveFailures > 0) {
                    val backoffDelay = (1.seconds * (1 shl minOf(consecutiveFailures, 5)))
                    minOf(backoffDelay, maxRetryDelay)
                } else {
                    1.seconds
                }
                delay(delay)
            }
        }
    }

    private suspend fun processLocationUpdates() {
        if (isUpdating) return
        isUpdating = true

        try {
            val userId = getCurrentUserId()
            val latestUpdate = locationUpdateQueue.poll() ?: return

            try {
                val locationUpdateRequest = LocationUpdateRequestDTO(latestUpdate)
                val response = api.updateUserLocation(userId, locationUpdateRequest)
                if (response.isSuccessful) {
                    _currentUser.value = latestUpdate.timestamp.let {
                        _currentUser.value?.copy(
                            currentLocation = Point.fromLngLat(
                                latestUpdate.longitude,
                                latestUpdate.latitude,
                            ),
                            lastLocationUpdate = it
                        )
                    }
                    locationUpdateQueue.clear()
                    consecutiveFailures = 0 // Reset failure counter on success
                } else {
                    locationUpdateQueue.offer(latestUpdate)
                    consecutiveFailures++
                    println("Failed to update location: ${response.code()} (Failures: $consecutiveFailures)")
                }
            } catch (e: SocketTimeoutException) {
                // Handle timeout specifically
                locationUpdateQueue.offer(latestUpdate)
                consecutiveFailures++
                println("Network timeout when updating location: ${e.message} (Failures: $consecutiveFailures)")
            } catch (e: IOException) {
                // Handle network errors
                locationUpdateQueue.offer(latestUpdate)
                consecutiveFailures++
                println("Network error when updating location: ${e.message} (Failures: $consecutiveFailures)")
            } catch (e: Exception) {
                locationUpdateQueue.offer(latestUpdate)
                consecutiveFailures++
                println("Error updating location: ${e.message} (Failures: $consecutiveFailures)")
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
        _currentUser.value = User(
            credentials.userId,
            credentials.email,
            credentials.displayName,
            credentials.fcmToken,
            credentials.photoUrl)
    }

    fun updateUserLocation(location: Point) {
        val request = LocationDTO(
            longitude = location.longitude(),
            latitude = location.latitude(),
            timestamp = System.currentTimeMillis(),
        )
        locationUpdateQueue.offer(request)
    }

    suspend fun registerUser() {
        try {
            val userRegistrationRequest = UserRegistrationRequestDTO(
                userID = _currentUser.value?.userId ?: "",
                displayName = _currentUser.value?.displayName ?: "",
                email = _currentUser.value?.email ?: "",
                photoURL = _currentUser.value?.photoUrl ?: "",
                fcmToken = _currentUser.value?.fcmToken ?: "",
                currentLocation = LocationDTO(0.0, 0.0, 0),
            )
            val response = api.registerUser(userRegistrationRequest)
            println("Register user response: $response")
            if (!response.isSuccessful) {
                throw Exception("Failed to register user: ${response.code()}")
            }
        } catch (e: SocketTimeoutException) {
            println("Network timeout when registering user: ${e.message}")
            throw e
        } catch (e: IOException) {
            println("Network error when registering user: ${e.message}")
            throw e
        } catch (e: Exception) {
            println("Error registering user: ${e.message}")
            throw e
        }
    }

    suspend fun broadcastMessage(activity: String) {
        try {
            val response = api.broadcastMessage(
                userId = getCurrentUserId(),
                eventName = BroadcastMessageRequestDTO(activity),
            )
            println("Broadcast message response: $response")
            if (!response.isSuccessful) {
                throw Exception("Failed to broadcast message: ${response.code()}")
            }
        } catch (e: SocketTimeoutException) {
            println("Network timeout when broadcasting message: ${e.message}")
            throw e
        } catch (e: IOException) {
            println("Network error when broadcasting message: ${e.message}")
            throw e
        } catch (e: Exception) {
            println("Error broadcasting message: ${e.message}")
            throw e
        }
    }

    fun clearCurrentUser() {
        _currentUser.value = null
    }

    private fun LogDTO.toLog(): Log = Log(
        sender = sender_name,
        activity = eventName,
        senderLocation = sender_location.let { Point.fromLngLat(it.longitude, it.latitude) },
        timestamp = timestamp
    )

    suspend fun refreshLogs() {
        try {
            val userId = _currentUser.value?.userId ?: return
            val response = api.getLogs(userId)
            println("Get Logs response: $response")
            if (response.isSuccessful) {
                val logs = response.body()?.map { it.toLog() } ?: emptyList()
                _logs.value = logs
            } else {
                throw Exception("Failed to fetch logs: ${response.code()}")
            }
        } catch (e: SocketTimeoutException) {
            println("Network timeout when refreshing logs: ${e.message}")
            throw e
        } catch (e: IOException) {
            println("Network error when refreshing logs: ${e.message}")
            throw e
        } catch (e: Exception) {
            println("Error refreshing logs: ${e.message}")
            throw e
        }
    }

    fun getLogs(): List<Log> {
        return _logs.value
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