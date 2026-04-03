package com.nomad.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nomad.android.ui.chat.ChatScreen
import com.nomad.android.ui.dashboard.DashboardScreen
import com.nomad.android.ui.emergency.EmergencyScreen
import com.nomad.android.ui.knowledge.KnowledgeScreen
import com.nomad.android.ui.maps.MapsScreen
import com.nomad.android.ui.settings.SettingsScreen

@Composable
fun NomadNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
        modifier = modifier,
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(navController = navController)
        }
        composable(Routes.MAPS) {
            MapsScreen()
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
        composable(Routes.SETTINGS) {
            SettingsScreen()
        }
    }
}
