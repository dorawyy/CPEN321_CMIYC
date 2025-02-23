package com.example.cmiyc.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cmiyc.data.Friend
import com.example.cmiyc.repositories.UserRepository
import com.example.cmiyc.repository.FriendsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModelFactory(
    private val userRepository: UserRepository,
    private val friendsRepository: FriendsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(userRepository, friendsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class HomeViewModel(
    private val userRepository: UserRepository,
    private val friendsRepository: FriendsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state

    init {
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        viewModelScope.launch {
            while (true) {
                try {
                    val friends = friendsRepository.getFriends()
                    _state.update { it.copy(
                        friends = friends,
                        error = null
                    )}
                } catch (e: Exception) {
                    _state.update { it.copy(error = e.message) }
                }
                kotlinx.coroutines.delay(5000L) // Update every 5 seconds
            }
        }
    }

    fun updateStatus(status: String) {
        // TODO: Implement status update when API is ready
        viewModelScope.launch {
            try {
                // Call API to update status
                _state.update { it.copy(error = null) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

data class HomeScreenState(
    val friends: List<Friend> = emptyList(),
    val error: String? = null
)