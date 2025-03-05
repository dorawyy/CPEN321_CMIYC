package com.example.cmiyc.test

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
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

    @Before
    fun setup() {
        Intents.init()
        // Ensure test account is added to device settings
        uiDevice.executeShellCommand("pm grant ${composeTestRule.activity.packageName} android.permission.ACCESS_FINE_LOCATION")

        uiAutomation.executeShellCommand("svc wifi enable")
        uiAutomation.executeShellCommand("svc data enable")

        login()
    }


    @Test
    fun test1ViewLogs() {
        composeTestRule.onNodeWithTag("log_button").performClick()

        // Verify log screen elements
        composeTestRule.waitUntil(1000*30*5) {
            try {
                composeTestRule.onNodeWithText("Test").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        composeTestRule.onNodeWithText("Test").assertIsDisplayed()
    }

    @Test
    fun test2ViewLogsFailure() {
        composeTestRule.onNodeWithTag("log_button").performClick()

        // Execute the shell command to disable WiFi
        uiAutomation.executeShellCommand("svc wifi disable")
        uiAutomation.executeShellCommand("svc data disable")

        // Verify log screen elements
        composeTestRule.waitUntil(1000*30*6) {
            try {
                composeTestRule.onNodeWithText("Sync Problem").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        composeTestRule.onNodeWithText("Sync Problem").assertIsDisplayed()

        uiAutomation.executeShellCommand("svc wifi enable")
        uiAutomation.executeShellCommand("svc data enable")
    }

    private fun login() {
        // Handle location permission dialog
        handleLocationPermission()

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("login_button").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        // Click Google login button
        composeTestRule.onNodeWithTag("login_button").performClick()

        // Handle Google account selection
        selectGoogleAccount(testAccountEmail)

        // Verify main screen elements
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("broadcast_button").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        composeTestRule.onNodeWithTag("broadcast_button").assertExists()
    }

    private fun selectGoogleAccount(email: String) {
        try {
            // Wait for account picker
            uiDevice.wait(Until.findObject(By.textContains("Choose an account")), 3000)

            // Scroll through accounts if needed
            uiDevice.findObject(
                UiSelector()
                    .textMatches("(?i).*$email.*")
            ).click()
        } catch (e: Exception) {
            // If already signed in, proceed
        }
    }

    private fun handleLocationPermission() {
        try {
            uiDevice.wait(Until.findObject(
                By.textContains("Allow")), 3000
            )

            uiDevice.findObject(
                UiSelector()
                    .textMatches("(?i)Only this time|Allow|While using the app")
            ).click()
        } catch (e: Exception) {
            // Permission dialog not found
        }
    }

    @After
    fun tearDown() {
        UserRepository.clearCurrentUser()

        // Sign out after test
        composeTestRule.activity.runOnUiThread {
            GoogleSignIn.getClient(
                composeTestRule.activity,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            ).signOut()
        }

        uiAutomation.executeShellCommand("svc wifi enable")
        uiAutomation.executeShellCommand("svc data enable")

        Intents.release()
    }
}