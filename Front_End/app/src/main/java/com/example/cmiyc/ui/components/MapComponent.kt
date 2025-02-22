package com.example.cmiyc.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.cmiyc.R
import com.example.cmiyc.ui.viewmodels.FriendLocation
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

@Composable
fun MapComponent(
    context: Context,
    mapViewportState: MapViewportState,
    friendLocations: List<FriendLocation>,
    modifier: Modifier = Modifier
) {
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
                        zoom(14.0)
                    }
                )
            }
        }

        val defaultUserIcon = rememberIconImage(
            key = R.drawable.default_user_icon,
            painter = painterResource(id = R.drawable.default_user_icon)
        )

        friendLocations.forEach { friend ->
            PointAnnotation(point = friend.point) {
                interactionsState.onClicked {
                    Toast.makeText(
                        context,
                        "Clicked on ${friend.name}'s annotation",
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                }
                iconImage = defaultUserIcon
                textField = "${friend.name}\n${friend.status}"
                textOffset = listOf(0.0, 5.0)
            }
        }
    }
}