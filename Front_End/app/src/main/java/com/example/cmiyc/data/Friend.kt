package com.example.cmiyc.data

import com.mapbox.geojson.Point

data class Friend(
    val userId: String,
    val name: String,
    val email: String,
    val photoURL: String? = null,
    val location: Point? = null,
    val lastUpdated: Long? = null,
    val isBanned: Boolean,
)