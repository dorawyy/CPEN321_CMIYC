package com.example.cmiyc.repositories

import com.example.cmiyc.api.ApiClient
import com.example.cmiyc.api.ApiService
import com.example.cmiyc.api.dto.*
import com.example.cmiyc.data.AdminUserItem
import com.example.cmiyc.data.Log
import com.example.cmiyc.data.User
import com.mapbox.geojson.Point
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration.Companion.seconds

/**
 * Central repository for managing user data and operations.
 *
 * This singleton object handles user authentication, registration, profile management,
 * and coordinates with specialized managers for location updates, activity logs,
 * and administrative functions.
 *
 * The repository exposes state flows for UI components to observe changes in user data,
 * authentication status, and admin privileges.
 */
object UserRepository {
    private val api = ApiClient.apiService
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _isRegistrationComplete = MutableStateFlow(false)
    val isRegistrationComplete: StateFlow<Boolean> = _isRegistrationComplete

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    private val _adminUsers = MutableStateFlow<List<AdminUserItem>>(emptyList())
    val adminUsers: StateFlow<List<AdminUserItem>> = _adminUsers

    val locationManager = LocationManager(scope, api, _currentUser)
    val logManager = LogManager(scope, api, _currentUser)
    val adminManager = AdminManager(scope, api, _currentUser, _adminUsers)

    /**
     * Checks if a user is currently authenticated.
     *
     * @return True if a user is authenticated and registration is complete, false otherwise.
     */
    fun isAuthenticated(): Boolean {
        return isRegistrationComplete.value
    }

    /**
     * Gets the ID of the currently authenticated user.
     *
     * @return The user ID as a String.
     * @throws IllegalStateException If no user is authenticated.
     */
    fun getCurrentUserId(): String {
        return currentUser.value?.userId
            ?: throw IllegalStateException("User not authenticated")
    }

    /**
     * Sets the current user from authentication credentials.
     *
     * This method is typically called after successful authentication
     * but before registration with the backend.
     *
     * @param credentials The user data obtained from authentication.
     */
    fun setCurrentUser(credentials: User) {
        _currentUser.value = User(
            credentials.userId,
            credentials.email,
            credentials.displayName,
            credentials.fcmToken,
            credentials.photoUrl)
    }

    /**
     * Updates the Firebase Cloud Messaging token for the current user.
     *
     * This token is used for delivering push notifications to the user's device.
     *
     * @param fcmToken The new FCM token to register.
     * @throws Various exceptions for network and server errors.
     */
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

    /**
     * Registers the current user with the backend server.
     *
     * This method should be called after authentication and setting the current user.
     * It synchronizes the user data with the backend and establishes the session.
     *
     * @return True if registration was successful, false if the user is banned.
     * @throws Various exceptions for network and server errors.
     */
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

            response.body()?.let {
                if (it.isBanned) {
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

    /**
     * Broadcasts a system message or event to relevant users.
     *
     * This method is typically used for notifications about user activity
     * or system-wide announcements.
     *
     * @param activity The activity or event name to broadcast.
     * @throws Various exceptions for network and server errors.
     */
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

    /**
     * Clears the current user data and resets authentication state.
     *
     * This is used when logging out or when authentication is invalidated.
     */
    fun clearCurrentUser() {
        _currentUser.value = null
        _isAdmin.value = false
        _isRegistrationComplete.value = false
    }

    /**
     * Signs out the current user.
     *
     * This method handles the sign-out process, clearing local user data.
     */
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

    /**
     * Sets whether the user is requesting administrative privileges.
     *
     * This is used during registration to indicate admin status request.
     *
     * @param requested True to request admin privileges, false otherwise.
     */
    fun setAdminRequested(requested: Boolean) {
        _isAdmin.value = requested
    }

    /**
     * Cleans up resources used by the repository.
     *
     * Should be called when the application is terminating or the user is signing out.
     */
    fun cleanup() {
        scope.cancel()
        locationManager.cleanup()
    }
}

/**
 * Manager class for handling user location updates.
 *
 * This class manages the queueing, throttling, and error handling for
 * user location updates. It implements exponential backoff for retries
 * and maintains the user's current location state.
 *
 * @property scope The coroutine scope for background operations.
 * @property api The API service for network requests.
 * @property _currentUser Mutable state flow of the current user.
 */
class LocationManager(
    private val scope: CoroutineScope,
    private val api: ApiService,
    private val _currentUser: MutableStateFlow<User?>
) {
    private val locationUpdateQueue = ConcurrentLinkedQueue<LocationDTO>()

    private var isUpdating = false

    private var consecutiveFailures = 0
    private val maxRetryDelay = 60.seconds

    private val _locationUpdateError = MutableStateFlow<String?>(null)
    val locationUpdateError: StateFlow<String?> = _locationUpdateError

    private val maxConsecutiveFailures = 20

    init {
        startLocationUpdateWorker()
    }

    /**
     * Starts the background worker that processes queued location updates.
     *
     * This worker handles sending updates to the server, implementing
     * exponential backoff for retries, and updating the user's location state.
     */
    private fun startLocationUpdateWorker() {
        scope.launch {
            while (isActive) {
                if (UserRepository.isAuthenticated() && locationUpdateQueue.isNotEmpty() && !isUpdating) {
                    isUpdating = true
                    try {
                        val userId = UserRepository.getCurrentUserId()
                        val latestUpdate = locationUpdateQueue.poll() ?: continue
                        try {
                            val locationUpdateRequest = LocationUpdateRequestDTO(latestUpdate)
                            val startTime = System.currentTimeMillis()
                            val response = api.updateUserLocation(userId, locationUpdateRequest)
                            val endTime = System.currentTimeMillis()
                            println("Location Update API call took ${endTime - startTime} ms")
                            if (response.isSuccessful) {
                                _currentUser.value = latestUpdate.timestamp.let {
                                    _currentUser.value?.copy(
                                        currentLocation = Point.fromLngLat(latestUpdate.longitude, latestUpdate.latitude,),
                                        lastLocationUpdate = it
                                    )
                                }
                                locationUpdateQueue.clear()
                                consecutiveFailures = 0
                                _locationUpdateError.value = null
                            } else {
                                locationUpdateQueue.offer(latestUpdate)
                                consecutiveFailures++
                                if (consecutiveFailures >= maxConsecutiveFailures) {
                                    _locationUpdateError.value = "Failed to update location. Your friends may not see your current position."
                                }
                            }
                        } catch (e: SocketTimeoutException) {
                            locationUpdateQueue.offer(latestUpdate)
                            consecutiveFailures++
                            if (consecutiveFailures >= maxConsecutiveFailures) {
                                _locationUpdateError.value = "Network timeout when updating location. Your friends may not see your current position."
                            }
                        } catch (e: IOException) {
                            locationUpdateQueue.offer(latestUpdate)
                            consecutiveFailures++
                            if (consecutiveFailures >= maxConsecutiveFailures) {
                                _locationUpdateError.value = "Network error when updating location. Your friends may not see your current position."
                            }
                        }
                    } finally {
                        isUpdating = false
                    }
                }
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

    /**
     * Updates the user's location.
     *
     * This method adds a new location update to the queue for processing.
     * The actual network request is handled by the background worker.
     *
     * @param location The new user location as a GeoJSON Point.
     */
    fun updateUserLocation(location: Point) {
        val request = LocationDTO(
            longitude = location.longitude(),
            latitude = location.latitude(),
            timestamp = System.currentTimeMillis(),
        )
        locationUpdateQueue.offer(request)
    }

    fun cleanup() {
    }
}

/**
 * Manager class for handling activity logs.
 *
 * This class provides access to system and user activity logs,
 * including events from friends and system notifications.
 *
 * @property scope The coroutine scope for background operations.
 * @property api The API service for network requests.
 * @property _currentUser Mutable state flow of the current user.
 */
class LogManager(
    private val scope: CoroutineScope,
    private val api: ApiService,
    private val _currentUser: MutableStateFlow<User?>
) {
    private val _logs = MutableStateFlow<List<Log>>(emptyList())
    val logs: StateFlow<List<Log>> = _logs

    /**
     * Converts a LogDTO from the API to the domain Log model.
     *
     * @return A Log domain object mapped from this DTO.
     */
    private fun LogDTO.toLog(): Log = Log(
        sender = fromName,
        activity = eventName,
        senderLocation = location.let { Point.fromLngLat(it.longitude, it.latitude) },
        timestamp = location.timestamp
    )

    /**
     * Refreshes the activity logs from the server.
     *
     * Updates the [logs] StateFlow with the latest activity data.
     *
     * @throws Various exceptions for network and server errors.
     */
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

    /**
     * Gets the current list of activity logs.
     *
     * @return The current list of Log objects.
     */
    fun getLogs(): List<Log> {
        return _logs.value
    }
}

/**
 * Manager class for administrative functions.
 *
 * This class provides capabilities for user management and moderation,
 * available only to users with administrative privileges.
 *
 * @property scope The coroutine scope for background operations.
 * @property api The API service for network requests.
 * @property _currentUser Mutable state flow of the current user.
 * @property _adminUsers Mutable state flow of users for admin management.
 */
class AdminManager(
    private val scope: CoroutineScope,
    private val api: ApiService,
    private val _currentUser: MutableStateFlow<User?>,
    private val _adminUsers: MutableStateFlow<List<AdminUserItem>>
) {
    /**
     * Converts a UserDTO from the API to the AdminUserItem model.
     *
     * @return An AdminUserItem domain object mapped from this DTO.
     */
    private fun UserDTO.toAdminUserItem(): AdminUserItem = AdminUserItem(
        userId = userID,
        name = displayName,
        email = email,
        photoURL = photoURL,
        isBanned = isBanned
    )

    /**
     * Retrieves all users in the system for administrative management.
     *
     * Updates the [adminUsers] StateFlow with the latest user data.
     *
     * @return A Result containing a list of AdminUserItem objects on success,
     *         or an Exception on failure.
     */
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

    /**
     * Bans a user from the system.
     *
     * This administrative function prevents the targeted user from accessing the system.
     * After banning, the user list is refreshed.
     *
     * @param targetUserId The ID of the user to ban.
     * @return A Result indicating success or containing an Exception on failure.
     */
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
                getAllUsers()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to ban user: ${response.code()}"))
            }
        } catch (e: HttpException) {
            Result.failure(e)
        }
    }
}