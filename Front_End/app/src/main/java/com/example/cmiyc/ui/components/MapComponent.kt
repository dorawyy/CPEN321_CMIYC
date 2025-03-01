package com.example.cmiyc.ui.components

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.cmiyc.R
import com.example.cmiyc.data.Friend
import com.example.cmiyc.repositories.UserRepository
import com.mapbox.geojson.Point
import com.mapbox.maps.Image
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.annotation.IconImage
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
    var lastUpdatedLocation by remember { mutableStateOf<Point?>(null) }
    val thresholdDistance = 25.0 // distance in meters

    var isFirstUpdate = true
    // Define a fixed size for the friend icon (in dp)
    val iconSizeDp = 64.dp
    val density = LocalDensity.current
    val iconSizePx = with(density) { iconSizeDp.roundToPx() }

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
                if (isFirstUpdate) {
                    mapViewportState.setCameraOptions(
                        cameraOptions {
                            center(point)
                            zoom(14.0)
                        }
                    )
                    isFirstUpdate = false
                } else {
                    mapViewportState.setCameraOptions(
                        cameraOptions { center(point) }
                    )
                }

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
                val friendIcon = if (!friend.photoURL.isNullOrEmpty()) {
                    LoadFriendIcon(
                        context = context,
                        photoUrl = friend.photoURL,
                        iconSizePx = iconSizePx,
                    )
                } else {
                    defaultUserIcon
                }
                PointAnnotation(point = location) {
                    interactionsState.onClicked {
                        Toast.makeText(
                            context,
                            "Clicked on ${friend.name}'s annotation",
                            Toast.LENGTH_SHORT
                        ).show()
                        true
                    }
                    iconImage = friendIcon
                    textField = friend.name
                    textOffset = listOf(0.0, 2.0)
                }
            }
        }
    }
}

@Composable
private fun LoadFriendIcon(
    context: Context,
    photoUrl: String,
    iconSizePx: Int,
): IconImage {
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(photoUrl)
            .size(iconSizePx)
            .placeholder(R.drawable.default_user_icon)
            .error(R.drawable.default_user_icon)
            .allowHardware(false)
            .build()
    )

    // Track the loading state
    var isLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(painter.state) {
        if (painter.state is AsyncImagePainter.State.Success) {
            isLoaded = true
        }
    }

    return rememberIconImage(
        key = if (isLoaded) "loaded_$photoUrl" else "loading_$photoUrl",
        painter = painter,
    )
}