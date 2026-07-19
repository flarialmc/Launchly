package com.zeuroux.launchly.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.zeuroux.launchly.R
import com.zeuroux.launchly.auth.AuthState
import com.zeuroux.launchly.ui.theme.LaunchlyTheme
import com.zeuroux.launchly.viewmodel.OnboardingUiState
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun signedOutStateKeepsSignInVisible() {
        compose.setContent {
            LaunchlyTheme {
                OnboardingScreen(
                    OnboardingUiState(false, false, AuthState.SignedOut, false),
                    onSignIn = {},
                    onContinue = {},
                    onDismissReset = {}
                )
            }
        }
        compose.onNodeWithTag("sign_in").assertIsDisplayed()
    }

    @Test
    fun resetNoticeUsesAccessibleSharedPrompt() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        compose.setContent {
            LaunchlyTheme {
                OnboardingScreen(
                    OnboardingUiState(false, false, AuthState.SignedOut, true),
                    onSignIn = {},
                    onContinue = {},
                    onDismissReset = {}
                )
            }
        }

        compose.onNodeWithText(context.getString(R.string.reset_notice_title)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.close)).assertIsDisplayed()
    }

    @Test
    fun compactNavigationKeepsAllDestinationsReachable() {
        var selected = ""
        compose.setContent {
            LaunchlyTheme {
                CompactNavigation(Route.LIBRARY.path) { selected = it }
            }
        }

        compose.onNodeWithTag("nav_library").assertIsDisplayed()
        compose.onNodeWithTag("nav_downloads").assertIsDisplayed()
        compose.onNodeWithTag("nav_settings").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(Route.SETTINGS.path, selected) }
    }

    @Test
    fun addVersionActionRemainsIconAccessible() {
        var clicked = false
        compose.setContent {
            LaunchlyTheme {
                LaunchlyAddVersionButton(onClick = { clicked = true })
            }
        }

        compose.onNodeWithTag("add_version").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(true, clicked) }
    }
}
