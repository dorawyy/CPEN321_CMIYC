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

/**
 * Factory class for creating instances of HomeViewModel.
 *
 * This factory follows the ViewModelProvider.Factory pattern to allow
 * dependency injection for the HomeViewModel class. It ensures that
 * the appropriate ViewModel type is created.
 */
class HomeViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * ViewModel for the Home screen.
 *
 * This ViewModel manages the state and business logic for the home screen,
 * including real-time friend location tracking, background polling, and
 * activity broadcasting. It monitors for network issues and provides
 * appropriate user feedback for various error conditions.
 */
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

    /**
     * Starts background polling for friend locations.
     *
     * This method initiates periodic updates of friend locations and
     * monitors for consecutive failures to provide appropriate error feedback.
     * It should be called when the home screen becomes active.
     */
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

    /**
     * Stops background polling for friend locations.
     *
     * This method terminates the periodic updates of friend locations.
     * It should be called when the home screen becomes inactive to
     * conserve system resources and battery power.
     */
    fun stopPolling() {
        // Stop the polling in FriendsRepository
        FriendsRepository.stopHomeScreenPolling()
    }

    /**
     * Called when the ViewModel is being destroyed.
     *
     * Performs cleanup by canceling all coroutines to prevent memory leaks.
     */
    override fun onCleared() {
        super.onCleared()
        // Cancel all coroutines when ViewModel is cleared
        viewModelScope.cancel()
    }

    /**
     * Clears the broadcast success flag.
     *
     * This method is typically called after displaying a success message
     * to the user to prevent showing the same message repeatedly.
     */
    fun clearBroadcastSuccess() {
        _state.update { it.copy(broadcastSuccess = false) }
    }

    /**
     * Clears any polling error messages and resets failure counter.
     *
     * This method is called after the user acknowledges a polling error
     * to reset the error state and monitoring.
     */
    fun clearPollingError() {
        _state.update { it.copy(pollingError = null) }
        // Reset the counter when user acknowledges the error
        consecutiveFriendPollingFailures = 0
    }

    /**
     * Broadcasts an activity message to friends.
     *
     * This method sends an activity update that will be visible to friends
     * in their activity logs. It provides appropriate error handling and
     * success feedback.
     *
     * @param activity The activity message to broadcast.
     */
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

    /**
     * Clears any error message from the state.
     *
     * This method is typically called after an error has been displayed to the user
     * and acknowledged.
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

/**
 * Data class representing the state of the Home screen.
 *
 * This immutable state container holds all the data needed to render the
 * home screen UI, including the list of friends, error messages, and
 * success flags.
 *
 * @property friends The list of friends with their current locations.
 * @property error Optional error message for operation failures.
 * @property pollingError Optional error message specific to background polling issues.
 * @property broadcastSuccess Flag indicating a successful activity broadcast.
 */
data class HomeScreenState(
    val friends: List<Friend> = emptyList(),
    val error: String? = null,
    val pollingError: String? = null,
    val broadcastSuccess: Boolean = false
)