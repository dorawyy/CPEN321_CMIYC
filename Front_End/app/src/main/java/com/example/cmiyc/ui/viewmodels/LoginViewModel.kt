package com.example.cmiyc.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cmiyc.data.User
import com.example.cmiyc.location.LocationManager
import com.example.cmiyc.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModelFactory(
    private val userRepository: UserRepository,
    private val locationManager: LocationManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(userRepository, locationManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class LoginViewModel(
    private val userRepository: UserRepository,
    private val locationManager: LocationManager
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
                // Create user credentials
                val credentials = User(
                    email = email,
                    displayName = displayName,
                    userId = idToken,
                    photoUrl = photoUrl,
                    currentLocation = null,
                )

                // Save user credentials and user
                userRepository.setCurrentUser(credentials)

                // Start location updates
                locationManager.startLocationUpdates()

                _loginState.value = LoginState.Success(
                    email = email,
                    displayName = displayName,
                    idToken = idToken,
                    photoUrl = photoUrl,
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
                // Clear user credentials
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
        val photoUrl: String? = null
    ) : LoginState()
    data class Error(val message: String) : LoginState()
}