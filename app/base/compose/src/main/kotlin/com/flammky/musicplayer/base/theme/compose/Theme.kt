package com.flammky.musicplayer.base.theme.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import com.flammky.musicplayer.base.theme.Theme
import kotlin.math.ln

@Composable
fun Theme.Companion.primaryColorAsState(): State<Color> {
	val state = remember {
		mutableStateOf(Color.Unspecified)
	}
	state.value = MaterialTheme.colorScheme.primary
	return state
}

@Composable
fun Theme.Companion.surfaceColorAsState(): State<Color> {
	val state = remember {
		mutableStateOf(Color.Unspecified)
	}
	state.value = MaterialTheme.colorScheme.surface
	return state
}

@Composable
fun Theme.Companion.backgroundColorAsState(): State<Color> {
	val state = remember {
		mutableStateOf<Color>(Color.Unspecified)
	}
	state.value = MaterialTheme.colorScheme.background
	return state
}

@Composable
fun Theme.Companion.backgroundContentColorAsState(): State<Color> {
	val state = remember {
		mutableStateOf<Color>(Color.Unspecified)
	}
	state.value = MaterialTheme.colorScheme.onBackground
	return state
}

@Composable
fun Theme.Companion.secondaryContainerContentColorAsState(): State<Color> {
	val state = remember {
		mutableStateOf<Color>(Color.Unspecified)
	}
	state.value = MaterialTheme.colorScheme.onSecondaryContainer
	return state
}

@Composable
fun Theme.Companion.elevatedTonalPrimarySurfaceAsState(elevation: Dp): State<Color> {
	val state = remember {
		mutableStateOf<Color>(Color.Unspecified)
	}
	val alpha = remember(elevation) {
		((4.5f * ln(x = (elevation).value + 1)) + 2f) / 100f
	}
	val primary = primaryColorAsState().value
	val surface = surfaceColorAsState().value
	state.value = remember(alpha, primary, surface) {
		primary.copy(alpha = alpha).compositeOver(surface)
	}
	return state
}

// TODO: create Provides
