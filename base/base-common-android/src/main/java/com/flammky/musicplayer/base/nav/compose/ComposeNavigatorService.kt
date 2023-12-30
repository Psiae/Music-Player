package com.flammky.musicplayer.base.nav.compose

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavController

interface ComposeNavigatorService {

	fun navigateToModuleRoot(
		controller: NavController,
		module: String
	): Boolean

}

val LocalComposeNavigatorService = compositionLocalOf<ComposeNavigatorService> {
	error("CompositionLocal ComposeNavigatorService was not provided")
}
