package com.example.cmiyc.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cmiyc.data.Friend
import com.example.cmiyc.data.FriendRequest
import com.example.cmiyc.data.User
import com.example.cmiyc.repositories.UserRepository
import com.example.cmiyc.repository.FriendsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FriendsViewModelFactory(
    private val userRepository: UserRepository,
    private val friendsRepository: FriendsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FriendsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FriendsViewModel(userRepository, friendsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class FriendsViewModel(
    private val userRepository: UserRepository,
    private val friendsRepository: FriendsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FriendsScreenState())
    val state: StateFlow<FriendsScreenState> = _state

    private var pollingJob: Job? = null

    init {
        loadFriends()
        startPeriodicUpdates()
        startPollingFriendRequests()
    }

    fun updateState(update: (FriendsScreenState) -> FriendsScreenState) {
        _state.update(update)
    }

    private fun startPeriodicUpdates() {
        viewModelScope.launch {
            while (true) {
                delay(1000) // Update every 30 seconds
                loadFriends()
            }
        }
    }

    private fun loadFriends() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                val friends = friendsRepository.getFriends()
                _state.update { it.copy(
                    friends = friends,
                    isLoading = false,
                    error = null
                )}
            } catch (e: Exception) {
                _state.update { it.copy(
                    error = e.message,
                    isLoading = false
                )}
            }
        }
    }

    fun refresh() {
        loadFriends()
    }

    private fun startPollingFriendRequests() {
        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val requests = friendsRepository.getFriendRequests()
                    _state.update { it.copy(friendRequests = requests) }
                } catch (e: Exception) {
                    _state.update { it.copy(error = "Failed to fetch friend requests: ${e.message}") }
                }
                delay(1000)
            }
        }
    }

    fun acceptRequest(requestId: String) {
        viewModelScope.launch {
            try {
                friendsRepository.acceptFriendRequest(requestId)
                // Refresh both friends list and requests
                loadFriends()
                refreshFriendRequests()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun denyRequest(requestId: String) {
        viewModelScope.launch {
            try {
                friendsRepository.denyFriendRequest(requestId)
                // Refresh requests
                refreshFriendRequests()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private suspend fun refreshFriendRequests() {
        try {
            val requests = friendsRepository.getFriendRequests()
            _state.update { it.copy(friendRequests = requests) }
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message) }
        }
    }

    fun searchFriends(query: String) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(
                    searchQuery = query,
                    isSearching = true
                )}

                if (query.isNotEmpty()) {
                    val results = friendsRepository.searchUsers(query)
                    _state.update { it.copy(
                        searchResults = results,
                        isSearching = true,
                        error = null
                    )}
                } else {
                    _state.update { it.copy(
                        searchResults = emptyList(),
                        isSearching = false
                    )}
                }
            } catch (e: Exception) {
                _state.update { it.copy(
                    error = e.message,
                    isSearching = false
                )}
            }
        }
    }

    fun addFriend(targetUserId: String) {
        viewModelScope.launch {
            try {
                friendsRepository.sendFriendRequest(targetUserId)
                _state.update { state ->
                    state.copy(
                        searchResults = state.searchResults.filter { it.userId != targetUserId },
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun removeFriend(targetUserId: String) {
        viewModelScope.launch {
            try {
                friendsRepository.removeFriend(targetUserId)
                loadFriends() // Reload friends list after removal
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

data class FriendsScreenState(
    val friends: List<Friend> = emptyList(),
    val searchResults: List<Friend> = emptyList(),
    val friendRequests: List<FriendRequest> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val error: String? = null,
    val showRequestsDialog: Boolean = false
)