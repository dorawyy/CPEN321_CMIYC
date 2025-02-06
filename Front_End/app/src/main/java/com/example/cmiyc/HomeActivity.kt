package com.example.cmiyc

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cmiyc.ui.theme.CMIYCTheme
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.animation.MapAnimationOptions
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
        var showDialog = remember { mutableStateOf(false)};
        var userInput  = remember { mutableStateOf("")};

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Wrap the Button in a Box and apply padding to Box
            Box(
                Modifier.weight(1f)
            ) {
                Map()
            }


            // Wrap the Button in a Box and apply padding to Box
            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
            ) {
                Button(
                    onClick = { showDialog.value = true },
                    modifier = Modifier.fillMaxWidth().padding(16.dp) // Ensures button width matches parent
                ) {
                    Text("Click Me")
                }
            }

            // Show input popup when showDialog is true
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
                            // Handle input submission (e.g., save value, update UI, etc.)

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

        // Function to set the map to a preset location (e.g., coordinates for a city or place)
        fun setPresetLocation() {
            val presetLocation = Point.fromLngLat(-73.9857, 40.7484) // Example: Coordinates for Times Square, NYC
            mapViewportState.flyTo(
                cameraOptions {
                    center(presetLocation)
                },
                MapAnimationOptions.mapAnimationOptions { duration(5000) }
            )
            Log.d(TAG, "Setting map to preset location: $presetLocation")
        }

        fun setPuckLocation() {
            mapViewportState.transitionToFollowPuckState()
            Log.d(TAG, "Setting map to user location")
        }

        var permissionsListener: PermissionsListener = object : PermissionsListener {
            override fun onExplanationNeeded(permissionsToExplain: List<String>) {

            }

            override fun onPermissionResult(granted: Boolean) {
                if (granted) {
                    // Permission sensitive logic called here, such as activating the Maps SDK's LocationComponent to show the device's location
                    Log.d(TAG, "Location Permission Granted")
                    setPuckLocation()
                } else {

                    // User denied the permission
                    Log.d(TAG, "Location Permission Denied")
                    setPresetLocation();
                }
            }
        }

        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Permission sensitive logic called here, such as activating the Maps SDK's LocationComponent to show the device's location
            Log.d(TAG, "Location Permission Already Granted")
        } else {
            permissionsManager = PermissionsManager(permissionsListener)
            permissionsManager.requestLocationPermissions(this)
        }


        MapboxMap(
            mapViewportState = mapViewportState,
            modifier = Modifier.fillMaxSize()

        ) {
            MapEffect(Unit) { mapView ->
                mapView.location.updateSettings {
                    locationPuck = createDefault2DPuck(withBearing = true)
                    enabled = true
                    puckBearing = PuckBearing.COURSE
                    puckBearingEnabled = true
                }
                mapViewportState.transitionToFollowPuckState()
            }
        }
    }


    @Preview(showBackground = true)
    @Composable
    fun HomePage() {
        CMIYCTheme {
            HomeScreen()
        }
    }
}


