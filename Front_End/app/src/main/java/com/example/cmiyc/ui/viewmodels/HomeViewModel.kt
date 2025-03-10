package com.example.cmiyc.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil.network.HttpException
import com.example.cmiyc.data.Friend
import com.example.cmiyc.repositories.UserRepository
import com.example.cmiyc.repository.FriendsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
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

    // Track consecutive polling failures for friends
    private var consecutiveFriendPollingFailures = 0
    private val maxPollingFailures = 20

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
        // Use FriendsRepository's built-in polling mechanism
        FriendsRepository.startHomeScreenPolling()

        // Start monitoring for consecutive failures
        viewModelScope.launch {
            while (isActive) {
                if (FriendsRepository.consecutiveFailures >= maxPollingFailures) {
                    val error = FriendsRepository.lastError
                    val errorMsg = when (error) {
                        is SocketTimeoutException -> "Network timeout when refreshing friend locations. The app may not show current locations."
                        is IOException -> "Network error when refreshing friend locations. The app may not show current locations."
                        else -> "Error refreshing friend locations: ${error?.message}. The app may not show current locations."
                    }
                    _state.update { it.copy(pollingError = errorMsg) }
                } else if (FriendsRepository.consecutiveFailures == 0) {
                    // Clear any previous polling errors when successful
                    _state.update { it.copy(pollingError = null) }
                }
                delay(5000) // Check error state every 5 seconds
            }
        }
    }

    // Stop polling when ViewModel is cleared
    fun stopPolling() {
        // Stop the polling in FriendsRepository
        FriendsRepository.stopHomeScreenPolling()
    }

    override fun onCleared() {
        super.onCleared()
        // Cancel all coroutines when ViewModel is cleared
        viewModelScope.cancel()
    }

    fun clearBroadcastSuccess() {
        _state.update { it.copy(broadcastSuccess = false) }
    }

    fun clearPollingError() {
        _state.update { it.copy(pollingError = null) }
        // Reset the counter when user acknowledges the error
        consecutiveFriendPollingFailures = 0
    }

    fun broadcastMessage(activity: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    UserRepository.broadcastMessage(activity)
                }
                // Clear error and possibly show success feedback
                _state.update { it.copy(
                    error = null,
                    broadcastSuccess = true // Add this to your state
                )}
            } catch (e: SocketTimeoutException) {
                _state.update { it.copy(
                    error = "Your broadcast may not have been sent due to a network timeout. Your friends might not receive this update."
                )}
            } catch (e: IOException) {
                _state.update { it.copy(
                    error = "Your broadcast may not have been sent due to a network error. Your friends might not receive this update."
                )}
            } catch (e: HttpException) {
                _state.update { it.copy(
                    error = "Error: ${e.message ?: "Failed to broadcast message"}"
                )}
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

data class HomeScreenState(
    val friends: List<Friend> = emptyList(),
    val error: String? = null,
    val pollingError: String? = null,
    val broadcastSuccess: Boolean = false
)