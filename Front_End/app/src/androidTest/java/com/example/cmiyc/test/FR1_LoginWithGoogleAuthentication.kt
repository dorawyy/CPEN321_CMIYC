package com.example.cmiyc.test

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FR1_LoginWithGoogleAuthentication {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val testAccountEmail = "guanhua.qiao2020@gmail.com" // Replace with actual test account

    @Before
    fun setup() {
        Intents.init()
        // Ensure test account is added to device settings
        uiDevice.executeShellCommand("pm grant ${composeTestRule.activity.packageName} android.permission.ACCESS_FINE_LOCATION")
    }

    @Test
    fun realGoogleLoginFlow() {

        // Handle location permission dialog
        handleLocationPermission()

        // Click Google login button
        composeTestRule.onNodeWithText("Sign in with Google").performClick()

        // Handle Google account selection
        selectGoogleAccount(testAccountEmail)

        // Verify main screen elements
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("Broadcast").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        composeTestRule.onNodeWithText("Broadcast").assertIsDisplayed()
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
        // Sign out after test
        composeTestRule.activity.runOnUiThread {
            GoogleSignIn.getClient(
                composeTestRule.activity,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            ).signOut()
        }
        Intents.release()
    }
}