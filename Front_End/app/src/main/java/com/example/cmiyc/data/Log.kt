package com.example.cmiyc.data

import com.mapbox.geojson.Point

/**
 * Data class representing an activity log entry in the system.
 *
 * This class captures information about system events and user activities,
 * including who performed the action, what was done, where it occurred,
 * and when it happened.
 *
 * @property sender The name or identifier of the user who generated the activity.
 * @property activity The description of the action or event that occurred.
 * @property senderLocation The geographical location where the activity took place, as a GeoJSON Point.
 * @property timestamp The time (in milliseconds since epoch) when the activity occurred.
 */
data class Log(
    val sender: String,
    val activity: String,
    val senderLocation: Point,
    val timestamp: Long,
)