package com.example.cmiyc.data

import com.mapbox.geojson.Point

data class User(
    val userId: String,
    val email: String,
    val displayName: String,
    val currentLocation: Point? = null,
    val lastLocationUpdate: Long = System.currentTimeMillis()
)

data class UserCredentials(
    val email: String,
    val userId: String,
    val displayName: String,
)