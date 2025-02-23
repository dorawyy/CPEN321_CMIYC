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
                    _state.update { it.copy(isLoading = false) }
                } catch (e: Exception) {
                    _state.update {
                        it.copy(
                            error = e.message ?: "Failed to refresh logs",
                            isLoading = false
                        )
                    }
                }
                delay(30000) // Refresh every 30 seconds
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

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}

data class LogScreenState(
    val logs: List<Log> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)