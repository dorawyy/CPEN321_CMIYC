package com.example.cmiyc.navigation

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cmiyc.repositories.UserRepository
import com.example.cmiyc.ui.screens.FriendsScreen
import com.example.cmiyc.ui.screens.HomeScreen
import com.example.cmiyc.ui.screens.LoginScreen
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object Log : Screen("log")
    object Friends : Screen("friends")
}

@Composable
fun AppNavigation(
    startDestination: String = Screen.Login.route,
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current

    val permissionsListener = remember {
        object : PermissionsListener {
            override fun onExplanationNeeded(permissionsToExplain: List<String>) {
                Log.d("Permissions", "Explanation needed for: $permissionsToExplain")
            }

            override fun onPermissionResult(granted: Boolean) {
                if (granted) {
                    Log.d("Permissions", "Location permissions granted")
                } else {
                    Log.d("Permissions", "Location permissions denied")
                }
            }
        }
    }

    val permissionsManager = remember { PermissionsManager(permissionsListener) }

    LaunchedEffect(Unit) {
        if (!PermissionsManager.areLocationPermissionsGranted(context)) {
            permissionsManager.requestLocationPermissions(context as Activity)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { email, displayName, idToken ->
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToLog = {
                    navController.navigate(Screen.Log.route)
                },
                onNavigateToFriends = {
                    navController.navigate(Screen.Friends.route)
                },
            )
        }

//        composable(Screen.Profile.route) {
//            ProfileScreen(
//                onNavigateBack = {
//                    navController.popBackStack()
//                }
//            )
//        }
//
//        composable(Screen.Log.route) {
//            LogScreen(
//                onNavigateBack = {
//                    navController.popBackStack()
//                }
//            )
//        }
//
        composable(Screen.Friends.route) {
            FriendsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}