package com.example.cmiyc.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cmiyc.data.Log
import com.example.cmiyc.repositories.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Factory class for creating instances of LogViewModel with dependencies.
 *
 * This factory follows the ViewModelProvider.Factory pattern to facilitate
 * dependency injection for the LogViewModel. It ensures that the
 * ViewModel is created with the necessary UserRepository dependency.
 *
 * @property userRepository The repository responsible for user and log data.
 */
class LogViewModelFactory (
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LogViewModel::class.java)) {
            return LogViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * ViewModel for the Activity Log screen.
 *
 * This ViewModel manages the state and business logic for the activity log interface,
 * including periodic refreshing of logs, caching of resolved addresses, and handling
 * of error conditions during network operations.
 *
 * @property userRepository The repository responsible for user and log data.
 */
class LogViewModel (
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LogScreenState())
    val state: StateFlow<LogScreenState> = _state.asStateFlow()

    private var refreshJob: Job? = null

    // Track consecutive refresh failures
    private var consecutiveRefreshFailures = 0
    private val maxRefreshFailures = 5

    init {
        startPeriodicRefresh()
        observeLogs()
    }


    private fun startPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                try {
                    _state.update { it.copy(isLoading = true) }
                    userRepository.logManager.refreshLogs()
                    // Reset failure counter on success
                    consecutiveRefreshFailures = 0
                    // Clear any persistent refresh error
                    _state.update { it.copy(isLoading = false, refreshError = null) }
                } catch (e: SocketTimeoutException) {
                    handleRefreshFailure("Network timeout when refreshing activity logs. Please check your connection.")
                } catch (e: IOException) {
                    handleRefreshFailure("Network error when refreshing activity logs. Please check your connection.")
                } catch (e: HttpException) { // exception
                    handleRefreshFailure("Failed to refresh activity logs: ${e.message}")
                } finally {
                    _state.update { it.copy(isLoading = false) }
                }
                delay(30000) // 30 seconds between refreshes
            }
        }
    }


    private fun handleRefreshFailure(errorMessage: String) {
        consecutiveRefreshFailures++
        println("Activity log refresh failed (${consecutiveRefreshFailures}/$maxRefreshFailures): $errorMessage")

        // Only show error dialog after hitting the threshold
        if (consecutiveRefreshFailures >= maxRefreshFailures) {
            _state.update {
                it.copy(
                    refreshError = "Unable to refresh activity logs after multiple attempts. The displayed logs may be outdated."
                )
            }
        }
    }


    private fun observeLogs() {
        viewModelScope.launch {
            userRepository.logManager.logs.collect { logs ->
                _state.update { it.copy(logs = logs) }
            }
        }
    }

    /**
     * Updates the cached address for a specific log entry.
     *
     * This method is called when an address is resolved for a log entry,
     * allowing the address to be cached and reused instead of being
     * recalculated repeatedly.
     *
     * @param logId The unique identifier for the log entry.
     * @param address The resolved address string.
     */
    fun updateLogAddress(logId: String, address: String) {
        _state.update { currentState ->
            val updatedAddresses = currentState.logAddresses.toMutableMap().apply {
                put(logId, address)
            }
            currentState.copy(logAddresses = updatedAddresses)
        }
    }

    /**
     * Clears any general error message from the state.
     *
     * This method is typically called after an error has been displayed to the user
     * and acknowledged.
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /**
     * Clears any refresh-specific error message from the state.
     *
     * This method is called after a refresh error has been displayed to the user
     * and acknowledged. It also resets the consecutive failure counter.
     */
    fun clearRefreshError() {
        _state.update { it.copy(refreshError = null) }
        // Reset the counter when user acknowledges
        consecutiveRefreshFailures = 0
    }

    /**
     * Called when the ViewModel is being destroyed.
     *
     * Cancels the background refresh job to prevent memory leaks and
     * unnecessary network operations.
     */
    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}

/**
 * Data class representing the state of the Activity Log screen.
 *
 * This immutable state container holds all the data needed to render the
 * activity log UI, including the list of logs, cached addresses, loading state,
 * and error messages.
 *
 * @property logs The list of activity log entries to display.
 * @property logAddresses Map of log IDs to their resolved address strings for caching.
 * @property isLoading Flag indicating whether a refresh operation is in progress.
 * @property error Optional general error message.
 * @property refreshError Optional error message specific to refresh operations.
 */
data class LogScreenState(
    val logs: List<Log> = emptyList(),
    val logAddresses: Map<String, String> = mapOf(), // Map of logId to address
    val isLoading: Boolean = false,
    val error: String? = null,
    val refreshError: String? = null
)