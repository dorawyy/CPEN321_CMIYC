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
                    userRepository.refreshLogs()
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
            userRepository.logs.collect { logs ->
                _state.update { it.copy(logs = logs) }
            }
        }
    }

    fun updateLogAddress(logId: String, address: String) {
        _state.update { currentState ->
            val updatedAddresses = currentState.logAddresses.toMutableMap().apply {
                put(logId, address)
            }
            currentState.copy(logAddresses = updatedAddresses)
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun clearRefreshError() {
        _state.update { it.copy(refreshError = null) }
        // Reset the counter when user acknowledges
        consecutiveRefreshFailures = 0
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}

data class LogScreenState(
    val logs: List<Log> = emptyList(),
    val logAddresses: Map<String, String> = mapOf(), // Map of logId to address
    val isLoading: Boolean = false,
    val error: String? = null,
    val refreshError: String? = null
)