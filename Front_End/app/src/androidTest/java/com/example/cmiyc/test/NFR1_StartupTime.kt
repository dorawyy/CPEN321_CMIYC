package com.example.cmiyc.test


import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
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
import org.hamcrest.number.OrderingComparison.lessThan
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class NFR1_StartupTime {

    companion object {
        private const val PACKAGE_NAME = "com.example.cmiyc"
        private const val COLD_START_THRESHOLD = 5000L // 5 seconds
        private const val WARM_START_THRESHOLD = 2000L // 2 seconds
        private const val HOT_START_THRESHOLD = 1500L // 1.5 seconds
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation

    private val TAG = "NFR1_StartupTime"

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setup() {
        Log.d(TAG, "Test setup started")
        // Ensure test account is added to device settings
        uiDevice.executeShellCommand("pm grant ${composeTestRule.activity.packageName} android.permission.ACCESS_FINE_LOCATION")
        Log.d(TAG, "Test setup completed")
    }

    @After
    fun tearDown() {
        Log.d(TAG, "Test teardown started")
        // Sign out after test
        UserRepository.clearCurrentUser()
        Log.d(TAG, "Cleared current user")

        composeTestRule.activity.runOnUiThread {
            GoogleSignIn.getClient(
                composeTestRule.activity,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            ).signOut()
            Log.d(TAG, "Signed out from Google")
        }

        uiAutomation.executeShellCommand("svc wifi enable")
        uiAutomation.executeShellCommand("svc data enable")
        Log.d(TAG, "WiFi and data enabled")

        Intents.release()
        Log.d(TAG, "Test teardown completed")
    }

    @Test
    fun test1ColdStartTime() {
        Log.d(TAG, "test1ColdStartTime Start")
        Log.d(TAG, "StartTime -------")
        val startTime = System.currentTimeMillis()
        Intents.init()

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("login_button").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        val duration = System.currentTimeMillis() - startTime
        Log.d(TAG, "Duration: $duration")
        assertThat("Cold start time exceeded", duration, lessThan(COLD_START_THRESHOLD))
        Log.d(TAG, "test1ColdStartTime End")
    }

    @Test
    fun test2WarmStartTime() {
        Log.d(TAG, "test2WarmStartTime Start")
        Log.d(TAG, "setup for warm start")
        Intents.init()
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("login_button").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        Log.d(TAG, "Kill the app process")
        Intents.release()

        Log.d(TAG, "Relaunch and measure warm start time")
        val startTime = System.currentTimeMillis()
        Intents.init()

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("login_button").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        val duration = System.currentTimeMillis() - startTime
        Log.d(TAG, "Duration: $duration")
        assertThat("Warm start time exceeded", duration, lessThan(WARM_START_THRESHOLD))
        Log.d(TAG, "test2WarmStartTime End")
    }

    @Test
    fun test3HotStartTime() {
        Log.d(TAG, "test3HotStartTime Start")
        Log.d(TAG, "setup for Hot start")
        Intents.init()
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("login_button").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        Log.d(TAG, "Send the app to the background by pressing the Home button")
        uiDevice.pressHome()
        // Wait for a brief moment to simulate background duration
        Thread.sleep(1000)

        Log.d(TAG, "Re-start the app from background")
        uiDevice.pressRecentApps()

        // Select the first app in the list (your app)
        val appWindow = device.findObject(
            UiSelector()
                .className("android.view.ViewGroup") // Adjust class name if needed
                .index(0) // First entry in Recent Apps
        )
        appWindow.click()

        val startTime = System.currentTimeMillis()
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("login_button").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        val duration = System.currentTimeMillis() - startTime
        Log.d(TAG, "Duration: $duration")
        assertThat("Hot start time exceeded", duration, lessThan(HOT_START_THRESHOLD))
        Log.d(TAG, "test3HotStartTime End")
    }

}