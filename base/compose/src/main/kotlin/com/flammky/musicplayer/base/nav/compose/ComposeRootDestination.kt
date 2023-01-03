package com.flammky.musicplayer.base.nav.compose

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable

data class ComposeRootDestination(
	val routeID: String,
	val label: String,
	val iconResource: IconResource,
	val selectedIconResource: IconResource
) {

	sealed interface IconResource {

		// inline ?
		data class ResID(
			@DrawableRes val id: Int
		) : IconResource

		data class ComposeImageVector(
			// composable that return a value should trigger recomposition
			val getVector: @Composable () -> androidx.compose.ui.graphics.vector.ImageVector
		) : IconResource
	}
}
