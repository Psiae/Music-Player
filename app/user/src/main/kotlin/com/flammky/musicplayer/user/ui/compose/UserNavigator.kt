package com.flammky.musicplayer.user.ui.compose

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.contains

object UserNavigator {
    private const val rootRoute = "com.flammky.musicplayer.user.ui"

    fun NavGraphBuilder.addDestination() {
        composable(rootRoute) {

        }
    }

    /**
     * Navigate to the root of the graph
     */
    fun NavController.navigateToRoot(
        saveState: Boolean = false
    ) {
        check(graph.contains(rootRoute)) {
            "${this@UserNavigator::class.simpleName} Could not find destination: $rootRoute within " +
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
