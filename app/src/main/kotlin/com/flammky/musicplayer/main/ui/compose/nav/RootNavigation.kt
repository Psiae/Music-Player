@file:OptIn(ExperimentalMaterial3Api::class)

package com.flammky.musicplayer.main.ui.compose.nav

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.flammky.musicplayer.base.compose.LocalLayoutVisibility
import com.flammky.musicplayer.base.compose.NoInlineColumn
import com.flammky.musicplayer.base.nav.compose.ComposeRootDestination
import com.flammky.musicplayer.base.nav.compose.ComposeRootNavigation
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.*
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.root.main.RootPlaybackControl
import com.flammky.musicplayer.player.presentation.root.main.rememberRootPlaybackControlState

@Composable
internal fun RootNavigation(user: User) {
	rememberRootNavigationState(user)
		.run {
			Layout(withBackground = false)
		}
}

private class RootNavigationState(
	private val user: User,
	private val navController: NavHostController,
) {

	private var restored = false

	val rememberShowFullPlaybackControllerState = mutableStateOf(false)

	fun performRestore(bundle: Bundle) {
		check(!restored)
		restored = true
		if (
			user.uid == bundle.getString(User::class.qualifiedName!! + "_" + User::uid.name) &&
				user.verify == bundle.getString(User::class.qualifiedName!! + "_" + User::verify.name)
		) {
			rememberShowFullPlaybackControllerState.value =
				bundle.getBoolean(::rememberShowFullPlaybackControllerState.name)
		}
	}

	@Composable
	fun Layout(
		modifier: Modifier = Modifier,
		withBackground: Boolean = false
	) {
		val compactBottomPadding = remember {
			mutableStateOf(0.dp)
		}
		// I think the compact version can just be part of the main one ?
		val playbackControlState =
			rememberRootPlaybackControlState(
				user,
				LocalViewModelStoreOwner.current!!
			).apply {
				updateBottomBarHeight(
					with(LocalDensity.current) { compactBottomPadding.value.roundToPx() }
				)
			}
		val localBackgroundColorState = Theme.backgroundColorAsState()
		BoxWithConstraints(
			modifier = remember(
				modifier,
				withBackground,
				if (withBackground) localBackgroundColorState.value else Unit
			) {
				var acc: Modifier = modifier.fillMaxSize()
				if (withBackground) {
					acc = acc.background(localBackgroundColorState.value)
				}
				acc
			}
		) {
			Scaffold(
				modifier = Modifier.fillMaxSize(),
				bottomBar = {
					BottomNavigation(navController = navController)
				}
			) { contentPadding ->
				compactBottomPadding
					.apply {
						value = contentPadding.calculateBottomPadding()
					}
				val topBarVisibility = remember {
					mutableStateOf(0.dp)
				}.apply {
					value = maxOf(
						LocalLayoutVisibility.LocalTopBar.current,
						contentPadding.calculateTopPadding()
					)
				}
				val bottomBarVisibility = remember {
					mutableStateOf(0.dp)
				}.apply {
					value = maxOf(
						LocalLayoutVisibility.LocalBottomBar.current,
						contentPadding.calculateBottomPadding(),
						with(LocalDensity.current) { playbackControlState.compactTopPositionFromAnchor.toDp() }
					)
				}
				CompositionLocalProvider(
					LocalLayoutVisibility.LocalTopBar provides topBarVisibility.value,
					LocalLayoutVisibility.LocalBottomBar provides bottomBarVisibility.value
				) {
					NavHost(
						modifier = Modifier,
						navController = navController,
						startDestination = ComposeRootNavigation.navigators[0].getRootDestination().routeID,
					) {
						ComposeRootNavigation.navigators.forEach {
							it.addRootDestination(this, navController)
						}
					}
					BackHandler(
						playbackControlState.hasBackPressConsumer,
						playbackControlState::consumeBackPress
					)
				}
			} // Scaffold Layout End
			RootPlaybackControl(state = playbackControlState)
		}
	}

	companion object {
		fun saver(
			user: User,
			navController: NavHostController
		): Saver<RootNavigationState, Bundle> = Saver(
			save = { state ->
				Bundle()
					.apply {
						putString(
							User::class.qualifiedName!! + "_" + User::uid.name,
							state.user.uid
						)
						putString(
							User::class.qualifiedName!! + "_" + User::verify.name,
							state.user.verify
						)
						putBoolean(
							state::rememberShowFullPlaybackControllerState.name,
							state.rememberShowFullPlaybackControllerState.value
						)
					}
			},
			restore = { bundle ->
				RootNavigationState(user, navController)
					.apply {
						performRestore(bundle)
					}
			}
		)
	}
}

@Composable
private fun rememberRootNavigationState(
	user: User,
	navController: NavHostController = rememberNavController()
): RootNavigationState {
	return rememberSaveable(
		saver = RootNavigationState.saver(
			user,
			navController
		)
	) {
		RootNavigationState(
			user,
			navController
		)
	}
}

@Composable
private fun BottomNavigation(
	// TODO: don't pass navController here
	navController: NavController
) {
	val backstackEntryState = navController.currentBackStackEntryAsState()
	val navigators = ComposeRootNavigation.navigators
	val currentRoute = backstackEntryState.value?.destination?.route

	if (navigators.find { it.getRootDestination().routeID == currentRoute } == null) {
		return
	}

	// TODO: Customizable indication
	NoRippleColor {

		val absBackgroundColor = Theme.backgroundColorAsState().value
		val alpha = ( /*preference*/ 1f).coerceAtLeast(0.3f)
		val backgroundColor = Theme
			.elevatedTonalPrimarySurfaceAsState(elevation = 5.dp).value
			.copy(alpha = alpha)
		val backgroundModifier = remember(alpha, backgroundColor, absBackgroundColor) {
			if (alpha == 1f) {
				Modifier.background(backgroundColor)
			} else {
				Modifier.background(
					brush = Brush
						.verticalGradient(
							listOf(
								backgroundColor,
								backgroundColor,
								backgroundColor,
								backgroundColor.copy(alpha = 0.7f).compositeOver(absBackgroundColor)
							)
						)
				)
			}
		}

		NoInlineColumn(
			modifier = backgroundModifier.navigationBarsPadding()
		) {
			Surface(
				color = /* already set above by column */ Color.Transparent,
				contentColor = Theme.secondaryContainerContentColorAsState().value,
				tonalElevation = 0.dp,
			) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.height(56.dp + 16.dp + 2.dp)
						.selectableGroup(),
					horizontalArrangement = Arrangement.spacedBy(2.dp),
				) {
					val startRouteID = navigators[0].getRootDestination().routeID
					navigators.forEach { rootNavigator ->
						val destination = rootNavigator.getRootDestination()
						val selected = destination.routeID == currentRoute
						val interactionSource = remember { MutableInteractionSource() }
						val pressed by interactionSource.collectIsPressedAsState()
						val sizeFactor by animateFloatAsState(targetValue = if (pressed) 0.94f else 1f)
						NavigationBarItem(
							modifier = Modifier.fillMaxHeight(),
							selected = selected,
							onClick = {
								if (destination.routeID != backstackEntryState.value?.destination?.route) {
									rootNavigator.navigateToRoot(navController) {
										launchSingleTop = true
										restoreState = true
										popUpTo(startRouteID) {
											saveState = true
										}
									}
								}
							},
							icon = {
								val painter = when (
									val res =
										if (selected) destination.selectedIconResource else destination.iconResource
								) {
									is ComposeRootDestination.IconResource.ResID -> {
										painterResource(id = res.id)
									}
									is ComposeRootDestination.IconResource.ComposeImageVector -> {
										rememberVectorPainter(image = res.getVector())
									}
								}
								Icon(
									modifier = Modifier.size(24.dp),
									painter = painter,
									tint = if (selected) {
										Theme.secondaryContainerContentColorAsState().value
									} else {
										Theme.surfaceVariantContentColorAsState().value
									},
									contentDescription = null
								)
							},
							label = {
								Text(
									color = Theme.backgroundContentColorAsState().value,
									fontWeight = MaterialTheme.typography.labelMedium.fontWeight,
									fontSize = (MaterialTheme.typography.labelMedium.fontSize.value).sp,
									fontStyle = MaterialTheme.typography.labelMedium.fontStyle,
									lineHeight = MaterialTheme.typography.labelMedium.lineHeight,
									letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing,
									text = destination.label
								)
							},
							interactionSource = interactionSource,
						)
					}
				}
			}
		}
	}
}
