package com.example.cmiyc.api.dto

data class LogDTO(
    val eventName: String,
    val sender_name: String,
    val timestamp: Long,
    val sender_location: LocationDTO,
)