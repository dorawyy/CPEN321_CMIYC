package com.example.cmiyc.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil.network.HttpException
import com.example.cmiyc.data.AdminUserItem
import com.example.cmiyc.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException

class AdminViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class AdminViewModel : ViewModel() {

    private val _state = MutableStateFlow(AdminScreenState())
    val state: StateFlow<AdminScreenState> = _state

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                val result = UserRepository.getAllUsers()
                result.fold(
                    onSuccess = { users ->
                        _state.update {
                            it.copy(
                                users = users,
                                filteredUsers = filterUsersWithQuery(users, it.filterQuery),
                                isLoading = false,
                                error = null
                            )
                        }
                    },
                    onFailure = { error ->
                        _state.update {
                            it.copy(
                                error = error.message ?: "Failed to load users",
                                isLoading = false
                            )
                        }
                    }
                )
            } catch (e: SocketTimeoutException) {
                _state.update {
                    it.copy(
                        error = e.message ?: "Failed to load users",
                        isLoading = false
                    )
                }
            }
            catch (e: IOException) {
                _state.update {
                    it.copy(
                        error = e.message ?: "Failed to load users",
                        isLoading = false
                    )
                }
            }
            catch (e: HttpException) {
                _state.update {
                    it.copy(
                        error = e.message ?: "Failed to load users",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun banUser(userId: String) {
        viewModelScope.launch {
            try {
                val result = UserRepository.banUser(userId)
                result.fold(
                    onSuccess = {
                        // Refresh user list after ban
                        loadUsers()
                    },
                    onFailure = { error ->
                        _state.update {
                            it.copy(error = error.message ?: "Failed to ban user")
                        }
                    }
                )
            } catch (e: SocketTimeoutException) {
                _state.update {
                    it.copy(error = e.message ?: "Failed to ban user")
                }
            } catch (e: IOException) {
                _state.update {
                    it.copy(error = e.message ?: "Failed to ban user")
                }
            } catch (e: HttpException) {
                _state.update {
                    it.copy(error = e.message ?: "Failed to ban user")
                }
            }
        }
    }

    fun filterUsers(query: String) {
        _state.update { currentState ->
            currentState.copy(
                filterQuery = query,
                filteredUsers = filterUsersWithQuery(currentState.users, query)
            )
        }
    }

    private fun filterUsersWithQuery(users: List<AdminUserItem>, query: String): List<AdminUserItem> {
        return if (query.isEmpty()) {
            users
        } else {
            users.filter { user ->
                user.name.contains(query, ignoreCase = true) ||
                        user.email.contains(query, ignoreCase = true)
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

data class AdminScreenState(
    val users: List<AdminUserItem> = emptyList(),
    val filteredUsers: List<AdminUserItem> = emptyList(),
    val filterQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)