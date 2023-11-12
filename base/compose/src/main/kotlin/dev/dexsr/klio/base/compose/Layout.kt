package dev.dexsr.klio.base.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
		val measurable = measurables[0]
		val placeable = measurable.measure(contentConstraints)
		return@MeasurePolicy layout(
			max(constraints.minWidth, placeable.width),
			max(constraints.minHeight, placeable.height),
			placementBlock = { placeable.place(0, 0, 0f) }
		)
	}
	var width = 0
	var height = 0
	val placeables = measurables.fastMap { measurable ->
		measurable.measure(contentConstraints)
			.also { placeable ->
				if (placeable.width > width) width = placeable.width
				if (placeable.height > height) height = placeable.height
			}
	}
	layout(
		max(width, constraints.minWidth),
		max(height, constraints.minHeight)
	) {
		placeables.fastForEach { placeable -> placeable.place(0, 0, 0f) }
	}
}

val SimpleStackLayoutMeasurePolicy = simpleStackLayoutMeasurePolicy(propagateMinConstraints = false)

@Composable
inline fun Stack(
	modifier: Modifier = Modifier,
	propagateMinConstraints: Boolean = false,
	content: @Composable () -> Unit
) {
	val measurePolicy =
		if (!propagateMinConstraints) SimpleStackLayoutMeasurePolicy
		else remember { simpleStackLayoutMeasurePolicy(propagateMinConstraints) }
	Layout(
		modifier = modifier,
		content = content,
		measurePolicy = measurePolicy
	)
}
