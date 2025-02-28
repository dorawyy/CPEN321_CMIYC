package com.example.cmiyc.ui.components

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.cmiyc.R
import com.example.cmiyc.data.Friend
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
    val thresholdDistance = 100.0 // distance in meters
    var isFirstUpdate = true

    fun calculateDistance(from: Point, to: Point): Double {
        val distanceInKilometers = TurfMeasurement.distance(from, to)

        // Convert to meters if needed
        val distanceInMeters = distanceInKilometers * 1000
        return distanceInMeters
    }

    fun postLocationUpdate(point: Point) {
        Log.d("MapComponent", "Test")
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
                mapViewportState.setCameraOptions(
                    cameraOptions {
                        center(point)
                        if (isFirstUpdate) {
                            zoom(14.0)
                            isFirstUpdate = false
                        }
                    }
                )
            }

            mapView.location.addOnIndicatorPositionChangedListener { point ->
                // Calculate distance if we have a previous location
                if (lastUpdatedLocation == null || calculateDistance(lastUpdatedLocation!!, point) > thresholdDistance) {
                    lastUpdatedLocation = point
                    postLocationUpdate(point) // Function to trigger your POST request
                }

                mapViewportState.setCameraOptions(
                    cameraOptions {
                        center(point)
                    }
                )
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