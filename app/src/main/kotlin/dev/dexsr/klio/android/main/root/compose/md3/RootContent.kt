package dev.dexsr.klio.android.main.root.compose.md3

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.flammky.musicplayer.base.compose.LocalLayoutVisibility
import com.flammky.musicplayer.main.presentation.root.MaterialDesign3Theme
import dev.dexsr.klio.android.main.root.compose.OldRootNavHost
import dev.dexsr.klio.base.compose.*
import dev.dexsr.klio.android.base.systeminsets.compose.systemNavigationBarsPadding
import dev.dexsr.klio.base.resource.LocalImage
import dev.dexsr.klio.base.theme.md3.MD3Spec
import dev.dexsr.klio.base.theme.md3.MD3Theme
import dev.dexsr.klio.base.theme.md3.components.navigationbar.BottomNavigationBarElevation
import dev.dexsr.klio.base.theme.md3.components.navigationbar.BottomNavigationBarHeightDp
import dev.dexsr.klio.base.theme.md3.components.navigationbar.BottomNavigationBarWidthDp
import dev.dexsr.klio.base.theme.md3.compose.backgroundContentColorAsState
import dev.dexsr.klio.base.theme.md3.compose.secondaryContainerContentColorAsState
import dev.dexsr.klio.base.theme.md3.compose.surfaceColorAtElevation
import dev.dexsr.klio.base.theme.md3.compose.surfaceVariantContentColorAsState
import dev.dexsr.klio.player.android.presentation.root.*

@Composable
internal fun MD3RootContent(
	modifier: Modifier = Modifier,
) {
	// test
	MaterialDesign3Theme {
		CompositionLocalProvider(
			LocalLayoutVisibility.Top provides with(LocalDensity.current) {
				WindowInsets.statusBars.getTop(LocalDensity.current).toDp()
			}
		) {
			val pState = rememberPlaybackControlScreenState()
			val pCompactState = rememberRootCompactPlaybackControlPanelState(
				onArtClicked = { pState.show() },
				onSurfaceClicked = { pState.show() }
			)
			MD3RootContent(
				modifier = modifier,
				compactPlaybackControlPanelState = pCompactState,
				playbackControlScreenState = pState,
				navHostController = rememberNavController()
			)
		}
	}
}

@Composable
private fun MD3RootContent(
	modifier: Modifier,
	compactPlaybackControlPanelState: RootCompactPlaybackControlPanelState,
	playbackControlScreenState: PlaybackControlScreenState,
	// TEMP
	navHostController: NavHostController
) {
	val lifecycleOwner = LocalLifecycleOwner.current
	val navHostBackPressDispatcherOwner = remember(lifecycleOwner) {
		object : OnBackPressedDispatcherOwner, LifecycleOwner by lifecycleOwner {
			override val onBackPressedDispatcher: OnBackPressedDispatcher = OnBackPressedDispatcher()
		}
	}
	MD3RootContentProvidableLocals(

	) {
		SubcomposeLayout(modifier) { constraints ->

			val contentConstraints = constraints.copy(minWidth = 0, minHeight = 0)

			val bottomNavigationBar = subcompose("BottomNavigationBar") {
				MD3RootBottomNavigationBar(
					state = rememberRootBottomNavigationBarState(navController = navHostController),
					systemNavigationBarSpacing = true
				)
			}.fastMap { it.measure(contentConstraints) }

			val compactPlaybackControlPanel = subcompose("CompactPlaybackControlPanel") {
				MD3RootCompactPlaybackControlPanel(
					state = compactPlaybackControlPanelState,
					bottomSpacing = bottomNavigationBar.maxOfOrNull { it.height }?.toDp() ?: 0.dp
				)
			}.fastMap { it.measure(contentConstraints) }

			val navHost = subcompose("NavHost") {
				OldRootNavHost(
					onBackPressedDispatcherOwner = navHostBackPressDispatcherOwner,
					topBarVisibilitySpacing = LocalLayoutVisibility.Top.current,
					bottomBarVisibilitySpacing = maxOf(
						bottomNavigationBar.maxOfOrNull { it.height }?.toDp() ?: 0.dp,
						compactPlaybackControlPanelState.heightFromAnchor,
					),
					navHostController = navHostController
				)
			}.fastMap { it.measure(contentConstraints) }

			val playbackControlScreen = subcompose("PlaybackControlScreen") {
				RootPlaybackControlScreen(state = playbackControlScreenState)
			}.fastMap { it.measure(contentConstraints) }

			layout(constraints.maxWidth, constraints.maxHeight) {
				run {
					navHost.fastForEach { it.place(x = 0, y = 0) }
				}
				run {
					val height = bottomNavigationBar.fastMaxBy { it.height }?.height ?: 0
					bottomNavigationBar.fastForEach { it.place(x = 0, y = constraints.maxHeight - height) }
				}
				run {
					val height = compactPlaybackControlPanel.fastMaxBy { it.height }?.height ?: 0
					compactPlaybackControlPanel.fastForEach { it.place(x = 0, y = constraints.maxHeight - height) }
				}
				run {
					val height = playbackControlScreen.fastMaxBy { it.height }?.height ?: 0
					playbackControlScreen.fastForEach { it.place(x = 0, y = constraints.maxHeight - height) }
				}
			}
		}
	}
	BackHandler(
		// no short-circuit so the snapshot is read
		enabled = playbackControlScreenState.backPressRegistry.hasBackPressConsumer() or
			// ugly workaround due to BackPressDispatcher has no snapshot mechanism
			navHostController.run {
				currentBackStackEntryAsState().value?.destination?.id != graph.startDestinationId
			}
	) {
		if (playbackControlScreenState.backPressRegistry.consumeBackPress()) {
			return@BackHandler
		}
		if (navHostBackPressDispatcherOwner.onBackPressedDispatcher.hasEnabledCallbacks()) {
			navHostBackPressDispatcherOwner.onBackPressedDispatcher.onBackPressed()
			return@BackHandler
		}
	}
}

@Composable
private fun MD3RootContentProvidableLocals(
	content: ComposableFun
) {
	CompositionLocalProvider(
		content = content
	)
}

// TODO: internal for testing with existing
@Composable
internal fun MD3RootBottomNavigationBar(
	modifier: Modifier = Modifier,
	state: RootBottomNavigationBarState,
	systemNavigationBarSpacing: Boolean
) {
	Box(
		modifier = modifier
	) {
		MD3RootBottomNavigationBarContent(
			modifier = Modifier
				.background(MD3Theme.surfaceColorAtElevation(elevation = MD3Spec.BottomNavigationBarElevation.dp))
				.combineIf(systemNavigationBarSpacing) { Modifier.systemNavigationBarsPadding() }
				.height(MD3Spec.BottomNavigationBarHeightDp.checkedDpStatic())
				.width(MD3Spec.BottomNavigationBarWidthDp.checkedDpStatic()),
			state = state
		)
	}
}

@Composable
private fun MD3RootBottomNavigationBarContent(
	modifier: Modifier = Modifier,
	state: RootBottomNavigationBarState
) {
	Row(
		modifier = modifier
			.fillMaxWidth()
			.selectableGroup()
	) {
		val destinations = remember(state) { state.destinationsFlow() }
			.collectAsState(initial = emptyList())
			.value

		val currentDestination = remember(state) { state.currentDestinationFlow() }
			.collectAsState(initial = null)
			.value

		// Note: the implementation were made for constant option
		// If we ever find the need for dynamic options we need to map the states
		destinations.forEach { destination ->
			val selected = currentDestination?.id == destination.id
			NavigationBarItem(
				selected = selected,
				enabled = remember(destination) {
					state.destinationEnabledAsFlow(destination)
				}.collectAsState(initial = false).value,
				onClick = { state.navigate(destination) },
				icon = {
					val image = remember(destination, selected) {
						state.destinationImageAsFlow(destination, selected)
					}.collectAsState(initial = LocalImage.None).value
					val painter = when {
						image is LocalImage.Resource -> painterResource(id = image.value)
						else -> NoOpPainter
					}
					Icon(
						modifier = Modifier.size(24.dp),
						painter = painter,
						tint = if (selected) {
							MD3Theme.secondaryContainerContentColorAsState().value
						} else {
							MD3Theme.surfaceVariantContentColorAsState().value
						},
						contentDescription = null
					)
				},
				label = {
					Text(
						color = MD3Theme.backgroundContentColorAsState().value,
						fontWeight = MaterialTheme.typography.labelMedium.fontWeight,
						fontSize = MaterialTheme.typography.labelMedium.fontSize,
						fontStyle = MaterialTheme.typography.labelMedium.fontStyle,
						lineHeight = MaterialTheme.typography.labelMedium.lineHeight,
						letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing,
						text = remember(destination) {
							state.destinationLabelAsFlow(destination)
						}.collectAsState(initial = "").value
					)
				}
			)
		}
	}
}
