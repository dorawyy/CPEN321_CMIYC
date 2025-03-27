package com.example.cmiyc.data

/**
 * Data class representing a pending friend request in the system.
 *
 * This class contains essential information about a received friend connection request,
 * including the requestor's identity and when the request was made.
 *
 * @property userId The unique identifier of the user who sent the friend request.
 * @property displayName The display name of the user who sent the friend request.
 * @property timestamp The time (in milliseconds since epoch) when the friend request was sent.
 */
data class FriendRequest(
    val userId: String,
    val displayName: String,
    val timestamp: Long
)