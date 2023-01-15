package com.flammky.musicplayer.library.presentation.nav

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.flammky.musicplayer.base.nav.compose.ComposeRootDestination
import com.flammky.musicplayer.base.nav.compose.ComposeRootNavigator
import com.flammky.musicplayer.library.R

object LibraryRootNavigator : ComposeRootNavigator() {

	private val rootDestination = ComposeRootDestination(
		routeID = "root_library",
		label = "Library",
		iconResource = ComposeRootDestination.IconResource
			.ResID(R.drawable.library_outlined_base_128_24),
		selectedIconResource = ComposeRootDestination.IconResource
			.ResID(R.drawable.library_filled_base_128_24)
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

	override fun addRootDestination(navGraphBuilder: NavGraphBuilder, controller: NavController) {
		navGraphBuilder.composable(rootDestination.routeID) {
			com.flammky.musicplayer.library.presentation.Library()
		}
	}
}
