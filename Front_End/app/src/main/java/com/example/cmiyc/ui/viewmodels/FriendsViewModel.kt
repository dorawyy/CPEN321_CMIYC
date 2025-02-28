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
            FriendsRepository.friends
                .combine(FriendsRepository.isFriendsLoading) { friends, isLoading ->
                    Pair(friends, isLoading)
                }
                .collect { (friends, isLoading) ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            friends = friends,
                            filteredFriends = if (currentState.filterQuery.isEmpty()) {
                                friends
                            } else {
                                friends.filter { friend ->
                                    friend.name.contains(currentState.filterQuery, ignoreCase = true) ||
                                            friend.email.contains(currentState.filterQuery, ignoreCase = true)
                                }
                            },
                            isLoading = isLoading
                        )
                    }
                }
        }

        // Collect friend requests data from repository
        viewModelScope.launch {
            FriendsRepository.friendRequests.collect { requests ->
                _uiState.update { it.copy(friendRequests = requests) }
            }
        }
    }

    // Handle screen lifecycle
    fun onScreenEnter() {
        // Start polling for friend requests when screen is active
        FriendsRepository.startRequestsPollingOnly()
        // Also refresh data immediately
        viewModelScope.launch {
            refreshFriendRequests()
            refreshFriends()
        }
    }

    fun onScreenExit() {
        // Stop polling for friend requests when screen is inactive
        FriendsRepository.stopRequestsPollingOnly()
    }

    override fun onCleared() {
        super.onCleared()
        // Make sure polling is stopped when ViewModel is cleared
        FriendsRepository.stopRequestsPollingOnly()
    }

    fun updateState(update: (FriendsScreenState) -> FriendsScreenState) {
        _uiState.update(update)
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                refreshFriends()
                refreshFriendRequests()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private suspend fun refreshFriends() {
        FriendsRepository.refreshFriends().onFailure { e ->
            _uiState.update { it.copy(error = e.message) }
        }
    }

    private suspend fun refreshFriendRequests() {
        FriendsRepository.refreshFriendRequests().onFailure { e ->
            _uiState.update { it.copy(error = e.message) }
        }
    }

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

    fun filterFriends(query: String) {
        _uiState.update { currentState ->
            currentState.copy(
                filterQuery = query,
                filteredFriends = if (query.isEmpty()) {
                    currentState.friends
                } else {
                    currentState.friends.filter { friend ->
                        friend.name.contains(query, ignoreCase = true) ||
                                friend.email.contains(query, ignoreCase = true)
                    }
                }
            )
        }
    }

    fun updateEmailInput(email: String) {
        _uiState.update { it.copy(emailInput = email) }
    }

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
    val error: String? = null,
    val showRequestsDialog: Boolean = false,
    val showAddFriendDialog: Boolean = false,
    val emailInput: String = ""
)