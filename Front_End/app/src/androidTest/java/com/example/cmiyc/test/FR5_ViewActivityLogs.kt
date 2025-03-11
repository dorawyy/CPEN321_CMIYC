package com.example.cmiyc.test

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.example.cmiyc.MainActivity
import com.example.cmiyc.repositories.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class FR5_ViewActivityLogs {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val testAccountEmail = "guanhua.qiao2020@gmail.com" // Replace with actual test account

    private val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
    private val TAG = "FR5_ViewActivityLogs"

    @Before
    fun setup() {
        Log.d(TAG, "Test setup started")
        Intents.init()
        // Ensure test account is added to device settings
        uiDevice.executeShellCommand("pm grant ${composeTestRule.activity.packageName} android.permission.ACCESS_FINE_LOCATION")
        Log.d(TAG, "Location permission granted")

        uiAutomation.executeShellCommand("svc wifi enable")
        uiAutomation.executeShellCommand("svc data enable")
        Log.d(TAG, "WiFi and data enabled")

        login()
    }

    @Test
    fun test1ViewLogs() {
        Log.d(TAG, "Starting test1ViewLogs")
        composeTestRule.onNodeWithTag("log_button").performClick()
        Log.d(TAG, "Clicked on log button")

        // Verify log screen elements
        composeTestRule.waitUntil(1000 * 30 * 5) {
            try {
                composeTestRule.onNodeWithText("Test").assertExists()
                Log.d(TAG, "Log entry 'Test' found")
                true
            } catch (e: AssertionError) {
                false
            }
        }
        composeTestRule.onNodeWithText("Test").assertIsDisplayed()
        Log.d(TAG, "Log entry 'Test' is displayed")
    }

    @Test
    fun test2ViewLogsFailure() {
        Log.d(TAG, "Starting test2ViewLogsFailure")
        composeTestRule.onNodeWithTag("log_button").performClick()
        Log.d(TAG, "Clicked on log button")

        // Execute the shell command to disable WiFi
        uiAutomation.executeShellCommand("svc wifi disable")
        uiAutomation.executeShellCommand("svc data disable")
        Log.d(TAG, "WiFi and data disabled for failure simulation")

        // Verify log screen elements
        composeTestRule.waitUntil(1000 * 30 * 6) {
            try {
                composeTestRule.onNodeWithText("Sync Problem").assertExists()
                Log.d(TAG, "Sync Problem message found")
                true
            } catch (e: AssertionError) {
                false
            }
        }
        composeTestRule.onNodeWithText("Sync Problem").assertIsDisplayed()
        Log.d(TAG, "Sync Problem message is displayed")
    }

    private fun login() {
        Log.d(TAG, "Starting login process")
        // Handle location permission dialog
        handleLocationPermission()

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("login_button").assertExists()
                Log.d(TAG, "Login button found")
                true
            } catch (e: AssertionError) {
                Log.d(TAG, "Login button not found")
                false
            }
        }
        // Click Google login button
        composeTestRule.onNodeWithTag("login_button").performClick()
        Log.d(TAG, "Clicked on login button")

        // Handle Google account selection
        selectGoogleAccount(testAccountEmail)

        // Verify main screen elements
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("broadcast_button").assertExists()
                Log.d(TAG, "Broadcast button found, login successful")
                true
            } catch (e: AssertionError) {
                Log.d(TAG, "Broadcast button not found")
                false
            }
        }
        composeTestRule.onNodeWithTag("broadcast_button").assertExists()
    }

    private fun selectGoogleAccount(email: String) {
        Log.d(TAG, "Selecting Google account: $email")
        try {
            // Wait for account picker
            uiDevice.wait(Until.findObject(By.textContains("Choose an account")), 3000)
            Log.d(TAG, "Account picker dialog found")

            // Scroll through accounts if needed
            uiDevice.findObject(
                UiSelector()
                    .textMatches("(?i).*$email.*")
            ).click()
            Log.d(TAG, "Selected Google account: $email")
        } catch (e: Exception) {
            Log.d(TAG, "Account selection failed or already signed in: ${e.message}")
        }
    }

    private fun handleLocationPermission() {
        Log.d(TAG, "Handling location permission dialog")
        try {
            uiDevice.wait(Until.findObject(By.textContains("Allow")), 3000)
            Log.d(TAG, "Location permission dialog found")

            uiDevice.findObject(
                UiSelector()
                    .textMatches("(?i)Only this time|Allow|While using the app")
            ).click()
            Log.d(TAG, "Location permission granted")
        } catch (e: Exception) {
            Log.d(TAG, "Location permission dialog not found or already granted")
        }
    }

    @After
    fun tearDown() {
        Log.d(TAG, "Starting tearDown")
        UserRepository.clearCurrentUser()
        Log.d(TAG, "Cleared current user")

        // Sign out after test
        composeTestRule.activity.runOnUiThread {
            GoogleSignIn.getClient(
                composeTestRule.activity,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            ).signOut()
            Log.d(TAG, "Signed out from Google account")
        }

        Intents.release()
        Log.d(TAG, "Intents released")
    }
}