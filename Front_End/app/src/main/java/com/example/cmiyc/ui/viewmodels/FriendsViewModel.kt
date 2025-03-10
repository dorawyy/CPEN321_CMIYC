package com.example.cmiyc.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil.network.HttpException
import com.example.cmiyc.data.Friend
import com.example.cmiyc.data.FriendRequest
import com.example.cmiyc.repository.FriendsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.net.SocketTimeoutException

class FriendsViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FriendsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FriendsViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class FriendsViewModel : ViewModel() {
    // Internal state
    private val _uiState = MutableStateFlow(FriendsScreenState())
    val state: StateFlow<FriendsScreenState> = _uiState

    init {
        // Collect friends data from repository
        viewModelScope.launch {
            FriendsRepository.friends.collect { friends ->
                _uiState.update { currentState ->
                    currentState.copy(
                        friends = friends,
                        filteredFriends = filterFriendsWithQuery(friends, currentState.filterQuery)
                    )
                }
            }
        }

        // Collect loading state
        viewModelScope.launch {
            FriendsRepository.isFriendsLoading.collect { isLoading ->
                _uiState.update { it.copy(isLoading = isLoading) }
            }
        }

        // Collect friend requests data
        viewModelScope.launch {
            FriendsRepository.friendRequests.collect { requests ->
                _uiState.update { it.copy(friendRequests = requests) }
            }
        }

        // Collect requests loading state
        viewModelScope.launch {
            FriendsRepository.isRequestsLoading.collect { isLoading ->
                _uiState.update { it.copy(isRequestsLoading = isLoading) }
            }
        }
    }

    private fun filterFriendsWithQuery(friends: List<Friend>, query: String): List<Friend> {
        return if (query.isEmpty()) {
            friends
        } else {
            friends.filter { friend ->
                friend.name.contains(query, ignoreCase = true) ||
                        friend.email.contains(query, ignoreCase = true)
            }
        }
    }

    // Handle screen lifecycle
    fun onScreenEnter() {
        // Fetch friends once when entering screen
        viewModelScope.launch {
            try {
                FriendsRepository.fetchFriendsOnce()
            } catch (e: SocketTimeoutException) {
                _uiState.update { it.copy(error = "Network timeout while loading friends. Please check your connection and try again.") }
            } catch (e: IOException) {
                _uiState.update { it.copy(error = "Network error while loading friends. Please check your connection and try again.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load friends: ${e.message}") }
            }
        }
    }

    fun onScreenExit() {
        // Nothing to clean up since we don't start polling in this screen
    }

    // Load friend requests when button is clicked
    fun loadFriendRequests() {
        viewModelScope.launch {
            try {
                val result = FriendsRepository.fetchFriendRequestsOnce()
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(showRequestsDialog = true, error = null) }
                    },
                    onFailure = { e ->
                        val errorMsg = when (e) {
                            is SocketTimeoutException -> "Network timeout while loading friend requests. Please try again."
                            is IOException -> "Network error while loading friend requests. Please check your connection."
                            else -> "Failed to load friend requests: ${e.message}"
                        }
                        _uiState.update { it.copy(error = errorMsg) }
                    }
                )
            }
            catch (e: SocketTimeoutException) {
                _uiState.update { it.copy(error = "Network timeout while loading friend requests. Please try again.") }
            }
            catch (e: IOException) {
                _uiState.update { it.copy(error = "Network error while loading friend requests. Please check your connection.") }
            }
        }
    }

    // Update UI state
    fun updateState(update: (FriendsScreenState) -> FriendsScreenState) {
        _uiState.update(update)
    }

    // Pull to refresh functionality
    fun refresh() {
        viewModelScope.launch {
            try {
                FriendsRepository.fetchFriendsOnce()
            } catch (e: SocketTimeoutException) {
                _uiState.update { it.copy(error = "Network timeout while refreshing. Please try again later.") }
            } catch (e: IOException) {
                _uiState.update { it.copy(error = "Network error while refreshing. Please check your connection.") }
            } catch (e: HttpException) {
                _uiState.update { it.copy(error = "Failed to refresh: ${e.message}") }
            }
        }
    }

    // Accept a friend request
    fun acceptRequest(userId: String) {
        viewModelScope.launch {
            try {
                val result = FriendsRepository.acceptFriendRequest(userId)
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(error = null) }
                    },
                    onFailure = { e ->
                        val errorMsg = when (e) {
                            is SocketTimeoutException -> "Network timeout while accepting friend request. Please try again."
                            is IOException -> "Network error while accepting friend request. Please check your connection."
                            else -> "Failed to accept friend request: ${e.message}"
                        }
                        _uiState.update { it.copy(error = errorMsg) }
                    }
                )
            }
            catch (e: SocketTimeoutException) {
                _uiState.update { it.copy(error = "Network timeout while accepting friend request. Please try again.") }
            } catch (e: IOException) {
                _uiState.update { it.copy(error = "Network error while accepting friend request. Please check your connection.") }
            } catch (e: HttpException) {
                _uiState.update { it.copy(error = "Failed to accept friend request: ${e.message}") }
            }
        }
    }

    // Deny a friend request
    fun denyRequest(userId: String) {
        viewModelScope.launch {
            try {
                val result = FriendsRepository.denyFriendRequest(userId)
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(error = null) }
                    },
                    onFailure = { e ->
                        val errorMsg = when (e) {
                            is SocketTimeoutException -> "Network timeout while declining friend request. Please try again."
                            is IOException -> "Network error while declining friend request. Please check your connection."
                            else -> "Failed to decline friend request: ${e.message}"
                        }
                        _uiState.update { it.copy(error = errorMsg) }
                    }
                )
            }
            catch (e: SocketTimeoutException) {
                _uiState.update { it.copy(error = "Network timeout while accepting friend request. Please try again.") }
            } catch (e: IOException) {
                _uiState.update { it.copy(error = "Network error while accepting friend request. Please check your connection.") }
            } catch (e: HttpException) {
                _uiState.update { it.copy(error = "Failed to accept friend request: ${e.message}") }
            }

        }
    }

    // Filter friends by name or email
    fun filterFriends(query: String) {
        _uiState.update { currentState ->
            currentState.copy(
                filterQuery = query,
                filteredFriends = filterFriendsWithQuery(currentState.friends, query)
            )
        }
    }

    // Update email input for adding friend
    fun updateEmailInput(email: String) {
        _uiState.update { it.copy(emailInput = email) }
    }

    // Send a friend request
    fun sendFriendRequest() {
        viewModelScope.launch {
            try {
                val email = state.value.emailInput.trim()
                if (email.isEmpty()) {
                    _uiState.update { it.copy(error = "Please enter an email address") }
                    return@launch
                }

                val result = FriendsRepository.sendFriendRequest(email)
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(
                            showAddFriendDialog = false,
                            emailInput = "",
                            error = null,
                            requestSentSuccessful = true,
                            successMessage = "Friend request sent successfully to $email"
                        )}
                    },
                    onFailure = { e ->
                        val errorMsg = when (e) {
                            is SocketTimeoutException -> "Network timeout while sending friend request. Please try again."
                            is IOException -> "Network error while sending friend request. Please check your connection."
                            else -> "Failed to send friend request: ${e.message}"
                        }
                        _uiState.update { it.copy(error = errorMsg) }
                    }
                )
            } catch (e: SocketTimeoutException) {
                _uiState.update { it.copy(error = "Network timeout while accepting friend request. Please try again.") }
            } catch (e: IOException) {
                _uiState.update { it.copy(error = "Network error while accepting friend request. Please check your connection.") }
            } catch (e: HttpException) {
                _uiState.update { it.copy(error = "Failed to accept friend request: ${e.message}") }
            }
        }
    }

    // Remove a friend
    fun removeFriend(targetUserId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val result = FriendsRepository.removeFriend(targetUserId)
                result.fold(
                    onSuccess = {
                        // Call the success callback
                        onSuccess()
                    },
                    onFailure = { e ->
                        val errorMsg = when (e) {
                            is SocketTimeoutException -> "Network timeout while removing friend. Please try again."
                            is IOException -> "Network error while removing friend. Please check your connection."
                            else -> "Failed to remove friend: ${e.message}"
                        }
                        _uiState.update { it.copy(error = errorMsg) }
                    }
                )
            } catch (e: SocketTimeoutException) {
                _uiState.update { it.copy(error = "Network timeout while accepting friend request. Please try again.") }
            } catch (e: IOException) {
                _uiState.update { it.copy(error = "Network error while accepting friend request. Please check your connection.") }
            } catch (e: HttpException) {
                _uiState.update { it.copy(error = "Failed to accept friend request: ${e.message}") }
            }
        }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(requestSentSuccessful = false, successMessage = null) }
    }

    // Clear any error message
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class FriendsScreenState(
    val friends: List<Friend> = emptyList(),
    val filteredFriends: List<Friend> = emptyList(),
    val friendRequests: List<FriendRequest> = emptyList(),
    val filterQuery: String = "",
    val isLoading: Boolean = false,
    val isRequestsLoading: Boolean = false,
    val error: String? = null,
    val showRequestsDialog: Boolean = false,
    val showAddFriendDialog: Boolean = false,
    val emailInput: String = "",
    val requestSentSuccessful: Boolean = false,
    val successMessage: String? = null
)