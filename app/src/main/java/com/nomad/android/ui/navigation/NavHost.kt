package com.nomad.android.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun NomadNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(Routes.DASHBOARD) {
                PipBoyPlaceholder("DASHBOARD")
            }
            composable(Routes.MAPS) {
                PipBoyPlaceholder("MAPS")
            }
            composable(Routes.KNOWLEDGE) {
                PipBoyPlaceholder("KNOWLEDGE")
            }
            composable(Routes.CHAT) {
                PipBoyPlaceholder("CHAT")
            }
            composable(Routes.EMERGENCY) {
                PipBoyPlaceholder("EMERGENCY")
            }
        }
    }
}

@Composable
private fun PipBoyPlaceholder(screenName: String) {
    Text(
        text = screenName,
        color = Color(0xFF14FE17),
        fontFamily = FontFamily.Monospace,
        fontSize = 24.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxSize(),
    )
}
