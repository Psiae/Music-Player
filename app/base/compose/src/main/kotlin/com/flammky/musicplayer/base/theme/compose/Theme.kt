package com.flammky.musicplayer.base.theme.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import com.flammky.musicplayer.base.theme.Theme
import kotlin.math.ln

@Composable
fun Theme.Companion.primaryColorAsState(): State<Color> {
	return rememberUpdatedState(newValue = MaterialTheme.colorScheme.primary)
}

@Composable
fun Theme.Companion.surfaceColorAsState(): State<Color> {
	return rememberUpdatedState(newValue = MaterialTheme.colorScheme.surface)
}

@Composable
fun Theme.Companion.surfaceVariantColorAsState(): State<Color> {
	return rememberUpdatedState(newValue = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
fun Theme.Companion.backgroundColorAsState(): State<Color> {
	return rememberUpdatedState(newValue = MaterialTheme.colorScheme.background)
}

@Composable
fun Theme.Companion.backgroundContentColorAsState(): State<Color> {
	return rememberUpdatedState(newValue = MaterialTheme.colorScheme.onBackground)
}

@Composable
fun Theme.Companion.secondaryContainerContentColorAsState(): State<Color> {
	return rememberUpdatedState(newValue = MaterialTheme.colorScheme.onSecondaryContainer)
}

@Composable
fun Theme.Companion.elevatedTonalPrimarySurfaceAsState(elevation: Dp): State<Color> {
	val state = remember {
		mutableStateOf<Color>(Color.Unspecified)
	}.apply {
		val alpha = remember(elevation) {
			((4.5f * ln(x = (elevation).value + 1)) + 2f) / 100f
		}
		val primary = primaryColorAsState().value
		val surface = surfaceColorAsState().value
		value = remember(alpha, primary, surface) {
			primary.copy(alpha = alpha).compositeOver(surface)
		}
	}
	return state
}

// TODO: create Provides
