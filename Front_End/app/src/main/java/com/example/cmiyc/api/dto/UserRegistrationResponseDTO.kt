package com.example.cmiyc.api.dto

/**
 * Data transfer object for the response to a user registration request.
 *
 * This DTO provides information about the status of the newly registered user,
 * including whether they are banned or have administrative privileges.
 *
 * @property isBanned Boolean flag indicating whether the user's account is currently banned.
 * @property isAdmin Boolean flag indicating whether the user has administrative privileges.
 */
data class UserRegistrationResponseDTO (
    val isBanned: Boolean,
    val isAdmin: Boolean,
)