package com.example.cmiyc.test

import android.util.Log
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
class FR2_ManageFriends {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val testAccountEmail1 = "guanhua.qiao2020@gmail.com" // Replace with actual test account
    private val testAccountEmail2 = "omiduckai@gmail.com" // Replace with actual test account

    private val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
    private val TAG = "FR2_ManageFriends"

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
    }

    @Test
    fun test1AddFriend() {
        Log.d(TAG, "Starting test1AddFriend")
        login(testAccountEmail1)
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("friends_button").assertExists().performClick()
                Log.d(TAG, "Clicked on friends button")
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Verify log screen elements
        Log.d(TAG, "Friends screen opened")
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("addFriends_button").assertExists().performClick()
                Log.d(TAG, "Clicked on add friends button")
                true
            } catch (e: AssertionError) {
                false
            }
        }

        Log.d(TAG, "Add Friend button clicked")
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("friendEmail_Input").assertExists().performTextInput(testAccountEmail2)
                Log.d(TAG, "Entered friend email: $testAccountEmail2")
                true
            } catch (e: AssertionError) {
                false
            }
        }

        Log.d(TAG, "Friend email input entered: $testAccountEmail2")
        composeTestRule.onNodeWithTag("submitFriendEmail_button").performClick()
        Log.d(TAG, "Clicked submit friend email button")

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("friendEmail_Input").assertExists()
                false
            } catch (e: AssertionError) {
                true
            }
        }
        composeTestRule.onNodeWithTag("friendEmail_Input").assertDoesNotExist()
        Log.d(TAG, "Friend email submitted and input field removed")
    }

    @Test
    fun test2AddFriendFailure() {
        Log.d(TAG, "Starting test2AddFriendFailure")
        login(testAccountEmail2)
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("friends_button").assertExists().performClick()
                Log.d(TAG, "Clicked on friends button"); true
            } catch (e: AssertionError) { false }
        }

        // Verify log screen elements
        Log.d(TAG, "Friends screen opened")
        composeTestRule.waitUntil(5000) {
            try { composeTestRule.onNodeWithTag("addFriends_button").assertExists().performClick()
                Log.d(TAG, "Clicked on add friends button"); true
            } catch (e: AssertionError) { false }
        }

        // test adding non-exisitng email
        Log.d(TAG, "Attempting to add invalid email")
        composeTestRule.waitUntil(5000) {
            try { composeTestRule.onNodeWithTag("friendEmail_Input").assertExists().performTextInput("invalidEmail@invalid.com")
                Log.d(TAG, "Entered invalid email: invalidEmail@invalid.com"); true
            } catch (e: AssertionError) { false }
        }

        composeTestRule.onNodeWithTag("submitFriendEmail_button").performClick()
        Log.d(TAG, "Clicked submit friend email button")

        composeTestRule.waitUntil(5000) {
            try { composeTestRule.onNodeWithText("Error").assertExists()
                Log.d(TAG, "Error dialog displayed for invalid email"); true
            } catch (e: AssertionError) { false }
        }
        composeTestRule.onNodeWithText("OK").performClick()
        Log.d(TAG, "Clicked OK on error dialog")

        // testing network error
        // Execute the shell command to disable WiFi
        uiAutomation.executeShellCommand("svc wifi disable")
        uiAutomation.executeShellCommand("svc data disable")
        Log.d(TAG, "WiFi and data disabled for network error simulation")

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("friendEmail_Input").assertExists(); true
            } catch (e: AssertionError) { false }
        }
        composeTestRule.onNodeWithTag("friendEmail_Input").performTextInput(testAccountEmail2) // replay with test email 2
        Log.d(TAG, "Entered friend email: $testAccountEmail2")

        composeTestRule.onNodeWithTag("submitFriendEmail_button").performClick()
        Log.d(TAG, "Clicked submit friend email button")

        // Verify log screen elements
        composeTestRule.waitUntil(5000) {
            try { composeTestRule.onNodeWithText("Error").assertExists()
                Log.d(TAG, "Error dialog displayed for network error"); true
            } catch (e: AssertionError) { false }
        }
        composeTestRule.onNodeWithText("Error").assertExists()
        Log.d(TAG, "Network error handled, error dialog shown")
    }

    @Test
    fun test3RespondFriendRequestFailure() {
        Log.d(TAG, "Starting test3RespondFriendRequestFailure")
        login(testAccountEmail2)
        composeTestRule.onNodeWithTag("friends_button").performClick()
        Log.d(TAG, "Clicked on friends button")

        // Verify log screen elements
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("friendRequests_button").assertExists().performClick()
                Log.d(TAG, "Clicked on friend requests button")
                true
            } catch (e: AssertionError) { false }
        }

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onAllNodesWithTag("acceptFriend_button", useUnmergedTree = true).onFirst().assertExists()
                Log.d(TAG, "Found accept friend button")
                true
            } catch (e: AssertionError) { false }
        }

        // Execute the shell command to disable WiFi
        uiAutomation.executeShellCommand("svc wifi disable")
        uiAutomation.executeShellCommand("svc data disable")
        Log.d(TAG, "WiFi and data disabled for request failure simulation")
        Thread.sleep(3000)

        composeTestRule.onAllNodesWithTag("acceptFriend_button").onFirst().performClick()
        Log.d(TAG, "Clicked accept friend button")

        // Verify log screen elements
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("Error").assertExists()
                Log.d(TAG, "Error dialog displayed for friend request failure")
                true
            } catch (e: AssertionError) { false }
        }
        composeTestRule.onNodeWithText("Error").assertExists()
        Log.d(TAG, "Error dialog shown for friend request failure")
    }

    @Test
    fun test4RespondFriendRequest() {
        Log.d(TAG, "Starting test4RespondFriendRequest")
        login(testAccountEmail2)
        composeTestRule.onNodeWithTag("friends_button").performClick()
        Log.d(TAG, "Clicked on friends button")

        // Verify log screen elements
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("friendRequests_button").assertExists().performClick()
                Log.d(TAG, "Clicked on friend requests button")
                true
            } catch (e: AssertionError) {
                false
            }
        }

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onAllNodesWithTag("acceptFriend_button", useUnmergedTree = true).onFirst().assertExists().performClick()
                Log.d(TAG, "Clicked accept friend button")
                true
            } catch (e: AssertionError) {
                false
            }
        }
        Log.d(TAG, "Friend request accepted successfully")
    }

    @Test
    fun test5RemoveFriendFailure() {
        Log.d(TAG, "Starting test5RemoveFriendFailure")
        login(testAccountEmail2)
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("friends_button").assertExists().performClick()
                Log.d(TAG, "Clicked on friends button")
                true
            } catch (e: AssertionError) {
                false
            }
        }

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onAllNodesWithTag("removeFriend_button", useUnmergedTree = true).onFirst().assertExists()
                Log.d(TAG, "Found remove friend button")
                true
            } catch (e: AssertionError) {
                false
            }
        }

        uiAutomation.executeShellCommand("svc wifi disable")
        uiAutomation.executeShellCommand("svc data disable")
        Log.d(TAG, "WiFi and data disabled for remove friend failure simulation")

        Thread.sleep(5000)

        composeTestRule.onAllNodesWithTag("removeFriend_button", useUnmergedTree = true).onFirst().performClick()
        Log.d(TAG, "Clicked remove friend button")

        // Verify log screen elements
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("Error").assertExists()
                Log.d(TAG, "Error dialog displayed for remove friend failure")
                true
            } catch (e: AssertionError) {
                false
            }
        }
        composeTestRule.onNodeWithText("Error").assertExists()
        Log.d(TAG, "Error dialog shown for remove friend failure")
    }

    @Test
    fun test6RemoveFriendRequest() {
        Log.d(TAG, "Starting test6RemoveFriendRequest")
        login(testAccountEmail2)
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("friends_button").assertExists().performClick()
                Log.d(TAG, "Clicked on friends button")
                true
            } catch (e: AssertionError) {
                false
            }
        }

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onAllNodesWithTag("removeFriend_button", useUnmergedTree = true).onFirst().assertExists().performClick()
                Log.d(TAG, "Clicked remove friend button")
                true
            } catch (e: AssertionError) {
                false
            }
        }
        Log.d(TAG, "Friend removed successfully")
    }

    private fun login(email: String) {
        Log.d(TAG, "Starting login process for email: $email")
        // Handle location permission dialog
        handleLocationPermission()

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("login_button").assertExists()
                Log.d(TAG, "Login button found")
                true
            } catch (e: AssertionError) {
                false
            }
        }
        // Click Google login button
        composeTestRule.onNodeWithTag("login_button").performClick()
        Log.d(TAG, "Clicked login button")

        // Handle Google account selection
        selectGoogleAccount(email)

        // Verify main screen elements
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("broadcast_button").assertExists()
                Log.d(TAG, "Broadcast button found, login successful")
                true
            } catch (e: AssertionError) {
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