package com.kylentt.mediaplayer.ui.screen

import com.kylentt.mediaplayer.ui.util.UIConstants
import com.kylentt.mediaplayer.ui.util.UIConstants.HOME_SCREEN_NAME
import com.kylentt.mediaplayer.ui.util.UIConstants.HOME_SCREEN_ROUTE
import com.kylentt.mediaplayer.ui.util.UIConstants.MAIN_SCREEN_NAME
import com.kylentt.mediaplayer.ui.util.UIConstants.MAIN_SCREEN_ROUTE
import com.kylentt.mediaplayer.ui.util.UIConstants.PERMISSION_SCREEN
import com.kylentt.mediaplayer.ui.util.UIConstants.SEARCH_SCREEN_NAME
import com.kylentt.mediaplayer.ui.util.UIConstants.SEARCH_SCREEN_ROUTE
import com.kylentt.mediaplayer.ui.util.UIConstants.SETTINGS_SCREEN

const val Root_route = UIConstants.ROOT_ROUTE
const val Main_route = UIConstants.MAIN_ROUTE
const val Extra_route = UIConstants.EXTRA_ROUTE

sealed class Screen(val name: String, val route: String) {
    object MainScreen : Screen(name = MAIN_SCREEN_NAME, route = MAIN_SCREEN_ROUTE)
    object HomeScreen : Screen(name = HOME_SCREEN_NAME, route = HOME_SCREEN_ROUTE)
    object SearchScreen : Screen(name = SEARCH_SCREEN_NAME, route = SEARCH_SCREEN_ROUTE)
    object PermissionScreen : Screen(name = "Permission", route = PERMISSION_SCREEN)
    object SettingsScreen : Screen(name = "Settings", route = SETTINGS_SCREEN)

    // TODO: More Screen?
}