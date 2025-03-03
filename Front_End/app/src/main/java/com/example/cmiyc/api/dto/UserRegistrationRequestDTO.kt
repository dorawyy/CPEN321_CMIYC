package com.example.cmiyc.api.dto

data class UserRegistrationRequestDTO(
    val userID: String,
    val displayName: String,
    val email: String,
    val photoURL: String,
    val fcmToken: String,
    val currentLocation: LocationDTO,
    val isAdmin: Boolean,
)