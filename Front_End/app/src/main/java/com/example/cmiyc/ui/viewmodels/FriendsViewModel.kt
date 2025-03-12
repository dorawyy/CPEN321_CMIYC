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

class FriendsViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FriendsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FriendsViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class FriendsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FriendsScreenState())
    val state: StateFlow<FriendsScreenState> = _uiState
    private val networkHandler = FriendNetworkHandler(_uiState)

    init {
        viewModelScope.launch {
            FriendsRepository.friends.collect { friends ->
                _uiState.update { currentState ->
                    currentState.copy(
                        friends = friends,
                        filteredFriends = filterFriendsWithQuery(friends, currentState.filterQuery)
                    )
                }
            }
        }
        collectRepositoryStates()
    }

    private fun collectRepositoryStates() {
        viewModelScope.launch {
            FriendsRepository.isFriendsLoading.collect { isLoading ->
                _uiState.update { it.copy(isLoading = isLoading) }
            }
        }

        viewModelScope.launch {
            FriendsRepository.friendRequests.collect { requests ->
                _uiState.update { it.copy(friendRequests = requests) }
            }
        }

        viewModelScope.launch {
            FriendsRepository.isRequestsLoading.collect { isLoading ->
                _uiState.update { it.copy(isRequestsLoading = isLoading) }
            }
        }
    }

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
    fun handleScreenLifecycle(isEntering: Boolean) {
        if (isEntering) {
            networkHandler.fetchFriends(viewModelScope)
        }
    }
    fun updateState(update: (FriendsScreenState) -> FriendsScreenState) {
        _uiState.update(update)
    }
    fun filterFriends(query: String) {
        _uiState.update { currentState ->
            currentState.copy(
                filterQuery = query,
                filteredFriends = filterFriendsWithQuery(currentState.friends, query)
            )
        }
    }
    fun manageFriendRequests(action: FriendRequestAction) {
        when (action) {
            is FriendRequestAction.Load -> networkHandler.loadFriendRequests(viewModelScope)
            is FriendRequestAction.Accept -> networkHandler.acceptRequest(viewModelScope, action.userId)
            is FriendRequestAction.Deny -> networkHandler.denyRequest(viewModelScope, action.userId)
            is FriendRequestAction.Send -> {
                _uiState.update { it.copy(emailInput = action.email) }
                if (action.shouldSend) networkHandler.sendFriendRequest(viewModelScope, state.value.emailInput)
            }
        }
    }
    fun manageFriend(action: FriendAction) {
        when (action) {
            is FriendAction.Remove -> networkHandler.removeFriend(viewModelScope, action.userId, action.onSuccess)
            is FriendAction.Refresh -> networkHandler.fetchFriends(viewModelScope)
        }
    }
    fun manageMessages(action: MessageAction) {
        when (action) {
            is MessageAction.ClearError -> _uiState.update { it.copy(error = null) }
            is MessageAction.ClearSuccess -> _uiState.update { it.copy(requestSentSuccessful = false, successMessage = null) }
        }
    }
}
class FriendNetworkHandler(private val _uiState: MutableStateFlow<FriendsScreenState>) {

    fun fetchFriends(scope: CoroutineScope) {
        scope.launch {
            try {
                FriendsRepository.fetchFriendsOnce()
            } catch (e: Exception) {
                handleNetworkError(e, "loading friends")
            }
        }
    }

    fun loadFriendRequests(scope: CoroutineScope) {
        scope.launch {
            try {
                val result = FriendsRepository.fetchFriendRequestsOnce()
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(showRequestsDialog = true, error = null) }
                    },
                    onFailure = { e ->
                        handleNetworkError(e, "loading friend requests")
                    }
                )
            } catch (e: Exception) {
                handleNetworkError(e, "loading friend requests")
            }
        }
    }

    fun acceptRequest(scope: CoroutineScope, userId: String) {
        scope.launch {
            try {
                val result = FriendsRepository.acceptFriendRequest(userId)
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(error = null) }
                    },
                    onFailure = { e ->
                        handleNetworkError(e, "accepting friend request")
                    }
                )
            } catch (e: Exception) {
                handleNetworkError(e, "accepting friend request")
            }
        }
    }

    fun denyRequest(scope: CoroutineScope, userId: String) {
        scope.launch {
            try {
                val result = FriendsRepository.denyFriendRequest(userId)
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(error = null) }
                    },
                    onFailure = { e ->
                        handleNetworkError(e, "declining friend request")
                    }
                )
            } catch (e: Exception) {
                handleNetworkError(e, "declining friend request")
            }
        }
    }

    fun sendFriendRequest(scope: CoroutineScope, email: String) {
        scope.launch {
            try {
                val trimmedEmail = email.trim()
                if (trimmedEmail.isEmpty()) {
                    _uiState.update { it.copy(error = "Please enter an email address") }
                } else {
                    val result = FriendsRepository.sendFriendRequest(trimmedEmail)
                    result.fold(
                        onSuccess = {
                            _uiState.update {
                                it.copy(
                                    showAddFriendDialog = false,
                                    emailInput = "",
                                    error = null,
                                    requestSentSuccessful = true,
                                    successMessage = "Friend request sent successfully to $trimmedEmail"
                                )
                            }
                        },
                        onFailure = { e ->
                            handleNetworkError(e, "sending friend request")
                        }
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "sending friend request")
            }
        }
    }

    fun removeFriend(scope: CoroutineScope, targetUserId: String, onSuccess: () -> Unit = {}) {
        scope.launch {
            try {
                val result = FriendsRepository.removeFriend(targetUserId)
                result.fold(
                    onSuccess = { onSuccess() },
                    onFailure = { e ->
                        handleNetworkError(e, "removing friend")
                    }
                )
            } catch (e: Exception) {
                handleNetworkError(e, "removing friend")
            }
        }
    }

    private fun handleNetworkError(e: Throwable, action: String) {
        val errorMsg = when (e) {
            is SocketTimeoutException -> "Network timeout while $action. Please try again."
            is IOException -> "Network error while $action. Please check your connection."
            is HttpException -> "Failed to $action: ${e.message}"
            else -> "Failed to $action: ${e.message}"
        }
        _uiState.update { it.copy(error = errorMsg) }
    }
}

sealed class FriendRequestAction {
    object Load : FriendRequestAction()
    data class Accept(val userId: String) : FriendRequestAction()
    data class Deny(val userId: String) : FriendRequestAction()
    data class Send(val email: String, val shouldSend: Boolean = true) : FriendRequestAction()
}

sealed class FriendAction {
    data class Remove(val userId: String, val onSuccess: () -> Unit = {}) : FriendAction()
    object Refresh : FriendAction()
}

sealed class MessageAction {
    object ClearError : MessageAction()
    object ClearSuccess : MessageAction()
}

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