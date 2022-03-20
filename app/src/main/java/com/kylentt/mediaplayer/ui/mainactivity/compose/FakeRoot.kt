package com.kylentt.mediaplayer.ui.mainactivity.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.kylentt.mediaplayer.ui.mainactivity.compose.components.RootBottomNav
import com.kylentt.mediaplayer.ui.mainactivity.compose.root.BottomNavigationRoute
import com.kylentt.mediaplayer.ui.mainactivity.compose.screen.home.FakeHomeScreen

@Composable
fun FakeRoot() {
    RootScaffold(
        bottomBar = {
            RootBottomNav(ripple = true, selectedRoute = BottomNavigationRoute.routeList[0].route, navigateTo = {})
        }, containerColor = MaterialTheme.colorScheme.surface
    ) {
        FakeHomeScreen()
    }
}