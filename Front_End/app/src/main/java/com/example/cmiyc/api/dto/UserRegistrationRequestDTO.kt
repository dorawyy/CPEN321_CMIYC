package com.example.cmiyc.api.dto

/**
 * Data transfer object for user registration requests.
 *
 * This DTO contains all necessary information to register a new user in the system,
 * including personal details, contact information, device token, location, and admin status.
 *
 * @property userID The unique identifier for the new user, typically provided by the authentication system.
 * @property displayName The display name or username the user wishes to use in the system.
 * @property email The email address associated with the user's account.
 * @property photoURL The URL to the user's profile photo.
 * @property fcmToken The Firebase Cloud Messaging token for the user's device to receive push notifications.
 * @property currentLocation The initial geographical location of the user, represented as a LocationDTO.
 * @property isAdmin Boolean flag indicating whether the user should be registered with administrative privileges.
 */
data class UserRegistrationRequestDTO(
    val userID: String,
    val displayName: String,
    val email: String,
    val photoURL: String,
    val fcmToken: String,
    val currentLocation: LocationDTO,
    val isAdmin: Boolean,
)