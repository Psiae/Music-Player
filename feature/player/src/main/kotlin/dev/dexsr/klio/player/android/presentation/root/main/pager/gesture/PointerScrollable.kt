package dev.dexsr.klio.player.android.presentation.root.main.pager.gesture

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import dev.dexsr.klio.player.android.presentation.root.main.pager.PlaybackPagerScrollableState
import dev.dexsr.klio.player.android.presentation.root.main.pager.overscroll.PlaybackPagerOverscrollEffect
import kotlinx.coroutines.CoroutineScope

fun Modifier.pointerScrollable(
    state: PlaybackPagerScrollableState,
    orientation: Orientation,
    overscrollEffect: PlaybackPagerOverscrollEffect
): Modifier = composed {

    val upOverscrollEffect = rememberUpdatedState(newValue = overscrollEffect)

    val dragScrollLogic = remember(state) {
        PlaybackPagerDragScrollLogic(state, upOverscrollEffect)
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

    // not ready yet
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
        // ignore, if any
        return
    }
}

private class PlaybackPagerDragScrollLogic(
    private val scrollableState: PlaybackPagerScrollableState,
    private val latestOverscrollEffect: State<PlaybackPagerOverscrollEffect>
) {

    private var performFlingKey: Any? = null

    fun shouldScrollImmediately(): Boolean {
        return scrollableState.allowUserGestureInterruptScroll && scrollableState.isScrollInProgress
    }

    suspend fun CoroutineScope.onDragStarted(startedPosition: Offset) {
        // no-op
    }

    suspend fun CoroutineScope.onDragStopped(velocity: Float) {
        performFlingKey?.let {
            scrollableState.performFling(key = it, velocity, OverscrollEffectDelegate())
        }
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

    inner class OverscrollEffectDelegate(): PlaybackPagerOverscrollEffect {

        override fun applyToScroll(
            delta: Offset,
            source: NestedScrollSource,
            performScroll: (Offset) -> Offset
        ): Offset {
            return latestOverscrollEffect.value.applyToScroll(delta, source, performScroll)
        }

        override suspend fun applyToFling(
            velocity: Velocity,
            performFling: suspend (Velocity) -> Velocity
        ) {
            return latestOverscrollEffect.value.applyToFling(velocity, performFling)
        }

        override val isInProgress: Boolean
            get() = latestOverscrollEffect.value.isInProgress

        override val effectModifier: Modifier
            get() = error("NO OP")
    }
}