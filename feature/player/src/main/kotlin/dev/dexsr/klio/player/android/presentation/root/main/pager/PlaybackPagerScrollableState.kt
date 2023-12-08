package dev.dexsr.klio.player.android.presentation.root.main.pager

import dev.dexsr.klio.android.base.checkInMainLooper
import dev.dexsr.klio.player.android.presentation.root.main.PlaybackPagerController
import dev.dexsr.klio.player.android.presentation.root.main.pager.overscroll.PlaybackPagerOverscrollEffect
import kotlinx.coroutines.CancellationException

class PlaybackPagerScrollableState(
    // maybe: scroll connection
    private val pagerController: PlaybackPagerController
) : PlaybackPagerGestureScrollableState {

    private var _isScrollInProgress = false

    val isScrollInProgress
        get() = _isScrollInProgress

    val allowUserGestureInterruptScroll: Boolean
        get() = true

    private val ignoreOngoingUserScroll: Boolean
        get() = pagerController.correctingTimeline

    private var currentDrag: UserDragScroll? = null
    private var latestDrag: UserDragScroll? = null

    override suspend fun bringChildFocusScroll(scroll: suspend PlaybackPagerScrollScope.() -> Unit) {

    }

    override suspend fun userDragScroll(scroll: suspend PlaybackPagerScrollScope.() -> Unit) {
        checkInMainLooper()
        currentDrag?.newDrag()
        val drag = UserDragScroll().apply {
            initDrag()
        }
        currentDrag = drag
        latestDrag = drag
        val dragConnection = pagerController.newUserDragScroll()
        try {
            object : PlaybackPagerScrollScope {
                override fun scrollBy(pixels: Float): Float {
                    drag.ensureActive()
                    // call to newUserDragScroll should make this to false, so it's actually ongoing
                    if (ignoreOngoingUserScroll) return 0f
                    val performScroll = pagerController.userPerformScroll(-pixels, dragConnection)
                    return performScroll
                }
            }.run {
                scroll()
            }
            dragConnection.dragEnd()
            drag.dragEnd()
        } catch (ex: Exception) {
            dragConnection.dragEnd()
            drag.dragEnd(ex as? CancellationException ?: CancellationException(null, ex))
            throw ex
        } finally {
            currentDrag = null
        }
    }

    override suspend fun performFling(
        key: Any,
        velocity: Float,
        overscrollEffect: PlaybackPagerOverscrollEffect
    ) {
        checkInMainLooper()
        val drag = latestDrag
        val consume = drag?.startFling(key)
        if (consume != true) return
        val flingConnection = pagerController.newUserDragFlingScroll(drag)
        if (flingConnection?.isActive != true) {
            // flingConnection is closed, we can early return
            // maybe: convey reason
            drag.flingEnd(CancellationException("flingConnection already closed"))
        }
        // TODO
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

        fun initDrag() {
            if (dragActive) return
            if (dragEnded) return
            dragActive = true
        }

        fun newDrag() {
            if (dragActive) error("duplicate userDragScroll")
        }

        fun dragEnd(ex: CancellationException? = null) {
            if (!dragActive) return
            dragActive = false
            ex?.let { cause ->
                scrollCancellationException = cause
            }
            dragEnded = true
            startFlingKey = Any()
        }

        fun flingEnd(ex: CancellationException? = null) {
            if (!flingActive) return
            flingActive = false
            startFlingKey = null
        }

        fun ensureActive(): Unit {
            checkInMainLooper()
            if (!dragActive) {
                scrollCancellationException?.let { cause ->
                    throw CancellationException("DragScroll cancelled", cause)
                }
                throw CancellationException("DragScroll not started")
            }
        }

        fun startFling(key: Any): Boolean {
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
}