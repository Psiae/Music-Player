package dev.dexsr.klio.player.android.presentation.root.main.pager

import androidx.annotation.MainThread
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import dev.dexsr.klio.android.base.checkInMainLooper
import dev.dexsr.klio.base.compose.SnapshotRead
import dev.dexsr.klio.player.android.presentation.root.main.PlaybackPagerLayoutInfo
import dev.dexsr.klio.player.android.presentation.root.main.PlaybackPagerMeasureResult
import dev.dexsr.klio.player.android.presentation.root.main.PlaybackPagerScrollPosition
import timber.log.Timber
import kotlin.math.abs

class PlaybackPagerLayoutState() {
    private var remeasurement: Remeasurement? = null

    private val awaitFirstLayoutModifier = AwaitFirstLayoutModifier()

    private val remeasurementModifier = object : RemeasurementModifier {

        override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
            this@PlaybackPagerLayoutState.remeasurement = remeasurement
        }
    }

    private val scrollPosition = PlaybackPagerScrollPosition()


    private val _pagerLayoutInfoState = mutableStateOf<PlaybackPagerLayoutInfo>(
        PlaybackPagerLayoutInfo.UNSET
    )

    internal val pagerLayoutInfo
        @SnapshotRead get() = _pagerLayoutInfoState.value

    internal var numMeasurePasses = 0

    private var _layoutCount = 0

    // scroll variables

    var scrollToBeConsumed = 0f
        private set

    private var _scrollCount = 0

    // scroll variables - end

    val modifier = Modifier
        .then(remeasurementModifier)
        .then(awaitFirstLayoutModifier)

    val currentPage
        @SnapshotRead get() = scrollPosition.currentPage

    val firstVisiblePage: Int
        @SnapshotRead get() = scrollPosition.firstVisiblePage

    val firstVisiblePageOffset: Int
        @SnapshotRead get() = scrollPosition.scrollOffset

    val scrollOffset: Int
        @SnapshotRead get() = scrollPosition.scrollOffset

    // whether or not the layout can scroll forward
    var canScrollForward: Boolean by mutableStateOf(false)
        private set
    var canScrollBackward: Boolean by mutableStateOf(false)
        private set

    val isScrollInProgress
        // TODO: @SnapshotRead
        get() = _scrollCount > 0

    fun layoutEnter() {
        checkInMainLooper()
        check(++_layoutCount == 1) {
            "PlaybackPagerLayoutState, only one layout should host the state at a time"
        }
    }

    @MainThread
    fun layoutExit() {
        checkInMainLooper()
        check(--_layoutCount == 0) {
            "PlaybackPagerLayoutState, layoutExit imbalance, should not be invoked multiple times"
        }
    }

    fun scrollEnter(
        debugName: String?
    ) {
        checkInMainLooper()
        ++_scrollCount
    }

    fun scrollExit(
        debugName: String?
    ) {
        checkInMainLooper()
        check(--_scrollCount >= 0) {
            "scrollCount imbalance, don't forget to call exit"
        }
    }

    fun scrollToPosition(
        firstVisiblePageIndex: Int,
        scrollOffset: Int,
    ) {
        Timber.d("PlaybackPagerLayoutState_DEBUG: scrollToPosition(firstVisiblePageIndex=$firstVisiblePageIndex, scrollOffset=$scrollOffset)")
        scrollPosition
            .requestPosition(firstVisiblePageIndex, scrollOffset)
        remeasurement?.forceRemeasure()
    }

    fun scrollBy(
        distance: Float,
        debugPerformerName: String? = null
    ): Float {
        Timber.d("PlaybackPagerController_DEBUG_s: performScroll(distance=$distance, performer=$debugPerformerName, canScrollForward=$canScrollForward, canScrollBackward=$canScrollBackward, firstVisiblePage=$firstVisiblePage, offset=$firstVisiblePageOffset, currentPage=$currentPage, pageSize=${pagerLayoutInfo.pageSize})")
        return -doScrollBy(-distance)
    }

    private fun doScrollBy(
        distance: Float
    ): Float {
        if (distance < 0 && !canScrollForward || distance > 0 && !canScrollBackward) {
            return 0f
        }
        check(abs(scrollToBeConsumed) <= 0.5f) {
            "entered drag with non-zero pending scroll: $scrollToBeConsumed"
        }
        scrollToBeConsumed += distance

        // scrollToBeConsumed will be consumed synchronously during the forceRemeasure invocation
        // inside measuring we do scrollToBeConsumed.roundToInt() so there will be no scroll if
        // we have less than 0.5 pixels
        if (abs(scrollToBeConsumed) > 0.5f) {
            /*val preScrollToBeConsumed = scrollToBeConsumed*/
            remeasurement?.forceRemeasure()
            /*if (prefetchingEnabled) {
                notifyPrefetch(preScrollToBeConsumed - scrollToBeConsumed)
            }*/
        }

        // here scrollToBeConsumed is already consumed during the forceRemeasure invocation
        if (abs(scrollToBeConsumed) <= 0.5f) {
            // We consumed all of it - we'll hold onto the fractional scroll for later, so report
            // that we consumed the whole thing
            return distance
        } else {
            val scrollConsumed = distance - scrollToBeConsumed
            // We did not consume all of it - return the rest to be consumed elsewhere (e.g.,
            // nested scrolling)
            scrollToBeConsumed = 0f // We're not consuming the rest, give it back
            return scrollConsumed
        }
    }

    internal fun onMeasureResult(result: PlaybackPagerMeasureResult) {
        scrollPosition.updateFromMeasureResult(result)
        scrollToBeConsumed -= result.consumedScroll
        _pagerLayoutInfoState.value = result
        canScrollForward = result.canScrollForward
        canScrollBackward = (result.firstVisiblePage?.index ?: 0) != 0 ||
                result.firstVisiblePageOffset != 0
        numMeasurePasses++
        if (!isScrollInProgress) {
            /*settledPageState.value = currentPage*/
        }
    }
}