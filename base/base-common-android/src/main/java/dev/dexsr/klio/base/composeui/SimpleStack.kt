package dev.dexsr.klio.base.composeui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import kotlin.math.max

fun simpleStackLayoutMeasurePolicy(
	propagateMinConstraints: Boolean
): MeasurePolicy = MeasurePolicy { measurables, constraints ->
	if (measurables.isEmpty()) {
		return@MeasurePolicy layout(
			constraints.minWidth,
			constraints.minHeight,
			placementBlock = {}
		)
	}
	val contentConstraints = if (propagateMinConstraints) {
		constraints
	} else {
		constraints.copy(minWidth = 0, minHeight = 0)
	}
	if (measurables.size == 1) {
		val placeable = measurables[0].measure(contentConstraints)
		return@MeasurePolicy layout(
			placeable.width,
			placeable.height,
			placementBlock = { placeable.place(0, 0, 0f) }
		)
	}
	var width = 0
	var height = 0
	val placeables = measurables.fastMap { measurable ->
		measurable.measure(contentConstraints)
			.also { placeable ->
				width = max(placeable.width, width)
				height = max(placeable.height, height)
			}
	}
	layout(
		width,
		height,
		placementBlock = {
			placeables.fastForEach { placeable -> placeable.place(0, 0, 0f) }
		}
	)
}

val SimpleStackLayoutMeasurePolicy = simpleStackLayoutMeasurePolicy(propagateMinConstraints = false)
val SimpleStackLayoutMeasurePolicy2 = simpleStackLayoutMeasurePolicy(propagateMinConstraints = true)

// a faster simple stacking layout implementation than [Box]
@Composable
inline fun SimpleStack(
	modifier: Modifier = Modifier,
	propagateMinConstraints: Boolean = false,
	content: @Composable () -> Unit
) {
	Box {

	}
	val measurePolicy =
		if (!propagateMinConstraints) SimpleStackLayoutMeasurePolicy
		else SimpleStackLayoutMeasurePolicy2
	Layout(
		modifier = modifier,
		content = content,
		measurePolicy = measurePolicy
	)
}
