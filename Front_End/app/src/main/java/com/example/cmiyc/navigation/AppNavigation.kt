package com.example.cmiyc.navigation

import HomeScreen
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
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.cmiyc.repositories.UserRepository

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val currentUser by UserRepository.currentUser.collectAsState()
    val isRegistrationComplete by UserRepository.isRegistrationComplete.collectAsState()

    // User is fully authenticated if there's a current user AND registration is complete
    val isFullyAuthenticated = currentUser != null && isRegistrationComplete

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

    val activity = context as Activity

    // Request permissions if needed
    LaunchedEffect(Unit) {
        if (!hasLocationPermission && !permissionRequested) {
            locationPermissionLauncher.launch(locationPermissions)
        }
    }

    LaunchedEffect(isFullyAuthenticated) {
        if (isFullyAuthenticated) {
            // Only navigate to home if we're on the login screen
            if (navController.currentDestination?.route == "login") {
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            }
        } else {
            // If user becomes unauthenticated while in the app, navigate back to login
            if (navController.currentDestination?.route != "login") {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    // Handle notification navigation
    LaunchedEffect(isFullyAuthenticated) {
        if (isFullyAuthenticated && activity.intent.getBooleanExtra("NAVIGATE_TO_LOG", false)) {
            navController.navigate("log")
            // Clear the flag to avoid repeated navigation
            activity.intent.removeExtra("NAVIGATE_TO_LOG")
        }
    }

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { _, _, _ ->
                    // Navigation is now handled by the LaunchedEffect above
                    // No need to navigate here
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