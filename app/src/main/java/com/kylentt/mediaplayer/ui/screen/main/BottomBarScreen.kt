package com.kylentt.mediaplayer.ui.screen.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.kylentt.mediaplayer.ui.screen.Screen

sealed class BottomBarScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {

    object Home : BottomBarScreen(
        route = Screen.HomeScreen.route,
        title = Screen.HomeScreen.name,
        icon = Icons.Default.Home
    )

    object Search : BottomBarScreen(
        route = Screen.SearchScreen.route,
        title = Screen.SearchScreen.name,
        icon = Icons.Default.Search
    )

    // TODO : Other Screen
}
