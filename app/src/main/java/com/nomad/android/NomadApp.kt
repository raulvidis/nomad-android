package com.nomad.android

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nomad.android.ui.onboarding.OnboardingScreen
import com.nomad.android.ui.onboarding.OnboardingViewModel
import com.nomad.android.ui.theme.CrtScreen
import com.nomad.android.ui.theme.NomadTheme
import com.nomad.android.ui.theme.PipBoyStatusBar
import com.nomad.android.ui.theme.PipBoyBottomNav
import com.nomad.android.ui.navigation.NomadNavHost

@Composable
fun NomadApp() {
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val isOnboardingComplete by onboardingViewModel.isOnboardingComplete.collectAsStateWithLifecycle(initialValue = false)
    val onboardingState by onboardingViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

    NomadTheme {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { innerPadding ->
            CrtScreen(modifier = Modifier.padding(innerPadding)) {
                if (!isOnboardingComplete) {
                    OnboardingScreen(viewModel = onboardingViewModel)
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        PipBoyStatusBar(
                            isAiOnline = onboardingState.data.selectedModel.isNotEmpty(),
                            storagePercent = onboardingState.data.hardwareInfo?.let {
                                val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
                                val total = stat.blockCountLong * stat.blockSizeLong
                                val avail = stat.availableBlocksLong * stat.blockSizeLong
                                if (total > 0) ((total - avail).toFloat() / total.toFloat() * 100).toInt() else 0
                            } ?: 0
                        )
                        NomadNavHost(navController = navController, modifier = Modifier.weight(1f))
                        PipBoyBottomNav(navController = navController, currentRoute = currentRoute)
                    }
                }
            }
        }
    }
}
