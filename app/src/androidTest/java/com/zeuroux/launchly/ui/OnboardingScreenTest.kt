package com.zeuroux.launchly.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zeuroux.launchly.auth.AuthState
import com.zeuroux.launchly.ui.theme.LaunchlyTheme
import com.zeuroux.launchly.viewmodel.OnboardingUiState
import org.junit.Rule
import org.junit.Test
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
}
