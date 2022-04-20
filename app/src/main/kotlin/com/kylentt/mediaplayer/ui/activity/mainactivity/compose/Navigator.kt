@file:OptIn(ExperimentalAnimationApi::class)

package com.kylentt.mediaplayer.ui.activity.mainactivity.compose

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.kylentt.mediaplayer.ui.activity.mainactivity.compose.home.HomeScreen

sealed class Screen(val route: String) {
    object Home : Screen(Home_Screen_route)
    object Search : Screen(Search_Screen_route)
    object Library : Screen(Library_Screen_route)
    object Extra : Screen(Extra_Screen_route)

    companion object {
        const val Home_Screen_route = "Home"
        const val Search_Screen_route = "Search"
        const val Library_Screen_route = "Library"
        const val Extra_Screen_route = "Extra"
    }
}

object MainBottomNavigation {
    val screenList = listOf(
        Screen.Home,
        Screen.Search,
        Screen.Library,
        Screen.Extra
    )
    val routeList = screenList.map { it.route }
    var startDestination = routeList.first()
}

@Composable
fun AnimatedMainAppNavigator(
    controller: NavHostController,
    modifier: Modifier = Modifier
) {
    AnimatedNavHost(
        navController = controller,
        startDestination = MainBottomNavigation.startDestination
    ) {
        addHomeRoute(controller)
        addSearchRoute(controller)
        addLibraryRoute(controller)
        addExtraRoute(controller)
    }
}

private fun NavGraphBuilder.addHomeRoute(
    controller: NavHostController
) {
    navigation(
        route = Screen.Home.route,
        startDestination = Screen.Home.route,
    ) {
        addHome(controller)
    }
}

private fun NavGraphBuilder.addHome(
    controller: NavHostController
) {
    composable(
        route = Screen.Home.route
    ) {
        HomeScreen(
            openSearchScreen = { /*TODO*/ },
            openLibraryScreen = { /*TODO*/ },
            openExtraScreen = { /*TODO*/ },
            openSettingScreen = { /*TODO*/ }) {
        }
    }
}

private fun NavGraphBuilder.addSearchRoute(
    controller: NavHostController
) {

}

private fun NavGraphBuilder.addLibraryRoute(
    controller: NavHostController
) {

}

private fun NavGraphBuilder.addExtraRoute(
    controller: NavHostController
) {

}

