package dev.dexsr.klio.player.android.presentation.root.main.pager

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach

class PlaybackPagerMeasuredPage(
    val index: Int,
    val size: Int,
    val placeables: List<Placeable>,
    val visualOffset: IntOffset,
    val key: Any,
    val orientation: Orientation,
    val horizontalAlignment: Alignment.Horizontal?,
    val verticalAlignment: Alignment.Vertical?,
    val layoutDirection: LayoutDirection,
    val reverseLayout: Boolean,
    val beforeContentPadding: Int,
    val afterContentPadding: Int,
) {

    val crossAxisSize: Int

    init {
        var maxCrossAxis = 0
        placeables.fastForEach {
            maxCrossAxis = maxOf(
                maxCrossAxis,
                if (orientation != Orientation.Vertical) it.height else it.width
            )
        }
        crossAxisSize = maxCrossAxis
    }

    fun position(
        offset: Int,
        layoutWidth: Int,
        layoutHeight: Int
    ): PlaybackPagerPositionedPage {
        val placeables = mutableListOf<PlaybackPagerItemPlaceable>()
        val mainAxisLayoutSize =
            if (orientation == Orientation.Vertical) layoutHeight else layoutWidth
        var mainAxisOffset = if (reverseLayout) {
            mainAxisLayoutSize - offset - size
        } else {
            offset
        }
        var index = if (reverseLayout) this.placeables.lastIndex else 0
        while (if (reverseLayout) index >= 0 else index < this.placeables.size) {
            val it = this.placeables[index]
            val addIndex = if (reverseLayout) 0 else placeables.size
            val placeableOffset = if (orientation == Orientation.Vertical) {
                val x = requireNotNull(horizontalAlignment)
                    .align(it.width, layoutWidth, layoutDirection)
                IntOffset(x, mainAxisOffset)
            } else {
                val y = requireNotNull(verticalAlignment).align(it.height, layoutHeight)
                IntOffset(mainAxisOffset, y)
            }
            mainAxisOffset += if (orientation == Orientation.Vertical) it.height else it.width
            placeables.add(
                addIndex,
                PlaybackPagerItemPlaceable(placeableOffset, it),
            )
            if (reverseLayout) index-- else index++
        }
        return PlaybackPagerPositionedPage(
            offset = offset,
            index = this.index,
            size = this.size,
            key = key,
            orientation = orientation,
            innerPlaceables = placeables,
            visualOffset = visualOffset,
        )
    }
}