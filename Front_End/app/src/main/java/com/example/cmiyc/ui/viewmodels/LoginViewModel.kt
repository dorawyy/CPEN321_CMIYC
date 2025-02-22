package com.example.cmiyc.ui.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LoginViewModel : ViewModel() {
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Initial)
    val loginState: StateFlow<LoginState> = _loginState

    fun handleSignInResult(email: String?, displayName: String?, idToken: String?) {
        if (email != null && displayName != null && idToken != null) {
            _loginState.value = LoginState.Success(email, displayName, idToken)
        } else {
            _loginState.value = LoginState.Error("Sign in failed")
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Initial
    }
}

sealed class LoginState {
    object Initial : LoginState()
    data class Success(
        val email: String,
        val displayName: String,
        val idToken: String
    ) : LoginState()
    data class Error(val message: String) : LoginState()
}