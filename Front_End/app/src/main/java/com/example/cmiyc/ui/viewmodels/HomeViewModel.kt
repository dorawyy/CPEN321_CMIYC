package com.example.cmiyc.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cmiyc.data.Friend
import com.example.cmiyc.repositories.UserRepository
import com.example.cmiyc.repository.FriendsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.SocketTimeoutException

class HomeViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class HomeViewModel : ViewModel() {

    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state

    init {
        // Subscribe to the friends data flow from the repository
        viewModelScope.launch {
            FriendsRepository.friends.collect { friends ->
                _state.update { it.copy(
                    friends = friends,
                    error = null
                )}
            }
        }
    }

    // Start polling when ViewModel is created
    fun startPolling() {
        FriendsRepository.startHomeScreenPolling()
    }

    // Stop polling when ViewModel is cleared
    fun stopPolling() {
        FriendsRepository.stopHomeScreenPolling()
    }

    override fun onCleared() {
        super.onCleared()
        // Stop polling when ViewModel is cleared
        FriendsRepository.stopHomeScreenPolling()
    }

    fun broadcastMessage(activity: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    UserRepository.broadcastMessage(activity)
                }
                _state.update { it.copy(error = null) }
            } catch (e: SocketTimeoutException) {
                // Just log the timeout error
                println("Network timeout when broadcasting message: ${e.message}")
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