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

/**
 * Main navigation component for the application.
 *
 * This composable function sets up the navigation graph for the entire application,
 * handles authentication state, and defines all navigation routes between screens.
 * It observes the authentication state from UserRepository to determine which
 * screens should be accessible.
 */
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
        /**
         * Login screen route.
         *
         * This is the entry point of the application where users authenticate.
         * Upon successful login, the NavigationHandler will redirect to the appropriate screen.
         */
        composable("login") {
            LoginScreen(
                onLoginSuccess = { _,_,_ ->
                    // Navigation is handled by NavigationHandler
                }
            )
        }

        /**
         * Home screen route.
         *
         * The main screen of the application after authentication, showing
         * the map and primary user interface. Provides navigation to all
         * other authenticated screens.
         */
        composable("home") {
            HomeScreen(
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToLog = { navController.navigate("log") },
                onNavigateToFriends = { navController.navigate("friends") },
                onNavigateToAdmin = { navController.navigate("admin") }
            )
        }

        /**
         * Profile screen route.
         *
         * Displays and allows editing of the user's profile information.
         * Also provides the option to sign out of the application.
         */
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

        /**
         * Log screen route.
         *
         * Displays activity logs and events from the system and other users.
         */
        composable("log") {
            LogScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        /**
         * Friends screen route.
         *
         * Manages friend relationships, including viewing current friends,
         * sending friend requests, and responding to received requests.
         */
        composable("friends") {
            FriendsScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        /**
         * Admin screen route.
         *
         * Provides administrative functions such as user management
         * and system moderation. Only accessible to users with
         * administrative privileges.
         */
        composable("admin") {
            AdminScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}
