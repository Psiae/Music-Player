package dev.dexsr.klio.player.android.presentation.root.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot

// androidx.compose.foundation-android:1.5.0-beta01

class PlaybackPagerScrollPosition(
    initialPage: Int = 0,
    initialScrollOffset: Int = 0,
) {

    var firstVisiblePage by mutableIntStateOf(initialPage)
    var currentPage by mutableIntStateOf(initialPage)

    var scrollOffset by mutableIntStateOf(initialScrollOffset)
        private set

    private var hadFirstNotEmptyLayout = false

    /** The last know key of the page at [firstVisiblePage] position. */
    private var lastKnownFirstPageKey: Any? = null

    /**
     * Updates the current scroll position based on the results of the last measurement.
     */
    fun updateFromMeasureResult(measureResult: PlaybackPagerMeasureResult) {
        lastKnownFirstPageKey = measureResult.firstVisiblePage?.key
        // we ignore the index and offset from measureResult until we get at least one
        // measurement with real pages. otherwise the initial index and scroll passed to the
        // state would be lost and overridden with zeros.
        if (hadFirstNotEmptyLayout || measureResult.pageCount > 0) {
            hadFirstNotEmptyLayout = true
            val scrollOffset = measureResult.firstVisiblePageOffset
            check(scrollOffset >= 0f) { "scrollOffset should be non-negative ($scrollOffset)" }

            Snapshot.withoutReadObservation {
                update(
                    measureResult.firstVisiblePage?.index ?: 0,
                    scrollOffset
                )
                measureResult.closestPageToSnapPosition?.index?.let {
                    if (it != this.currentPage) {
                        this.currentPage = it
                    }
                }
            }
        }
    }

    /**
     * Updates the scroll position - the passed values will be used as a start position for
     * composing the pages during the next measure pass and will be updated by the real
     * position calculated during the measurement. This means that there is no guarantee that
     * exactly this index and offset will be applied as it is possible that:
     * a) there will be no page at this index in reality
     * b) page at this index will be smaller than the asked scrollOffset, which means we would
     * switch to the next page
     * c) there will be not enough pages to fill the viewport after the requested index, so we
     * would have to compose few elements before the asked index, changing the first visible page.
     */
    fun requestPosition(firstVisiblePageIndex: Int, scrollOffset: Int) {
        update(firstVisiblePageIndex, scrollOffset)
        // clear the stored key as we have a direct request to scroll to [index] position and the
        // next [checkIfFirstVisibleItemWasMoved] shouldn't override this.
        lastKnownFirstPageKey = null
    }

    private fun update(index: Int, scrollOffset: Int) {
        require(index >= 0f) { "Index should be non-negative ($index)" }
        if (index != this.firstVisiblePage) {
            this.firstVisiblePage = index
        }
        if (scrollOffset != this.scrollOffset) {
            this.scrollOffset = scrollOffset
        }
    }
}