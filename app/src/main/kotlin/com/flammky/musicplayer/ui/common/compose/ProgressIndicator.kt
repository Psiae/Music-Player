package com.flammky.musicplayer.ui.common.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max

// TODO: Link to M3 spec when available.
/**
 * Determinate Material Design linear progress indicator.
 *
 * Progress indicators express an unspecified wait time or display the duration of a process.
 *
 * ![Linear progress indicator image](https://developer.android.com/images/reference/androidx/compose/material3/linear-progress-indicator.png)
 *
 * By default there is no animation between [progress] values. You can use
 * [ProgressIndicatorDefaults.ProgressAnimationSpec] as the default recommended [AnimationSpec] when
 * animating progress, such as in the following example:
 *
 * @sample androidx.compose.material3.samples.LinearProgressIndicatorSample
 *
 * @param progress the progress of this progress indicator, where 0.0 represents no progress and 1.0
 * represents full progress. Values outside of this range are coerced into the range.
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color color of this progress indicator
 * @param trackColor color of the track behind the indicator, visible when the progress has not
 * reached the area of the overall indicator yet
 */
@Composable
fun LinearIndeterminateProgressIndicator(
	progress: Float,
	modifier: Modifier = Modifier,
	color: Color = MaterialTheme.colorScheme.primary,
	trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
	Canvas(
		modifier
			.progressSemantics(progress)
			.size(LinearIndicatorWidth, LinearIndicatorHeight)
	) {
		val strokeWidth = size.height
		drawLinearIndicatorTrack(trackColor, strokeWidth)
		drawLinearIndicator(0f, progress, color, strokeWidth)
	}
}

// TODO: Link to M3 spec when available.
/**
 * Indeterminate Material Design linear progress indicator.
 *
 * Progress indicators express an unspecified wait time or display the duration of a process.
 *
 * ![Linear progress indicator image](https://developer.android.com/images/reference/androidx/compose/material3/linear-progress-indicator.png)
 *
 * @sample androidx.compose.material3.samples.IndeterminateLinearProgressIndicatorSample
 *
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color color of this progress indicator
 * @param trackColor color of the track behind the indicator, visible when the progress has not
 * reached the area of the overall indicator yet
 */
@Composable
fun LinearIndeterminateProgressIndicator(
	modifier: Modifier = Modifier,
	color: Color = MaterialTheme.colorScheme.primary,
	trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
	duration: Int = LinearAnimationDuration,
	firstLineHeadDelay: Int = FirstLineHeadDelay,
	firstLineHeadDuration: Int = FirstLineHeadDuration,
	firstLineTailDelay: Int = FirstLineTailDelay,
	firstLineTailDuration: Int = FirstLineTailDuration,
	secondLineHeadDelay: Int = SecondLineHeadDelay,
	secondLineHeadDuration: Int = SecondLineHeadDuration,
	secondLineTailDelay: Int = SecondLineTailDelay,
	secondLineTailDuration: Int = SecondLineTailDuration,
	continueTraverse: () -> Boolean
) {
	val infiniteTransition = rememberInfiniteTransition()
	// Fractional position of the 'head' and 'tail' of the two lines drawn, i.e. if the head is 0.8
	// and the tail is 0.2, there is a line drawn from between 20% along to 80% along the total
	// width.
	val firstLineHead = infiniteTransition.animateFloat(
		0f,
		1f,
		infiniteRepeatable(
			animation = keyframes {
				durationMillis = duration
				0f at firstLineHeadDelay with FirstLineHeadEasing
				1f at firstLineHeadDuration + FirstLineHeadDelay
			}
		)
	)

	val firstLineTail = infiniteTransition.animateFloat(
		0f,
		1f,
		infiniteRepeatable(
			animation = keyframes {
				durationMillis = duration
				0f at firstLineTailDelay with FirstLineTailEasing
				1f at firstLineTailDuration + firstLineTailDelay
			}
		)
	)

	val secondLineHead = infiniteTransition.animateFloat(
		0f,
		1f,
		infiniteRepeatable(
			animation = keyframes {
				durationMillis = duration
				0f at secondLineHeadDelay with SecondLineHeadEasing
				1f at secondLineHeadDuration + secondLineHeadDelay
			}
		)
	)

	val secondLineTail = infiniteTransition.animateFloat(
		0f,
		1f,
		infiniteRepeatable(
			animation = keyframes {
				durationMillis = duration
				0f at secondLineTailDelay with SecondLineTailEasing
				1f at secondLineTailDuration + secondLineTailDelay
			}
		)
	)

	val firstLineHeadState by firstLineHead
	val firstLineHeadTailState by firstLineTail

	val firstTraverse = firstLineHeadState == firstLineHeadTailState

	val secondLineHeadState by secondLineHead
	val secondLineHeadTailState by secondLineTail

	val secondTraverse = secondLineHeadState == secondLineHeadTailState

	val draw = remember(key1 = firstTraverse, key2 = secondTraverse) {
		if (firstTraverse || secondTraverse) continueTraverse() else true
	}

	Canvas(
		modifier
			.progressSemantics()
			.size(LinearIndicatorWidth, LinearIndicatorHeight)
	) {

		val strokeWidth = size.height
		drawLinearIndicatorTrack(trackColor, strokeWidth)

		if (firstLineHeadState - firstLineHeadTailState > 0 && draw) {
			drawLinearIndicator(
				firstLineHeadState,
				firstLineHeadTailState,
				color,
				strokeWidth
			)
		}

		if (secondLineHeadState - secondLineHeadTailState > 0 && draw) {
			drawLinearIndicator(
				secondLineHeadState,
				secondLineHeadTailState,
				color,
				strokeWidth
			)
		}
	}
}

private fun DrawScope.drawLinearIndicator(
	startFraction: Float,
	endFraction: Float,
	color: Color,
	strokeWidth: Float
) {
	val width = size.width
	val height = size.height
	// Start drawing from the vertical center of the stroke
	val yOffset = height / 2

	val isLtr = layoutDirection == LayoutDirection.Ltr
	val barStart = (if (isLtr) startFraction else 1f - endFraction) * width
	val barEnd = (if (isLtr) endFraction else 1f - startFraction) * width

	// Progress line
	drawLine(color, Offset(barStart, yOffset), Offset(barEnd, yOffset), strokeWidth)
}

private fun DrawScope.drawLinearIndicatorTrack(
	color: Color,
	strokeWidth: Float
) = drawLinearIndicator(0f, 1f, color, strokeWidth)

// TODO: Link to M3 spec when available.
/**
 * Determinate Material Design circular progress indicator.
 *
 * Progress indicators express an unspecified wait time or display the duration of a process.
 *
 * ![Circular progress indicator image](https://developer.android.com/images/reference/androidx/compose/material3/circular-progress-indicator.png)
 *
 * By default there is no animation between [progress] values. You can use
 * [ProgressIndicatorDefaults.ProgressAnimationSpec] as the default recommended [AnimationSpec] when
 * animating progress, such as in the following example:
 *
 * @sample androidx.compose.material3.samples.CircularProgressIndicatorSample
 *
 * @param progress the progress of this progress indicator, where 0.0 represents no progress and 1.0
 * represents full progress. Values outside of this range are coerced into the range.
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color color of this progress indicator
 * @param strokeWidth stroke width of this progress indicator
 */
@Composable
fun CircularProgressIndicator(
	progress: Float,
	modifier: Modifier = Modifier,
	color: Color = MaterialTheme.colorScheme.primary,
	strokeWidth: Dp = 4.dp
) {
	val stroke = with(LocalDensity.current) {
		Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt)
	}
	Canvas(
		modifier
			.progressSemantics(progress)
			.size(CircularIndicatorDiameter)
	) {
		// Start at 12 o'clock
		val startAngle = 270f
		val sweep = progress * 360f
		drawDeterminateCircularIndicator(startAngle, sweep, color, stroke)
	}
}

// TODO: Link to M3 spec when available.
/**
 * Indeterminate Material Design circular progress indicator.
 *
 * Progress indicators express an unspecified wait time or display the duration of a process.
 *
 * ![Circular progress indicator image](https://developer.android.com/images/reference/androidx/compose/material3/circular-progress-indicator.png)
 *
 * @sample androidx.compose.material3.samples.IndeterminateCircularProgressIndicatorSample
 *
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color color of this progress indicator
 * @param strokeWidth stroke width of this progress indicator
 */
@Composable
fun CircularProgressIndicator(
	modifier: Modifier = Modifier,
	color: Color = MaterialTheme.colorScheme.primary,
	strokeWidth: Dp = 4.dp
) {
	val stroke = with(LocalDensity.current) {
		Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Square)
	}

	val transition = rememberInfiniteTransition()
	// The current rotation around the circle, so we know where to start the rotation from
	val currentRotation = transition.animateValue(
		0,
		RotationsPerCycle,
		Int.VectorConverter,
		infiniteRepeatable(
			animation = tween(
				durationMillis = RotationDuration * RotationsPerCycle,
				easing = LinearEasing
			)
		)
	)
	// How far forward (degrees) the base point should be from the start point
	val baseRotation = transition.animateFloat(
		0f,
		BaseRotationAngle,
		infiniteRepeatable(
			animation = tween(
				durationMillis = RotationDuration,
				easing = LinearEasing
			)
		)
	)
	// How far forward (degrees) both the head and tail should be from the base point
	val endAngle = transition.animateFloat(
		0f,
		JumpRotationAngle,
		infiniteRepeatable(
			animation = keyframes {
				durationMillis = HeadAndTailAnimationDuration + HeadAndTailDelayDuration
				0f at 0 with CircularEasing
				JumpRotationAngle at HeadAndTailAnimationDuration
			}
		)
	)
	val startAngle = transition.animateFloat(
		0f,
		JumpRotationAngle,
		infiniteRepeatable(
			animation = keyframes {
				durationMillis = HeadAndTailAnimationDuration + HeadAndTailDelayDuration
				0f at HeadAndTailDelayDuration with CircularEasing
				JumpRotationAngle at durationMillis
			}
		)
	)
	Canvas(
		modifier
			.progressSemantics()
			.size(CircularIndicatorDiameter)
	) {
		val currentRotationAngleOffset = (currentRotation.value * RotationAngleOffset) % 360f

		// How long a line to draw using the start angle as a reference point
		val sweep = abs(endAngle.value - startAngle.value)

		// Offset by the constant offset and the per rotation offset
		val offset = StartAngleOffset + currentRotationAngleOffset + baseRotation.value
		drawIndeterminateCircularIndicator(
			startAngle.value + offset,
			strokeWidth,
			sweep,
			color,
			stroke
		)
	}
}

private fun DrawScope.drawCircularIndicator(
	startAngle: Float,
	sweep: Float,
	color: Color,
	stroke: Stroke
) {
	// To draw this circle we need a rect with edges that line up with the midpoint of the stroke.
	// To do this we need to remove half the stroke width from the total diameter for both sides.
	val diameterOffset = stroke.width / 2
	val arcDimen = size.width - 2 * diameterOffset
	drawArc(
		color = color,
		startAngle = startAngle,
		sweepAngle = sweep,
		useCenter = false,
		topLeft = Offset(diameterOffset, diameterOffset),
		size = Size(arcDimen, arcDimen),
		style = stroke
	)
}

private fun DrawScope.drawDeterminateCircularIndicator(
	startAngle: Float,
	sweep: Float,
	color: Color,
	stroke: Stroke
) = drawCircularIndicator(startAngle, sweep, color, stroke)

private fun DrawScope.drawIndeterminateCircularIndicator(
	startAngle: Float,
	strokeWidth: Dp,
	sweep: Float,
	color: Color,
	stroke: Stroke
) {
	// Length of arc is angle * radius
	// Angle (radians) is length / radius
	// The length should be the same as the stroke width for calculating the min angle
	val squareStrokeCapOffset =
		(180.0 / PI).toFloat() * (strokeWidth / (CircularIndicatorDiameter / 2)) / 2f

	// Adding a square stroke cap draws half the stroke width behind the start point, so we want to
	// move it forward by that amount so the arc visually appears in the correct place
	val adjustedStartAngle = startAngle + squareStrokeCapOffset

	// When the start and end angles are in the same place, we still want to draw a small sweep, so
	// the stroke caps get added on both ends and we draw the correct minimum length arc
	val adjustedSweep = max(sweep, 0.1f)

	drawCircularIndicator(adjustedStartAngle, adjustedSweep, color, stroke)
}

/**
 * Contains the default values used for [LinearIndeterminateProgressIndicator] and [CircularProgressIndicator].
 */
object ProgressIndicatorDefaults {
	/**
	 * The default [AnimationSpec] that should be used when animating between progress in a
	 * determinate progress indicator.
	 */
	val ProgressAnimationSpec = SpringSpec(
		dampingRatio = Spring.DampingRatioNoBouncy,
		stiffness = Spring.StiffnessVeryLow,
		// The default threshold is 0.01, or 1% of the overall progress range, which is quite
		// large and noticeable. We purposefully choose a smaller threshold.
		visibilityThreshold = 1 / 1000f
	)
}

// LinearProgressIndicator Material specs

// Width is given in the spec but not defined as a token.
/*@VisibleForTesting*/
internal val LinearIndicatorWidth = 240.dp

/*@VisibleForTesting*/
internal val LinearIndicatorHeight = 4.dp

// CircularProgressIndicator Material specs
// Diameter of the indicator circle
/*@VisibleForTesting*/
internal val CircularIndicatorDiameter =
	48.dp - 4.dp * 2

//
// Indeterminate linear indicator transition specs
//

// Duration of the head and tail animations for both lines
private const val FirstLineHeadDuration = 750
private const val FirstLineTailDuration = 850
private const val SecondLineHeadDuration = 567
private const val SecondLineTailDuration = 533

// Delay before the start of the head and tail animations for both lines
private const val FirstLineHeadDelay = 0
private const val FirstLineTailDelay = 333
private const val SecondLineHeadDelay = 1000
private const val SecondLineTailDelay = 1267

// Total duration for one cycle
private const val LinearAnimationDuration = 1267 + 533

private val FirstLineHeadEasing = CubicBezierEasing(0.2f, 0f, 0.8f, 1f)
private val FirstLineTailEasing = CubicBezierEasing(0.4f, 0f, 1f, 1f)
private val SecondLineHeadEasing = CubicBezierEasing(0f, 0f, 0.65f, 1f)
private val SecondLineTailEasing = CubicBezierEasing(0.1f, 0f, 0.45f, 1f)

// Indeterminate circular indicator transition specs

// The animation comprises of 5 rotations around the circle forming a 5 pointed star.
// After the 5th rotation, we are back at the beginning of the circle.
private const val RotationsPerCycle = 5

// Each rotation is 1 and 1/3 seconds, but 1332ms divides more evenly
private const val RotationDuration = 1332

// When the rotation is at its beginning (0 or 360 degrees) we want it to be drawn at 12 o clock,
// which means 270 degrees when drawing.
private const val StartAngleOffset = -90f

// How far the base point moves around the circle
private const val BaseRotationAngle = 286f

// How far the head and tail should jump forward during one rotation past the base point
private const val JumpRotationAngle = 290f

// Each rotation we want to offset the start position by this much, so we continue where
// the previous rotation ended. This is the maximum angle covered during one rotation.
private const val RotationAngleOffset = (BaseRotationAngle + JumpRotationAngle) % 360f

// The head animates for the first half of a rotation, then is static for the second half
// The tail is static for the first half and then animates for the second half
private const val HeadAndTailAnimationDuration = (RotationDuration * 0.5).toInt()
private const val HeadAndTailDelayDuration = HeadAndTailAnimationDuration

// The easing for the head and tail jump
private val CircularEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
