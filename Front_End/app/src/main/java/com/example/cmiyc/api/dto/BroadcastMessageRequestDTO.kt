package com.example.cmiyc.api.dto

/**
 * Data transfer object for broadcasting a message to users.
 *
 * This DTO is used when sending event broadcast messages throughout the system.
 * It contains the name of the event that should be broadcast.
 *
 * @property eventName The name of the event to be broadcast to users.
 */
data class BroadcastMessageRequestDTO(
    val eventName: String
)