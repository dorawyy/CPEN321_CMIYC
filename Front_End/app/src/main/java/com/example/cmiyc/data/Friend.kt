package com.example.cmiyc.data

import com.mapbox.geojson.Point

data class Friend(
    val userId: String,
    val name: String,
    val email: String,
    val status: String = "Offline",
    val location: Point? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)