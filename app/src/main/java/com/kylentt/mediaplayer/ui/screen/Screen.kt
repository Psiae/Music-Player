package com.kylentt.mediaplayer.ui.screen

import com.kylentt.mediaplayer.ui.util.UIConstants
import com.kylentt.mediaplayer.ui.util.UIConstants.HOME_SCREEN
import com.kylentt.mediaplayer.ui.util.UIConstants.MAIN_SCREEN
import com.kylentt.mediaplayer.ui.util.UIConstants.PERMISSION_SCREEN
import com.kylentt.mediaplayer.ui.util.UIConstants.SETTINGS_SCREEN

const val Root_route = UIConstants.ROOT_ROUTE
const val Main_route = UIConstants.MAIN_ROUTE
const val Extra_route = UIConstants.EXTRA_ROUTE

sealed class Screen(val route: String) {
    object MainScreen : Screen(MAIN_SCREEN)
    object HomeScreen : Screen(HOME_SCREEN)
    object PermissionScreen : Screen(PERMISSION_SCREEN)
    object SettingsScreen : Screen(SETTINGS_SCREEN)

    // TODO: More Screen?
}