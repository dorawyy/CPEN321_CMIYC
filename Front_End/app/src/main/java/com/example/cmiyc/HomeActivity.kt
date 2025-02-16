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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.cmiyc.ui.theme.CMIYCTheme
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.common.MapboxSDKCommon.getContext
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.logD
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.delay


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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HomeScreen() {
        var showDialog = remember { mutableStateOf(false) }
        var userInput = remember { mutableStateOf("") }

        // Scaffold provides a structure with a top bar, content, etc.
        Scaffold(
            topBar = {
                androidx.compose.material3.TopAppBar(
                    title = {},
                    // Left button
                    navigationIcon = {
                        androidx.compose.material3.IconButton(onClick = {
                            // TODO: Handle left button click (e.g., open a drawer)
                        }) {
                            androidx.compose.material3.Icon(
                                painter = painterResource(id = R.drawable.log_icon), // Replace with your left icon
                                contentDescription = "Left Button"
                            )
                        }
                    },
                    // Right button
                    actions = {
                        androidx.compose.material3.IconButton(onClick = {
                            // TODO: Handle right button click (e.g., navigate to settings)
                        }) {
                            androidx.compose.material3.Icon(
                                painter = painterResource(id = R.drawable.user_profile_icon), // Replace with your right icon
                                contentDescription = "Right Button"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), // Respect Scaffold's inner padding
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // The map occupies the majority of the screen.
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    Map()
                }
                // Bottom button area.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Button(
                        onClick = { showDialog.value = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("Broadcast")
                    }
                }
                if (showDialog.value) {
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
                                Log.d(TAG, "Set Status ${userInput.value}")
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
    }

    // Simulate a POST request that returns friend locations as JSON.
// In a real scenario, you would use Retrofit/OkHttp along with a JSON parser.
    private suspend fun fetchFriendLocationsFromServer(): List<FriendLocation> {
        delay(2000L) // Simulate network delay
        // Simulated JSON parsing: here we directly create sample data.
        return listOf(
            FriendLocation(
                userId = "user1",
                name = "Alice",
                status = "Online",
                point = Point.fromLngLat(-123.25041000865335, 49.26524685838906)
            ),
            FriendLocation(
                userId = "user2",
                name = "Bob",
                status = "Busy",
                point = Point.fromLngLat(-123.251, 49.265)
            ),
            FriendLocation(
                userId = "user3",
                name = "Carol",
                status = "Away",
                point = Point.fromLngLat(-123.250, 49.2655)
            )
        )
    }

    @Composable
    fun Map() {
        val mapViewportState = rememberMapViewportState()
        val mapView = remember { MapView(this@HomeActivity) }
        val pointAnnotationManager = remember { mapView.annotations.createPointAnnotationManager() }

        // Set up a state to hold friend locations from our fake POST request.
        val friendLocations = remember { mutableStateOf<List<FriendLocation>>(emptyList()) }

        // Simulate fetching friend locations when the composable enters composition.
        LaunchedEffect(Unit) {
            friendLocations.value = fetchFriendLocationsFromServer()
        }

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

        val defaultUserIcon = rememberIconImage(
            key = R.drawable.default_user_icon,
            painter = painterResource(id = R.drawable.default_user_icon)
        )

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

            // Create an annotation for each friend.
            friendLocations.value.forEach { friend ->
                PointAnnotation(point = friend.point) {
                    interactionsState.onClicked {
                        Toast.makeText(
                            this@HomeActivity,
                            "Clicked on ${friend.name}'s annotation",
                            Toast.LENGTH_SHORT
                        ).show()
                        true
                    }
                    // Use the default icon.
                    iconImage = defaultUserIcon
                    // Display the friend's name and status as text below the icon.
                    textField = "${friend.name}\n${friend.status}"
                    // Optionally, adjust text properties (offset, size, color, etc.)
                    textOffset = listOf(0.0, 5.0)
                }
            }
        }
    }
}

// Define a data model for a friend's location.
data class FriendLocation(
    val userId: String,
    val name: String,
    val status: String,
    val point: Point
)
