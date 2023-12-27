package dev.dexsr.klio.library.ui.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.base.compose.LocalLayoutVisibility
import com.flammky.musicplayer.base.compose.NoInline
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundContentColorAsState
import com.flammky.musicplayer.base.theme.compose.surfaceContentColorAsState
import com.flammky.musicplayer.library.R
import dev.dexsr.klio.base.compose.ComposeBackPressRegistry
import dev.dexsr.klio.base.compose.LocalComposeBackPressRegistry
import dev.dexsr.klio.base.compose.SimpleStack
import dev.dexsr.klio.base.theme.md3.MD3Theme
import dev.dexsr.klio.base.theme.md3.compose.localMaterial3Surface
import dev.dexsr.klio.base.theme.md3.compose.primaryColorAsState
import dev.dexsr.klio.library.ui.root.LibraryRootNavigator

@Composable
fun LibraryUiMain(
	rootNavigator: LibraryRootNavigator
) {
	val navigator = rememberMainNavigator()
	Column(
		modifier = Modifier
			.padding(
				top = LocalLayoutVisibility.Top.current,
			)
	) {
		CompositionLocalProvider(
			LocalLayoutVisibility.Top provides 0.dp,
		) {
			LibraryUiMainNavigationTab(navigator = navigator)
			LibraryUiMainNavHost(navigator = navigator)
		}
	}
	LibraryUiMainSubScreenHost(navigator = navigator)

	NoInline {
		val bpReg = LocalComposeBackPressRegistry.current
		DisposableEffect(
			bpReg,
			effect = {
				if (bpReg == null) return@DisposableEffect onDispose {  }
				val consumer = ComposeBackPressRegistry.BackPressConsumer {
					navigator.pop()
				}
				bpReg.registerBackPressConsumer(consumer)
				onDispose { bpReg.unregisterBackPressConsumer(consumer) }
			}
		)
	}
}

@Composable
private fun LibraryUiMainNavHost(
	navigator: LibraryMainNavigator,
) {
	SimpleStack(
		modifier = Modifier
			.fillMaxSize()
			.localMaterial3Surface()
	) {
		navigator.currentDestination.content.invoke()
	}
}

@Composable
fun LibraryUiMainNavigationTab(
	navigator: LibraryMainNavigator
) {
	// we should define better way
	val tabDestinationToIndex = remember {
		mapOf<String, Int>("device" to 0, "spotify" to 1, "ytm" to 2)
	}
	TabRow(
		// implement our own indicator
		selectedTabIndex = Int.MAX_VALUE,
		indicator = { tabPositions ->
			val selectedTabIndex = tabDestinationToIndex[navigator.currentDestination.route]
			if (selectedTabIndex == null || selectedTabIndex !in tabPositions.indices) {
				return@TabRow
			}
			val currentTabPosition = tabPositions[selectedTabIndex]
			val currentTabWidth by animateDpAsState(
				targetValue = currentTabPosition.width,
				animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
				label = ""
			)
			val indicatorOffset by animateDpAsState(
				targetValue = currentTabPosition.left,
				animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
				label = ""
			)
			TabRowDefaults.Indicator(
				Modifier
					.wrapContentSize(Alignment.BottomStart)
					.offset(x = indicatorOffset + (currentTabPosition.width / 2 - 45.dp))
					.width(90.dp),
				height = 2.dp,
				color = MD3Theme.primaryColorAsState().value
			)
		}
	) {
		LeadingIconTab(
			selected = navigator.currentDestination.route == "device",
			onClick = {
				navigator.navigate("device")
			},
			icon = {
				Icon(
					modifier = Modifier.size(20.dp),
					painter = painterResource(id = R.drawable.touchscreen_96),
					contentDescription = "device",
					tint = MD3Theme.primaryColorAsState().value
				)
			},
			text = {
				Text(
					text = "Device",
					style = MaterialTheme.typography.titleSmall,
					color = Theme.backgroundContentColorAsState().value
				)
			}
		)
		LeadingIconTab(
			selected = navigator.currentDestination.route == "spotify",
			onClick = {
				navigator.navigate("spotify")
			},
			icon = {
				Icon(
					modifier = Modifier.size(20.dp),
					painter = painterResource(id = R.drawable.spotify_logo_without_text),
					contentDescription = "device",
					tint = Color.Unspecified
				)
			},
			text = {
				Text(
					text = "Spotify",
					style = MaterialTheme.typography.titleSmall,
					color = Theme.surfaceContentColorAsState().value
				)
			}
		)
		LeadingIconTab(
			selected = navigator.currentDestination.route == "ytm",
			onClick = {
				navigator.navigate("ytm")
			},
			icon = {
				Icon(
					modifier = Modifier.size(20.dp),
					painter = painterResource(id = R.drawable.ytm_240),
					contentDescription = "youtube",
					tint = Color.Unspecified
				)
			},
			text = {
				Text(
					text = "Youtube",
					style = MaterialTheme.typography.titleSmall,
					color = Theme.surfaceContentColorAsState().value
				)
			}
		)
	}
}

@Composable
private fun LibraryUiMainSubScreenHost(
	navigator: LibraryMainNavigator
) {
	navigator.currentSubDestination?.let { dest ->
		SimpleStack(
			modifier = Modifier
				.fillMaxSize()
				.localMaterial3Surface()
		) {
			dest.content.invoke()
		}
	}
}
