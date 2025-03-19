package com.example.cmiyc.api.dto

/**
 * Data transfer object for requesting a location update.
 *
 * This DTO is used to send a user's updated geographical location to the system.
 * It wraps a LocationDTO object containing the new location coordinates and timestamp.
 *
 * @property currentLocation The user's current geographical location details.
 */
data class LocationUpdateRequestDTO(
    val currentLocation: LocationDTO,
)