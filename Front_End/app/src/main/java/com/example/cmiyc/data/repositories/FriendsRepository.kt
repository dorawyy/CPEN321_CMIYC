package com.example.cmiyc.repository

import android.util.Log
import com.example.cmiyc.api.ApiClient
import com.example.cmiyc.data.Friend
import com.example.cmiyc.data.FriendRequest
import com.example.cmiyc.repositories.UserRepository
import com.example.cmiyc.api.dto.*
import com.mapbox.geojson.Point

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.net.SocketTimeoutException

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

    // Update polling frequency for Home Screen (5 seconds)
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
                    throw e
                } catch (e: Exception) {
                    // Track failures and the error
                    _consecutiveFailures.value++
                    _lastError.value = e
                    println("Friend polling failed (${_consecutiveFailures.value}): ${e.message}")
                }

                try {
                    delay(5000)
                } catch (e: CancellationException) {
                    // Handle cancellation during delay
                    throw e
                }
            }
        }
    }

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
    )

    private fun FriendDTO.toFriendRequest(): FriendRequest = FriendRequest(
        userId = userID,
        displayName = displayName,
        timestamp = currentLocation.timestamp,
    )

    // Fetch friend requests once
    suspend fun fetchFriendRequestsOnce(): Result<List<FriendRequest>> {
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
        } catch (e: Exception) {
            println("Error fetching friend requests: ${e.message}")
            Result.failure(e)
        } finally {
            _isRequestsLoading.value = false
        }
    }

    // Fetch friends data
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
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            println("Error fetching friends: ${e.message}")
            throw e
        }
    }

    // Fetch friends once
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
                Result.failure(Exception("Failed to fetch friends: ${response.code()}"))
            }
        } catch (e: Exception) {
            println("Error fetching friends: ${e.message}")
            Result.failure(e)
        } finally {
            _isFriendsLoading.value = false
        }
    }

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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}