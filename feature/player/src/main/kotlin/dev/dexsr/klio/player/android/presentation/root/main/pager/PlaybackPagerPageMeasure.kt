package dev.dexsr.klio.player.android.presentation.root.main.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection

@OptIn(ExperimentalFoundationApi::class)
internal fun PlaybackPagerMeasureScope.getAndMeasure(
    index: Int,
    childConstraints: Constraints,
    pagerItemProvider: PlaybackPagerItemLayoutProvider,
    visualPageOffset: IntOffset,
    orientation: Orientation,
    horizontalAlignment: Alignment.Horizontal?,
    verticalAlignment: Alignment.Vertical?,
    afterContentPadding: Int,
    beforeContentPadding: Int,
    layoutDirection: LayoutDirection,
    reverseLayout: Boolean,
    pageAvailableSize: Int
): PlaybackPagerMeasuredPage {
    val key = index
    val placeable = measure(index, childConstraints)

    return PlaybackPagerMeasuredPage(
        index = index,
        placeables = placeable,
        visualOffset = visualPageOffset,
        horizontalAlignment = horizontalAlignment,
        verticalAlignment = verticalAlignment,
        afterContentPadding = afterContentPadding,
        beforeContentPadding = beforeContentPadding,
        layoutDirection = layoutDirection,
        reverseLayout = reverseLayout,
        size = pageAvailableSize,
        orientation = orientation,
        key = key
    )
}