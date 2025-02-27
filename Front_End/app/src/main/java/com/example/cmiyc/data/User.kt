package com.example.cmiyc.data

import com.mapbox.geojson.Point

data class User(
    val userId: String,
    val email: String,
    val displayName: String,
    val fcmToken: String? = null,
    val photoUrl: String? = null,
    val currentLocation: Point? = null,
    val lastLocationUpdate: Long = System.currentTimeMillis()
)