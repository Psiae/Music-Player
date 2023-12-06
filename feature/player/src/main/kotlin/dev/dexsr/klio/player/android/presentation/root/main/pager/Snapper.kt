package dev.dexsr.klio.player.android.presentation.root.main.pager

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPositionInLayout
import androidx.compose.foundation.pager.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import dev.dexsr.klio.player.android.presentation.root.main.PlaybackPagerController
import dev.dexsr.klio.player.android.presentation.root.main.PlaybackPagerLayoutInfo
import dev.dexsr.klio.player.android.presentation.root.main.mainAxisViewportSize
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sign

@OptIn(ExperimentalFoundationApi::class)
internal fun SnapLayoutInfoProvider(
    pagerState: PlaybackPagerController,
    pagerSnapDistance: PagerSnapDistance,
    decayAnimationSpec: DecayAnimationSpec<Float>,
    snapPositionalThreshold: Float
): SnapLayoutInfoProvider {
    return object : SnapLayoutInfoProvider {
        val layoutInfo: PlaybackPagerLayoutInfo
            get() = pagerState.pagerLayoutInfo

        fun Float.isValidDistance(): Boolean {
            return this != Float.POSITIVE_INFINITY && this != Float.NEGATIVE_INFINITY
        }

        override fun Density.calculateSnappingOffset(currentVelocity: Float): Float {
            var lowerBoundOffset = Float.NEGATIVE_INFINITY
            var upperBoundOffset = Float.POSITIVE_INFINITY

            layoutInfo.visiblePagesInfo.fastForEach { page ->
                val offset = calculateDistanceToDesiredSnapPosition(
                    mainAxisViewPortSize = layoutInfo.mainAxisViewportSize,
                    beforeContentPadding = layoutInfo.beforeContentPadding,
                    afterContentPadding = layoutInfo.afterContentPadding,
                    itemSize = layoutInfo.pageSize,
                    itemOffset = page.offset,
                    itemIndex = page.index,
                    snapPositionInLayout = SnapAlignmentStartToStart
                )

                // Find page that is closest to the snap position, but before it
                if (offset <= 0 && offset > lowerBoundOffset) {
                    lowerBoundOffset = offset
                }

                // Find page that is closest to the snap position, but after it
                if (offset >= 0 && offset < upperBoundOffset) {
                    upperBoundOffset = offset
                }
            }

            val isForward = pagerState.isScrollingForward()

            val offsetFromSnappedPosition =
                pagerState.dragGestureDelta() / layoutInfo.pageSize.toFloat()

            val offsetFromSnappedPositionOverflow =
                offsetFromSnappedPosition - offsetFromSnappedPosition.toInt().toFloat()

            val finalDistance = when (sign(currentVelocity)) {
                0f -> {
                    if (offsetFromSnappedPositionOverflow.absoluteValue > snapPositionalThreshold) {
                        if (isForward) upperBoundOffset else lowerBoundOffset
                    } else {
                        if (isForward) lowerBoundOffset else upperBoundOffset
                    }
                }

                1f -> upperBoundOffset
                -1f -> lowerBoundOffset
                else -> 0f
            }

            return if (finalDistance.isValidDistance()) {
                finalDistance
            } else {
                0f
            }
        }

        override fun Density.calculateSnapStepSize(): Float = layoutInfo.pageSize.toFloat()

        override fun Density.calculateApproachOffset(initialVelocity: Float): Float {
            val effectivePageSizePx = pagerState.pagerLayoutInfo.pageSize + /*pagerState.pagerLayoutInfo.pageSpacing*/ 0
            val animationOffsetPx =
                decayAnimationSpec.calculateTargetValue(0f, initialVelocity)
            val startPage = if (initialVelocity < 0) {
                pagerState.firstVisiblePage + 1
            } else {
                pagerState.firstVisiblePage
            }

            val scrollOffset =
                layoutInfo.visiblePagesInfo.fastFirstOrNull { it.index == startPage }?.offset ?: 0

            /*debugLog {
                "Initial Offset=$scrollOffset " +
                        "\nAnimation Offset=$animationOffsetPx " +
                        "\nFling Start Page=$startPage " +
                        "\nEffective Page Size=$effectivePageSizePx"
            }*/

            val targetOffsetPx = startPage * effectivePageSizePx + animationOffsetPx

            val targetPageValue = targetOffsetPx / effectivePageSizePx
            val targetPage = if (initialVelocity > 0) {
                ceil(targetPageValue)
            } else {
                floor(targetPageValue)
            }.toInt().coerceIn(0, pagerState.pagerLayoutInfo.pageCount)

            /*debugLog { "Fling Target Page=$targetPage" }*/

            val correctedTargetPage = pagerSnapDistance.calculateTargetPage(
                startPage,
                targetPage,
                initialVelocity,
                pagerState.pagerLayoutInfo.pageSize,
                /*pagerState.pagerLayoutInfo.pageSpacing*/ 0
            ).coerceIn(0, pagerState.pagerLayoutInfo.pageCount)

            /*debugLog { "Fling Corrected Target Page=$correctedTargetPage" }*/

            val proposedFlingOffset = (correctedTargetPage - startPage) * effectivePageSizePx

            /*debugLog { "Proposed Fling Approach Offset=$proposedFlingOffset" }*/

            val flingApproachOffsetPx =
                (proposedFlingOffset.absoluteValue - scrollOffset.absoluteValue).coerceAtLeast(0)

            return if (flingApproachOffsetPx == 0) {
                flingApproachOffsetPx.toFloat()
            } else {
                flingApproachOffsetPx * initialVelocity.sign
            }.also {
                /*debugLog { "Fling Approach Offset=$it" }*/
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun Density.calculateDistanceToDesiredSnapPosition(
    mainAxisViewPortSize: Int,
    beforeContentPadding: Int,
    afterContentPadding: Int,
    itemSize: Int,
    itemOffset: Int,
    itemIndex: Int,
    snapPositionInLayout: SnapPositionInLayout
): Float {
    val containerSize = mainAxisViewPortSize - beforeContentPadding - afterContentPadding

    val desiredDistance = with(snapPositionInLayout) {
        position(containerSize, itemSize, itemIndex)
    }.toFloat()

    return itemOffset - desiredDistance
}

@OptIn(ExperimentalFoundationApi::class)
internal val SnapAlignmentStartToStart = SnapPositionInLayout { _, _, _ -> 0 }

@OptIn(ExperimentalFoundationApi::class)
private fun PlaybackPagerController.isScrollingForward() = dragGestureDelta() < 0

@OptIn(ExperimentalFoundationApi::class)
private fun PlaybackPagerController.dragGestureDelta() = if (pagerLayoutInfo.orientation == Orientation.Horizontal) {
    upDownDifference.x
} else {
    upDownDifference.y
}