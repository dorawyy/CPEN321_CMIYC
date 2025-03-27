package com.example.cmiyc.data

import com.mapbox.geojson.Point

/**
 * Data class representing a friend connection in the system.
 *
 * This class contains comprehensive information about a connected friend,
 * including their personal details, real-time location data, and account status.
 *
 * @property userId The unique identifier of the friend.
 * @property name The display name of the friend.
 * @property email The email address associated with the friend's account.
 * @property photoURL The URL to the friend's profile photo, may be null if no photo is set.
 * @property location The friend's current geographical location as a GeoJSON Point, may be null if location is unavailable.
 * @property lastUpdated Timestamp (in milliseconds) of when the friend's location was last updated, may be null if no location updates have occurred.
 * @property isBanned Boolean flag indicating whether the friend's account is currently banned.
 */
data class Friend(
    val userId: String,
    val name: String,
    val email: String,
    val photoURL: String? = null,
    val location: Point? = null,
    val lastUpdated: Long? = null,
    val isBanned: Boolean,
)