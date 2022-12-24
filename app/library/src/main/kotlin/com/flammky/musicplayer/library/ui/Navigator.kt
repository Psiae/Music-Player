package com.flammky.musicplayer.library.ui

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.contains

object LibraryNavigator {
	private const val rootRoute = "Library"

	fun NavGraphBuilder.addRoot() {
		composable("Library") {
			Library()
		}
	}

	/**
	 * Navigate to the root of the graph
	 */
	fun NavController.navigateToRoot(
		saveState: Boolean = false
	) {
		check(graph.contains(rootRoute)) {
			"${this@LibraryNavigator::class.simpleName} Could not find destination: $rootRoute within " +
				"$this navigation graph:\n" +
				"$graph\n" +
				"did you forgot to call LibraryNavigator.addRoot() ?"
		}
		navigate(rootRoute) {
			popUpTo(rootRoute) {
				this.saveState = saveState
			}
			launchSingleTop = true
			restoreState = true
		}
	}
}


internal object Navigator {

}
