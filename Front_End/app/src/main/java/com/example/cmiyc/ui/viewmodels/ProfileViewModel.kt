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

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

data class ProfileScreenState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

