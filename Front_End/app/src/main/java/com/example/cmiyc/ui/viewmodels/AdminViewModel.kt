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

/**
 * Factory class for creating instances of AdminViewModel.
 *
 * This factory follows the ViewModelProvider.Factory pattern to allow
 * dependency injection for the AdminViewModel class. It ensures that
 * the appropriate ViewModel type is created.
 */
class AdminViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * ViewModel for the Admin Panel screen.
 *
 * This ViewModel manages the state and business logic for the admin panel,
 * including loading users, filtering the user list, and performing administrative
 * actions like banning users. It handles network operations and error states
 * for administrative functions.
 */
class AdminViewModel : ViewModel() {

    private val _state = MutableStateFlow(AdminScreenState())
    val state: StateFlow<AdminScreenState> = _state

    init {
        loadUsers()
    }

    /**
     * Loads all users from the repository.
     *
     * This method fetches the complete list of users for administrative management,
     * handles loading states, and captures any errors that occur during the process.
     */
    fun loadUsers() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                val result = UserRepository.adminManager.getAllUsers()
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

    /**
     * Bans a user from the system.
     *
     * This administrative action prevents the specified user from accessing
     * the system. After a successful ban, the user list is refreshed to
     * reflect the updated status.
     *
     * @param userId The unique identifier of the user to ban.
     */
    fun banUser(userId: String) {
        viewModelScope.launch {
            try {
                val result = UserRepository.adminManager.banUser(userId)
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

    /**
     * Filters the user list based on a search query.
     *
     * This method updates the state with a filter query and applies the
     * filter to show only users matching the search criteria by name or email.
     *
     * @param query The search string used to filter users.
     */
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
 * Data class representing the state of the Admin Panel screen.
 *
 * This immutable state container holds all the data needed to render the
 * admin panel UI, including the user list, filter state, loading status,
 * and error messages.
 *
 * @property users The complete list of users in the system.
 * @property filteredUsers The list of users after applying search filters.
 * @property filterQuery The current search query string.
 * @property isLoading Flag indicating whether a loading operation is in progress.
 * @property error Optional error message if an operation failed.
 */
data class AdminScreenState(
    val users: List<AdminUserItem> = emptyList(),
    val filteredUsers: List<AdminUserItem> = emptyList(),
    val filterQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)