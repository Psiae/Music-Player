package dev.dexsr.klio.player.android.presentation.root.main.pager

import dev.dexsr.klio.android.base.checkInMainLooper
import dev.dexsr.klio.player.android.presentation.root.main.PlaybackPagerController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive

class PlaybackPagerScrollableState(
    private val pagerController: PlaybackPagerController
) : PlaybackPagerGestureScrollableState {

    private var _isScrollInProgress = false

    val isScrollInProgress
        get() = _isScrollInProgress

    val allowUserGestureInterruptScroll: Boolean
        get() = true

    private val ignoreOngoingUserScroll: Boolean
        get() = pagerController.correctingTimeline

    private var currentDrag: Job? = null
    private var awaitNewFling: CompletableJob? = null
    private var currentFling: Job? = null

    override suspend fun bringChildFocusScroll(scroll: suspend PlaybackPagerScrollScope.() -> Unit) {

    }

    override suspend fun userDragScroll(scroll: suspend PlaybackPagerScrollScope.() -> Unit) {
        checkInMainLooper()
        // await call to performFling
        awaitNewFling?.join()

        val task = Job()
        currentDrag?.let {
            error("duplicate userDragScroll")
        }
        currentFling?.cancel()
        currentDrag = task

        val drag = pagerController.newUserDragScroll()
        try {
            object : PlaybackPagerScrollScope {
                override fun scrollBy(pixels: Float): Float {
                    task.ensureActive()
                    if (ignoreOngoingUserScroll) return 0f
                    val performScroll = pagerController.userPerformScroll(-pixels, drag)
                    return performScroll
                }
            }.run {
                scroll()
            }
            task.complete()
        } catch (ex: Exception) {
            task.cancel(CancellationException(null, ex))
            throw ex
        } finally {
            currentDrag = null
        }
        awaitNewFling = Job()
    }

    override suspend fun performFling(velocity: Float) {
        checkInMainLooper()
        // maybe assert
        awaitNewFling?.complete() ?: return
    }
}