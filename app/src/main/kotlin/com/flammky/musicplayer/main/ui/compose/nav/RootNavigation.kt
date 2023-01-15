@file:OptIn(ExperimentalMaterial3Api::class)

package com.flammky.musicplayer.main.ui.compose.nav

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.flammky.musicplayer.base.compose.LocalLayoutVisibility
import com.flammky.musicplayer.base.compose.NoInlineColumn
import com.flammky.musicplayer.base.compose.rewrite
import com.flammky.musicplayer.base.nav.compose.ComposeRootDestination
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.*
import com.flammky.musicplayer.main.MainActivity
import com.flammky.musicplayer.playbackcontrol.presentation.compose.TransitioningPlaybackControl
import com.flammky.musicplayer.playbackcontrol.presentation.compose.compact.TransitioningCompactPlaybackControl

@Composable
internal fun MainActivity.RootNavigation() {
	MaterialTheme.colorScheme
	val navController = rememberNavController()
	RootScaffold(navController)
}

@Composable
private fun MainActivity.RootScaffold(
	navController: NavHostController
) {
	val showFullPlaybackControllerState = rememberSaveable {
		mutableStateOf(false)
	}
	val topLevelNavigationRegisteredState = remember {
		mutableStateOf(false)
	}
	Scaffold(
		modifier = Modifier.fillMaxSize(),
		bottomBar = { BottomNavigation(navController = navController) },
		containerColor = Theme.backgroundColorAsState().value
	) { contentPadding ->
		val playbackControlVisibilityOffset = remember {
			mutableStateOf(0.dp)
		}
		val topBarVisibility = remember {
			mutableStateOf(0.dp)
		}.rewrite {
			contentPadding.calculateTopPadding()
		}
		val bottomBarVisibility = remember {
			mutableStateOf(0.dp)
		}.rewrite {
			maxOf(
				contentPadding.calculateBottomPadding(),
				playbackControlVisibilityOffset.value.unaryMinus()
			)
		}
		CompositionLocalProvider(
			LocalLayoutVisibility.LocalTopBar provides contentPadding.calculateTopPadding(),
			LocalLayoutVisibility.LocalBottomBar provides bottomBarVisibility.value
		) {
			NavHost(
				modifier = Modifier
					// will also consume insets for children
					.statusBarsPadding(),
				navController = navController,
				startDestination = RootNavigator.navigators[0].getRootDestination().routeID,
			) {
				RootNavigator.navigators.forEach { it.addRootDestination(this, navController) }
			}.also {
				topLevelNavigationRegisteredState.value = true
			}
		}
		TransitioningCompactPlaybackControl(
			bottomVisibilityVerticalPadding = contentPadding.calculateBottomPadding(),
			onArtworkClicked = { showFullPlaybackControllerState.value = true },
			onBaseClicked = { showFullPlaybackControllerState.value = true },
			onVerticalVisibilityChanged = { playbackControlVisibilityOffset.value = it }
		)
	}
	TransitioningPlaybackControl(
		showSelfState = remember {
			derivedStateOf { showFullPlaybackControllerState.value && topLevelNavigationRegisteredState.value }
		},
		dismiss = { showFullPlaybackControllerState.value = false }
	)
}

@Composable
private fun MainActivity.BottomNavigation(
	navController: NavController
) {
	val backstackEntryState = navController.currentBackStackEntryAsState()
	val navigators = RootNavigator.navigators
	val currentRoute = backstackEntryState.value?.destination?.route

	if (navigators.find { it.getRootDestination().routeID == currentRoute } == null) {
		return
	}

	// TODO: Customizable indication
	NoRippleColor {

		val absBackgroundColor = Theme.backgroundColorAsState().value
		val alpha = (/* preference */ 1f).coerceAtLeast(0.3f)
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
