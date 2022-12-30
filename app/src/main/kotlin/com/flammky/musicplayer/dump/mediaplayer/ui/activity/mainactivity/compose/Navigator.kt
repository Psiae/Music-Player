@file:OptIn(ExperimentalAnimationApi::class, ExperimentalAnimationApi::class)

package com.flammky.musicplayer.dump.mediaplayer.ui.activity.mainactivity.compose

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.flammky.musicplayer.dump.mediaplayer.ui.activity.mainactivity.compose.home.HomeScreen
import com.flammky.musicplayer.home.nav.compose.HomeRootNavigator
import com.flammky.musicplayer.library.nav.compose.LibraryRootNavigator
import com.flammky.musicplayer.library.ui.LibraryNavigator
import com.flammky.musicplayer.search.nav.compose.SearchRootNavigator
import com.flammky.musicplayer.user.nav.compose.UserRootNavigator

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
		startDestination = HomeRootNavigator.getRootDestination().routeID,
	) {
		HomeRootNavigator.addRootDestination(this, controller)
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
		startDestination = SearchRootNavigator.getRootDestination().routeID
	) {
		SearchRootNavigator.addRootDestination(this, controller)
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
		startDestination = LibraryRootNavigator.getRootDestination().routeID
	) {
		LibraryRootNavigator.addRootDestination(this, controller)
	}
}

private fun NavGraphBuilder.addLibraryScreen(
	controller: NavHostController
) {
	with(LibraryNavigator) { addRoot() }
}

const val userRoute = "UserRoute"
private fun NavGraphBuilder.addUserRoute(
	controller: NavHostController
) {
	navigation(
		route = userRoute,
		startDestination = UserRootNavigator.getRootDestination().routeID
	) {
	  UserRootNavigator.addRootDestination(this, controller)
	}
}
