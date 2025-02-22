package com.example.cmiyc.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FriendsScreenState(
    val friends: List<Friend> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Friend> = emptyList(),
    val isSearching: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class FriendsViewModel : ViewModel() {
    private val _state = MutableStateFlow(FriendsScreenState())
    val state: StateFlow<FriendsScreenState> = _state

    init {
        loadFriends()
    }

    private fun loadFriends() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                // TODO: Replace with actual API call
                val friendsList = listOf(
                    Friend("1", "Ahri", "Ahri@example.com", "Online"),
                    Friend("2", "Gwen", "Gwen@example.com", "Offline"),
                    Friend("3", "Camille", "Camille@example.com", "Away")
                )
                _state.update { it.copy(
                    friends = friendsList,
                    isLoading = false
                ) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    error = "Failed to load friends: ${e.message}",
                    isLoading = false
                ) }
            }
        }
    }

    fun searchFriends(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(
                searchQuery = query,
                isSearching = query.isNotEmpty()
            ) }

            if (query.isNotEmpty()) {
                try {
                    // TODO: Replace with actual API call
                    val results = listOf(
                        Friend("4", "Darius", "Darius@example.com"),
                        Friend("5", "Garen", "Garen@example.com")
                    ).filter {
                        it.name.contains(query, ignoreCase = true) ||
                                it.email.contains(query, ignoreCase = true)
                    }
                    _state.update { it.copy(searchResults = results) }
                } catch (e: Exception) {
                    _state.update { it.copy(error = "Search failed: ${e.message}") }
                }
            } else {
                _state.update { it.copy(searchResults = emptyList()) }
            }
        }
    }

    fun addFriend(friend: Friend) {
        viewModelScope.launch {
            try {
                // TODO: Replace with actual API call
                _state.update { currentState ->
                    currentState.copy(
                        friends = currentState.friends + friend,
                        searchResults = currentState.searchResults - friend
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to add friend: ${e.message}") }
            }
        }
    }

    fun removeFriend(friend: Friend) {
        viewModelScope.launch {
            try {
                // TODO: Replace with actual API call
                _state.update { currentState ->
                    currentState.copy(
                        friends = currentState.friends - friend
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to remove friend: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

data class Friend(
    val userId: String,
    val name: String,
    val email: String,
    val status: String = "Offline"
)