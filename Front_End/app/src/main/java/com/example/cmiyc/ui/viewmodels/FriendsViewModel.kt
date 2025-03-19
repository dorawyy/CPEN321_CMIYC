package com.example.cmiyc.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil.network.HttpException
import com.example.cmiyc.data.Friend
import com.example.cmiyc.data.FriendRequest
import com.example.cmiyc.repository.FriendsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Factory class for creating instances of FriendsViewModel.
 *
 * This factory follows the ViewModelProvider.Factory pattern to allow
 * dependency injection for the FriendsViewModel class. It ensures that
 * the appropriate ViewModel type is created.
 */
class FriendsViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FriendsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FriendsViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * ViewModel for the Friends screen.
 *
 * This ViewModel manages the state and business logic for the friends management interface,
 * including:
 * - Loading and displaying friends and friend requests
 * - Filtering friends by search query
 * - Sending and responding to friend requests
 * - Removing existing friends
 *
 * It combines multiple data flows from the repository to maintain a consistent UI state
 * and handles network operations with appropriate error handling.
 */
class FriendsViewModel : ViewModel() {
    // Internal state
    private val _uiState = MutableStateFlow(FriendsScreenState())
    val state: StateFlow<FriendsScreenState> = _uiState

    init {
        viewModelScope.launch {
            combine(
                FriendsRepository.friends,
                FriendsRepository.isFriendsLoading,
                FriendsRepository.friendRequests,
                FriendsRepository.isRequestsLoading
            ) { friends, isFriendsLoading, friendRequests, isRequestsLoading ->
                // This block is called whenever any of the combined flows emit a new value
                _uiState.update { currentState ->
                    currentState.copy(
                        friends = friends,
                        filteredFriends = filterFriendsWithQuery(friends, currentState.filterQuery),
                        isLoading = isFriendsLoading,
                        friendRequests = friendRequests,
                        isRequestsLoading = isRequestsLoading
                    )
                }
            }.collect()
        }
    }

    /**
     * Called when the Friends screen is entered.
     *
     * Triggers an initial data refresh to ensure the friend list is up-to-date.
     */
    fun onScreenEnter() {
        // Fetch friends once when entering screen
        refresh()
    }

    /**
     * Called when the Friends screen is exited.
     *
     * Currently doesn't perform any operations but is included for lifecycle completeness.
     */
    fun onScreenExit() {
    }

    /**
     * Loads friend requests and displays the requests dialog.
     *
     * Fetches the latest friend requests from the repository and updates the UI state
     * to show the requests dialog. Handles network errors with appropriate messages.
     */
    fun loadFriendRequests() {
        viewModelScope.launch {
            try {
                val result = FriendsRepository.fetchFriendRequestsOnce()
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(showRequestsDialog = true, error = null) }
                    },
                    onFailure = { e ->
                        val errorMsg = when (e) {
                            is SocketTimeoutException -> "Network timeout while loading friend requests. Please try again."
                            is IOException -> "Network error while loading friend requests. Please check your connection."
                            else -> "Failed to load friend requests: ${e.message}"
                        }
                        _uiState.update { it.copy(error = errorMsg) }
                    }
                )
            }
            catch (e: SocketTimeoutException) {
                _uiState.update { it.copy(error = "Network timeout while loading friend requests. Please try again.") }
            }
            catch (e: IOException) {
                _uiState.update { it.copy(error = "Network error while loading friend requests. Please check your connection.") }
            }
        }
    }

    /**
     * Updates the UI state through a transformation function.
     *
     * Provides a generic way to update the state that allows UI components
     * to trigger state changes without directly accessing the MutableStateFlow.
     *
     * @param update A transformation function that takes the current state and returns a new state.
     */
    fun updateState(update: (FriendsScreenState) -> FriendsScreenState) {
        _uiState.update(update)
    }

    /**
     * Refreshes the friends list.
     *
     * Fetches the latest friend data from the repository, typically used for pull-to-refresh
     * functionality. Handles network errors with appropriate user feedback.
     */
    fun refresh() {
        viewModelScope.launch {
            try {
                FriendsRepository.fetchFriendsOnce()
            } catch (e: SocketTimeoutException) {
                _uiState.update { it.copy(error = "Network timeout while refreshing. Please try again later.") }
            } catch (e: IOException) {
                _uiState.update { it.copy(error = "Network error while refreshing. Please check your connection.") }
            } catch (e: HttpException) {
                _uiState.update { it.copy(error = "Failed to refresh: ${e.message}") }
            }
        }
    }

    /**
     * Accepts a friend request.
     *
     * Processes a friend request acceptance, updates the friends list on success,
     * and handles error cases with appropriate user feedback.
     *
     * @param userId The ID of the user whose friend request is being accepted.
     */
    fun acceptRequest(userId: String) {
        viewModelScope.launch {
            try {
                val result = FriendsRepository.acceptFriendRequest(userId)
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(error = null) }
                    },
                    onFailure = { e ->
                        val errorMsg = when (e) {
                            is SocketTimeoutException -> "Network timeout while accepting friend request. Please try again."
                            is IOException -> "Network error while accepting friend request. Please check your connection."
                            else -> "Failed to accept friend request: ${e.message}"
                        }
                        _uiState.update { it.copy(error = errorMsg) }
                    }
                )
            }
            catch (e: SocketTimeoutException) {
                _uiState.update { it.copy(error = "Network timeout while accepting friend request. Please try again.") }
            } catch (e: IOException) {
                _uiState.update { it.copy(error = "Network error while accepting friend request. Please check your connection.") }
            } catch (e: HttpException) {
                _uiState.update { it.copy(error = "Failed to accept friend request: ${e.message}") }
            }
        }
    }

    /**
     * Denies a friend request.
     *
     * Processes a friend request denial and handles error cases with
     * appropriate user feedback.
     *
     * @param userId The ID of the user whose friend request is being denied.
     */
    fun denyRequest(userId: String) {
        viewModelScope.launch {
            try {
                val result = FriendsRepository.denyFriendRequest(userId)
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(error = null) }
                    },
                    onFailure = { e ->
                        val errorMsg = when (e) {
                            is SocketTimeoutException -> "Network timeout while declining friend request. Please try again."
                            is IOException -> "Network error while declining friend request. Please check your connection."
                            else -> "Failed to decline friend request: ${e.message}"
                        }
                        _uiState.update { it.copy(error = errorMsg) }
                    }
                )
            }
            catch (e: SocketTimeoutException) {
                _uiState.update { it.copy(error = "Network timeout while accepting friend request. Please try again.") }
            } catch (e: IOException) {
                _uiState.update { it.copy(error = "Network error while accepting friend request. Please check your connection.") }
            } catch (e: HttpException) {
                _uiState.update { it.copy(error = "Failed to accept friend request: ${e.message}") }
            }

        }
    }

    /**
     * Updates input text for either friend filtering or email input.
     *
     * This method serves two purposes based on the isFilter parameter:
     * 1. When isFilter is true, it updates the filter query and filtered friends list
     * 2. When isFilter is false, it updates the email input for adding a friend
     *
     * @param input The text input from the user.
     * @param isFilter Boolean flag indicating whether this is for filtering (true) or email input (false).
     */
    fun updateInput(input: String, isFilter: Boolean = false) {
        if (isFilter) {
            _uiState.update { currentState ->
                currentState.copy(
                    filterQuery = input,
                    filteredFriends = filterFriendsWithQuery(currentState.friends, input)
                )
            }
        } else {
            _uiState.update { it.copy(emailInput = input) }
        }
    }

    /**
     * Sends a friend request to a user by email.
     *
     * Validates the email input, sends the request, and handles success/failure
     * scenarios with appropriate UI updates and user feedback.
     */
    fun sendFriendRequest() {
        viewModelScope.launch {
            try {
                val email = state.value.emailInput.trim()
                if (email.isEmpty()) {
                    _uiState.update { it.copy(error = "Please enter an email address") }
                }
                else {
                    val result = FriendsRepository.sendFriendRequest(email)
                    result.fold(
                        onSuccess = {
                            _uiState.update {
                                it.copy(
                                    showAddFriendDialog = false,
                                    emailInput = "",
                                    error = null,
                                    requestSentSuccessful = true,
                                    successMessage = "Friend request sent successfully to $email"
                                )
                            }
                        },
                        onFailure = { e ->
                            val errorMsg = when (e) {
                                is SocketTimeoutException -> "Network timeout while sending friend request. Please try again."
                                is IOException -> "Network error while sending friend request. Please check your connection."
                                else -> "Failed to send friend request: ${e.message}"
                            }
                            _uiState.update { it.copy(error = errorMsg) }
                        }
                    )
                }
            } catch (e: SocketTimeoutException) {
                _uiState.update { it.copy(error = "Network timeout while accepting friend request. Please try again.") }
            } catch (e: IOException) {
                _uiState.update { it.copy(error = "Network error while accepting friend request. Please check your connection.") }
            } catch (e: HttpException) {
                _uiState.update { it.copy(error = "Failed to accept friend request: ${e.message}") }
            }
        }
    }

    /**
     * Removes a friend from the user's friend list.
     *
     * Processes the removal of a friendship connection and provides a callback
     * for UI components to react to the successful removal.
     *
     * @param targetUserId The ID of the friend to remove.
     * @param onSuccess Optional callback that will be invoked on successful removal.
     */
    fun removeFriend(targetUserId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val result = FriendsRepository.removeFriend(targetUserId)
                result.fold(
                    onSuccess = {
                        // Clear any errors and call success callback
                        _uiState.update { it.copy(error = null) }
                        onSuccess()
                    },
                    onFailure = { e ->
                        val errorMsg = when (e) {
                            is SocketTimeoutException -> "Network timeout while removing friend. Please try again."
                            is IOException -> "Network error while removing friend. Please check your connection."
                            else -> "Failed to remove friend: ${e.message}"
                        }
                        _uiState.update { it.copy(error = errorMsg) }
                    }
                )
            } catch (e: SocketTimeoutException) {
                _uiState.update { it.copy(error = "Network timeout while accepting friend request. Please try again.") }
            } catch (e: IOException) {
                _uiState.update { it.copy(error = "Network error while accepting friend request. Please check your connection.") }
            } catch (e: HttpException) {
                _uiState.update { it.copy(error = "Failed to accept friend request: ${e.message}") }
            }
        }
    }
}

/**
 * Filters a list of friends based on a search query.
 *
 * This helper function filters friends whose name or email contains
 * the search query (case-insensitive). If the query is empty, it returns
 * the original list unfiltered.
 *
 * @param friends The list of friends to filter.
 * @param query The search string to match against.
 * @return A filtered list of friends that match the query.
 */
private fun filterFriendsWithQuery(friends: List<Friend>, query: String): List<Friend> {
    return if (query.isEmpty()) {
        friends
    } else {
        friends.filter { friend ->
            friend.name.contains(query, ignoreCase = true) ||
                    friend.email.contains(query, ignoreCase = true)
        }
    }
}

/**
 * Data class representing the state of the Friends screen.
 *
 * This immutable state container holds all the data needed to render the
 * friends UI, including lists of friends and requests, dialog visibility,
 * loading states, and error/success messages.
 *
 * @property friends The complete list of friends.
 * @property filteredFriends The list of friends after applying search filters.
 * @property friendRequests The list of pending friend requests.
 * @property filterQuery The current search query string.
 * @property isLoading Flag indicating whether the friends list is loading.
 * @property isRequestsLoading Flag indicating whether friend requests are loading.
 * @property error Optional error message if an operation failed.
 * @property showRequestsDialog Flag controlling the visibility of the friend requests dialog.
 * @property showAddFriendDialog Flag controlling the visibility of the add friend dialog.
 * @property emailInput The current text in the email input field.
 * @property requestSentSuccessful Flag indicating a successfully sent friend request.
 * @property successMessage Optional success message to display.
 */
data class FriendsScreenState(
    val friends: List<Friend> = emptyList(),
    val filteredFriends: List<Friend> = emptyList(),
    val friendRequests: List<FriendRequest> = emptyList(),
    val filterQuery: String = "",
    val isLoading: Boolean = false,
    val isRequestsLoading: Boolean = false,
    val error: String? = null,
    val showRequestsDialog: Boolean = false,
    val showAddFriendDialog: Boolean = false,
    val emailInput: String = "",
    val requestSentSuccessful: Boolean = false,
    val successMessage: String? = null
)