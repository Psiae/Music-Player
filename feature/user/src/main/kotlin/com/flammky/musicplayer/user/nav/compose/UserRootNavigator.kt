package com.flammky.musicplayer.user.nav.compose

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.flammky.musicplayer.base.nav.compose.ComposeRootDestination
import com.flammky.musicplayer.base.nav.compose.ComposeRootNavigation
import com.flammky.musicplayer.base.nav.compose.ComposeRootNavigator
import com.flammky.musicplayer.user.R
import com.flammky.musicplayer.user.ui.compose.User

object UserRootNavigator : ComposeRootNavigator("library") {

	private val rootDestination = ComposeRootDestination(
		routeID = "root_user",
		label = "User",
		iconResource = ComposeRootDestination.IconResource
			.ResID(R.drawable.user_circle_outlined_base_512_24),
		selectedIconResource = ComposeRootDestination.IconResource
			.ResID(R.drawable.user_circle_filled_base_512_24)
	)

	override fun getRootDestination(): ComposeRootDestination {
		return rootDestination
	}

	override fun navigateToRoot(
		controller: NavController,
		navOptionsBuilder: NavOptionsBuilder.() -> Unit
	) {
		controller.navigate(rootDestination.routeID, navOptionsBuilder)
	}

	override fun addRootDestination(
		navGraphBuilder: NavGraphBuilder,
		controller: NavController,
		onAppliedScope: @Composable () -> Unit
	) {
		navGraphBuilder.composable(rootDestination.routeID) {
			val nc = rememberNavController()
			NavHost(navController = nc, "start") {
				composable("start") {
					User {
						// yes, ignore this
						controller.navigate(it) {
							val isCurrentStart =
								controller.graph.findStartDestination().route == rootDestination.routeID
							restoreState = true
							launchSingleTop = true
							popUpTo(rootDestination.routeID) {
								inclusive = !isCurrentStart
								saveState = true
							}
						}
					}
				}
				with(ComposeRootNavigation.getNavigatorById("library")!!) {
					addRootDestination(this@NavHost, nc) {
						onAppliedScope()
					}
				}
			}
			onAppliedScope()
		}
	}
}
