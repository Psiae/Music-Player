package com.flammky.musicplayer.library.ui

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

object LibraryNavigator {

	fun NavGraphBuilder.addRoot() {
		composable("Library") {
			Library()
		}
	}

	fun NavController.navigateToRoot() {
		navigate("Library")
	}
}
