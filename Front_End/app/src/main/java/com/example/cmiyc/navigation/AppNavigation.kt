package com.example.cmiyc.navigation

import HomeScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cmiyc.repositories.UserRepository
import com.example.cmiyc.ui.screens.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val currentUser by UserRepository.currentUser.collectAsState()
    val isRegistrationComplete by UserRepository.isRegistrationComplete.collectAsState()

    val isFullyAuthenticated = currentUser != null && isRegistrationComplete

    NavigationHandler(
        navController = navController,
        isFullyAuthenticated = isFullyAuthenticated
    )

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { _,_,_ ->
                    // Navigation is handled by NavigationHandler
                }
            )
        }
        composable("home") {
            HomeScreen(
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToLog = { navController.navigate("log") },
                onNavigateToFriends = { navController.navigate("friends") },
                onNavigateToAdmin = { navController.navigate("admin") }
            )
        }
        composable("profile") {
            ProfileScreen(
                onNavigateBack = { navController.navigateUp() },
                onSignedOut = {
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
        composable("log") {
            LogScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable("friends") {
            FriendsScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable("admin") {
            AdminScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}
