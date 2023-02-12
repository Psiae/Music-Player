package dev.flammky.compose_components.reorderable

import android.util.Log
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import com.flammky.musicplayer.base.compose_components.reorderable.ReorderResultHandle
import dev.flammky.compose_components.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

class ReorderableLazyListState internal constructor(
    val coroutineScope: CoroutineScope,
    val lazyListState: LazyListState,
    private val onDragStart: ((item: ItemPosition) -> Boolean)?,
    // should provide `cancelled` reason
    // should we provide a `handle` as an option to persist the mask ?
    private val onDragEnd: (
			(cancelled: Boolean,
			 from: ItemPosition,
			 to: ItemPosition,
			 handle: ReorderResultHandle) -> Unit
		)?,
    // should we handle the listing ?
    private val movable: ((from: ItemPosition, to: ItemPosition) -> Boolean)?,
    private val onMove: ((from: ItemPosition, to: ItemPosition) -> Boolean)?,
    private val dragCancelAnimation: DragCancelAnimation,
    private val maxScrollPerFramePx: Float
) : ReorderableScrollableState<LazyListItemInfo>() {

    private val _applier = RealReorderableLazyListApplier(this)

    // TODO: we can wrap these into an independent instance
    private var _draggingId: Long? = null
    private var _draggingItemStartSnapshot by mutableStateOf<LazyListItemInfo?>(null)
    private var _draggingItemLatestSnapshot by mutableStateOf<LazyListItemInfo?>(null)
    private var _draggingLatestScope by mutableStateOf<InternalReorderableLazyListScope?>(null)
    private var _expectDraggingItemCurrentIndex by mutableStateOf<Int?>(null)
    private var _draggingItemStartDownOffset = Offset.Zero
    private var _draggingItemStartDraggingOffset = Offset.Zero
    private var _draggingItemDeltaFromStart by mutableStateOf(Offset.Zero)
    private var _draggingItemDeltaFromCurrent by mutableStateOf(Offset.Zero)
    private var _draggingDropTarget: LazyListItemInfo? = null

    private var _autoScrollerJob: Job? = null

    internal val applier: ReorderableLazyListApplier = _applier
    internal val childReorderStartChannel: Channel<ReorderDragStart> = Channel()
    override val scrollChannel: Channel<Float> = Channel()

    override val isVerticalScroll: Boolean =
        lazyListState.layoutInfo.orientation == Orientation.Vertical

    override val isHorizontalScroll: Boolean =
        lazyListState.layoutInfo.orientation == Orientation.Horizontal

    override val visibleItemsInfo: List<LazyListItemInfo>
        @SnapshotRead
        get() = lazyListState.layoutInfo.visibleItemsInfo

    override val reverseLayout: Boolean
        @SnapshotRead
        get() = lazyListState.layoutInfo.reverseLayout

    override val viewportSizePx: IntSize
        @SnapshotRead
        get() = lazyListState.layoutInfo.viewportSize

    override val expectDraggingItemIndex: Int?
        @SnapshotRead
        get() = _expectDraggingItemCurrentIndex

    override val currentLayoutDraggingItemIndex: Int?
        @SnapshotRead
        get() = _draggingItemStartSnapshot?.let {
            _applier.indexOfKey(it.key).takeIf { i -> i != -1 }
        }

    override val draggingItemKey: Any?
        @SnapshotRead
        get() = _draggingItemStartSnapshot?.itemKey

    override val draggingItemPosition: ItemPosition?
        @SnapshotRead
        get() = _draggingItemStartSnapshot
            ?.let {
                ItemPosition(expectDraggingItemIndex ?: return@let null, it.itemKey)
            }

    override val draggingItemDelta: Offset
        @SnapshotRead
        get() = _draggingItemStartSnapshot?.let { snap ->
            visibleItemsInfo.fastFirstOrNull { visible ->
                visible.key == snap.key
            }?.let { inLayout ->
                if (isVerticalScroll) {
                    verticalOffset(snap.topPos + _draggingItemDeltaFromStart.y - inLayout.topPos)
                } else if (isHorizontalScroll) {
                    horizontalOffset(snap.leftPos + _draggingItemDeltaFromStart.x - inLayout.leftPos)
                } else exhaustedStateException()
            }
        } ?: Offset.Zero

    override val draggingItemLeftPos: Float?
        @SnapshotRead
        get() = _draggingItemStartSnapshot
            ?.let { draggingItem ->
                lazyListState.layoutInfo.visibleItemsInfo
                    .fastFirstOrNull {
                        it.key == draggingItem.key
                    }
                    ?.run { leftPos + _draggingItemDeltaFromStart.x }
            }

    override val draggingItemTopPos: Float?
        @SnapshotRead
        get() = _draggingItemStartSnapshot
            ?.let { draggingItem ->
                lazyListState.layoutInfo.visibleItemsInfo
                    .fastFirstOrNull {
                        it.key == draggingItem.key
                    }
                    ?.run { topPos + _draggingItemDeltaFromStart.y }
            }

    override val draggingItemRightPos: Float?
        @SnapshotRead
        get() = _draggingItemStartSnapshot
            ?.let { draggingItem ->
                lazyListState.layoutInfo.visibleItemsInfo
                    .fastFirstOrNull {
                        it.key == draggingItem.key
                    }
                    ?.run { rightPos + _draggingItemDeltaFromStart.x }
            }

    override val draggingItemBottomPos: Float?
        @SnapshotRead
        get() = _draggingItemStartSnapshot
            ?.let { draggingItem ->
                lazyListState.layoutInfo.visibleItemsInfo
                    .fastFirstOrNull {
                        it.key == draggingItem.key
                    }
                    ?.run { bottomPos + _draggingItemDeltaFromStart.y }
            }

    override val draggingItemLayoutPosition: LayoutPosition?
        @SnapshotRead
        get() = _draggingItemStartSnapshot
            ?.let { draggingItem ->
                lazyListState.layoutInfo.visibleItemsInfo
                    .fastFirstOrNull {
                        it.key == draggingItem.key
                    }
                    ?.run {
                        val delta = _draggingItemDeltaFromStart
                        LayoutPosition(
                            leftPos + delta.x,
                            topPos + delta.y,
                            rightPos + delta.x,
                            bottomPos + delta.y,
                        )
                    }
            }

    override val draggingItemLayoutInfo: LazyListItemInfo?
        @SnapshotRead
        get() = _draggingItemStartSnapshot
            ?.let { draggingItem ->
                lazyListState.layoutInfo.visibleItemsInfo
                    .fastFirstOrNull {  visibleItem ->
                        visibleItem.key == draggingItem.key
                    }
            }

    override val cancellingItemDelta: Offset
        @SnapshotRead
        get() = dragCancelAnimation.animatedOffset

    override val cancellingItemPosition: ItemPosition?
        @SnapshotRead
        get() = dragCancelAnimation.cancellingItemPosition

    override val LazyListItemInfo.layoutPositionInParent: LayoutPosition
        get() = LayoutPosition(
            leftPos.toFloat(),
            topPos.toFloat(),
            rightPos.toFloat(),
            bottomPos.toFloat(),
        )

    override val LazyListItemInfo.linearLayoutPositionInParent: LinearLayoutPosition
        get() = LinearLayoutPosition(
            startPos.toFloat(),
            endPos.toFloat()
        )

    override val draggingItemStartPos: Float?
        get() = if (isVerticalScroll) draggingItemTopPos else draggingItemLeftPos

    override val draggingItemEndPos: Float?
        get() = if (isVerticalScroll) draggingItemBottomPos else draggingItemRightPos

    override val firstVisibleItemIndex: Int
        @SnapshotRead
        get() = lazyListState.firstVisibleItemIndex

    override val firstVisibleItemScrollOffset: Int
        @SnapshotRead
        get() = lazyListState.firstVisibleItemScrollOffset

    override val viewportStartOffset: Int
        @SnapshotRead
        get() = lazyListState.layoutInfo.viewportStartOffset

    override val viewportEndOffset: Int
        @SnapshotRead
        get() = lazyListState.layoutInfo.viewportEndOffset

    override val LazyListItemInfo.itemIndex: Int
        get() = index

    override val LazyListItemInfo.itemKey: Any
        get() = key

    override val LazyListItemInfo.leftPos: Int
        @SnapshotRead
        get() = when {
            isVerticalScroll -> 0
            reverseLayout -> viewportSizePx.width - offset - size
            else -> offset
        }

    override val LazyListItemInfo.rightPos: Int
        @SnapshotRead
        get() = when {
            isVerticalScroll -> 0
            reverseLayout -> viewportSizePx.width - offset
            else -> offset + size
        }

    override val LazyListItemInfo.topPos: Int
        @SnapshotRead
        get() = when {
            !isVerticalScroll -> 0
            reverseLayout -> viewportSizePx.height - offset - size
            else -> offset
        }

    override val LazyListItemInfo.bottomPos: Int
        @SnapshotRead
        get() = when {
            !isVerticalScroll -> 0
            reverseLayout -> viewportSizePx.height - offset
            else -> offset + size
        }

    override val LazyListItemInfo.startPos: Int
        get() = if (isVerticalScroll) {
            if (reverseLayout)
                viewportSizePx.height - offset - size
            else offset
        } else {
            if (reverseLayout)
                viewportSizePx.width - offset - size
            else
                offset
        }

    override val LazyListItemInfo.endPos: Int
        get() = if (isVerticalScroll) {
            if (reverseLayout)
                viewportSizePx.height - offset
            else
                offset + size
        } else {
            if (reverseLayout)
                viewportSizePx.width - offset
            else
                offset + size
        }

    override val LazyListItemInfo.height: Int
        get() = if (isVerticalScroll) {
            size
        } else {
            0
        }

    override val LazyListItemInfo.width: Int
        get() = if (isVerticalScroll) {
            size
        } else {
            0
        }

    /**
     * @see ReorderableState.onStartDrag
     */
    override fun onStartDrag(
        id: Long,
        startComposition: InternalReorderableLazyListScope,
        startX: Float,
        startY: Float,
        startSlopX: Float,
        startSlopY: Float,
        expectKey: Any,
        expectIndex: Int
    ): Boolean {
        internalReorderableStateCheck(_draggingId == null) {
            "Unexpected Dragging ID during onStartDrag, " +
                    "expect=${null}, actual=$_draggingId, inMainLooper=${inMainLooper()}"
        }
        val x: Float
        val y: Float
        // consider the viewport offset of the scroll axis (Content Padding)
        if (isVerticalScroll) {
            Log.d("Reorderable_DEBUG", "$reverseLayout")
            x = 0f
            y = if (reverseLayout) {
                -viewportStartOffset
            } else {
                viewportStartOffset
            } + startY
        } else {
            x = if (reverseLayout) {
                -viewportStartOffset
            } else {
                viewportStartOffset
            } + startX
            y = 0f
        }
        // find the dragged Item according to the Drag input position
        return visibleItemsInfo
            .fastFirstOrNull {
                x.toInt() in it.leftPos..it.rightPos && y.toInt() in it.topPos..it.bottomPos
            }
            ?.takeIf { itemInfo ->
                itemInfo.key == expectKey &&
                itemInfo.index == expectIndex &&
                _applier.onStartReorder(startComposition, ItemPosition(itemInfo.index, itemInfo.key)) &&
                onDragStart?.invoke(ItemPosition(itemInfo.itemIndex, itemInfo.itemKey)) != false
            }
            ?.let { itemInfo ->
                _draggingId = id
                _draggingLatestScope = startComposition
                _draggingItemStartSnapshot = itemInfo
                _expectDraggingItemCurrentIndex = itemInfo.index
                _draggingItemStartDraggingOffset = Offset(x, y)
                _draggingItemStartDownOffset = Offset(x - startSlopX, y - startSlopY)
                _draggingItemDeltaFromStart = Offset(startSlopX, startSlopY)
            } != null
    }

    override fun onDrag(
        id: Long,
        dragX: Float,
        dragY: Float,
        expectKey: Any
    ): Boolean {
        internalReorderableStateCheck(id == _draggingId) {
            "Unexpected Dragging ID during onDrag, " +
                "expect=$id, actual=$_draggingId, inMainLooper=${inMainLooper()}"
        }
        val snap = _draggingItemStartSnapshot
            ?: return false
        internalReorderableStateCheck(expectKey == snap.key) {
            "Unexpected Item Key during onDrag, " +
                    "expect=${expectKey}, actual=${snap.key}, inMainLooper=${inMainLooper()}"
        }
        val draggingInfo = lazyListState.layoutInfo.visibleItemsInfo
            .fastFirstOrNull {  visibleItem ->
                visibleItem.key == snap.key
            }
            ?: return false
        val dragDelta =
            if (isVerticalScroll) {
                verticalOffset(_draggingItemDeltaFromStart.y + dragY)
            } else {
                horizontalOffset(_draggingItemDeltaFromStart.x + dragX)
            }.also {
                _draggingItemDeltaFromStart = it
            }
        val checkMoveAllow = checkShouldMoveToTarget(
            deltaX = dragDelta.x.toInt(),
            deltaY = dragDelta.y.toInt(),
            snap = snap,
            draggingInfo = draggingInfo
        )
        if (!checkMoveAllow) {
            return false
        }
        val scrollAllow = checkOnDragOverscroll(
            id, expectKey, draggingInfo, dragDelta
        )
        if (!scrollAllow) {
            return false
        }
        return true
    }

    private fun checkOnDragOverscroll(
        dragId: Long,
        key: Any,
        draggingItemInfo: LazyListItemInfo,
        draggingDelta: Offset
    ): Boolean {
        autoscroll(
            dragId, key,
            interpolateAutoScrollOffset(
                draggingItemInfo.endPos - draggingItemInfo.startPos,
                calculateOverscrollOffset(draggingDelta),
                0,
                maxScrollPerFramePx
            )
        )
        return true
    }

    private fun calculateOverscrollOffset(draggingDelta: Offset): Float {
        val delta =
            if (isVerticalScroll) {
                draggingDelta.y
            } else {
                draggingDelta.x
            }
        return when {
            delta < 0 -> {
                val draggingOffset =
                    if (isVerticalScroll)
                        _draggingItemStartSnapshot!!.topPos + delta
                    else
                        _draggingItemStartSnapshot!!.leftPos + delta
                (draggingOffset - viewportStartOffset).coerceAtMost(0f)
            }
            delta > 0 -> {
                val draggingOffset =
                    if (isVerticalScroll)
                        _draggingItemStartSnapshot!!.bottomPos + delta
                    else
                        _draggingItemStartSnapshot!!.rightPos + delta
                (draggingOffset - viewportEndOffset).coerceAtLeast(0f)
            }
            else -> 0f
        }
    }

    private fun calculateCurrentAutoScrollOffset(
        frameTimeMillis: Long,
        maxScrollPx: Float,
    ): Float {
        val (size: Int, outSize: Float) = _draggingItemStartSnapshot
            ?.let { itemInfo ->
                itemInfo.endPos - itemInfo.startPos to calculateOverscrollOffset(_draggingItemDeltaFromStart)
            }
            ?: return 0f
        return interpolateAutoScrollOffset(size, outSize, frameTimeMillis, maxScrollPx)
    }

    private fun interpolateAutoScrollOffset(
        viewLength: Int,
        viewOutOfBoundsOffset: Float,
        frameTimeMillis: Long,
        maxScrollPx: Float,
    ): Float {
        if (viewOutOfBoundsOffset == 0f) return 0f
        val outOfBoundsRatio = (1f * abs(viewOutOfBoundsOffset) / viewLength)
        val timeRatio = (frameTimeMillis.toFloat() / 1500)
        val accel = (timeRatio.coerceAtMost(1f).pow(5))
        val calc = sign(viewOutOfBoundsOffset) * maxScrollPx * run {
            val t = 1 - outOfBoundsRatio.coerceAtMost(1f)
            1 - t * t * t * t
        } * accel
        return calc.takeIf { it != 0f } ?: if (viewOutOfBoundsOffset > 0) 1f else -1f
    }

    private var latestAutoScrollId: Long? = null
    private var latestAutoScrollKey: Any? = null
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun autoscroll(id: Long, key: Any, scrollOffset: Float) {
        if (scrollOffset != 0f) {
            if (_autoScrollerJob?.isActive == true) {
                return
            }
            _autoScrollerJob = coroutineScope.launch {
                latestAutoScrollId = id
                latestAutoScrollKey = key
                val scroller = launch {
                    var scroll = scrollOffset
                    var startMs = 0L
                    while (scroll != 0f && _autoScrollerJob?.isActive == true) {
                        withFrameMillis { frameMs ->
                            if (startMs == 0L) {
                                startMs = frameMs
                            } else {
                                scroll = calculateCurrentAutoScrollOffset(
                                    frameMs - startMs,
                                    maxScrollPerFramePx
                                )
                            }
                        }
                        lazyListState.scrollBy(if (reverseLayout) -scroll else scroll)
                    }
                }
                val toDragApplier = launch {
                    snapshotFlow { expectDraggingItemIndex != null }
                        .flatMapLatest { if (it) snapshotFlow { visibleItemsInfo } else flowOf(null) }
                        .filterNotNull()
                        .distinctUntilChanged { old, new ->
                            old.firstOrNull()?.index == new.firstOrNull()?.index && old.count() == new.count()
                        }
                        .collect {
                            onDrag(latestAutoScrollId!!, 0f, 0f, latestAutoScrollKey!!)
                        }
                }
                scroller.join()
                toDragApplier.join()
            }
        } else {
            _autoScrollerJob?.cancel()
            _autoScrollerJob = null
        }
    }

    override fun onDragEnd(id: Long, endX: Float, endY: Float, expectKey: Any) {
        dragEnded(false, id, endX, endY, expectKey)
    }

    override fun onDragCancelled(id: Long, endX: Float, endY: Float, expectKey: Any) {
        dragEnded(true, id, endX, endY, expectKey)
    }

    private fun dragEnded(
        cancelled: Boolean,
        id: Long,
        endX: Float,
        endY: Float,
        expectKey: Any
    ) {
        internalReorderableStateCheck(id == _draggingId) {
            "Inconsistent Dragging ID during onDragCancelled, " +
                    "expect=$id, actual=$_draggingId, inMainLooper=${inMainLooper()}"
        }
        internalReorderableStateCheck(expectKey == _draggingItemStartSnapshot?.key) {
            "Unexpected Expect Key during onDragCancelled, " +
                    "expect=$expectKey, actual=${_draggingItemStartSnapshot?.key}, inMainLooper=${inMainLooper()}"
        }
        val endIndex = _expectDraggingItemCurrentIndex!!
        val endDelta = _draggingItemDeltaFromStart
        val startSnap = _draggingItemStartSnapshot!!
        val startSnapLayoutPosition = startSnap.layoutPositionInParent
        val endOffset = draggingItemLayoutInfo?.layoutPositionInParent
            ?.let {
                Offset(startSnapLayoutPosition.left, startSnapLayoutPosition.top) + endDelta - Offset(it.left, it.top)
            }
            ?: Offset.Zero
        coroutineScope.launch {
            dragCancelAnimation.dragCancelled(
                ItemPosition(endIndex, startSnap.key),
                endOffset
            )
        }
        val handle = _applier.onEndReorder(
            _draggingLatestScope!!,
            cancelled,
            ItemPosition(startSnap.index, startSnap.key),
            ItemPosition(endIndex, startSnap.key)
        )
        this.onDragEnd?.invoke(
            cancelled,
            ItemPosition(startSnap.index, startSnap.key),
            ItemPosition(endIndex, _draggingLatestScope!!.itemOfIndex(endIndex)!!.key),
						handle ?: object : ReorderResultHandle { override fun done() = Unit }
        )
        _draggingId = null
        _draggingItemStartSnapshot = null
        _expectDraggingItemCurrentIndex = null
        _draggingItemStartDraggingOffset = Offset.Zero
        _draggingItemDeltaFromStart = Offset.Zero
        _draggingDropTarget = null
        _autoScrollerJob?.cancel()
    }

    private fun checkShouldMoveToTarget(
        deltaX: Int,
        deltaY: Int,
        snap: LazyListItemInfo,
        draggingInfo: LazyListItemInfo
    ): Boolean {
        if (deltaX == 0 && deltaY == 0) {
            return true
        }
        _draggingDropTarget = null
        // target properties
        val draggingLeftPos: Int
        val draggingTopPos: Int
        val draggingRightPos: Int
        val draggingBottomPos: Int
        val draggingStartPos: Int
        val draggingEndPos: Int
        val draggingCenterPos: Int
        if (isVerticalScroll) {
            draggingLeftPos = 0
            draggingTopPos = snap.topPos + deltaY
            draggingRightPos = 0
            draggingBottomPos = snap.bottomPos + deltaY
            draggingStartPos = draggingTopPos
            draggingEndPos = draggingBottomPos
            draggingCenterPos = (draggingStartPos + draggingEndPos) / 2
        } else if (isHorizontalScroll) {
            draggingLeftPos = snap.leftPos + deltaX
            draggingTopPos = 0
            draggingRightPos = snap.rightPos + deltaX
            draggingBottomPos = 0
            draggingStartPos = draggingLeftPos
            draggingEndPos = draggingRightPos
            draggingCenterPos = (draggingStartPos + draggingEndPos) / 2
        } else exhaustedStateException()

        var toEnd: Boolean? = null
        run {
            // we can improve this
            visibleItemsInfo.fastForEach { visibleItem ->
                if (visibleItem.itemIndex == draggingInfo.itemIndex) {
                    return@fastForEach
                }
                val visibleItemPosition = visibleItem.linearLayoutPositionInParent
                if (
                    draggingStartPos > visibleItemPosition.end ||
                    draggingEndPos < visibleItemPosition.start
                ) {
                    return@fastForEach
                }
                if (
                    movable?.invoke(
                        ItemPosition(expectDraggingItemIndex!!, draggingInfo.key),
                        ItemPosition(visibleItem.index, visibleItem.key)
                    ) != false
                ) {
                    _draggingDropTarget = visibleItem
                    toEnd = visibleItem.index > expectDraggingItemIndex!!
                    if (reverseLayout) toEnd = !toEnd!!
                }
                if (visibleItem.index > expectDraggingItemIndex!!) {
                    return@run
                }
            }
        }
        _draggingDropTarget?.takeIf { target ->
            val targetCenterPos = (target.startPos + target.endPos) / 2
            if (toEnd!!) draggingCenterPos > targetCenterPos else draggingCenterPos < targetCenterPos
        }?.let { target ->
            val fromPosition = ItemPosition(
                expectDraggingItemIndex!!,
                draggingInfo.key
            )
            val toPosition = ItemPosition(
                target.index,
                target.key
            )
            if (onMove?.invoke(fromPosition, toPosition) == false) {
                return false
            }
            if (!_applier.onMove(_draggingLatestScope!!, fromPosition, toPosition)) {
                return false
            }
            if (
                fromPosition.index == lazyListState.firstVisibleItemIndex ||
                toPosition.index == lazyListState.firstVisibleItemIndex
            ) {
                coroutineScope.launch {
                    lazyListState.scrollToItem(firstVisibleItemIndex, firstVisibleItemScrollOffset)
                }
            }
            _expectDraggingItemCurrentIndex = target.index
        }
        return true
    }
}

@Composable
fun rememberReorderableLazyListState(
    lazyListState: LazyListState,
    onMove: ((/*dragId: Int,*/ from: ItemPosition, to: ItemPosition) -> Boolean)
) = rememberReorderableLazyListState(
    lazyListState = lazyListState,
    onDragStart = null,
    onDragEnd = null,
    movable = null,
    onMove = onMove
)

@Composable
fun rememberReorderableLazyListState(
	lazyListState: LazyListState,
	onMove: ((/*dragId: Int,*/ from: ItemPosition, to: ItemPosition) -> Boolean)? = null,
	onDragStart: ((/*dragId: Int,*/ item: ItemPosition) -> Boolean)? = null,
	onDragEnd: ((/*dragId: Int,*/ cancelled: Boolean, from: ItemPosition, to: ItemPosition, handle: ReorderResultHandle) -> Unit)? = null,
	movable: ((/*dragId: Int,*/ item: ItemPosition, dragging: ItemPosition) -> Boolean)? = null,
	dragCancelAnimation: DragCancelAnimation = DragCancelAnimation(
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = Offset.VisibilityThreshold
        )
    ),
	maxScrollPerFrame: Dp = 30.dp
): ReorderableLazyListState {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    return remember(lazyListState) {
        ReorderableLazyListState(
            coroutineScope = coroutineScope,
            lazyListState = lazyListState,
            onDragStart = onDragStart,
            onDragEnd = onDragEnd,
            movable = movable,
            onMove = onMove,
            dragCancelAnimation = dragCancelAnimation,
            maxScrollPerFramePx = with(density) { maxScrollPerFrame.toPx() }
        )
    }
}
