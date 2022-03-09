package com.kylentt.mediaplayer.ui.screen

import com.kylentt.mediaplayer.ui.screen.Root.BottomBarScreen
import com.kylentt.mediaplayer.ui.util.UIConstants

sealed class BottomNavigationRoute(val screen: BottomBarScreen) {
    object HomeScreen : BottomNavigationRoute(BottomBarScreen.Home)
    object SearchScreen : BottomNavigationRoute(BottomBarScreen.Search)

    companion object {
        const val routeName = UIConstants.BOTTOM_BAR_ROUTE
        val routeList = listOf(
            BottomBarScreen.Home, BottomBarScreen.Search
        )
    }
}

