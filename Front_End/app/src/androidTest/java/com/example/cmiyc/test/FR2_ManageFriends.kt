package com.example.cmiyc.test

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
import kotlinx.coroutines.delay
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

    @Before
    fun setup() {
        Intents.init()
        // Ensure test account is added to device settings
        uiDevice.executeShellCommand("pm grant ${composeTestRule.activity.packageName} android.permission.ACCESS_FINE_LOCATION")

        uiAutomation.executeShellCommand("svc wifi enable")
        uiAutomation.executeShellCommand("svc data enable")
    }


    @Test
    fun test1AddFriend() {
        login(testAccountEmail1)
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("friends_button").assertExists().performClick()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Verify log screen elements
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("addFriends_button").assertExists().performClick()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("friendEmail_Input").assertExists().performTextInput(testAccountEmail2)
                true
            } catch (e: AssertionError) {
                false
            }
        }

        composeTestRule.onNodeWithTag("submitFriendEmail_button").performClick()

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("friendEmail_Input").assertExists()
                false
            } catch (e: AssertionError) {
                true
            }
        }
        composeTestRule.onNodeWithTag("friendEmail_Input").assertDoesNotExist()
    }

    @Test
    fun test2AddFriendFailure() {
        login(testAccountEmail2)
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("friends_button").assertExists().performClick()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Verify log screen elements
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("addFriends_button").assertExists().performClick()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // test adding non-exisitng email
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("friendEmail_Input").assertExists().performTextInput("invalidEmail@invalid.com")
                true
            } catch (e: AssertionError) {
                false
            }
        }

        composeTestRule.onNodeWithTag("submitFriendEmail_button").performClick()

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("Error").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        composeTestRule.onNodeWithText("OK").performClick()

        // testing network error
        // Execute the shell command to disable WiFi
        uiAutomation.executeShellCommand("svc wifi disable")
        uiAutomation.executeShellCommand("svc data disable")

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("friendEmail_Input").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        composeTestRule.onNodeWithTag("friendEmail_Input").performTextInput(testAccountEmail2) // replay with test email 2

        composeTestRule.onNodeWithTag("submitFriendEmail_button").performClick()

        // Verify log screen elements
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("Error").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        composeTestRule.onNodeWithText("Error").assertExists()
    }

    @Test
    fun test3RespondFriendRequestFailure() {
        login(testAccountEmail2)
        composeTestRule.onNodeWithTag("friends_button").performClick()

        // Verify log screen elements
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("friendRequests_button").assertExists().performClick()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onAllNodesWithTag("acceptFriend_button", useUnmergedTree = true).onFirst().assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Execute the shell command to disable WiFi
        uiAutomation.executeShellCommand("svc wifi disable")
        uiAutomation.executeShellCommand("svc data disable")

        Thread.sleep(3000)

        composeTestRule.onAllNodesWithTag("acceptFriend_button").onFirst().performClick()

        // Verify log screen elements
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("Error").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        composeTestRule.onNodeWithText("Error").assertExists()
    }

    @Test
    fun test4RespondFriendRequest() {
        login(testAccountEmail2)
        composeTestRule.onNodeWithTag("friends_button").performClick()

        // Verify log screen elements
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("friendRequests_button").assertExists().performClick()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onAllNodesWithTag("acceptFriend_button", useUnmergedTree = true).onFirst().assertExists().performClick()
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }


    @Test
    fun test5RemoveFriendFailure() {
        login(testAccountEmail2)
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("friends_button").assertExists().performClick()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onAllNodesWithTag("removeFriend_button", useUnmergedTree = true).onFirst().assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        uiAutomation.executeShellCommand("svc wifi disable")
        uiAutomation.executeShellCommand("svc data disable")

        Thread.sleep(5000)

        composeTestRule.onAllNodesWithTag("removeFriend_button", useUnmergedTree = true).onFirst().performClick()

        // Verify log screen elements
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("Error").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        composeTestRule.onNodeWithText("Error").assertExists()
    }

    @Test
    fun test6RemoveFriendRequest() {
        login(testAccountEmail2)
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("friends_button").assertExists().performClick()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onAllNodesWithTag("removeFriend_button", useUnmergedTree = true).onFirst().assertExists().performClick()
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }

    private fun login(email: String) {
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
        selectGoogleAccount(email)

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

        Intents.release()
    }
}