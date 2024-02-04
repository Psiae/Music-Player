package dev.dexsr.klio.android.main.root.compose

import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.flammky.musicplayer.base.compose.LocalLayoutVisibility
import com.flammky.musicplayer.base.nav.compose.ComposeRootNavigation
import dev.dexsr.klio.base.composeui.ComposeBackPressRegistry
import dev.dexsr.klio.base.composeui.LocalComposeBackPressRegistry


@Composable
internal fun OldRootNavHost(
	onBackPressedDispatcherOwner: OnBackPressedDispatcherOwner,
	backPressRegistry: ComposeBackPressRegistry,
	topBarVisibilitySpacing: Dp,
	bottomBarVisibilitySpacing: Dp,
	navHostController: NavHostController
) {
	CompositionLocalProvider(
		LocalOnBackPressedDispatcherOwner provides onBackPressedDispatcherOwner,
		LocalComposeBackPressRegistry provides backPressRegistry,
		LocalLayoutVisibility.Top provides topBarVisibilitySpacing,
		LocalLayoutVisibility.Bottom provides bottomBarVisibilitySpacing
	) {
		NavHost(
			modifier = Modifier,
			navController = navHostController,
			startDestination = ComposeRootNavigation.navigators[0].getRootDestination().routeID,
		) {
			ComposeRootNavigation.navigators.forEach {
				it.addRootDestination(this, navHostController)
			}
		}
	}
}
