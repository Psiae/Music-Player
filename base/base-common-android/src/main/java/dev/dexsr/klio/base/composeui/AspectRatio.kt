package dev.dexsr.klio.base.composeui

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.Modifier

fun Modifier.heightAspectRatio(
	/*@FloatRange(from = 0.0, fromInclusive = false)*/
	ratio: Float
) = aspectRatio(ratio, matchHeightConstraintsFirst = true)

fun Modifier.widthAspectRatio(
	/*@FloatRange(from = 0.0, fromInclusive = false)*/
	ratio: Float
) = aspectRatio(ratio, matchHeightConstraintsFirst = false)
