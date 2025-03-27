package com.example.cmiyc.api.dto

/**
 * Data transfer object representing a friend user in the system.
 *
 * This DTO contains comprehensive information about a friend, including their
 * personal details, contact information, current location, and account status.
 *
 * @property userID The unique identifier of the friend user.
 * @property displayName The display name or username of the friend.
 * @property email The email address associated with the friend's account.
 * @property photoURL The URL to the friend's profile photo.
 * @property fcmToken The Firebase Cloud Messaging token used for sending push notifications to this friend.
 * @property currentLocation The current geographical location of the friend, represented as a LocationDTO.
 * @property isBanned Boolean flag indicating whether the friend's account is currently banned.
 */
data class FriendDTO(
    val userID: String,
    val displayName: String,
    val email: String,
    val photoURL: String,
    val fcmToken: String,
    val currentLocation: LocationDTO,
    val isBanned: Boolean
)