package com.example.cmiyc.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cmiyc.data.Friend
import com.example.cmiyc.data.FriendRequest
import com.example.cmiyc.repository.FriendsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

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
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
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
                FriendsRepository.fetchFriendRequestsOnce()
                _uiState.update { it.copy(showRequestsDialog = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
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
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // Accept a friend request
    fun acceptRequest(userId: String) {
        viewModelScope.launch {
            try {
                val result = FriendsRepository.acceptFriendRequest(userId)
                result.onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // Deny a friend request
    fun denyRequest(userId: String) {
        viewModelScope.launch {
            try {
                val result = FriendsRepository.denyFriendRequest(userId)
                result.onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
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
                            error = null
                        )}
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(error = e.message) }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // Remove a friend
    fun removeFriend(targetUserId: String) {
        viewModelScope.launch {
            try {
                val result = FriendsRepository.removeFriend(targetUserId)
                result.onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
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
    val emailInput: String = ""
)