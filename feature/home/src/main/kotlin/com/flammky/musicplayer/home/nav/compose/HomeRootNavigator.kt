package com.flammky.musicplayer.home.nav.compose

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.flammky.musicplayer.base.nav.compose.ComposeRootDestination
import com.flammky.musicplayer.base.nav.compose.ComposeRootNavigator
import com.flammky.musicplayer.home.Home
import com.flammky.musicplayer.home.R

object HomeRootNavigator : ComposeRootNavigator("home") {

	private val rootDestination = ComposeRootDestination(
		routeID = "root_home",
		label = "Home",
		iconResource = ComposeRootDestination.IconResource
			.ResID(R.drawable.home_outlined_base_512_24),
		selectedIconResource = ComposeRootDestination.IconResource
			.ResID(R.drawable.home_filled_base_512_24)
	)

	override fun getRootDestination(): ComposeRootDestination = rootDestination

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
		navGraphBuilder.composable(
			rootDestination.routeID
		) {
			Home {
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
			onAppliedScope()
		}
	}
}
