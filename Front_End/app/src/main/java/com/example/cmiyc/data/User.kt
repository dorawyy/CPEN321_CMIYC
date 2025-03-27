package com.example.cmiyc.data

import com.mapbox.geojson.Point

/**
 * Data class representing a user in the system.
 *
 * This class contains comprehensive information about a user account,
 * including their identity, contact information, device details,
 * current location, and tracking data.
 *
 * @property userId The unique identifier of the user.
 * @property email The email address associated with the user's account.
 * @property displayName The display name or username of the user.
 * @property fcmToken The Firebase Cloud Messaging token used for sending push notifications to this user's device, may be null if not available.
 * @property photoUrl The URL to the user's profile photo, may be null if no photo is set.
 * @property currentLocation The user's current geographical location as a GeoJSON Point, may be null if location is unavailable.
 * @property lastLocationUpdate Timestamp (in milliseconds) of when the user's location was last updated, defaults to the current time.
 */
data class User(
    val userId: String,
    val email: String,
    val displayName: String,
    val fcmToken: String? = null,
    val photoUrl: String? = null,
    val currentLocation: Point? = null,
    val lastLocationUpdate: Long = System.currentTimeMillis()
)