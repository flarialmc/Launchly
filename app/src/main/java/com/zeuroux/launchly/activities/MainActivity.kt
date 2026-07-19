package com.zeuroux.launchly.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeuroux.launchly.LaunchlyApp
import com.zeuroux.launchly.ui.AppShell
import com.zeuroux.launchly.ui.LaunchlyLoadingScreen
import com.zeuroux.launchly.ui.OnboardingScreen
import com.zeuroux.launchly.ui.theme.LaunchlyTheme
import com.zeuroux.launchly.viewmodel.AddVersionViewModel
import com.zeuroux.launchly.viewmodel.DownloadsViewModel
import com.zeuroux.launchly.viewmodel.LaunchlyViewModelFactory
import com.zeuroux.launchly.viewmodel.LibraryViewModel
import com.zeuroux.launchly.viewmodel.OnboardingViewModel
import com.zeuroux.launchly.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.BLACK)
        )
        setContent {
            LaunchlyTheme {
                LaunchlyRoot()
            }
        }
    }

    @Composable
    private fun LaunchlyRoot() {
        val factory = LaunchlyViewModelFactory(application)
        val onboarding: OnboardingViewModel = viewModel(factory = factory)
        val library: LibraryViewModel = viewModel(factory = factory)
        val downloads: DownloadsViewModel = viewModel(factory = factory)
        val settings: SettingsViewModel = viewModel(factory = factory)
        val addVersion: AddVersionViewModel = viewModel(factory = factory)
        val onboardingState by onboarding.state.collectAsStateWithLifecycle()
        val loginLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { }
        val startLogin = {
            loginLauncher.launch(Intent(this, LoginActivity::class.java))
        }

        when {
            onboardingState.loading -> LaunchlyLoadingScreen()
            !onboardingState.complete -> OnboardingScreen(
                state = onboardingState,
                onSignIn = startLogin,
                onContinue = onboarding::finishOnboarding,
                onDismissReset = onboarding::dismissResetNotice
            )
            else -> AppShell(
                app = application as LaunchlyApp,
                libraryViewModel = library,
                downloadsViewModel = downloads,
                settingsViewModel = settings,
                addVersionViewModel = addVersion,
                onSignIn = startLogin
            )
        }
    }
}
