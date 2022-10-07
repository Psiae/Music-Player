package com.flammky.musicplayer.library.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.contains
import androidx.navigation.get

object LibraryNavigator {

	fun NavGraphBuilder.addRoot() {
		composable("library") {
			Library()
		}
	}

	fun NavHostController.addRoot() {
		if (!graph.contains("library")) {
			ComposeNavigator.Destination(navigatorProvider[ComposeNavigator::class]) {
				Library()
			}.apply {
				route = "library"
				graph.addDestination(this)
			}
		}
	}
}
