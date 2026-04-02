package com.nomad.android

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nomad.android.ui.navigation.NomadNavHost
import com.nomad.android.ui.theme.CrtScreen
import com.nomad.android.ui.theme.NomadTheme
import com.nomad.android.ui.theme.PipBoyBottomNav
import com.nomad.android.ui.theme.PipBoyStatusBar
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NomadApp : Application()

@Composable
fun NomadApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

    NomadTheme {
        CrtScreen {
            Column(modifier = Modifier.fillMaxSize()) {
                PipBoyStatusBar()
                NomadNavHost(
                    navController = navController,
                    modifier = Modifier.weight(1f),
                )
                PipBoyBottomNav(
                    navController = navController,
                    currentRoute = currentRoute,
                )
            }
        }
    }
}
