package com.zeuroux.launchly.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeuroux.launchly.LaunchlyApp
import com.zeuroux.launchly.ui.AppShell
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
        enableEdgeToEdge()
        setContent {
            LaunchlyTheme {
                Surface(Modifier.fillMaxSize()) {
                    LaunchlyRoot()
                }
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
            onboardingState.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
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
