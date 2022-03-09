package com.kylentt.mediaplayer.ui.screen.Root

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.kylentt.mediaplayer.R
import com.kylentt.mediaplayer.ui.screen.Screen

sealed class BottomBarScreen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val outlinedIcon: ImageVector
) {

    object Home : BottomBarScreen(
        route = Screen.HomeScreen.route,
        title = Screen.HomeScreen.name,
        icon = Icons.Default.Home,
        outlinedIcon = Icons.Outlined.Home
    )

    object Search : BottomBarScreen(
        route = Screen.SearchScreen.route,
        title = Screen.SearchScreen.name,
        icon = Icons.Default.Search,
        outlinedIcon = Icons.Outlined.Search
    )

    object Library : BottomBarScreen(
        route = Screen.LibraryScreen.route,
        title = Screen.LibraryScreen.name,
        icon = Icons.Default.Home,
        outlinedIcon = Icons.Outlined.Home
    )

    companion object {

        @Composable
        fun getLibraryIcon(): ImageVector {
            return ImageVector.vectorResource(id = R.drawable.outline_subscriptions_24)
        }
    }

    // TODO : Other Screen
}
