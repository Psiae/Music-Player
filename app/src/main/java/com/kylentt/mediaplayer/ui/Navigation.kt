package com.kylentt.mediaplayer.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kylentt.mediaplayer.domain.presenter.ControllerViewModel
import com.kylentt.mediaplayer.ui.home.HomeScreen
import com.kylentt.mediaplayer.ui.landing.PermissionScreen
import com.kylentt.mediaplayer.ui.landing.SplashScreen

@Composable
fun Navigation(
    start: String = Screen.PermissionScreen.route,
    vm: ControllerViewModel,
    action: (NavController) -> Unit
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = start) {
        composable(Screen.PermissionScreen.route) {
            PermissionScreen()
        }
        composable(Screen.HomeScreen.route) {
            HomeScreen(vm)
        }
    }

    action(navController)
}
