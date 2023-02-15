package com.flammky.musicplayer.search.nav.compose

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.flammky.musicplayer.base.nav.compose.ComposeRootDestination
import com.flammky.musicplayer.base.nav.compose.ComposeRootNavigator
import com.flammky.musicplayer.search.R
import com.flammky.musicplayer.search.Search

object SearchRootNavigator : ComposeRootNavigator("search") {

	private val rootDestination = ComposeRootDestination(
		routeID = "root_search",
		label = "Search",
		iconResource = ComposeRootDestination.IconResource
			.ResID(R.drawable.search_outlined_base_128_24),
		selectedIconResource = ComposeRootDestination.IconResource
			.ResID(R.drawable.search_filled_base_128_24),
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
		navGraphBuilder.composable(rootDestination.routeID) {
			Search {
				controller.navigate(it) {
					val isCurrentStart = controller.graph.findStartDestination().route == rootDestination.routeID
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
