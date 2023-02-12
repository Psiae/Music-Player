package dev.flammky.compose_components.reorderable

import android.util.Log
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.util.fastFirstOrNull
import com.flammky.musicplayer.base.compose_components.reorderable.ReorderResultHandle
import dev.flammky.compose_components.core.SnapshotRead
import kotlinx.coroutines.CancellationException

internal interface ReorderableLazyListApplier {

    val lazyLayoutModifiers: Modifier

    fun onLazyListScope(
        lazyListScope: LazyListScope,
        itemProvider: ReorderableLazyListItemProvider
    )
}

internal class RealReorderableLazyListApplier(
    private val state: ReorderableLazyListState
) : ReorderableLazyListApplier {

    private var _currentItemProvider by mutableStateOf<ReorderableLazyListItemProvider?>(null)

    private val pointerInputFilterModifier: Modifier = Modifier.pointerInput(Unit) {
        // for each possible gesture, install child drag listener
        forEachGesture {
            // await first DragStart, for each gesture we only accept the first emission
            state.childReorderStartChannel.receive()
                .let { dragStart ->
                    val slop = dragStart.slop
                    // received DragStart, create pointer event awaiter on this composable
                    awaitPointerEventScope {
                        // find the event to get the position in the parent
                        currentEvent.changes.fastFirstOrNull { pointerInputChange ->
                            pointerInputChange.id == dragStart.id
                        }?.takeIf { pointer ->
                            // check if the state allow the drag
                            state.onStartDrag(
                                pointer.id.value,
                                dragStart.composition,
                                pointer.position.x - slop.x,
                                pointer.position.y - slop.y,
                                slop.x,
                                slop.y,
                                expectKey = dragStart.selfKey,
                                expectIndex = dragStart.selfIndex
                            ).also {
                                Log.d("Reorderable_DEBUG", "state.onStartDrag(${dragStart.selfIndex}, ${dragStart.selfKey})=$it")
                            }
                        }?.let { _ ->
                            val lastDragId: PointerId = dragStart.id
                            var lastDragX: Float = dragStart.slop.x
                            var lastDragY: Float = dragStart.slop.y
                            val dragCompleted =
                                try {
                                    drag(dragStart.id) { onDrag ->
                                        // report each drag position change
                                        check(lastDragId == onDrag.id)
                                        lastDragX = onDrag.position.x
                                        lastDragY = onDrag.position.y
                                        val change = onDrag.positionChange()
                                        val accepted = state.onDrag(
                                            lastDragId.value,
                                            change.x,
                                            change.y,
                                            expectKey = dragStart.selfKey
                                        )
                                        if (!accepted) throw CancellationException()
                                    }
                                } catch (ce: CancellationException) {
                                    false
                                }
                            if (dragCompleted) {
                                // completed normally
                                state.onDragEnd(
                                    id = lastDragId.value,
                                    endX = lastDragX,
                                    endY = lastDragY,
                                    expectKey = dragStart.selfKey
                                )
                            } else {
                                // was cancelled
                                state.onDragCancelled(
                                    id = lastDragId.value,
                                    endX = lastDragX,
                                    endY = lastDragY,
                                    expectKey = dragStart.selfKey
                                )
                            }
                        }
                    }
                }
        }
    }

    override val lazyLayoutModifiers: Modifier = pointerInputFilterModifier

    override fun onLazyListScope(
        lazyListScope: LazyListScope,
        itemProvider: ReorderableLazyListItemProvider
    ) {
        _currentItemProvider = itemProvider.apply {
            provideToLayout(lazyListScope)
        }
    }

    fun onStartReorder(
        composition: InternalReorderableLazyListScope,
        from: ItemPosition
    ): Boolean {
        return _currentItemProvider?.onStartReorder(composition, from) ?: return false
    }

    fun onMove(
        composition: InternalReorderableLazyListScope,
        from: ItemPosition,
        new: ItemPosition
    ): Boolean {
        return _currentItemProvider?.onMove(composition, from, new) ?: return false
    }

    fun onEndReorder(
        composition: InternalReorderableLazyListScope,
        cancelled: Boolean,
        from: ItemPosition,
        to: ItemPosition
    ): ReorderResultHandle? {
        return _currentItemProvider?.onEndReorder(composition, cancelled, from, to)
    }

    @SnapshotRead
    fun indexOfKey(key: Any): Int = _currentItemProvider?.indexOfKey(key) ?: -1
}
