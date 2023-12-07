package dev.dexsr.klio.player.android.presentation.root.main.pager.gesture

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import dev.dexsr.klio.player.android.presentation.root.main.pager.PlaybackPagerScrollableState
import kotlinx.coroutines.CoroutineScope

fun Modifier.pointerScrollable(
    state: PlaybackPagerScrollableState,
    orientation: Orientation
): Modifier = composed {

    val dragScrollLogic = remember(state) {
        PlaybackPagerDragScrollLogic(state)
    }

    val draggableState = remember(dragScrollLogic) {
        PlaybackPagerDraggableState(dragScrollLogic)
    }

    this
        .draggable(
            state = draggableState,
            orientation = orientation,
            enabled = true,
            interactionSource = null,
            startDragImmediately = dragScrollLogic.shouldScrollImmediately(),
            onDragStarted = { position ->
                with(dragScrollLogic) { onDragStarted(position) }
            },
            onDragStopped = { velocity ->
                with(dragScrollLogic) { onDragStopped(velocity) }
            },
            reverseDirection = false
        )

    Modifier
}

private class PlaybackPagerDraggableState(
    private val scrollable: PlaybackPagerDragScrollLogic
): DraggableState {

    override suspend fun drag(
        dragPriority: MutatePriority,
        block: suspend DragScope.() -> Unit
    ) {
        if (dragPriority != MutatePriority.UserInput) return
        scrollable.userScroll(block)
    }

    override fun dispatchRawDelta(delta: Float) {
        // ignore
        return
    }
}

private class PlaybackPagerDragScrollLogic(
    private val scrollableState: PlaybackPagerScrollableState
) {

    fun shouldScrollImmediately(): Boolean {
        return scrollableState.allowUserGestureInterruptScroll && scrollableState.isScrollInProgress
    }

    suspend fun CoroutineScope.onDragStarted(startedPosition: Offset) {
        // no-op
    }

    suspend fun CoroutineScope.onDragStopped(velocity: Float) {
        scrollableState.performFling(velocity)
    }

    suspend fun userScroll(
        block: suspend DragScope.() -> Unit
    ) {
        scrollableState.userDragScroll {
            object : DragScope {
                override fun dragBy(pixels: Float) {
                    scrollBy(pixels)
                }
            }.block()
        }
    }
}