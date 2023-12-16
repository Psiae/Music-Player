package dev.dexsr.klio.player.android.presentation.root.main.pager

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastForEach

class PlaybackPagerPositionedPage(
    index: Int,
    offset: Int,
    size: Int,
    val key: Any,
    private val innerPlaceables: List<PlaybackPagerItemPlaceable>,
    val orientation: Orientation,
    val visualOffset: IntOffset
): PlaybackPagerPageInfo(index, offset, size) {

    fun place(
        scope: Placeable.PlacementScope,
    ) = with(scope) {
        innerPlaceables.fastForEach { placeable ->

            // we decide RTL behavior later
            placeable.rawPlaceable.placeWithLayer(position = placeable.offset + visualOffset)
        }
    }
}

class PlaybackPagerItemPlaceable(
    val offset: IntOffset,
    val rawPlaceable: Placeable,
)