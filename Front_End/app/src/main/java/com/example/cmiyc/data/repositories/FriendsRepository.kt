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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    // Mutex to prevent concurrent network calls
    private val friendsMutex = Mutex()
    private val requestsMutex = Mutex()

    // Coroutine scope for background operations
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Background polling jobs
    private var friendsPollingJob: Job? = null
    private var requestsPollingJob: Job? = null

    // Flag to control the background worker state
    @Volatile
    private var isActive = false

    init {
        // Start only friends polling immediately, not friend requests
        startFriendsPollingOnly()
    }

    fun startPolling() {
        if (isActive) return
        isActive = true

        // Start friends polling
        startFriendsPolling()

        // Start friend requests polling
        startRequestsPolling()
    }

    fun stopPolling() {
        isActive = false
        friendsPollingJob?.cancel()
        requestsPollingJob?.cancel()
    }

    // Only start friends polling (used in init)
    private fun startFriendsPollingOnly() {
        isActive = true
        friendsPollingJob?.cancel()
        friendsPollingJob = coroutineScope.launch {
            while (isActive) {
                fetchFriends()
                delay(10000) // Poll every 5 seconds
            }
        }
    }

    private fun startFriendsPolling() {
        friendsPollingJob?.cancel()
        friendsPollingJob = coroutineScope.launch {
            while (isActive) {
                fetchFriends()
                delay(10000) // Poll every 5 seconds
            }
        }
    }

    private fun startRequestsPolling() {
        requestsPollingJob?.cancel()
        requestsPollingJob = coroutineScope.launch {
            while (isActive) {
                fetchFriendRequests()
                delay(10000) // Poll every 10 seconds - less frequent than friends
            }
        }
    }

    // Start/stop request polling separately
    fun startRequestsPollingOnly() {
        requestsPollingJob?.cancel()
        requestsPollingJob = coroutineScope.launch {
            while (isActive) {
                fetchFriendRequests()
                delay(10000) // Poll every 10 seconds
            }
        }
    }

    fun stopRequestsPollingOnly() {
        requestsPollingJob?.cancel()
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

    private suspend fun fetchFriendRequests() {
        // Use mutex to prevent concurrent calls
        if (!requestsMutex.tryLock()) return

        try {
            _isRequestsLoading.value = true
            val response = api.getFriendRequests(UserRepository.getCurrentUserId())
            println("Friend requests response: $response")
            if (response.isSuccessful) {
                val requests = response.body()?.map { it.toFriendRequest() } ?: emptyList()
                _friendRequests.value = requests
            } else {
                println("Error fetching friend requests: ${response.code()}")
            }
        } catch (e: SocketTimeoutException) {
            println("Network timeout when fetching friend requests: ${e.message}")
            // Keep existing data
        } catch (e: IOException) {
            println("Network error when fetching friend requests: ${e.message}")
            // Keep existing data
        } catch (e: Exception) {
            println("Error fetching friend requests: ${e.message}")
            // Keep existing data
        } finally {
            _isRequestsLoading.value = false
            requestsMutex.unlock()
        }
    }

    // Public method to force refresh requests
    suspend fun refreshFriendRequests(): Result<List<FriendRequest>> {
        return try {
            requestsMutex.withLock {
                _isRequestsLoading.value = true
                val response = api.getFriendRequests(UserRepository.getCurrentUserId())
                println("Friend requests response: $response")
                if (response.isSuccessful) {
                    val requests = response.body()?.map { it.toFriendRequest() } ?: emptyList()
                    _friendRequests.value = requests
                    Result.success(requests)
                } else {
                    Result.failure(Exception("Failed to fetch friend requests: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isRequestsLoading.value = false
        }
    }

    private suspend fun fetchFriends() {
        // Use mutex to prevent concurrent calls
        if (!friendsMutex.tryLock()) return

        try {
            _isFriendsLoading.value = true
            val response = api.getFriends(UserRepository.getCurrentUserId())
            println("Get Friends response: $response")
            if (response.isSuccessful) {
                val friends = response.body()?.map { it.toFriend() } ?: emptyList()
                _friends.value = friends
            } else {
                println("Error fetching friends: ${response.code()}")
            }
        } catch (e: SocketTimeoutException) {
            println("Network timeout when fetching friends: ${e.message}")
            // Keep existing data
        } catch (e: IOException) {
            println("Network error when fetching friends: ${e.message}")
            // Keep existing data
        } catch (e: Exception) {
            println("Error fetching friends: ${e.message}")
            // Keep existing data
        } finally {
            _isFriendsLoading.value = false
            friendsMutex.unlock()
        }
    }

    // Public method to force refresh friends
    suspend fun refreshFriends(): Result<List<Friend>> {
        return try {
            friendsMutex.withLock {
                _isFriendsLoading.value = true
                val response = api.getFriends(UserRepository.getCurrentUserId())
                println("Get Friends response: $response")
                if (response.isSuccessful) {
                    val friends = response.body()?.map { it.toFriend() } ?: emptyList()
                    _friends.value = friends
                    Result.success(friends)
                } else {
                    Result.failure(Exception("Failed to fetch friends: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isFriendsLoading.value = false
        }
    }

    suspend fun acceptFriendRequest(friendId: String): Result<Unit> {
        return try {
            val response = api.acceptFriendRequest(
                userId = UserRepository.getCurrentUserId(),
                friendID = friendId
            )
            println("Accept Friend Request response: $response")
            if (response.isSuccessful) {
                // Trigger refreshes of both data sets
                coroutineScope.launch {
                    refreshFriendRequests()
                    refreshFriends()
                }
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
            val response = api.declineFriendRequest(
                userId = UserRepository.getCurrentUserId(),
                friendID = friendId
            )
            println("Decline Friend Request response: $response")
            if (response.isSuccessful) {
                // Trigger refresh of requests
                coroutineScope.launch {
                    refreshFriendRequests()
                }
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
            val response = api.sendFriendRequest(
                userId = UserRepository.getCurrentUserId(),
                friendEmail = friendEmail
            )
            println("Send Friend Request response: $response")
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
            val response = api.removeFriend(
                userId = UserRepository.getCurrentUserId(),
                friendID = friendId
            )
            println("Remove Friend response: $response")
            if (response.isSuccessful) {
                // Trigger refresh of friends
                coroutineScope.launch {
                    refreshFriends()
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to remove friend: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Clean up resources when the app is shutting down
    fun cleanup() {
        stopPolling()
        coroutineScope.cancel()
    }
}