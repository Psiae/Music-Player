package dev.flammky.compose_components.reorderable

import androidx.compose.ui.geometry.Offset
import dev.flammky.compose_components.core.LayoutPosition
import dev.flammky.compose_components.core.LinearLayoutPosition
import dev.flammky.compose_components.core.SnapshotRead

abstract class ReorderableState <ItemInfo> internal constructor() {

    /**
     * The expected index of the currently dragging Item,
     * does not represent the actual index on the layout
     *
     * State Change within the getter will notify SnapshotObserver that reads it
     */
    abstract val expectDraggingItemIndex: Int?
        @SnapshotRead
        get

    /**
     * The last known index of the currently dragging Item in the layout scope,
     * does not represent the actual index on the layout
     *
     * State Change within the getter will notify SnapshotObserver that reads it
     */
    abstract val currentLayoutDraggingItemIndex: Int?
        @SnapshotRead
        get

    /**
     * The Key of the currently dragging Item or null if none is dragged.
     * State Change within the getter will notify SnapshotObserver that reads it
     *
     */
    abstract val draggingItemKey: Any?
        @SnapshotRead
        get

    abstract val draggingItemPosition: ItemPosition?
        @SnapshotRead
        get

    abstract val draggingItemDelta: Offset
        @SnapshotRead
        get

    abstract val draggingItemLeftPos: Float?
        @SnapshotRead
        get

    abstract val draggingItemTopPos: Float?
        @SnapshotRead
        get

    abstract val draggingItemRightPos: Float?
        @SnapshotRead
        get

    abstract val draggingItemBottomPos: Float?
        @SnapshotRead
        get

    abstract val draggingItemStartPos: Float?
        @SnapshotRead
        get

    abstract val draggingItemEndPos: Float?
        @SnapshotRead
        get

    abstract val draggingItemLayoutPosition: LayoutPosition?
        @SnapshotRead
        get

    abstract val draggingItemLayoutInfo: ItemInfo?
        @SnapshotRead
        get

    abstract val cancellingItemPosition: ItemPosition?
        @SnapshotRead
        get

    abstract val cancellingItemDelta: Offset
        @SnapshotRead
        get

    protected abstract val firstVisibleItemIndex: Int
        @SnapshotRead
        get

    protected abstract val firstVisibleItemScrollOffset: Int
        @SnapshotRead
        get

    protected abstract val viewportStartOffset: Int
        @SnapshotRead
        get

    protected abstract val viewportEndOffset: Int
        @SnapshotRead
        get

    /**
     * The Index of the Layout Item within the Reorderable Scope
     */
    protected abstract val ItemInfo.itemIndex: Int

    /**
     * The Key of the Layout Item within the Reorderable Scope
     */
    protected abstract val ItemInfo.itemKey: Any

    //
    // Consider start and end variant ?
    //

    /**
     * The Left position of the Item relative to the Parent Layout ViewPort
     */
    protected abstract val ItemInfo.leftPos: Int
        @SnapshotRead
        get

    /**
     * The Right position of the Item relative to the Parent Layout Viewport
     */
    protected abstract val ItemInfo.rightPos: Int
        @SnapshotRead
        get


    /**
     * The Top position of the Item relative to the Parent Layout Viewport
     */
    protected abstract val ItemInfo.topPos: Int
        @SnapshotRead
        get

    /**
     * The Bottom position of the Item relative to the Parent Layout Viewport
     */
    protected abstract val ItemInfo.bottomPos: Int
        @SnapshotRead
        get

    /**
     * The Start position of the Item relative to the Parent Layout Viewport,
     * relative to orientation
     */
    protected abstract val ItemInfo.startPos: Int
        @SnapshotRead
        get

    /**
     * The End position of the Item relative to the Parent Layout Viewport,
     * relative to orientation
     */
    protected abstract val ItemInfo.endPos: Int
        @SnapshotRead
        get

    /**
     * The End position of the Item relative to the Parent Layout Viewport,
     * relative to orientation
     */
    protected abstract val ItemInfo.layoutPositionInParent: LayoutPosition
        @SnapshotRead
        get

    /**
     * The End position of the Item relative to the Parent Layout Viewport,
     * relative to orientation
     */
    protected abstract val ItemInfo.linearLayoutPositionInParent: LinearLayoutPosition
        @SnapshotRead
        get

    /**
     * The Height of the Item,
     */
    protected abstract val ItemInfo.height: Int
        @SnapshotRead
        get

    /**
     * The Width of the Item,
     */
    protected abstract val ItemInfo.width: Int
        @SnapshotRead
        get

    /**
     * onStartDrag event
     *
     * @param id the ID of the event
     * **
     * internal for test purposes, public if necessary
     * **
     *
     * @param startX the `x` axis position of this drag (0 is left unless reversed)
     * @param startY the `y` axis position of this drag (0 is top unless reversed)
     */
    internal abstract fun onStartDrag(
        id: Long,
        startComposition: InternalReorderableLazyListScope,
        startX: Float,
        startY: Float,
        startSlopX: Float,
        startSlopY: Float,
        expectKey: Any,
        expectIndex: Int
    ): Boolean

    /**
     * onDrag event
     *
     * @param id the ID of the start event, internal for test purposes
     * **
     * internal for test purposes, public if necessary
     * **
     *
     * @param dragX the `x` axis position of this drag (0 is left unless reversed)
     * @param dragY the `y` axis position of this drag (0 is top unless reversed)
     */
    internal abstract fun onDrag(
        id: Long,
        dragX: Float,
        dragY: Float,
        expectKey: Any
    ): Boolean

    /**
     * onDragEnd event
     *
     * @param id the ID of the start event, internal for test purposes
     * **
     * internal for test purposes, public if necessary
     * **
     * @param endX the `x` axis position of this drag (0 is left unless reversed)
     * @param endY the `y` axis position of this drag (0 is top unless reversed)
     */
    internal abstract fun onDragEnd(
        id: Long,
        endX: Float,
        endY: Float,
        expectKey: Any
    )

    /**
     * onDragCancelled event
     *
     * @param id the ID of the start event, internal for test purposes
     * **
     * internal for test purposes, public if necessary
     * **
     * @param endX the `x` axis position of this drag (0 is left unless reversed)
     * @param endY the `y` axis position of this drag (0 is top unless reversed)
     */
    internal abstract fun onDragCancelled(
        id: Long,
        endX: Float,
        endY: Float,
        expectKey: Any
    )
}