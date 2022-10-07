package com.flammky.musicplayer.library.ui.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.flammky.androidx.viewmodel.compose.activityViewModel
import com.flammky.musicplayer.library.localsong.ui.LocalSongDisplay
import com.flammky.musicplayer.library.localsong.ui.LocalSongNavigator

@Composable
internal fun LibraryRoot() {
	ApplyBackground()
	LibraryRootNavigation()
}

@Composable
private fun LibraryRootNavigation(
	navController: NavHostController = rememberNavController()
) {
	NavHost(
		navController = navController,
		startDestination = "content"
	) {
		composable("content") {
			LibraryRootContent(navController)
		}
		with(LocalSongNavigator) { addLocalSongDestinations() }
	}
}

@Composable
private fun LibraryRootContent(
	navController: NavController
) {
	Column(
		modifier = Modifier.fillMaxSize(),
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		LocalSongDisplay(
			viewModel = activityViewModel(),
			navigate = { route ->
				navController.navigate(route)
			}
		)
	}
}

@Composable
private fun ApplyBackground() {
	Box(modifier = Modifier
		.fillMaxSize()
		.background(MaterialTheme.colorScheme.background)
	)
}
