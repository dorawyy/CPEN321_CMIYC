package com.example.cmiyc.test

import android.util.Log
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
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
class FR1_LoginWithGoogleAuthentication {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val testAccountEmail = "guanhua.qiao2020@gmail.com" // Replace with actual test account

    private val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation

    private val TAG = "FR1_LoginWithGoogleAuthentication"

    @Before
    fun setup() {
        Log.d(TAG, "Test setup started")
        Intents.init()
        // Ensure test account is added to device settings
        uiDevice.executeShellCommand("pm grant ${composeTestRule.activity.packageName} android.permission.ACCESS_FINE_LOCATION")
        Log.d(TAG, "Test setup completed")
    }

    @Test
    fun test1Login() {
        Log.d(TAG, "Starting test1Login")
        login()
        Log.d(TAG, "test1Login completed")
    }

    @Test
    fun test2Signout() {
        Log.d(TAG, "Starting test2Signout")
        login()
        composeTestRule.onNodeWithTag("profile_button").performClick()
        Log.d(TAG, "Clicked on profile button")

        // Verify main screen elements
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("signout_button").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        Log.d(TAG, "Signout button exists, performing click")
        composeTestRule.onNodeWithTag("signout_button").performClick()

        // Verify main screen elements
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("login_button").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        Log.d(TAG, "Login button exists after signout")
        composeTestRule.onNodeWithTag("login_button").assertExists()
        Log.d(TAG, "test2Signout completed")
    }

    @Test
    fun test3LoginFailure() {
        Log.d(TAG, "Starting test3LoginFailure")
        // Get the UiAutomation instance
        // Execute the shell command to disable WiFi
        uiAutomation.executeShellCommand("svc wifi disable")
        uiAutomation.executeShellCommand("svc data disable")
        Log.d(TAG, "WiFi and data disabled")

        // Handle location permission dialog
        handleLocationPermission()

        // Click Google login button
        composeTestRule.onNodeWithTag("login_button").performClick()
        Log.d(TAG, "Clicked on login button")

        // Handle Google account selection
        selectGoogleAccount(testAccountEmail)

        // Verify main screen elements
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("Login Error").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        Log.d(TAG, "Login Error dialog exists")
        composeTestRule.onNodeWithText("Login Error").assertExists()

        // Click Google login button
        composeTestRule.onNodeWithText("OK").performClick()
        Log.d(TAG, "Clicked OK on Login Error dialog")

        // Verify main screen elements
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("login_button").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        Log.d(TAG, "Login button exists after error")
        composeTestRule.onNodeWithTag("login_button").assertExists()
        Log.d(TAG, "test3LoginFailure completed")
    }

    private fun login() {
        Log.d(TAG, "Login process started")
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
        Log.d(TAG, "Login button exists, performing click")
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
        Log.d(TAG, "Broadcast button exists after login")
        composeTestRule.onNodeWithTag("broadcast_button").assertExists()
        Log.d(TAG, "Login process completed")
    }

    private fun selectGoogleAccount(email: String) {
        try {
            // Wait for account picker
            uiDevice.wait(Until.findObject(By.textContains("Choose an account")), 3000)
            Log.d(TAG, "Account picker displayed")

            // Scroll through accounts if needed
            uiDevice.findObject(
                UiSelector()
                    .textMatches("(?i).*$email.*")
            ).click()
            Log.d(TAG, "Selected account: $email")
        } catch (e: Exception) {
            // If already signed in, proceed
            Log.d(TAG, "Selected account: $email already signed in")
        }
    }

    private fun handleLocationPermission() {
        try {
            uiDevice.wait(Until.findObject(
                By.textContains("Allow")), 3000
            )
            Log.d(TAG, "Location permission dialog displayed")

            uiDevice.findObject(
                UiSelector()
                    .textMatches("(?i)Only this time|Allow|While using the app")
            ).click()
            Log.d(TAG, "Location permission granted")
        } catch (e: Exception) {
            // Permission dialog not found
            Log.d(TAG, "Location permission dialog not found")
        }
    }

    @After
    fun tearDown() {
        Log.d("FR1_LoginWithGoogleAuthentication", "Test teardown started")
        // Sign out after test
        UserRepository.clearCurrentUser()
        Log.d("FR1_LoginWithGoogleAuthentication", "Cleared current user")

        composeTestRule.activity.runOnUiThread {
            GoogleSignIn.getClient(
                composeTestRule.activity,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            ).signOut()
            Log.d("FR1_LoginWithGoogleAuthentication", "Signed out from Google")
        }

        uiAutomation.executeShellCommand("svc wifi enable")
        uiAutomation.executeShellCommand("svc data enable")
        Log.d("FR1_LoginWithGoogleAuthentication", "WiFi and data enabled")

        Intents.release()
        Log.d("FR1_LoginWithGoogleAuthentication", "Test teardown completed")
    }
}