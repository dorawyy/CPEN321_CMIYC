package com.example.cmiyc.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil.network.HttpException
import com.example.cmiyc.data.User
import com.example.cmiyc.repositories.UserRepository
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Factory class for creating instances of LoginViewModel with dependencies.
 *
 * This factory follows the ViewModelProvider.Factory pattern to facilitate
 * dependency injection for the LoginViewModel. It ensures that the
 * ViewModel is created with the necessary UserRepository dependency.
 *
 * @property userRepository The repository responsible for user data and authentication.
 */
class LoginViewModelFactory(
    private val userRepository: UserRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * ViewModel for the Login screen.
 *
 * This ViewModel manages the authentication flow, including Google Sign-In,
 * Firebase Cloud Messaging token retrieval, and user registration with the backend.
 * It handles various authentication states and error conditions.
 *
 * @property userRepository The repository responsible for user data and authentication.
 */
class LoginViewModel(
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Initial)
    val loginState: StateFlow<LoginState> = _loginState

    /**
     * Sets whether the user is requesting admin privileges.
     *
     * This is primarily used for testing administrative features during development
     * and would typically be restricted in production environments.
     *
     * @param requested Boolean flag indicating if admin privileges are requested.
     */
    fun setAdminRequested(requested: Boolean) {
        userRepository.setAdminRequested(requested)
    }

    /**
     * Processes the Google Sign-In result and completes the authentication flow.
     *
     * This method:
     * 1. Validates the required sign-in data
     * 2. Retrieves an FCM token for push notifications
     * 3. Creates a user object with the sign-in data
     * 4. Registers the user with the backend
     * 5. Updates the login state based on the result
     *
     * @param email The user's email address from Google Sign-In.
     * @param displayName The user's display name from Google Sign-In.
     * @param idToken The authentication token from Google Sign-In.
     * @param photoUrl The URL to the user's profile photo.
     */
    fun handleSignInResult(email: String?, displayName: String?, idToken: String?, photoUrl: String?) {
        if (email == null || displayName == null || idToken == null) {
            // This could happen due to network issues or Google Sign-In problems
            _loginState.value = LoginState.Error("Unable to sign in. Please check your internet connection and try again.")
            return
        }

        viewModelScope.launch {
            try {
                val tokenTask = Firebase.messaging.token
                val token = try {
                    tokenTask.await()
                } catch (e: HttpException) {
                    // If FCM token retrieval fails due to network issues, we can still proceed
                    // with a placeholder value, and we'll update it later when network is available
                    println("FCM token retrieval failed: ${e.message}")
                    "pending_token"
                }
                val user = User(
                    email = email,
                    displayName = displayName,
                    userId = email,
                    photoUrl = photoUrl,
                    currentLocation = null,
                    fcmToken = token,
                )
                userRepository.setCurrentUser(user)

                // Check if user is banned during registration
                try {
                    val registrationSuccess = userRepository.registerUser()
                    userRepository.setFCMToken(token)
                    if (!registrationSuccess) {
                        _loginState.value = LoginState.Banned
                    }
                    else {
                        _loginState.value = LoginState.Success(
                            email = email,
                            displayName = displayName,
                            idToken = email,
                            photoUrl = photoUrl,
                            fcmToken = token,
                        )
                    }
                } catch (e: SocketTimeoutException) {
                    _loginState.value = LoginState.Error("Network timeout during registration. Please check your connection and try again.")
                } catch (e: IOException) {
                    _loginState.value = LoginState.Error("Network error during registration. Please check your connection and try again.")
                }
            } catch (e: HttpException) {
                _loginState.value = LoginState.Error("Login failed: ${e.message ?: "Unknown error"}")
                resetState()
            }
        }
    }

    /**
     * Resets the authentication state.
     *
     * This method clears the current user data and returns the login state
     * to its initial state. It's typically called when authentication fails
     * or when the user logs out.
     */
    fun resetState() {
        viewModelScope.launch {
            try {
                userRepository.clearCurrentUser()
                _loginState.value = LoginState.Initial
            } catch (e: SocketTimeoutException) {
                _loginState.value = LoginState.Error("Network timeout during registration. Please check your connection and try again: ${e.message}")
            } catch (e: IOException) {
                _loginState.value = LoginState.Error("Network error during registration. Please check your connection and try again: ${e.message}")
            } catch (e: HttpException) {
                _loginState.value = LoginState.Error("Failed to reset state: ${e.message}")
            }
        }
    }

    /**
     * Called when the ViewModel is being destroyed.
     *
     * Performs cleanup operations when needed.
     */
    override fun onCleared() {
        super.onCleared()
    }
}

/**
 * Sealed class representing the different states of the login process.
 *
 * This class hierarchy defines all possible authentication states:
 * - Initial: The starting state before authentication begins
 * - Success: Authentication completed successfully
 * - Error: An error occurred during authentication
 * - Banned: User is banned from the system
 */
sealed class LoginState {
    object Initial : LoginState()
    /**
     * State representing successful authentication with user details.
     *
     * @property email The user's email address.
     * @property displayName The user's display name.
     * @property idToken The authentication token.
     * @property photoUrl Optional URL to the user's profile photo.
     * @property fcmToken Optional Firebase Cloud Messaging token for push notifications.
     */
    data class Success(
        val email: String,
        val displayName: String,
        val idToken: String,
        val photoUrl: String? = null,
        val fcmToken: String? = null
    ) : LoginState()
    data class Error(val message: String) : LoginState()
    object Banned : LoginState() // State for banned users
}