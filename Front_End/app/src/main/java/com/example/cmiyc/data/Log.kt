package com.example.cmiyc.data

import com.mapbox.geojson.Point

data class Log(
    val sender: String,
    val activity: String,
    val senderLocation: Point,
    val timestamp: String,

)