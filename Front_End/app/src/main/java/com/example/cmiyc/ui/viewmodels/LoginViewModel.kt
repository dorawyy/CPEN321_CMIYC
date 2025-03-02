package com.example.cmiyc.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cmiyc.data.User
import com.example.cmiyc.repositories.UserRepository
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.supervisorScope

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

class LoginViewModel(
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Initial)
    val loginState: StateFlow<LoginState> = _loginState

    fun handleSignInResult(email: String?, displayName: String?, idToken: String?, photoUrl: String?) {
        if (email == null || displayName == null || idToken == null) {
            _loginState.value = LoginState.Error("Sign in failed: Missing credentials")
            return
        }

        // Change to Loading state first
        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            try {
                // Use supervisorScope to prevent cancellation propagation
                supervisorScope {
                    // Get FCM token
                    val tokenTask = Firebase.messaging.token
                    val token = try {
                        tokenTask.await()
                    } catch (e: Exception) {
                        // Log the error but continue with null token
                        println("Failed to get FCM token: ${e.message}")
                        null
                    }

                    // Create user object
                    val user = User(
                        email = email,
                        displayName = displayName,
                        userId = email,
                        photoUrl = photoUrl,
                        currentLocation = null,
                        fcmToken = token,
                    )

                    // Set current user in repository
                    userRepository.setCurrentUser(user)

                    // Set FCM token if available
                    if (token != null) {
                        try {
                            userRepository.setFCMToken(token)
                        } catch (e: Exception) {
                            // Log the error but continue
                            println("Failed to set FCM token: ${e.message}")
                        }
                    }

                    // Register user with the API
                    try {
                        val registrationSuccess = userRepository.registerUser()
                        if (!registrationSuccess) {
                            _loginState.value = LoginState.Banned
                            return@supervisorScope
                        }

                        // Success! Update login state
                        _loginState.value = LoginState.Success(
                            email = email,
                            displayName = displayName,
                            idToken = email,
                            photoUrl = photoUrl,
                            fcmToken = token,
                        )
                    } catch (e: CancellationException) {
                        // Re-throw cancellation exceptions to properly handle coroutine cancellation
                        throw e
                    } catch (e: Exception) {
                        println("Registration failed: ${e.message}")
                        _loginState.value = LoginState.Error("Registration failed: ${e.message}")
                        resetState()
                    }
                }
            } catch (e: CancellationException) {
                // Handle cancellation specifically
                println("Login process was cancelled: ${e.message}")
                _loginState.value = LoginState.Error("Login process was cancelled")
                resetState()
            } catch (e: Exception) {
                // Handle all other exceptions
                println("Login failed with exception: ${e.message}")
                _loginState.value = LoginState.Error("Login failed: ${e.message}")
                resetState()
            }
        }
    }

    fun resetState() {
        viewModelScope.launch {
            try {
                userRepository.clearCurrentUser()
                _loginState.value = LoginState.Initial
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Failed to reset state: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}

sealed class LoginState {
    object Initial : LoginState()
    object Loading : LoginState() // New loading state
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