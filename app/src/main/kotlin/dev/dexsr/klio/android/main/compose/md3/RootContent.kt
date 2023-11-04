package dev.dexsr.klio.android.main.compose.md3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundContentColorAsState
import com.flammky.musicplayer.base.theme.compose.secondaryContainerContentColorAsState
import com.flammky.musicplayer.base.theme.compose.surfaceVariantContentColorAsState
import com.flammky.musicplayer.main.presentation.root.MaterialDesign3Theme
import com.flammky.musicplayer.main.presentation.root.RootEntry
import dev.dexsr.klio.base.compose.NoOpPainter
import dev.dexsr.klio.base.compose.checkedDpStatic
import dev.dexsr.klio.base.compose.combineIf
import dev.dexsr.klio.base.compose.consumeDownGesture
import dev.dexsr.klio.base.compose.systeminsets.systemNavigationBarsPadding
import dev.dexsr.klio.base.resource.LocalImage
import dev.dexsr.klio.base.theme.md3.MD3Spec
import dev.dexsr.klio.base.theme.md3.MD3Theme
import dev.dexsr.klio.base.theme.md3.components.navigationbar.BottomNavigationBarElevation
import dev.dexsr.klio.base.theme.md3.components.navigationbar.BottomNavigationBarHeightDp
import dev.dexsr.klio.base.theme.md3.components.navigationbar.BottomNavigationBarWidthDp
import dev.dexsr.klio.base.theme.md3.compose.surfaceColorAtElevation

@Composable
internal fun MD3RootContent(
	modifier: Modifier = Modifier,
) {
	/*MD3RootContent(
		modifier = modifier.navigationBarsPadding(),
		bottomNavigationBarState = rememberRootBottomNavigationBarState()
	)*/

	// test
	MaterialDesign3Theme {
		Box(modifier = Modifier) { RootEntry() }
	}
}

@Composable
private fun MD3RootContent(
	modifier: Modifier,
	bottomNavigationBarState: RootBottomNavigationBarState
) {
	SubcomposeLayout(modifier.fillMaxSize()) { constraints ->

		val bottomNavigationBar = subcompose("BottomNavigationBar") {
			RootBottomNavigationBar(state = bottomNavigationBarState, systemNavigationBarSpacing = true)
		}.map { it.measure(constraints) }

		layout(constraints.maxWidth, constraints.maxHeight) {
			bottomNavigationBar.forEach { it.place(x = 0, y = constraints.maxHeight) }
		}
	}
}

// TODO: internal for testing with existing
@Composable
internal fun RootBottomNavigationBar(
	modifier: Modifier = Modifier,
	state: RootBottomNavigationBarState,
	systemNavigationBarSpacing: Boolean
) {
	Box(
		modifier = modifier
			.consumeDownGesture()
	) {
		RootBottomNavigationBarBackground(modifier = Modifier.matchParentSize())
		RootBottomNavigationBarContent(
			modifier = Modifier
				.combineIf(systemNavigationBarSpacing) { Modifier.systemNavigationBarsPadding() }
				.height(MD3Spec.BottomNavigationBarHeightDp.checkedDpStatic())
				.width(MD3Spec.BottomNavigationBarWidthDp.checkedDpStatic())
				.align(Alignment.Center),
			state = state
		)
	}
}

@Composable
private fun RootBottomNavigationBarBackground(
	modifier: Modifier = Modifier,
) {
	Box(
		modifier = modifier
			.fillMaxSize()
			.background(MD3Theme.surfaceColorAtElevation(elevation = MD3Spec.BottomNavigationBarElevation.dp))
	)
}

@Composable
private fun RootBottomNavigationBarContent(
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
