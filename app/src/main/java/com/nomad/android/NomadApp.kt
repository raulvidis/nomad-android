package com.nomad.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nomad.android.ui.onboarding.OnboardingScreen
import com.nomad.android.ui.onboarding.OnboardingViewModel
import com.nomad.android.ui.theme.CrtScreen
import com.nomad.android.ui.theme.NomadTheme
import com.nomad.android.ui.theme.TerminalStatusBar
import com.nomad.android.ui.theme.TerminalBottomNav
import com.nomad.android.ui.navigation.NomadNavHost

@Composable
fun NomadApp() {
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val isOnboardingComplete by onboardingViewModel.isOnboardingComplete.collectAsStateWithLifecycle(initialValue = false)
    val onboardingState by onboardingViewModel.uiState.collectAsStateWithLifecycle()
    val showOnboarding = !isOnboardingComplete && !onboardingState.data.isComplete
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

    val context = LocalContext.current
    var batteryPercent by remember { mutableIntStateOf(getBatteryLevel(context)) }

    androidx.compose.runtime.DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    batteryPercent = (level * 100 / scale)
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
    }

    NomadTheme {
        CrtScreen {
            if (showOnboarding) {
                OnboardingScreen(viewModel = onboardingViewModel)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                ) {
                    TerminalStatusBar(
                        isAiOnline = onboardingState.data.selectedModel.isNotEmpty(),
                        batteryPercent = batteryPercent,
                    )
                    NomadNavHost(navController = navController, modifier = Modifier.weight(1f))
                    TerminalBottomNav(navController = navController, currentRoute = currentRoute)
                }
            }
        }
    }
}

private fun getBatteryLevel(context: Context): Int {
    return try {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    } catch (_: Exception) {
        0
    }
}
