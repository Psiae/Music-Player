package com.flammky.musicplayer.base.nav.compose

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder

abstract class ComposeRootNavigator(val id: String) {
	abstract fun getRootDestination(): ComposeRootDestination

	abstract fun navigateToRoot(
		controller: NavController,
		navOptionsBuilder: NavOptionsBuilder.() -> Unit
	)

	abstract fun addRootDestination(
		navGraphBuilder: NavGraphBuilder,
		controller: NavController,
		onAppliedScope: @Composable () -> Unit = {}
	)
}
