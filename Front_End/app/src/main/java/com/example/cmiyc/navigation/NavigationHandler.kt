package com.example.cmiyc.navigation

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavController

@Composable
fun NavigationHandler(
    navController: NavController,
    isFullyAuthenticated: Boolean
) {
    val context = LocalContext.current
    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val hasLocationPermission = remember {
        locationPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    var permissionRequested by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionRequested = true
        // Handle permission result if needed
    }

    val activity = context as Activity

    LaunchedEffect(Unit) {
        if (!hasLocationPermission && !permissionRequested) {
            locationPermissionLauncher.launch(locationPermissions)
        }
    }

    LaunchedEffect(isFullyAuthenticated) {
        if (isFullyAuthenticated) {
            if (navController.currentDestination?.route == "login") {
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            }
        } else {
            if (navController.currentDestination?.route != "login") {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    LaunchedEffect(isFullyAuthenticated) {
        if (isFullyAuthenticated && activity.intent.getBooleanExtra("NAVIGATE_TO_LOG", false)) {
            navController.navigate("log")
            activity.intent.removeExtra("NAVIGATE_TO_LOG")
        }
    }
}
