package com.flammky.musicplayer.library.ui.root

import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.flammky.android.content.context.ContextHelper
import com.flammky.androidx.content.context.findBase
import com.flammky.androidx.viewmodel.compose.activityViewModel
import com.flammky.musicplayer.base.compose.LocalLayoutVisibility
import com.flammky.musicplayer.base.compose.VisibilityViewModel
import com.flammky.musicplayer.library.localmedia.ui.LocalSongDisplay
import com.flammky.musicplayer.library.localmedia.ui.LocalSongNavigator

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
	val bottomVisibilityHeight = LocalLayoutVisibility.LocalBottomBar.current
	BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.verticalScroll(rememberScrollState()),
			verticalArrangement = Arrangement.spacedBy(16.dp)
		) {
			LocalSongDisplay(
				modifier = Modifier
					.padding(10.dp)
					.heightIn(max = 300.dp),
				viewModel = activityViewModel(),
				navigate = { route ->
					navController.navigate(route)
				}
			)
			Spacer(modifier = Modifier.height(bottomVisibilityHeight))
		}
	}
}

@Composable
private fun ApplyBackground() {
	Box(modifier = Modifier
		.fillMaxSize()
		.background(MaterialTheme.colorScheme.background)
	)
}

@Composable
private fun rememberContextHelper(): ContextHelper {
	val current = LocalContext.current
	return remember(current) {
		val context = if (current is ContextWrapper) current.findBase() else current
		ContextHelper(context)
	}
}

@Composable
private fun Int.toComposeDp(): Dp {
	return with(LocalDensity.current) { toDp() }
}

@Composable
private fun Float.toComposeDp(): Dp {
	return with(LocalDensity.current) { toDp() }
}

operator fun Float.times(other: Dp): Float {
	return this * other.value
}

