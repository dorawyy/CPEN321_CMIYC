package com.example.cmiyc

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cmiyc.ui.theme.CMIYCTheme
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.logD
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location

class HomeActivity : ComponentActivity() {
    companion object {
        private const val TAG = "HomeActivity"
    }

    private lateinit var permissionsManager: PermissionsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CMIYCTheme {
                HomeScreen()
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @Composable
    fun HomeScreen() {
        var showDialog = remember { mutableStateOf(false) }
        var userInput = remember { mutableStateOf("") }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.weight(1f)
            ) {
                Map()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Button(
                    onClick = { showDialog.value = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Click Me")
                }
            }

            if (showDialog.value) {
                Log.d(TAG, "Dialog Open")
                AlertDialog(
                    onDismissRequest = { showDialog.value = false },
                    title = { Text("Status Update") },
                    text = {
                        Column {
                            TextField(
                                value = userInput.value,
                                onValueChange = { userInput.value = it },
                                label = { Text("Your Status") }
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            showDialog.value = false
                            Log.d(TAG, "Set Status " + userInput.value)
                        }) {
                            Text("Update")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showDialog.value = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun Map() {
        val mapViewportState = rememberMapViewportState()
        val mapView = remember { MapView(this@HomeActivity) }
        val pointAnnotationManager = remember { mapView.annotations.createPointAnnotationManager() }

        fun setPresetLocation() {
            val presetLocation = Point.fromLngLat(-123.23612571995412, 49.25505567088336)
            mapViewportState.setCameraOptions(
                cameraOptions {
                    center(presetLocation)
                    zoom(14.0)
                }
            )
            Log.d(TAG, "Setting map to preset location: $presetLocation")
        }

        fun setPuckLocation() {
            mapViewportState.transitionToFollowPuckState()
            Log.d(TAG, "Setting map to user location")
        }

        fun fetchFriendLocations() {
            Log.d(TAG, "Add Friend Annotation")
            val userLocations = listOf(
                Point.fromLngLat(-123.25041000865335, 49.26524685838906),
                Point.fromLngLat(-123.24543004678874, 49.254817878891814)
            )

            userLocations.forEach { location ->
                val pointAnnotationOptions = PointAnnotationOptions()
                    .withPoint(location)
                    .withIconImage("red_marker")
                pointAnnotationManager.create(pointAnnotationOptions)
            }

            pointAnnotationManager.addClickListener { annotation ->
                Log.d(TAG, "Marker clicked: ${annotation.point}")
                true
            }
        }

        MapboxMap(
            mapViewportState = mapViewportState,
            modifier = Modifier.fillMaxSize()
        ) {
            MapEffect(Unit) { mapView ->
                // Set up the location component
                mapView.location.updateSettings {
                    locationPuck = createDefault2DPuck(withBearing = true)
                    enabled = true
                    puckBearing = PuckBearing.COURSE
                    puckBearingEnabled = true
                }
            }

            CircleAnnotation(
                Point.fromLngLat(-123.25041000865335, 49.26524685838906),
            ) {
                interactionsState.onClicked {
                    Toast.makeText(
                        this@HomeActivity,
                        "Clicked on single Circle Annotation: $it",
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                }
                    .onLongClicked {
                        Toast.makeText(
                            this@HomeActivity,
                            "Long Clicked on single Circle Annotation: $it",
                            Toast.LENGTH_SHORT
                        ).show()
                        true
                    }
                    .onDragged {
                        logD(
                            TAG,
                            "Dragging single Circle Annotation: $it",
                        )
                    }
                circleRadius = 20.0
                circleColor = Color.Blue
            }
        }

        val permissionsListener = object : PermissionsListener {
            override fun onExplanationNeeded(permissionsToExplain: List<String>) {}

            override fun onPermissionResult(granted: Boolean) {
                if (granted) {
                    Log.d(TAG, "Location Permission Granted")
                    setPuckLocation()
                } else {
                    Log.d(TAG, "Location Permission Denied")
                    setPresetLocation()
                }
            }
        }

        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.d(TAG, "Location Permission Already Granted")
            setPuckLocation()
        } else {
            permissionsManager = PermissionsManager(permissionsListener)
            permissionsManager.requestLocationPermissions(this)
        }

        fetchFriendLocations()
    }
}
