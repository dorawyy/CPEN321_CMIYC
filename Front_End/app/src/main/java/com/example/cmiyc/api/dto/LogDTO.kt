package com.example.cmiyc.api.dto

/**
 * Data transfer object representing a event log entry.
 *
 * This DTO captures information about events, including the event name,
 * the source of the event, and the geographical location where the event occurred.
 *
 * @property eventName The name or type of the event that was logged.
 * @property fromName The name of the user or that generated the event.
 * @property location The geographical location where the event occurred, represented as a LocationDTO.
 */
data class LogDTO(
    val eventName: String,
    val fromName: String,
    val location: LocationDTO,
)