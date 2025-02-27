package com.example.cmiyc.repositories

import com.example.cmiyc.api.ApiClient
import com.example.cmiyc.api.dto.*
import com.example.cmiyc.data.Log
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

    private val _logs = MutableStateFlow<List<Log>>(emptyList())
    val logs: StateFlow<List<Log>> = _logs

    // Queue for pending location updates
    private val locationUpdateQueue = ConcurrentLinkedQueue<LocationDTO>()

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
            val latestUpdate = locationUpdateQueue.poll() ?: return

            try {
                val locationUpdateRequest = LocationUpdateRequestDTO(latestUpdate)
                val response = api.updateUserLocation(userId, locationUpdateRequest)
                if (response.isSuccessful) {
                    _currentUser.value = _currentUser.value?.copy(
                        currentLocation = Point.fromLngLat(
                            latestUpdate.longitude,
                            latestUpdate.latitude,
                        ),
                        lastLocationUpdate = latestUpdate.timestamp
                    )
                    locationUpdateQueue.clear()
                } else {
                    locationUpdateQueue.offer(latestUpdate)
                }
            } catch (e: Exception) {
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
        val request = LocationDTO(
            longitude = location.longitude(),
            latitude = location.latitude(),
            timestamp = System.currentTimeMillis(),
        )
        locationUpdateQueue.offer(request)
    }

    suspend fun registerUser() {
        val userRegistrationRequest = UserRegistrationRequestDTO(
            userID = _currentUser.value?.userId ?: "",
            displayName = _currentUser.value?.displayName ?: "",
            email = _currentUser.value?.email ?: "",
            photoURL = _currentUser.value?.photoUrl ?: "",
            fcmToken = _currentUser.value?.fcmToken ?: "",
        )
        val response = api.registerUser(userRegistrationRequest)
        if (!response.isSuccessful) {
            throw Exception("Failed to register user: ${response.code()}")
        }
    }

    suspend fun broadcastMessage(activity: String) {
        val response = api.broadcastMessage(
            userId = getCurrentUserId(),
            eventName = activity,
        )
        if (!response.isSuccessful) {
            throw Exception("Failed to broadcast message: ${response.code()}")
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
            if (response.isSuccessful) {
                val logs = response.body()?.map { it.toLog() } ?: emptyList()
                _logs.value = logs
            } else {
                throw Exception("Failed to fetch logs: ${response.code()}")
            }
        } catch (e: Exception) {
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