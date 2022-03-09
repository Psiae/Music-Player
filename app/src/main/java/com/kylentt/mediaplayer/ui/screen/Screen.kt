package com.kylentt.mediaplayer.ui.screen

import com.kylentt.mediaplayer.ui.util.UIConstants.HOME_SCREEN_NAME
import com.kylentt.mediaplayer.ui.util.UIConstants.HOME_SCREEN_ROUTE
import com.kylentt.mediaplayer.ui.util.UIConstants.LIBRARY_SCREEN_NAME
import com.kylentt.mediaplayer.ui.util.UIConstants.LIBRARY_SCREEN_ROUTE
import com.kylentt.mediaplayer.ui.util.UIConstants.PERMISSION_SCREEN_NAME
import com.kylentt.mediaplayer.ui.util.UIConstants.PERMISSION_SCREEN_ROUTE
import com.kylentt.mediaplayer.ui.util.UIConstants.SEARCH_SCREEN_NAME
import com.kylentt.mediaplayer.ui.util.UIConstants.SEARCH_SCREEN_ROUTE
import com.kylentt.mediaplayer.ui.util.UIConstants.SETTINGS_SCREEN_NAME
import com.kylentt.mediaplayer.ui.util.UIConstants.SETTINGS_SCREEN_ROUTE

sealed class Screen(val name: String = "", val route: String = "") {

    object HomeScreen : Screen(name = HOME_SCREEN_NAME, route = HOME_SCREEN_ROUTE)
    object SearchScreen : Screen(name = SEARCH_SCREEN_NAME, route = SEARCH_SCREEN_ROUTE)
    object LibraryScreen: Screen(name = LIBRARY_SCREEN_NAME, route = LIBRARY_SCREEN_ROUTE)
    object PermissionScreen : Screen(name = PERMISSION_SCREEN_NAME, route = PERMISSION_SCREEN_ROUTE)
    object SettingsScreen : Screen(name = SETTINGS_SCREEN_NAME, route = SETTINGS_SCREEN_ROUTE)

    // TODO: More Screen?
}