package com.example.cmiyc.data

/**
 * Data class representing a user item for administrative purposes.
 *
 * This class contains essential user information required for administrative functions
 * such as user listing, monitoring, and moderation actions.
 *
 * @property userId The unique identifier of the user.
 * @property name The display name of the user.
 * @property email The email address associated with the user's account.
 * @property photoURL The URL to the user's profile photo, may be null if no photo is set.
 * @property isBanned Boolean flag indicating whether the user's account is currently banned.
 */
data class AdminUserItem (
    val userId: String,
    val name: String,
    val email: String,
    val photoURL: String?,
    val isBanned: Boolean
)