package com.nomad.android

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nomad.android.ui.onboarding.OnboardingScreen
import com.nomad.android.ui.onboarding.OnboardingViewModel
import com.nomad.android.ui.theme.CrtScreen
import com.nomad.android.ui.theme.NomadTheme

@Composable
fun NomadApp() {
    val onboardingViewModel: OnboardingViewModel = viewModel()
    val isOnboardingComplete by onboardingViewModel.isOnboardingComplete.collectAsState(initial = false)

    if (!isOnboardingComplete) {
        NomadTheme {
            CrtScreen {
                OnboardingScreen(
                    viewModel = onboardingViewModel
                )
            }
        }
    } else {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

        NomadTheme {
            CrtScreen {
                Column(modifier = Modifier.fillMaxSize()) {
                    com.nomad.android.ui.theme.PipBoyStatusBar()
                    com.nomad.android.ui.navigation.NomadNavHost(navController = navController, modifier = Modifier.weight(1f))
                    com.nomad.android.ui.theme.PipBoyBottomNav(navController = navController, currentRoute = currentRoute)
                }
            }
        }
    }
}
