package com.example.cmiyc.repositories

import com.example.cmiyc.api.ApiClient
import com.example.cmiyc.api.dto.*
import com.example.cmiyc.data.AdminUserItem
import com.example.cmiyc.data.Log
import com.example.cmiyc.data.User
import com.mapbox.geojson.Point
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import retrofit2.HttpException
import java.io.IOException
import java.lang.RuntimeException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration.Companion.seconds

object UserRepository {
    private val api = ApiClient.apiService
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    // Add registration complete flag
    private val _isRegistrationComplete = MutableStateFlow(false)
    val isRegistrationComplete: StateFlow<Boolean> = _isRegistrationComplete

    private val _logs = MutableStateFlow<List<Log>>(emptyList())
    val logs: StateFlow<List<Log>> = _logs

    // Add isAdmin flag
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    // For admin functionality
    private val _adminUsers = MutableStateFlow<List<AdminUserItem>>(emptyList())
    val adminUsers: StateFlow<List<AdminUserItem>> = _adminUsers

    // Queue for pending location updates
    private val locationUpdateQueue = ConcurrentLinkedQueue<LocationDTO>()

    // Flag to track if update job is running
    private var isUpdating = false

    // Error tracking for backoff
    private var consecutiveFailures = 0
    private val maxRetryDelay = 60.seconds

    // Location update state
    private val _locationUpdateError = MutableStateFlow<String?>(null)
    val locationUpdateError: StateFlow<String?> = _locationUpdateError

    // Maximum failures before showing an error
    private val maxConsecutiveFailures = 20

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
                val startTime = System.currentTimeMillis()
                val response = api.updateUserLocation(userId, locationUpdateRequest)
                val endTime = System.currentTimeMillis()
                println("Location Update API call took ${endTime - startTime} ms")
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

                    // Clear any existing error
                    _locationUpdateError.value = null
                } else {
                    locationUpdateQueue.offer(latestUpdate)
                    consecutiveFailures++
                    println("Failed to update location: ${response.code()} (Failures: $consecutiveFailures)")
                    checkConsecutiveFailures("Failed to update location. Your friends may not see your current position.")
                }
            } catch (e: SocketTimeoutException) {
                // Handle timeout specifically
                locationUpdateQueue.offer(latestUpdate)
                consecutiveFailures++
                println("Network timeout when updating location: ${e.message} (Failures: $consecutiveFailures)")
                checkConsecutiveFailures("Network timeout when updating location. Your friends may not see your current position.")
            } catch (e: IOException) {
                // Handle network errors
                locationUpdateQueue.offer(latestUpdate)
                consecutiveFailures++
                println("Network error when updating location: ${e.message} (Failures: $consecutiveFailures)")
                checkConsecutiveFailures("Network error when updating location. Your friends may not see your current position.")
            } catch (e: HttpException) {
                locationUpdateQueue.offer(latestUpdate)
                consecutiveFailures++
                println("Error updating location: ${e.message} (Failures: $consecutiveFailures)")
                checkConsecutiveFailures("Error updating location: ${e.message}. Your friends may not see your current position.")
            }
            catch (e: IllegalStateException) {
                locationUpdateQueue.offer(latestUpdate)
                consecutiveFailures++
                println("Error updating location: ${e.message} (Failures: $consecutiveFailures)")
                checkConsecutiveFailures("Error updating location: ${e.message}. Your friends may not see your current position.")
            }
        } finally {
            isUpdating = false
        }
    }

    private fun checkConsecutiveFailures(errorMessage: String) {
        if (consecutiveFailures >= maxConsecutiveFailures) {
            _locationUpdateError.value = errorMessage
        }
    }

    fun clearLocationUpdateError() {
        _locationUpdateError.value = null
        // Don't reset the counter here, let the system try to recover naturally
    }

    fun isAuthenticated(): Boolean {
        return isRegistrationComplete.value
    }

    fun getCurrentUserId(): String {
        return currentUser.value?.userId
            ?: throw IllegalStateException("User not authenticated")
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

    suspend fun setFCMToken(fcmToken: String) {
        try {
            val userId = getCurrentUserId()
            val fcmTokenRequest = FCMTokenRequestDTO(fcmToken)
            val startTime = System.currentTimeMillis()
            val response = api.setFCMToken(userId, fcmTokenRequest)
            val endTime = System.currentTimeMillis()
            println("Set FCM Token API call took ${endTime - startTime} ms")
            if (!response.isSuccessful) {
                throw HttpException(response)
            }
        } catch (e: SocketTimeoutException) {
            println("Network timeout when registering user: ${e.message}")
            throw e
        } catch (e: IOException) {
            println("Network error when registering user: ${e.message}")
            throw e
        } catch (e: HttpException) {
            println("Error registering user: ${e.message}")
            throw e
        } catch (e: IllegalStateException) {
            println("Error registering user: ${e.message}")
            throw e
        }
    }

    suspend fun registerUser(): Boolean {
        try {
            val userRegistrationRequest = UserRegistrationRequestDTO(
                userID = _currentUser.value?.userId ?: "",
                displayName = _currentUser.value?.displayName ?: "",
                email = _currentUser.value?.email ?: "",
                photoURL = _currentUser.value?.photoUrl ?: "",
                fcmToken = _currentUser.value?.fcmToken ?: "",
                currentLocation = LocationDTO(0.0, 0.0, 0),
                isAdmin = _isAdmin.value,
            )
            val response = api.registerUser(userRegistrationRequest)
            println("Register user response: $response")
            if (!response.isSuccessful) {
                throw HttpException(response)
            }

            // Handle admin status and banned status
            response.body()?.let {
                if (it.isBanned) {
                    // User is banned
                    return false
                }
            }

            _isRegistrationComplete.value = true
            return true
        } catch (e: SocketTimeoutException) {
            println("Network timeout when registering user: ${e.message}")
            throw e
        } catch (e: IOException) {
            println("Network error when registering user: ${e.message}")
            throw e
        } catch (e: HttpException) {
            println("Error registering user: ${e.message}")
            throw e
        }
    }

    suspend fun broadcastMessage(activity: String) {
        try {
            val startTime = System.currentTimeMillis()
            val response = api.broadcastMessage(
                userId = getCurrentUserId(),
                eventName = BroadcastMessageRequestDTO(activity),
            )
            println("Broadcast message response: $response")
            val endTime = System.currentTimeMillis()
            println("Broadcast Message API call took ${endTime - startTime} ms")
            if (!response.isSuccessful) {
                throw HttpException(response)
            }
        } catch (e: SocketTimeoutException) {
            println("Network timeout when broadcasting message: ${e.message}")
            throw e
        } catch (e: IOException) {
            println("Network error when broadcasting message: ${e.message}")
            throw e
        } catch (e: HttpException) {
            println("Error broadcasting message: ${e.message}")
            throw e
        } catch (e: IllegalStateException) {
            println("Error broadcasting message: ${e.message}")
            throw e
        }
    }

    fun clearCurrentUser() {
        _currentUser.value = null
        _isAdmin.value = false
        _isRegistrationComplete.value = false
    }

    private fun LogDTO.toLog(): Log = Log(
        sender = fromName,
        activity = eventName,
        senderLocation = location.let { Point.fromLngLat(it.longitude, it.latitude) },
        timestamp = location.timestamp
    )

    suspend fun refreshLogs() {
        try {
            val userId = _currentUser.value?.userId ?: return
            val startTime = System.currentTimeMillis()
            val response = api.getLogs(userId)
            println("Get Logs response: $response")
            val endTime = System.currentTimeMillis()
            println("Get Logs API call took ${endTime - startTime} ms")
            if (response.isSuccessful) {
                val logs = response.body()?.map { it.toLog() } ?: emptyList()
                _logs.value = logs
            } else {
                throw HttpException(response)
            }
        } catch (e: SocketTimeoutException) {
            println("Network timeout when refreshing logs: ${e.message}")
            throw e
        } catch (e: IOException) {
            println("Network error when refreshing logs: ${e.message}")
            throw e
        } catch (e: HttpException) {
            println("Error refreshing logs: ${e.message}")
            throw e
        }
    }

    fun getLogs(): List<Log> {
        return _logs.value
    }

    // Convert UserDTO to AdminUserItem
    private fun UserDTO.toAdminUserItem(): AdminUserItem = AdminUserItem(
        userId = userID,
        name = displayName,
        email = email,
        photoURL = photoURL,
        isBanned = isBanned
    )

    // Get all users for admin screen
    suspend fun getAllUsers(): Result<List<AdminUserItem>> {
        return try {
            val userId = _currentUser.value?.userId ?:
            return Result.failure(Exception("User not authenticated"))
            val startTime = System.currentTimeMillis()
            val response = api.getAllUsers(userId)
            val endTime = System.currentTimeMillis()
            println("Get All Users API call took ${endTime - startTime} ms")
            if (response.isSuccessful) {
                val users = response.body()?.map { it.toAdminUserItem() } ?: emptyList()
                _adminUsers.value = users
                Result.success(users)
            } else {
                Result.failure(Exception("Failed to fetch users: ${response.code()}"))
            }
        } catch (e: HttpException) {
            Result.failure(e)
        }
    }

    // Ban a user
    suspend fun banUser(targetUserId: String): Result<Unit> {
        return try {
            val userId = _currentUser.value?.userId ?:
            return Result.failure(Exception("User not authenticated"))
            val startTime = System.currentTimeMillis()
            val response = api.banUser(
                targetUserId,
                BanUserRequestDTO(adminID = userId)
            )
            val endTime = System.currentTimeMillis()
            println("Ban User API call took ${endTime - startTime} ms")

            if (response.isSuccessful) {
                // Refresh the users list after ban
                getAllUsers()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to ban user: ${response.code()}"))
            }
        } catch (e: HttpException) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            try {
                clearCurrentUser()
            } catch (e: HttpException) {
                println("Error during sign out: ${e.message}")
                throw e
            }
        }
    }

    fun cleanup() {
        scope.cancel()
    }

    fun setAdminRequested(requested: Boolean) {
        _isAdmin.value = requested
    }
}