package dev.dexsr.klio.android.main.root.compose.md3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import com.flammky.musicplayer.base.nav.compose.ComposeRootDestination
import com.flammky.musicplayer.base.nav.compose.ComposeRootNavigation
import dev.dexsr.klio.android.base.resource.AndroidLocalImage
import dev.dexsr.klio.base.kt.castOrNull
import dev.dexsr.klio.base.resource.LocalImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal abstract class RootBottomNavigationBarState {

	class Destination(
		val id: String
	)

	abstract fun currentDestinationFlow(): Flow<Destination?>

	abstract fun destinationsFlow(): Flow<List<Destination>>

	abstract fun destinationEnabledAsFlow(destination: Destination): Flow<Boolean>

	abstract fun destinationImageAsFlow(destination: Destination, selected: Boolean): Flow<LocalImage<*>>

	abstract fun navigate(destination: Destination)

	abstract fun destinationLabelAsFlow(destination: Destination): Flow<String>
}

@Composable
internal fun rememberRootBottomNavigationBarState(
	navController: NavController
): RootBottomNavigationBarState {

	return remember(navController) {
		object : RememberObserver {
			val state = NavControllerRootBottomNavigationBarState(navController)
			override fun onAbandoned() {
			}
			override fun onForgotten() {
			}
			override fun onRemembered() {
			}
		}.state
	}
}

internal class NavControllerRootBottomNavigationBarState(
	private val navController: NavController
) : RootBottomNavigationBarState() {

	override fun currentDestinationFlow(): Flow<Destination?> {
		return navController.currentBackStackEntryFlow
			.map { it.destination.route }
			.distinctUntilChanged()
			.map { route -> Destination(route ?: return@map null) }
	}

	override fun destinationsFlow(): Flow<List<Destination>> {
		return flow {
			// TODO: temp
			emit(ComposeRootNavigation.navigators.map { Destination(it.getRootDestination().routeID) })
		}
	}

	override fun destinationEnabledAsFlow(destination: Destination): Flow<Boolean> {
		return flow {
			// TODO: temp
			emit(true)
		}
	}

	override fun destinationImageAsFlow(
		destination: Destination,
		selected: Boolean
	): Flow<LocalImage<*>> {
		return flow {
			// TODO: temp
			val dest = 	ComposeRootNavigation.navigators
				.find { it.getRootDestination().routeID == destination.id  }
				?.getRootDestination()
				?: run {
					emit(LocalImage.None)
					return@flow
				}
			val res = if (selected) dest.selectedIconResource else dest.iconResource
			val resId = res.castOrNull<ComposeRootDestination.IconResource.ResID>()?.id
				?: run {
					emit(LocalImage.None)
					return@flow
				}
			emit(AndroidLocalImage.Resource(resId))
		}
	}

	override fun navigate(destination: Destination) {
		// TODO: temp
		if (destination.id != navController.currentBackStackEntry?.destination?.route) {
			val navigator = ComposeRootNavigation.navigators
				.find { it.getRootDestination().routeID == destination.id  }
				?: return

			navigator.navigateToRoot(navController) {
				launchSingleTop = true
				restoreState = true
				popUpTo(ComposeRootNavigation.navigators[0].getRootDestination().routeID) {
					saveState = true
				}
			}
		}
	}

	override fun destinationLabelAsFlow(destination: Destination): Flow<String> {
		return flow {
			// TODO: temp
			val dest = 	ComposeRootNavigation.navigators
				.find { it.getRootDestination().routeID == destination.id  }
				?.getRootDestination()
				?: run {
					emit("")
					return@flow
				}
			emit(dest.label)
		}
	}
}
