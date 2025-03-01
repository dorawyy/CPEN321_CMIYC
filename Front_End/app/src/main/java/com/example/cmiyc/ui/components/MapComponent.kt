package com.example.cmiyc.ui.components

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.cmiyc.R
import com.example.cmiyc.data.Friend
import com.example.cmiyc.repositories.UserRepository
import com.mapbox.geojson.Point
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.turf.TurfMeasurement

@Composable
fun MapComponent(
    context: Context,
    mapViewportState: MapViewportState,
    friends: List<Friend>,
    modifier: Modifier = Modifier
) {
    var lastUpdatedLocation: Point? = null
    val thresholdDistance = 25.0 // distance in meters
    var isFirstUpdate = true

    fun calculateDistance(from: Point, to: Point): Double {
        return TurfMeasurement.distance(from, to) * 1000 // Convert km to meters
    }

    MapboxMap(
        mapViewportState = mapViewportState,
        modifier = modifier
    ) {
        MapEffect(Unit) { mapView ->
            mapView.location.updateSettings {
                locationPuck = createDefault2DPuck(withBearing = true)
                enabled = true
                puckBearing = PuckBearing.COURSE
                puckBearingEnabled = true
            }

            mapView.location.addOnIndicatorPositionChangedListener { point ->
                // For camera centering
                mapViewportState.setCameraOptions(
                    cameraOptions {
                        center(point)
                        if (isFirstUpdate) {
                            zoom(14.0)
                            isFirstUpdate = false
                        }
                    }
                )

                // For location updates
                val shouldUpdate = if (lastUpdatedLocation == null) {
                    true  // Always update the first time
                } else {
                    calculateDistance(lastUpdatedLocation!!, point) > thresholdDistance
                }

                if (shouldUpdate) {
                    Log.d("MapComponent", "Location updated: moved ${lastUpdatedLocation?.let {
                        calculateDistance(it, point).toInt()
                    } ?: "first update"} meters")

                    lastUpdatedLocation = point
                    UserRepository.updateUserLocation(point)
                }
            }
        }

        val defaultUserIcon = rememberIconImage(
            key = R.drawable.default_user_icon,
            painter = painterResource(id = R.drawable.default_user_icon)
        )

        friends.forEach { friend ->
            friend.location?.let { location ->
                PointAnnotation(point = location) {
                    interactionsState.onClicked {
                        Toast.makeText(
                            context,
                            "Clicked on ${friend.name}'s annotation",
                            Toast.LENGTH_SHORT
                        ).show()
                        true
                    }
                    iconImage = defaultUserIcon
                    textField = friend.name
                    textOffset = listOf(0.0, 5.0)
                }
            }
        }
    }
}