package com.example.cmiyc.api.dto

/**
 * Data transfer object representing a user in the system.
 *
 * This DTO contains comprehensive information about a user, including their
 * personal details, contact information, current location, and account status.
 *
 * @property userID The unique identifier of the user.
 * @property displayName The display name or username of the user.
 * @property email The email address associated with the user's account.
 * @property photoURL The URL to the user's profile photo.
 * @property fcmToken The Firebase Cloud Messaging token used for sending push notifications to this user.
 * @property currentLocation The current geographical location of the user, represented as a LocationDTO.
 * @property isBanned Boolean flag indicating whether the user's account is currently banned.
 */
data class UserDTO(
    val userID: String,
    val displayName: String,
    val email: String,
    val photoURL: String,
    val fcmToken: String,
    val currentLocation: LocationDTO,
    val isBanned: Boolean,
)