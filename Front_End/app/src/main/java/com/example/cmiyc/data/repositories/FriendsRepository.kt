package com.example.cmiyc.repository

import com.example.cmiyc.api.ApiClient
import com.example.cmiyc.data.Friend
import com.example.cmiyc.data.FriendRequest
import com.example.cmiyc.repositories.UserRepository
import com.example.cmiyc.api.dto.*
import com.mapbox.geojson.Point

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Repository for managing friend relationships and friend requests.
 *
 * This singleton object handles all friend-related operations including:
 * - Fetching and caching friend data
 * - Managing friend requests
 * - Background polling for real-time updates
 * - Error handling and retry logic
 *
 * The repository exposes state flows for UI components to observe changes in friends
 * and friend requests data, as well as loading states.
 */
object FriendsRepository {
    private val api = ApiClient.apiService

    // StateFlows to expose data to consumers
    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends

    private val _friendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friendRequests: StateFlow<List<FriendRequest>> = _friendRequests

    // Loading states
    private val _isFriendsLoading = MutableStateFlow(false)
    val isFriendsLoading: StateFlow<Boolean> = _isFriendsLoading

    private val _isRequestsLoading = MutableStateFlow(false)
    val isRequestsLoading: StateFlow<Boolean> = _isRequestsLoading

    // Error tracking for polling
    private val _consecutiveFailures = MutableStateFlow(0)
    val consecutiveFailures: Int get() = _consecutiveFailures.value

    // Last error that occurred
    private val _lastError = MutableStateFlow<Throwable?>(null)
    val lastError: Throwable? get() = _lastError.value

    // Coroutine scope for background operations
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Background polling jobs
    private var friendsPollingJob: Job? = null

    /**
     * Starts polling for friend data updates at a 5-second interval.
     *
     * This method is typically called when the home screen becomes active.
     * It automatically cancels any existing polling job before starting a new one.
     */
    fun startHomeScreenPolling() {
        stopHomeScreenPolling() // Cancel any existing job first
        friendsPollingJob = coroutineScope.launch {
            while (isActive) {
                try {
                    fetchFriends()
                    // Reset failure counter on success
                    _consecutiveFailures.value = 0
                    _lastError.value = null
                } catch (e: CancellationException) {
                    // Handle cancellation during delay
                    println("Handle cancellation during delay: ${e.message}")
                    throw e
                } catch (e: IOException) {
                    _consecutiveFailures.value++
                    _lastError.value = e
                    println("Friend polling failed (${_consecutiveFailures.value}): ${e.message}")
                } catch (e: HttpException) {
                    _consecutiveFailures.value++
                    _lastError.value = e
                    println("Friend polling failed (${_consecutiveFailures.value}): ${e.message}")
                } catch (e: IllegalStateException) {
                    _consecutiveFailures.value++
                    _lastError.value = e
                    println("Friend polling failed (${_consecutiveFailures.value}): ${e.message}")
                }

                try {
                    delay(5000)
                } catch (e: CancellationException) {
                    // Handle cancellation during delay
                    println("Handle cancellation during delay: ${e.message}")
                    throw e
                }
            }
        }
    }

    /**
     * Stops the background polling for friend data.
     *
     * This method is typically called when the home screen becomes inactive
     * to conserve system resources and battery.
     */
    fun stopHomeScreenPolling() {
        friendsPollingJob?.cancel()
        friendsPollingJob = null
    }


    private fun FriendDTO.toFriend(): Friend = Friend(
        userId = userID,
        name = displayName,
        email = email,
        photoURL = photoURL,
        location = currentLocation.let { Point.fromLngLat(it.longitude, it.latitude) },
        lastUpdated = currentLocation.timestamp,
        isBanned = isBanned
    )


    suspend fun fetchFriendRequestsOnce(): Result<List<FriendRequest>> {
        fun FriendDTO.toFriendRequest(): FriendRequest = FriendRequest(
            userId = userID,
            displayName = displayName,
            timestamp = currentLocation.timestamp,
        )

        return try {
            _isRequestsLoading.value = true
            val startTime = System.currentTimeMillis()
            val response = api.getFriendRequests(UserRepository.getCurrentUserId())
            println("Get Friend Requests response: $response")
            val endTime = System.currentTimeMillis()
            println("Friend Requests API call took ${endTime - startTime} ms")

            if (response.isSuccessful) {
                val requests = response.body()?.map { it.toFriendRequest() } ?: emptyList()
                _friendRequests.value = requests
                Result.success(requests)
            } else {
                Result.failure(Exception("Failed to fetch friend requests: ${response.code()}"))
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(e)
        }
        catch (e: IOException) {
            Result.failure(e)
        }
        catch (e: HttpException) {
            Result.failure(e)
        } catch (e: IllegalStateException) {
            Result.failure(e)
        } finally {
            _isRequestsLoading.value = false
        }
    }


    private suspend fun fetchFriends() {
        try {
            val startTime = System.currentTimeMillis()
            val response = api.getFriends(UserRepository.getCurrentUserId())
            println("Get Friends response: $response")
            val endTime = System.currentTimeMillis()
            println("Friends API call took ${endTime - startTime} ms")

            if (response.isSuccessful) {
                val friends = response.body()?.map { it.toFriend() } ?: emptyList()
                _friends.value = friends
            } else {
                println("Error fetching friends: ${response.code()}")
                throw IOException("Server returned error code: ${response.code()}")
            }
        } catch (e: SocketTimeoutException) {
            println("Network timeout when fetching friends: ${e.message}")
            throw e
        } catch (e: IOException) {
            println("Network error when fetching friends: ${e.message}")
            throw e
        } catch (e: HttpException) {
            println("Error fetching friends: ${e.message}")
            throw e
        } catch (e: CancellationException) {
            println("Error fetching friends: ${e.message}")
            throw e
        } catch (e: IllegalStateException) {
            println("Error fetching friends: ${e.message}")
            throw e
        }
    }

    /**
     * Fetches friends data from the API once (without ongoing polling).
     *
     * Updates the [friends] StateFlow on success.
     * Sets the [isFriendsLoading] flag during the operation.
     *
     * @return A Result containing a list of Friend objects on success,
     *         or an Exception on failure.
     */
    suspend fun fetchFriendsOnce(): Result<List<Friend>> {
        return try {
            _isFriendsLoading.value = true
            val startTime = System.currentTimeMillis()
            val response = api.getFriends(UserRepository.getCurrentUserId())
            println("Get Friends response: $response")
            val endTime = System.currentTimeMillis()
            println("Friends API call took ${endTime - startTime} ms")

            if (response.isSuccessful) {
                val friends = response.body()?.map { it.toFriend() } ?: emptyList()
                _friends.value = friends
                Result.success(friends)
            } else {
                println("Failed to fetch friends: ${response.code()} ${response.message()}")
                Result.failure(Exception("Failed to fetch friends: ${response.code()}"))
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(e)
        }
        catch (e: IOException) {
            Result.failure(e)
        }
        catch (e: HttpException) {
            Result.failure(e)
        } catch (e: IllegalStateException) {
            Result.failure(e)
        } finally {
            _isFriendsLoading.value = false
        }
    }

    /**
     * Accepts a friend request.
     *
     * Calls the API to accept a pending friend request and refreshes both the friend
     * requests list and friends list on success.
     *
     * @param friendId The ID of the user whose friend request is being accepted.
     * @return A Result indicating success or containing an Exception on failure.
     */
    suspend fun acceptFriendRequest(friendId: String): Result<Unit> {
        return try {
            val startTime = System.currentTimeMillis()
            val response = api.acceptFriendRequest(
                userId = UserRepository.getCurrentUserId(),
                friendID = friendId
            )
            println("Accept Friend Request response: $response")
            val endTime = System.currentTimeMillis()
            println("Accept Friend Request API call took ${endTime - startTime} ms")

            if (response.isSuccessful) {
                // Refresh requests list after accepting
                fetchFriendRequestsOnce()
                // Also refresh friends list
                fetchFriendsOnce()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to accept friend request: ${response.code()}"))
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(e)
        }
        catch (e: IOException) {
            Result.failure(e)
        }
        catch (e: HttpException) {
            Result.failure(e)
        } catch (e: IllegalStateException) {
            Result.failure(e)
        }
    }

    /**
     * Denies a friend request.
     *
     * Calls the API to decline a pending friend request and refreshes the
     * friend requests list on success.
     *
     * @param friendId The ID of the user whose friend request is being declined.
     * @return A Result indicating success or containing an Exception on failure.
     */
    suspend fun denyFriendRequest(friendId: String): Result<Unit> {
        return try {
            val startTime = System.currentTimeMillis()
            val response = api.declineFriendRequest(
                userId = UserRepository.getCurrentUserId(),
                friendID = friendId
            )
            println("Decline Friend Request response: $response")
            val endTime = System.currentTimeMillis()
            println("Decline Friend Request API call took ${endTime - startTime} ms")

            if (response.isSuccessful) {
                // Refresh requests after declining
                fetchFriendRequestsOnce()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to decline friend request: ${response.code()}"))
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(e)
        }
        catch (e: IOException) {
            Result.failure(e)
        }
        catch (e: HttpException) {
            Result.failure(e)
        } catch (e: IllegalStateException) {
            Result.failure(e)
        }
    }

    /**
     * Sends a friend request to another user.
     *
     * Calls the API to send a friend request to a user specified by their email address.
     *
     * @param friendEmail The email address of the user to whom the friend request will be sent.
     * @return A Result indicating success or containing an Exception on failure.
     */
    suspend fun sendFriendRequest(friendEmail: String): Result<Unit> {
        return try {
            val startTime = System.currentTimeMillis()
            val response = api.sendFriendRequest(
                userId = UserRepository.getCurrentUserId(),
                friendEmail = friendEmail
            )
            println("Send Friend Request response: $response")
            val endTime = System.currentTimeMillis()
            println("Send Friend Request API call took ${endTime - startTime} ms")

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to send friend request: ${response.code()}"))
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(e)
        }
        catch (e: IOException) {
            Result.failure(e)
        }
        catch (e: HttpException) {
            Result.failure(e)
        } catch (e: IllegalStateException) {
            Result.failure(e)
        }
    }

    /**
     * Removes a friend from the user's friend list.
     *
     * Calls the API to terminate a friendship and refreshes the friends list on success.
     *
     * @param friendId The ID of the user to be removed from the friend list.
     * @return A Result indicating success or containing an Exception on failure.
     */
    suspend fun removeFriend(friendId: String): Result<Unit> {
        return try {
            val startTime = System.currentTimeMillis()
            val response = api.removeFriend(
                userId = UserRepository.getCurrentUserId(),
                friendID = friendId
            )
            println("Remove Friend response: $response")
            val endTime = System.currentTimeMillis()
            println("Remove Friend API call took ${endTime - startTime} ms")

            if (response.isSuccessful) {
                // Refresh friends list after removing a friend
                fetchFriendsOnce()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to remove friend: ${response.code()}"))
            }
        }
        catch (e: SocketTimeoutException) {
            Result.failure(e)
        }
        catch (e: IOException) {
            Result.failure(e)
        }
        catch (e: HttpException) {
            Result.failure(e)
        } catch (e: IllegalStateException) {
            Result.failure(e)
        }
    }
}