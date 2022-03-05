package com.kylentt.mediaplayer.ui

import com.kylentt.mediaplayer.core.util.Constants.HOME_SCREEN
import com.kylentt.mediaplayer.core.util.Constants.PERMISSION_SCREEN
import com.kylentt.mediaplayer.core.util.Constants.SPLASH_SCREEN

sealed class Screen(val route: String) {
    object HomeScreen : Screen(HOME_SCREEN)
    object PermissionScreen : Screen(PERMISSION_SCREEN)

    // TODO: More Screen?
}