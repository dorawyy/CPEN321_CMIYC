package com.example.cmiyc.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cmiyc.data.User
import com.example.cmiyc.repositories.UserRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

        viewModelScope.launch {
            try {
                val tokenTask = Firebase.messaging.token
                val token = tokenTask.await()
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
                val registrationSuccess = userRepository.registerUser()
                if (!registrationSuccess) {
                    _loginState.value = LoginState.Banned
                    return@launch
                }

                _loginState.value = LoginState.Success(
                    email = email,
                    displayName = displayName,
                    idToken = email,
                    photoUrl = photoUrl,
                    fcmToken = token,
                )
            } catch (e: Exception) {
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
    data class Success(
        val email: String,
        val displayName: String,
        val idToken: String,
        val photoUrl: String? = null,
        val fcmToken: String? = null
    ) : LoginState()
    data class Error(val message: String) : LoginState()
    object Banned : LoginState() // New state for banned users
}