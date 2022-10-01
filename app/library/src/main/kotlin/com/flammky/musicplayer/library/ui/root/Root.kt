package com.flammky.musicplayer.library.ui.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.flammky.musicplayer.library.localsong.LocalSongNavigator

@Composable
internal fun LibraryRoot() {
	ApplyBackground()
	LibraryRootNavigation()
}

@Composable
private fun LibraryRootNavigation() {
	val navController = rememberNavController()
	NavHost(
		navController = navController,
		startDestination = "content"
	) {
		composable("content") {
			LibraryRootContent(navController)
		}
		with(LocalSongNavigator) { addLocalSongDestinations(navController) }
	}
}

@Composable
private fun LibraryRootContent(navController: NavController) {

}

@Composable
private fun ApplyBackground() {
	Box(modifier = Modifier
		.fillMaxSize()
		.background(MaterialTheme.colorScheme.background)
	)
}
