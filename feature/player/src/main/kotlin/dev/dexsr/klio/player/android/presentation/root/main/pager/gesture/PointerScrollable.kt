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
import timber.log.Timber

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
        // TODO: check DragGestureDetector.kt
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
                with(dragScrollLogic) { onDragStopped(velocity, orientation) }
            },
            reverseDirection = false
        )
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
    // 02:32:14.940
    suspend fun CoroutineScope.onDragStopped(velocity: Float, orientation: Orientation) {
        Timber.d("PlaybackPagerPointerScrollable_DEBUG: onDragStopped(velocity=$velocity, key=$performFlingKey)")
        performFlingKey?.let {
            scrollableState.performFling(
                key = it,
                velocity = Velocity(
                    x = if (orientation == Orientation.Horizontal) velocity else 0f,
                    y = if (orientation == Orientation.Vertical) velocity else 0f
                ),
                OverscrollEffectDelegate()
            )
        }
    }

    suspend fun userScroll(
        block: suspend DragScope.() -> Unit
    ) {
        Timber.d("PlaybackPagerPointerScrollable_DEBUG: userScroll()")
        scrollableState
            .userDragScroll(OverscrollEffectDelegate()) {
                object : DragScope {
                    override fun dragBy(pixels: Float) {
                        scrollBy(pixels)
                    }
                }.block()
            }.also { performFlingKey = it }
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