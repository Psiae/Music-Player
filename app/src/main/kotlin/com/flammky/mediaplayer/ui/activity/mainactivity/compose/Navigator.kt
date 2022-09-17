@file:OptIn(ExperimentalAnimationApi::class, ExperimentalAnimationApi::class)

package com.flammky.mediaplayer.ui.activity.mainactivity.compose

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.flammky.mediaplayer.ui.activity.mainactivity.compose.home.HomeScreen
import com.flammky.musicplayer.ui.main.compose.screens.library.old.Library

sealed class Screen(
    val route: String,
    val label: String
) {
    object Home : Screen(Home_Screen_route, Home_Screen_label)
    object Search : Screen(Search_Screen_route, Search_Screen_label)
    object Library : Screen(Library_Screen_route, Library_Screen_label)
	object User : Screen(User_Screen_route, User_Screen_label)

    private companion object {
        const val Home_Screen_route = "Home"
        const val Search_Screen_route = "Search"
        const val Library_Screen_route = "Library"
		const val User_Screen_route = "User"

        const val Home_Screen_label = "Home"
        const val Search_Screen_label = "Search"
        const val Library_Screen_label = "Library"
		const val User_Screen_label = "User"
    }
}

@Composable
fun AnimatedMainAppNavigator(
    controller: NavHostController,
    modifier: Modifier = Modifier
) {

    NavHost(
        modifier = modifier,
        navController = controller,
        startDestination = "HomeRoute",
    ) {
        addHomeRoute(controller)
        addSearchRoute(controller)
        addLibraryRoute(controller)
				addUserRoute(controller)
    }
}

const val homeRoute = "HomeRoute"
private fun NavGraphBuilder.addHomeRoute(
    controller: NavHostController
) {
    navigation(
        route = homeRoute,
        startDestination = Screen.Home.route,
    ) {
        addHomeScreen(controller)
    }
}

private fun NavGraphBuilder.addHomeScreen(
    controller: NavHostController
) {
	composable(
        route = Screen.Home.route
    ) {
        HomeScreen(
            openSearchScreen = { /*TODO*/ },
            openLibraryScreen = { /*TODO*/ },
            openSettingScreen = { /*TODO*/ }
        ) {

        }
    }
}

const val searchRoute = "SearchRoute"
private fun NavGraphBuilder.addSearchRoute(
    controller: NavHostController
) {
    navigation(
        route = searchRoute,
        startDestination = Screen.Search.route
    ) {
        addSearchScreen(controller)
    }
}

private fun NavGraphBuilder.addSearchScreen(
    controller: NavHostController
) {
    composable(route = Screen.Search.route) {

    }
}

const val libraryRoute = "LibraryRoute"
private fun NavGraphBuilder.addLibraryRoute(
    controller: NavHostController
) {
    navigation(
        route = libraryRoute,
        startDestination = Screen.Library.route
    ) {
        addLibraryScreen(controller)
    }
}

private fun NavGraphBuilder.addLibraryScreen(
    controller: NavHostController
) {
    composable(
		route = Screen.Library.route
	) {
		Library()
		}
}

const val userRoute = "UserRoute"
private fun NavGraphBuilder.addUserRoute(
	controller: NavHostController
) {
	navigation(
		route = userRoute,
		startDestination = Screen.User.route
	) {
		composable(route = Screen.User.route) {

		}
	}
}
