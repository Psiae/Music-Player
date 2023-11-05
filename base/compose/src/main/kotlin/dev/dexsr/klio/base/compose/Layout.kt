package dev.dexsr.klio.base.compose

import androidx.compose.ui.layout.MeasurePolicy

val SimpleStackLayoutMeasurePolicy : MeasurePolicy = MeasurePolicy { measurables, constraints ->
	if (measurables.isEmpty()) {
		return@MeasurePolicy layout(
			constraints.minWidth,
			constraints.minHeight,
			placementBlock = {}
		)
	}
	if (measurables.size == 1) {
		val measurable = measurables[0]
		val placeable = measurable.measure(constraints)
		return@MeasurePolicy layout(
			placeable.width,
			placeable.height,
			placementBlock = { placeable.place(0, 0, 0f) }
		)
	}
	var width = 0
	var height = 0
	val placeables = measurables.map { measurable ->
		measurable.measure(constraints)
			.also { placeable ->
				if (placeable.width > width) width = placeable.width
				if (placeable.height > height) height = placeable.height
			}
	}
	layout(width, height) {
		placeables.forEach { placeable -> placeable.place(0, 0, 0f) }
	}
}
