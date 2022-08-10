package com.kylentt.musicplayer.ui.main.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

object MainNavigator {
	val controller
		@ReadOnlyComposable
		@Composable
		get() = LocalNavHostController.current

	@Composable
	fun ProvideNavHostController(
		controller: NavHostController = rememberNavController(),
		content: @Composable () -> Unit
	) = CompositionLocalProvider(LocalNavHostController provides controller, content = content)
}

val LocalNavHostController = staticCompositionLocalOf<NavHostController> {
	error("no Navigation Controller Provided")
}
