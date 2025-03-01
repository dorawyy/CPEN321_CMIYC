package com.example.cmiyc.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cmiyc.ui.screens.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var isLoggedIn by remember { mutableStateOf(false) }

    // Location permission handling
    val context = LocalContext.current
    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // Check if we have the required location permissions
    val hasLocationPermission = remember {
        locationPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    var permissionRequested by remember { mutableStateOf(false) }

    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionRequested = true
        // We don't need to do anything with the result as we'll check permissions again when needed
    }

    // Request permissions if needed
    LaunchedEffect(Unit) {
        if (!hasLocationPermission && !permissionRequested) {
            locationPermissionLauncher.launch(locationPermissions)
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "home" else "login"
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { _, _, _ ->
                    isLoggedIn = true
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onNavigateToProfile = {
                    navController.navigate("profile")
                },
                onNavigateToLog = {
                    navController.navigate("log")
                },
                onNavigateToFriends = {
                    navController.navigate("friends")
                },
                onNavigateToAdmin = {
                    navController.navigate("admin")
                }
            )
        }

        composable("profile") {
            ProfileScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                onSignedOut = {
                    isLoggedIn = false
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        composable("log") {
            LogScreen(
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }

        composable("friends") {
            FriendsScreen(
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }

        composable("admin") {
            AdminScreen(
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }
    }
}