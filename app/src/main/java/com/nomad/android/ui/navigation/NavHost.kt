package com.nomad.android.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
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
import com.nomad.android.ui.chat.ChatScreen
import com.nomad.android.ui.dashboard.DashboardScreen
import com.nomad.android.ui.emergency.EmergencyScreen
import com.nomad.android.ui.knowledge.KnowledgeScreen

@Composable
fun NomadNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
        modifier = modifier.fillMaxSize(),
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(navController = navController)
        }
        composable(Routes.MAPS) {
            PipBoyPlaceholder("MAPS")
        }
        composable(Routes.KNOWLEDGE) {
            KnowledgeScreen()
        }
        composable(Routes.CHAT) {
            ChatScreen()
        }
        composable(Routes.EMERGENCY) {
            EmergencyScreen()
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
