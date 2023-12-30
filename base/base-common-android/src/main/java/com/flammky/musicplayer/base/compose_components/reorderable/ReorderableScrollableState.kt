package dev.flammky.compose_components.reorderable

import androidx.compose.ui.unit.IntSize
import dev.flammky.compose_components.core.SnapshotRead
import kotlinx.coroutines.channels.Channel

abstract class ReorderableScrollableState <ScrollableItemInfo> internal constructor(

) : ReorderableState<ScrollableItemInfo>() {

    internal abstract val scrollChannel: Channel<Float>

    abstract val isVerticalScroll: Boolean
        @SnapshotRead get

    abstract val isHorizontalScroll: Boolean
        @SnapshotRead get

    abstract val reverseLayout: Boolean
        @SnapshotRead get

    abstract val viewportSizePx: IntSize
        @SnapshotRead get

    abstract val visibleItemsInfo: List<ScrollableItemInfo>
        @SnapshotRead get
}