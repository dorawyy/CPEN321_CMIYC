package com.example.cmiyc

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.cmiyc.ui.theme.CMIYCTheme
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import androidx.compose.material3.Surface
import com.example.cmiyc.api.ApiClient
import com.example.cmiyc.navigation.AppNavigation
import com.google.firebase.initialize

/**
 * Main entry point for the "Catch Me If You Can" application.
 *
 * This activity initializes the application components, sets up Firebase services,
 * configures the API client, and renders the main Compose UI. It also handles
 * intent updates for deep linking and notifications.
 */
class MainActivity : ComponentActivity() {
    /**
     * Called when the activity is first created.
     *
     * This method initializes the application by:
     * - Enabling edge-to-edge display
     * - Initializing Firebase services
     * - Setting up the API client
     * - Configuring the Compose UI with the application theme and navigation
     *
     * @param savedInstanceState If the activity is being re-initialized after previously
     *        being shut down, this contains the data it most recently supplied in onSaveInstanceState.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Firebase.initialize(this)
        ApiClient.initialize(applicationContext)
        setContent {
            CMIYCTheme {
                Surface {
                    AppNavigation()
                }
            }
        }
    }

    /**
     * Called when a new intent is received by the activity.
     *
     * This method updates the activity's intent, which is necessary for handling
     * deep links and notifications when the app is already running.
     *
     * @param intent The new intent that was started for the activity.
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the activity's intent
    }
}