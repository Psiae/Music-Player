package dev.dexsr.klio.player.android.presentation.root.main.pager

import androidx.compose.animation.SplineBasedFloatDecayAnimationSpec
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import dev.dexsr.klio.android.base.checkInMainLooper
import dev.dexsr.klio.player.android.presentation.root.main.PlaybackPagerController
import dev.dexsr.klio.player.android.presentation.root.main.pager.overscroll.PlaybackPagerOverscrollEffect
import dev.dexsr.klio.player.android.presentation.root.main.pager.scroll.PlaybackPagerFlingBehavior
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.math.sign

class PlaybackPagerScrollableState(
    // maybe: scroll connection
    private val pagerController: PlaybackPagerController,
) : PlaybackPagerGestureScrollableState {

    private val coroutineScope = CoroutineScope(SupervisorJob())

    @OptIn(ExperimentalFoundationApi::class)
    private val flingBehaviorFactory = { density: Density ->
        PlaybackPagerFlingBehavior(
            pagerState = pagerController,
            density = density,
            snapAnimationSpec = SpringSpec<Float>(
                Spring.DampingRatioNoBouncy,
                Spring.StiffnessMedium,
                null
            ),
            lowVelocityAnimationSpec = TweenSpec<Float>(
                500,
                0,
                LinearEasing
            ),
            highVelocityAnimationSpec = SplineBasedFloatDecayAnimationSpec(density)
                .generateDecayAnimationSpec<Float>()
        )
    }

    private val ignoreOngoingUserScroll: Boolean
        get() = pagerController.correctingTimeline

    private var currentDrag: UserDragScroll? = null
    private var latestDrag: UserDragScroll? = null

    val isScrollInProgress
        get() = pagerController.isScrollInProgress

    val allowUserGestureInterruptScroll: Boolean
        get() = true

    val scrollableOrientation = mutableStateOf(Orientation.Horizontal)

    override suspend fun bringChildFocusScroll(scroll: suspend PlaybackPagerScrollScope.() -> Unit) {

    }

    override suspend fun userDragScroll(
        overscrollEffect: PlaybackPagerOverscrollEffect,
        scroll: suspend PlaybackPagerScrollScope.() -> Unit
    ): Any {
        checkInMainLooper()
        currentDrag?.newDrag()
        val drag = UserDragScroll().apply {
            initDrag()
        }
        currentDrag = drag
        latestDrag = drag
        val dragConnection = pagerController.newUserDragScroll()
        var key: Any? = null
        try {
            object : PlaybackPagerScrollScope {
                override fun scrollBy(pixels: Float): Float {
                    drag.ensureDragActive()
                    // call to newUserDragScroll should make this to false, so it's actually ongoing
                    if (dragConnection.ignoreUserDrag()) return 0f
                    val dragDelta = pixels.toScrollAxisOffset()

                    val performScroll: (Offset) -> Offset = { delta ->
                        // Consume on a single axis
                        val axisConsumed = pagerController
                            .userPerformDragScroll(
                                // reverse to scroll delta
                                -delta.takeScrollAxisAsFloat(),
                                dragConnection
                            )
                            // reverse to drag delta
                            .unaryMinus()
                            .toScrollAxisOffset()

                        axisConsumed
                    }

                    return overscrollEffect
                        .applyToScroll(delta = dragDelta, NestedScrollSource.Drag, performScroll)
                        .takeScrollAxisAsFloat()
                }
            }.run {
                scroll()
            }
            drag.dragEnd(
                null,
                dragConnection.dragEnd()
            )?.let { key = it }
        } catch (ex: Exception) {
            drag.dragEnd(
                ex as? CancellationException ?: CancellationException(null, ex),
                dragConnection.dragEnd()
            )?.let { key = it }
            throw ex
        } finally {
            check(drag.dragEnded)
            currentDrag = null
            pagerController.userDragScrollEnd(dragConnection)
        }
        return (key ?: Unit)
    }

    override suspend fun performFling(
        key: Any,
        velocity: Velocity,
        overscrollEffect: PlaybackPagerOverscrollEffect
    ) {
        checkInMainLooper()
        val drag = latestDrag
        val consume = drag?.startFling(key)
        Timber.d("PlaybackPagerScrollableState_DEBUG: performFLing(velocity=$velocity, consume=$consume)")
        if (consume != true) return
        val flingConnection = pagerController.newUserDragFlingScroll(key)
        if (flingConnection?.isActive != true) {
            // flingConnection is closed, we can early return
            // maybe: convey reason
            drag.flingEnd(CancellationException("flingConnection already closed"))
            flingConnection?.let {
                it.flingEnd()
                pagerController.userDragFlingScrollEnd(it)
            }
            return
        }
        coroutineScope.launch(AndroidUiDispatcher.Main) {
            try {
                val scrollAxisVelocity = velocity.takeScrollAxisAsFloat()
                val expectedVelocitySign = flingConnection.expectedDragVelocitySign()
                if (
                    scrollAxisVelocity == 0f ||
                    expectedVelocitySign == null ||
                    expectedVelocitySign == sign(scrollAxisVelocity).toInt()
                ) {
                    doPerformFling(velocity, overscrollEffect, drag, flingConnection)
                    drag.flingEnd()
                    flingConnection.flingEnd()
                } else {
                    // seems like the velocity tracker is giving reversed result
                    // let the overscrollEffect absorb it regardless
                    overscrollEffect.applyToFling(velocity) { it }
                    throw CancellationException("unexpected post-drag velocity, velocity=$scrollAxisVelocity, expectedSign=${flingConnection.expectedDragVelocitySign()}")
                }
            } catch (ex: Exception) {
                Timber.d("PlaybackPagerScrollableState_DEBUG: performFLing_ex(ex=$ex)")
                drag.flingEnd(ex as? CancellationException ?: CancellationException("exception during fling"))
                flingConnection.flingEnd()
                flingConnection.cancel()
                throw ex
            } finally {
                pagerController.userDragFlingScrollEnd(flingConnection)
            }
        }
    }

    private suspend fun doPerformFling(
        initialVelocity: Velocity,
        overscrollEffect: PlaybackPagerOverscrollEffect,
        drag: UserDragScroll,
        controllerConnection: PlaybackPagerController.UserDragFlingInstance
    ) {
        val dragVelocity = initialVelocity.takeScrollAxis()

        val performFling: suspend (Velocity) -> Velocity = { postOverscrollVelocity ->
            val postFlingVelocity = doFlingScroll(postOverscrollVelocity, overscrollEffect, drag, controllerConnection)
            postOverscrollVelocity - postFlingVelocity
        }

        overscrollEffect.applyToFling(dragVelocity, performFling)
    }

    private suspend fun doFlingScroll(
        velocity: Velocity,
        overscrollEffect: PlaybackPagerOverscrollEffect,
        drag: UserDragScroll,
        connection: PlaybackPagerController.UserDragFlingInstance
    ): Velocity {
        var result = velocity

        val scope = object : ScrollScope {

            override fun scrollBy(pixels: Float): Float {
                // reverse to drag delta
                val dragDelta = pixels.unaryMinus().toScrollAxisOffset()

                val performScroll: (Offset) -> Offset = { postOverscrollDragDelta ->
                    drag.ensureFlingActive()

                    // Consume on a single axis
                    val axisConsumed = pagerController
                        .performFlingScroll(
                            postOverscrollDragDelta
                                .takeScrollAxisAsFloat()
                                // reverse to scroll delta
                                .unaryMinus(),
                            connection
                        )
                        // reverse to drag delta
                        .unaryMinus()
                        .toScrollAxisOffset()

                    axisConsumed
                }

                return overscrollEffect
                    // apply to overScroll with dragDelta
                    .applyToScroll(delta = dragDelta, NestedScrollSource.Fling, performScroll)
                    .takeScrollAxisAsFloat()
                    // reverse to scroll delta
                    .unaryMinus()
            }
        }
        with(scope) {
            @OptIn(ExperimentalFoundationApi::class)
            with(flingBehaviorFactory(pagerController.density)) {
                result = result
                    .updateScrollAxis(
                        // scroll is reversed drag
                        performFling(result.takeScrollAxisAsFloat().unaryMinus())
                    )
                    // reverse to drag velocity
                    .unaryMinus()
            }
        }
        return result
    }

    class UserDragScroll(

    ) {

        private var startFlingKey: Any? = null

        var dragActive: Boolean = false
            private set

        var dragEnded: Boolean = false
            private set

        var flingActive: Boolean = false
            private set

        var flingEnded: Boolean = false
            private set

        var scrollCancellationException: CancellationException? = null
            private set

        var overriden: Boolean = false
            private set

        fun initDrag() {
            if (dragActive) return
            if (dragEnded) return
            dragActive = true
        }

        fun newDrag() {
            if (dragActive) error("duplicate userDragScroll")
            overriden = true
            scrollCancellationException = CancellationException("Overriden by new drag")
            if (flingActive) flingEnd(scrollCancellationException)
        }

        fun dragEnd(
            ex: CancellationException? = null,
            connectionKey: Any
        ): Any? {
            if (!dragActive) return null
            dragActive = false
            dragEnded = true
            ex?.let { cause ->
                scrollCancellationException = cause
                return null
            }
            return connectionKey.also {
                startFlingKey = it
            }
        }

        fun flingEnd(ex: CancellationException? = null) {
            if (!flingActive) return
            flingActive = false
            startFlingKey = null
        }

        fun ensureDragActive(): Unit {
            checkInMainLooper()
            if (!dragActive) {
                scrollCancellationException?.let { cause ->
                    throw CancellationException("DragScroll cancelled", cause)
                }
                throw CancellationException("DragScroll not started")
            }
        }

        fun ensureFlingActive(): Unit {
            checkInMainLooper()
            if (!flingActive) {
                scrollCancellationException?.let { cause ->
                    throw CancellationException("DragScroll cancelled", cause)
                }
                throw CancellationException("FlingScroll not started")
            }
        }

        fun startFling(key: Any): Boolean {
            scrollCancellationException?.let {
                return false
            }
            if (flingActive) {
                return false
            }
            val start = key == startFlingKey
            if (start) {
                flingActive = true
            }
            return start
        }
    }

    class UserDragFlingKey(
        val pagerDragConnection: Any
    )

    private fun Velocity.takeScrollAxis(): Velocity =
        when(scrollableOrientation.value) {
            Orientation.Horizontal -> Velocity(x = x, y = 0f)
            Orientation.Vertical -> Velocity(x = 0f, y = y)
        }

    private fun Velocity.takeScrollAxisAsFloat(): Float =
        when(scrollableOrientation.value) {
            Orientation.Horizontal -> x
            Orientation.Vertical -> y
        }

    private fun Velocity.takeScrollAxisAsOffset(): Offset =
        when(scrollableOrientation.value) {
            Orientation.Horizontal -> if (x == 0f) Offset.Zero else Offset(x = x, y = 0f)
            Orientation.Vertical -> if (y == 0f) Offset.Zero else Offset(x = 0f, y = y)
        }

    private fun Velocity.updateScrollAxis(newValue: Float): Velocity =
        when (scrollableOrientation.value) {
            Orientation.Horizontal -> copy(x = newValue)
            Orientation.Vertical -> copy(y = newValue)
        }

    private fun Float.toScrollAxisOffset(): Offset =
        if (this == 0f) {
            Offset.Zero
        } else when(scrollableOrientation.value) {
            Orientation.Horizontal -> Offset(x = this, y = 0f)
            Orientation.Vertical -> Offset(x = 0f, y = this)
        }

    private fun Offset.takeScrollAxisAsFloat(): Float =
        when (scrollableOrientation.value) {
            Orientation.Horizontal -> x
            Orientation.Vertical -> y
        }
}