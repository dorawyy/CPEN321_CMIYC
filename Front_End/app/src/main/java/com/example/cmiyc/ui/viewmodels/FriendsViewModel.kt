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
        fetchFriends(false)
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

    // Handle friend requests (accept/deny) - combines two previous functions
    fun handleFriendRequest(userId: String, accept: Boolean) {
        viewModelScope.launch {
            try {
                val result = if (accept) {
                    FriendsRepository.acceptFriendRequest(userId)
                } else {
                    FriendsRepository.denyFriendRequest(userId)
                }

                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(error = null) }
                    },
                    onFailure = { e ->
                        val action = if (accept) "accepting" else "declining"
                        val errorMsg = when (e) {
                            is SocketTimeoutException -> "Network timeout while $action friend request. Please try again."
                            is IOException -> "Network error while $action friend request. Please check your connection."
                            else -> "Failed to $action friend request: ${e.message}"
                        }
                        _uiState.update { it.copy(error = errorMsg) }
                    }
                )
            }
            catch (e: SocketTimeoutException) {
                val action = if (accept) "accepting" else "declining"
                _uiState.update { it.copy(error = "Network timeout while $action friend request. Please try again.") }
            } catch (e: IOException) {
                val action = if (accept) "accepting" else "declining"
                _uiState.update { it.copy(error = "Network error while $action friend request. Please check your connection.") }
            } catch (e: HttpException) {
                val action = if (accept) "accepting" else "declining"
                _uiState.update { it.copy(error = "Failed to $action friend request: ${e.message}") }
            }
        }
    }

    // Update UI state
    fun updateState(update: (FriendsScreenState) -> FriendsScreenState) {
        _uiState.update(update)
    }

    // Fetch friends data - handles both initial load and refresh
    private fun fetchFriends(isRefresh: Boolean) {
        viewModelScope.launch {
            try {
                FriendsRepository.fetchFriendsOnce()
            } catch (e: SocketTimeoutException) {
                val context = if (isRefresh) "refreshing" else "loading friends"
                _uiState.update { it.copy(error = "Network timeout while $context. Please try again later.") }
            } catch (e: IOException) {
                val context = if (isRefresh) "refreshing" else "loading friends"
                _uiState.update { it.copy(error = "Network error while $context. Please check your connection.") }
            } catch (e: HttpException) {
                val context = if (isRefresh) "refreshing" else "loading friends"
                _uiState.update { it.copy(error = "Failed to $context: ${e.message}") }
            }
        }
    }

    // Pull to refresh functionality - now delegates to fetchFriends
    fun refresh() {
        fetchFriends(true)
    }

    // Accept a friend request - delegates to handleFriendRequest
    fun acceptRequest(userId: String) {
        handleFriendRequest(userId, true)
    }

    // Deny a friend request - delegates to handleFriendRequest
    fun denyRequest(userId: String) {
        handleFriendRequest(userId, false)
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
                }
                else {
                    val result = FriendsRepository.sendFriendRequest(email)
                    result.fold(
                        onSuccess = {
                            _uiState.update {
                                it.copy(
                                    showAddFriendDialog = false,
                                    emailInput = "",
                                    error = null,
                                    requestSentSuccessful = true,
                                    successMessage = "Friend request sent successfully to $email"
                                )
                            }
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
                }
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

    // Clear messages function - combines clearSuccessMessage and clearError
    fun clearMessages() {
        _uiState.update { it.copy(requestSentSuccessful = false, successMessage = null, error = null) }
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