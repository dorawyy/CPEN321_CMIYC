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

    fun setAdminRequested(requested: Boolean) {
        userRepository.setAdminRequested(requested)
    }

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

    override fun onCleared() {
        super.onCleared()
    }
}

sealed class LoginState {
    object Initial : LoginState()
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