package com.example.cmiyc.ui.components

import android.content.Context
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
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon

/**
 * Main map component that displays a Mapbox map with user location and friend markers.
 *
 * This composable integrates real-time location tracking, friend position visualization,
 * and map interaction capabilities. It displays the current user's location as a puck
 * and shows friend locations as custom markers with their profile pictures.
 *
 * @param context Android context used for Toast messages and image loading.
 * @param mapViewportState State object that controls the map's camera position and zoom level.
 * @param friends List of Friend objects to display on the map.
 * @param modifier Optional modifier for customizing the component layout.
 */
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
    val iconSizeDp = 64.dp
    val density = LocalDensity.current
    val iconSizePx = with(density) { iconSizeDp.roundToPx() }

    Box(modifier = modifier.fillMaxSize()) {
        MapboxMap(mapViewportState = mapViewportState, modifier = modifier) {
            LocationUpdateHandler(mapViewportState = mapViewportState, lastUpdatedLocation = lastUpdatedLocation, thresholdDistance = thresholdDistance, isFirstUpdate = isFirstUpdate,
                onLocationUpdated = { point ->
                    lastUpdatedLocation = point
                    UserRepository.locationManager.updateUserLocation(point)
                }
            )

            val defaultUserIcon = rememberIconImage(
                key = R.drawable.default_user_icon,
                painter = painterResource(id = R.drawable.default_user_icon)
            )

            friends.forEach { friend ->
                if (!friend.isBanned) {
                    friend.location?.let { location ->
                        val friendIcon = if (!friend.photoURL.isNullOrEmpty()) {
                            LoadFriendIcon(context = context, photoUrl = friend.photoURL, iconSizePx = iconSizePx)
                        } else {
                            defaultUserIcon
                        }
                        PointAnnotation(point = location) {
                            interactionsState.onClicked {
                                Toast.makeText(context, "Clicked on ${friend.name}'s annotation", Toast.LENGTH_SHORT).show()
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

        Box(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            LocationResetButton(context = context, lastUpdatedLocation = lastUpdatedLocation, mapViewportState = mapViewportState,)
        }
    }
}

/**
 * Calculates the distance between two geographical points in meters.
 *
 * Uses the Turf library's distance calculation and converts from kilometers to meters.
 *
 * @param from The starting point.
 * @param to The destination point.
 * @return The distance between the points in meters.
 */
fun calculateDistance(from: Point, to: Point): Double {
    return TurfMeasurement.distance(from, to) * 1000 // Convert km to meters
}

/**
 * Handler for user location updates on the map.
 *
 * This composable sets up the location puck (user position indicator) and manages
 * location update events. It implements a threshold-based approach to reduce
 * unnecessary updates, only triggering when the user moves a significant distance.
 *
 * @param mapViewportState State object that controls the map's camera.
 * @param lastUpdatedLocation The last location that triggered an update.
 * @param thresholdDistance The minimum distance in meters required to trigger a new update.
 * @param isFirstUpdate Flag indicating if this is the first location update.
 * @param onLocationUpdated Callback invoked when the location is updated.
 */
@Composable
fun LocationUpdateHandler(
    mapViewportState: MapViewportState,
    lastUpdatedLocation: Point?,
    thresholdDistance: Double,
    isFirstUpdate: Boolean,
    onLocationUpdated: (Point) -> Unit
) {
    MapEffect(Unit) { mapView ->
        mapView.location.updateSettings {
            locationPuck = createDefault2DPuck(withBearing = true)
            enabled = true
            puckBearing = PuckBearing.COURSE
            puckBearingEnabled = true
        }

        mapView.location.addOnIndicatorPositionChangedListener { point ->
            val shouldUpdate = lastUpdatedLocation?.let {
                calculateDistance(it, point) > thresholdDistance
            } ?: true

            if (shouldUpdate) {
                if (isFirstUpdate) {
                    mapViewportState.setCameraOptions(
                        cameraOptions { center(point); zoom(14.0) }
                    )
                }
                onLocationUpdated(point)
            }
        }
    }
}

/**
 * A floating action button that resets the map camera to the user's current location.
 *
 * This button allows users to quickly return to their current position on the map
 * after panning or exploring other areas.
 *
 * @param context Android context used for Toast messages.
 * @param lastUpdatedLocation The last known user location.
 * @param mapViewportState State object that controls the map's camera.
 */
@Composable
fun LocationResetButton(
    context: Context,
    lastUpdatedLocation: Point?,
    mapViewportState: MapViewportState
) {
    FloatingActionButton(
        onClick = {
            lastUpdatedLocation?.let { point ->
                mapViewportState.setCameraOptions(
                    cameraOptions { center(point); zoom(14.0) }
                )
            } ?: Toast.makeText(context, "User location not available", Toast.LENGTH_SHORT).show()
        },
    ) {
        Icon(
            painter = painterResource(id = R.drawable.locate_me),
            contentDescription = "Reset Camera"
        )
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