package com.example.cmiyc.api.dto

data class LogDTO(
    val eventName: String,
    val fromName: String,
    val location: LocationDTO,
)