@file:OptIn(ExperimentalMaterial3Api::class)

package com.flammky.musicplayer.main.ui.compose.nav

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.flammky.musicplayer.base.compose.NoInlineColumn
import com.flammky.musicplayer.base.nav.compose.ComposeRootDestination
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundColorAsState
import com.flammky.musicplayer.base.theme.compose.backgroundContentColorAsState
import com.flammky.musicplayer.base.theme.compose.elevatedTonalPrimarySurfaceAsState
import com.flammky.musicplayer.base.theme.compose.secondaryContainerContentColorAsState
import com.flammky.musicplayer.main.MainActivity
import com.flammky.musicplayer.ui.util.compose.NoRipple

@Composable
internal fun MainActivity.RootNavigation() {
	val navController = rememberNavController()
	RootScaffold(navController)
}

@Composable
private fun MainActivity.RootScaffold(
	navController: NavController
) {
	val showFullPlaybackControllerState = rememberSaveable() {
		mutableStateOf(false)
	}
	val topLevelNavigationRegisteredState = remember {
		mutableStateOf(false)
	}

}

@Composable
private fun MainActivity.BottomNavigation(
	navController: NavController
) {
	val backstackEntryState = navController.currentBackStackEntryAsState()
	val navigators = RootNavigator.navigators

	if (navigators.find { it.getRootDestination().routeID == backstackEntryState.value?.id } == null) {
		return
	}

	// TODO: Customizable indication
	NoRipple {

		val absBackgroundColor = Theme.backgroundColorAsState().value
		val backgroundColor = Theme.elevatedTonalPrimarySurfaceAsState(elevation = 5.dp).value
		val alpha = (/* preference */ 0.94f).coerceAtLeast(0.3f)
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
								backgroundColor.copy(alpha = 0.4f).compositeOver(absBackgroundColor)
							)
						)
				)
			}
		}

		NoInlineColumn(
			modifier = backgroundModifier.sizeIn(maxWidth = 60.dp),
			// for inset spacer if any
			verticalArrangement = Arrangement.Bottom
		) {
			androidx.compose.material.BottomNavigation(
				contentColor = Theme.secondaryContainerContentColorAsState().value,
				backgroundColor = /* already set above by column */ Color.Transparent,
				elevation = /* already set above by elevated backgroundColor */ 0.dp
			) {
				navigators.forEach { rootNavigator ->
					val destination = rootNavigator.getRootDestination()
					val selected = destination.routeID == backstackEntryState.value?.id
					val interactionSource = remember { MutableInteractionSource() }
					NoInlineColumn(
						modifier = Modifier
							.align(Alignment.CenterVertically)
							.weight(1f)
							.selectable(
								selected = selected,
								onClick = {
									if (destination.routeID != backstackEntryState.value?.destination?.route) {
										rootNavigator.navigateToRoot(navController) {
											launchSingleTop = true
											restoreState = true
											popUpTo(navController.graph.findStartDestination().id) {
												saveState = true
											}
										}
									}
								},
								enabled = true,
								role = Role.Tab,
								interactionSource = interactionSource,
								indication = /* TODO: Customizable */ null,
							),
						verticalArrangement = Arrangement.Bottom
					) {
						// TODO: Customizable indicator
						val pressed by interactionSource.collectIsPressedAsState()
						val sizeFactor by animateFloatAsState(targetValue = if (pressed) 0.94f else 1f)

						val painter = when (
							val res = if (selected) destination.selectedIconResource else destination.iconResource
						) {
							is ComposeRootDestination.IconResource.ResID -> {
								painterResource(id = res.id)
							}
							is ComposeRootDestination.IconResource.ComposeImageVector -> {
								rememberVectorPainter(image = res.getVector())
							}
						}

						Icon(
							modifier = Modifier.size(24.dp * sizeFactor),
							painter = painter,
							contentDescription = null
						)

						Text(
							color = Theme.backgroundContentColorAsState().value,
							fontWeight = MaterialTheme.typography.labelSmall.fontWeight,
							fontSize = (MaterialTheme.typography.labelSmall.fontSize.value * sizeFactor).sp,
							fontStyle = MaterialTheme.typography.labelSmall.fontStyle,
							lineHeight = MaterialTheme.typography.labelSmall.lineHeight,
							letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing,
							text = destination.label
						)
					}
				}
			}
		}
	}
}
