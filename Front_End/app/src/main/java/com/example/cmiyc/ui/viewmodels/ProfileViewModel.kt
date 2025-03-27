package com.example.cmiyc.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil.network.HttpException
import com.example.cmiyc.data.User
import com.example.cmiyc.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Factory class for creating instances of ProfileViewModel with dependencies.
 *
 * This factory follows the ViewModelProvider.Factory pattern to facilitate
 * dependency injection for the ProfileViewModel. It ensures that the
 * ViewModel is created with the necessary UserRepository dependency.
 *
 * @property userRepository The repository responsible for user data and operations.
 */
class ProfileViewModelFactory (
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * ViewModel for the Profile screen.
 *
 * This ViewModel manages the state and business logic for the user profile interface,
 * including retrieving user information and handling the sign-out process.
 * It observes the current user data from the repository and maintains the UI state.
 *
 * @property userRepository The repository responsible for user data and operations.
 */
class ProfileViewModel (
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileScreenState())
    val state: StateFlow<ProfileScreenState> = _state

    init {
        viewModelScope.launch {
            userRepository.currentUser.collect { user ->
                _state.update {
                    it.copy(
                        user = user,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Initiates the sign-out process.
     *
     * This method calls the repository to sign out the current user
     * and handles various error conditions that might occur during
     * the process.
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                userRepository.signOut()
            }
            catch (e: IOException) {
                _state.update {
                    it.copy(
                        error = "Network error: Please check your connection.",
                        isLoading = false
                    )
                }
            }
            catch (e: HttpException) {
                _state.update {
                    it.copy(
                        error = "Failed to sign out: ${e.message}",
                        isLoading = false
                    )
                }
            }
            catch (e: CancellationException) {
                _state.update {
                    it.copy(
                        error = "Cancellation Error: Failed to cancel: ${e.message}",
                        isLoading = false
                    )
                }
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
 * Data class representing the state of the Profile screen.
 *
 * This immutable state container holds all the data needed to render the
 * profile UI, including the user information, loading state, and error messages.
 *
 * @property user The current user information, or null if no user is authenticated.
 * @property isLoading Flag indicating whether a loading operation is in progress.
 * @property error Optional error message if an operation failed.
 */
data class ProfileScreenState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

