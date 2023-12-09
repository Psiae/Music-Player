package dev.dexsr.klio.player.android.presentation.root.main

import androidx.compose.foundation.checkScrollableContainerConstraints
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.*

internal class PlaybackPagerMeasurePolicy(
    private val isVertical: Boolean,
    private val eagerRange: Int,
    private val itemProvider: PlaybackPagerItemLayoutProvider,
    private val controller: PlaybackPagerController,
    private val horizontalAlignment: Alignment.Horizontal?,
    private val verticalAlignment: Alignment.Vertical?,
    /** The horizontal arrangement for items. Required when isVertical is false */
    private val horizontalArrangement: Arrangement.Horizontal? = null,
    /** The vertical arrangement for items. Required when isVertical is true */
    private val verticalArrangement: Arrangement.Vertical? = null,
) {

    fun subcomposeMeasurePolicy(
        scope: SubcomposeMeasureScope,
        containerConstraints: Constraints
    ): MeasureResult = with(scope) {
        checkScrollableContainerConstraints(
            constraints = containerConstraints,
            orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal
        )
        val mainAxisAvailableSize =
            if (isVertical) containerConstraints.maxHeight
            else containerConstraints.maxWidth
        val timeline: PlaybackPagerTimeline
        val firstVisiblePage: Int
        val firstVisiblePageOffset: Int
        Snapshot.withoutReadObservation {
            timeline = controller.renderData?.timeline ?: PlaybackPagerTimeline.UNSET
            firstVisiblePage = controller.firstVisiblePage
            firstVisiblePageOffset = controller.firstVisiblePageOffset
        }
        controller.density = this
        PlaybackPagerMeasureScope(
            contentFactory = PlaybackPagerContentFactory(itemProvider),
            subcomposeMeasureScope = this,
        ).measurePager(
            timeline = timeline,
            firstVisiblePage = firstVisiblePage,
            firstVisiblePageOffset = firstVisiblePageOffset,
            pagerItemProvider = itemProvider,
            mainAxisAvailableSize = mainAxisAvailableSize,
            scrollToBeConsumed = controller.scrollToBeConsumed,
            constraints = containerConstraints.offset(0, 0),
            orientation = if (isVertical) {
                Orientation.Vertical
            } else {
                Orientation.Horizontal
            },
            verticalAlignment = verticalAlignment,
            horizontalAlignment = horizontalAlignment,
            visualPageOffset = IntOffset(0, 0),
            beyondBoundsPageCount = eagerRange * 2,
            pageAvailableSize = mainAxisAvailableSize,
            layoutPlacement = { width, height, placement ->
                layout(
                    containerConstraints.constrainWidth(width + 0),
                    containerConstraints.constrainHeight(height + 0),
                    emptyMap(),
                    placement
                )
            }
        ).also {
            controller.onMeasureResult(it)
        }
    }
}