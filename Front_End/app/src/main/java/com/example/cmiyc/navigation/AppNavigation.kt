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

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var isLoggedIn by remember { mutableStateOf(false) }

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