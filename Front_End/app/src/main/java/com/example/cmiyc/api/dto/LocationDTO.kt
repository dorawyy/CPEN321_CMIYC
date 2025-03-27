package com.example.cmiyc.api.dto

/**
 * Data transfer object representing a geographical location.
 *
 * This DTO encapsulates location coordinates along with a timestamp indicating
 * when the location was recorded.
 *
 * @property latitude The latitude coordinate of the location in decimal degrees.
 * @property longitude The longitude coordinate of the location in decimal degrees.
 * @property timestamp The Unix timestamp (in milliseconds) when this location was recorded.
 */
data class LocationDTO(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
)