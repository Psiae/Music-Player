package dev.dexsr.klio.player.android.presentation.root.main.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.*
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMaxBy
import dev.dexsr.klio.player.android.libint.utils.fastFilter
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

class PlaybackPagerMeasureScope(
    private val contentFactory: PlaybackPagerContentFactory,
    private val subcomposeMeasureScope: SubcomposeMeasureScope
) : MeasureScope by subcomposeMeasureScope {


    /**
     * A cache of the previously composed items. It allows us to support [get]
     * re-executions with the same index during the same measure pass.
     */
    private val placeablesCache = hashMapOf<Int, List<Placeable>>()

    fun measure(index: Int, constraints: Constraints): List<Placeable> {
        val cachedPlaceable = placeablesCache[index]
        return if (cachedPlaceable != null) {
            cachedPlaceable
        } else {
            val key = index
            val itemContent = contentFactory.getContent(index)
            val measurables = subcomposeMeasureScope.subcompose(key, itemContent)
            List(measurables.size) { i ->
                measurables[i].measure(constraints)
            }.also {
                placeablesCache[index] = it
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun PlaybackPagerMeasureScope.measurePager(
    timeline: PlaybackPagerTimeline,
    pagerItemProvider: PlaybackPagerItemLayoutProvider,
    mainAxisAvailableSize: Int,
    firstVisiblePage: Int,
    firstVisiblePageOffset: Int,
    scrollToBeConsumed: Float,
    constraints: Constraints,
    orientation: Orientation,
    verticalAlignment: Alignment.Vertical?,
    horizontalAlignment: Alignment.Horizontal?,
    visualPageOffset: IntOffset,
    pageAvailableSize: Int,
    beyondBoundsPageCount: Int,
    layoutPlacement: (Int, Int, Placeable.PlacementScope.() -> Unit) -> MeasureResult
): PlaybackPagerMeasureResult {

    val itemCount = timeline.windows.size

    if (itemCount < 1 || timeline.currentIndex < 0) {
        return PlaybackPagerMeasureResult(
            measureResult = layout(0, 0) {},
            orientation = orientation,
            consumedScroll = 0f,
            pageCount = 0,
            firstVisiblePage = null,
            firstVisiblePageOffset = 0,
            canScrollForward = false,
            closestPageToSnapPosition = null,
            visiblePagesInfo = emptyList(),
            afterContentPadding = 0,
            pageSize = 0,
            viewportStartOffset = 0,
            viewportEndOffset = 0
        )
    }

    val pageCount = itemCount
    val pageSizeWithSpacing = pageAvailableSize
    val afterContentPadding = 0
    val beforeContentPadding = 0
    val reverseLayout = false
    val spaceBetweenPages = 0

    val childConstraints = Constraints(
        maxWidth = if (orientation == Orientation.Vertical) {
            constraints.maxWidth
        } else {
            pageAvailableSize
        },
        maxHeight = if (orientation != Orientation.Vertical) {
            constraints.maxHeight
        } else {
            pageAvailableSize
        }
    )

    var currentFirstPage = firstVisiblePage
    var currentFirstPageScrollOffset = firstVisiblePageOffset
    if (currentFirstPage >= pageCount) {
        // the data set has been updated and now we have less pages that we were
        // scrolled to before
        currentFirstPage = pageCount - 1
        currentFirstPageScrollOffset = 0
    }

    // represents the real amount of scroll we applied as a result of this measure pass.
    var scrollDelta = scrollToBeConsumed.roundToInt()

    // applying the whole requested scroll offset. we will figure out if we can't consume
    // all of it later
    currentFirstPageScrollOffset -= scrollDelta

    // if the current scroll offset is less than minimally possible
    if (currentFirstPage == 0 && currentFirstPageScrollOffset < 0) {
        scrollDelta += currentFirstPageScrollOffset
        currentFirstPageScrollOffset = 0
    }

    // this will contain all the measured pages representing the visible pages
    val visiblePages = mutableListOf<PlaybackPagerMeasuredPage>()

    // define min and max offsets
    val minOffset = 0
    val maxOffset = mainAxisAvailableSize

    // include the start padding so we compose pages in the padding area and neutralise page
    // spacing (if the spacing is negative this will make sure the previous page is composed)
    // before starting scrolling forward we will remove it back
    currentFirstPageScrollOffset += minOffset

    // max of cross axis sizes of all visible pages
    var maxCrossAxis = 0

    // we had scrolled backward or we compose pages in the start padding area, which means
    // pages before current firstPageScrollOffset should be visible. compose them and update
    // firstPageScrollOffset
    while (currentFirstPageScrollOffset < 0 && currentFirstPage > 0) {
        val previous = currentFirstPage - 1
        val measuredPage = getAndMeasure(
            index = previous,
            childConstraints = childConstraints,
            pagerItemProvider = pagerItemProvider,
            visualPageOffset = visualPageOffset,
            orientation = orientation,
            horizontalAlignment = horizontalAlignment,
            verticalAlignment = verticalAlignment,
            layoutDirection = layoutDirection,
            pageAvailableSize = pageAvailableSize,
            beforeContentPadding = beforeContentPadding,
            afterContentPadding = afterContentPadding,
            reverseLayout = reverseLayout
        )
        visiblePages.add(0, measuredPage)
        maxCrossAxis = maxOf(maxCrossAxis, measuredPage.crossAxisSize)
        currentFirstPageScrollOffset += pageSizeWithSpacing
        currentFirstPage = previous
    }

    // if we were scrolled backward, but there were not enough pages before. this means
    // not the whole scroll was consumed
    if (currentFirstPageScrollOffset < minOffset) {
        scrollDelta += currentFirstPageScrollOffset
        currentFirstPageScrollOffset = minOffset
    }

    // neutralize previously added padding as we stopped filling the before content padding
    currentFirstPageScrollOffset -= minOffset

    var index = currentFirstPage
    val maxMainAxis = (maxOffset + afterContentPadding).coerceAtLeast(0)
    var currentMainAxisOffset = -currentFirstPageScrollOffset

    // first we need to skip pages we already composed while composing backward
    visiblePages.fastForEach {
        index++
        currentMainAxisOffset += pageSizeWithSpacing
    }

    // then composing visible pages forward until we fill the whole viewport.
    // we want to have at least one page in visiblePages even if in fact all the pages are
    // offscreen, this can happen if the content padding is larger than the available size.
    while (index < pageCount &&
        (currentMainAxisOffset < maxMainAxis ||
                currentMainAxisOffset <= 0 || // filling beforeContentPadding area
                visiblePages.isEmpty())
    ) {
        val measuredPage = getAndMeasure(
            index = index,
            childConstraints = childConstraints,
            pagerItemProvider = pagerItemProvider,
            visualPageOffset = visualPageOffset,
            orientation = orientation,
            horizontalAlignment = horizontalAlignment,
            verticalAlignment = verticalAlignment,
            afterContentPadding = afterContentPadding,
            beforeContentPadding = beforeContentPadding,
            layoutDirection = layoutDirection,
            reverseLayout = reverseLayout,
            pageAvailableSize = pageAvailableSize
        )
        currentMainAxisOffset += pageSizeWithSpacing

        if (currentMainAxisOffset <= minOffset && index != pageCount - 1) {
            // this page is offscreen and will not be placed. advance firstVisiblePage
            currentFirstPage = index + 1
            currentFirstPageScrollOffset -= pageSizeWithSpacing
        } else {
            maxCrossAxis = maxOf(maxCrossAxis, measuredPage.crossAxisSize)
            visiblePages.add(measuredPage)
        }

        index++
    }

    // we didn't fill the whole viewport with pages starting from firstVisiblePage.
    // lets try to scroll back if we have enough pages before firstVisiblePage.
    if (currentMainAxisOffset < maxOffset) {
        val toScrollBack = maxOffset - currentMainAxisOffset
        currentFirstPageScrollOffset -= toScrollBack
        currentMainAxisOffset += toScrollBack
        while (currentFirstPageScrollOffset < beforeContentPadding &&
            currentFirstPage > 0
        ) {
            val previousIndex = currentFirstPage - 1
            val measuredPage = getAndMeasure(
                index = previousIndex,
                childConstraints = childConstraints,
                pagerItemProvider = pagerItemProvider,
                visualPageOffset = visualPageOffset,
                orientation = orientation,
                horizontalAlignment = horizontalAlignment,
                verticalAlignment = verticalAlignment,
                afterContentPadding = afterContentPadding,
                beforeContentPadding = beforeContentPadding,
                layoutDirection = layoutDirection,
                reverseLayout = reverseLayout,
                pageAvailableSize = pageAvailableSize
            )
            visiblePages.add(0, measuredPage)
            maxCrossAxis = maxOf(maxCrossAxis, measuredPage.crossAxisSize)
            currentFirstPageScrollOffset += pageSizeWithSpacing
            currentFirstPage = previousIndex
        }
        scrollDelta += toScrollBack
        if (currentFirstPageScrollOffset < 0) {
            scrollDelta += currentFirstPageScrollOffset
            currentMainAxisOffset += currentFirstPageScrollOffset
            currentFirstPageScrollOffset = 0
        }
    }

    // report the amount of pixels we consumed. scrollDelta can be smaller than
    // scrollToBeConsumed if there were not enough pages to fill the offered space or it
    // can be larger if pages were resized, or if, for example, we were previously
    // displaying the page 15, but now we have only 10 pages in total in the data set.
    val consumedScroll = if (scrollToBeConsumed.roundToInt().sign == scrollDelta.sign &&
        abs(scrollToBeConsumed.roundToInt()) >= abs(scrollDelta)
    ) {
        scrollDelta.toFloat()
    } else {
        scrollToBeConsumed
    }

    // the initial offset for pages from visiblePages list
    require(currentFirstPageScrollOffset >= 0)
    val visiblePagesScrollOffset = -currentFirstPageScrollOffset
    var firstPage = visiblePages.first()

    // even if we compose pages to fill before content padding we should ignore pages fully
    // located there for the state's scroll position calculation (first page + first offset)
    if (beforeContentPadding > 0 || spaceBetweenPages < 0) {
        for (i in visiblePages.indices) {
            val size = pageSizeWithSpacing
            if (currentFirstPageScrollOffset != 0 && size <= currentFirstPageScrollOffset &&
                i != visiblePages.lastIndex
            ) {
                currentFirstPageScrollOffset -= size
                firstPage = visiblePages[i + 1]
            } else {
                break
            }
        }
    }

    // Compose extra pages before
    val extraPagesBefore = createPagesBeforeList(
        currentFirstPage = currentFirstPage,
        beyondBoundsPageCount = beyondBoundsPageCount,
    ) {
        getAndMeasure(
            index = it,
            childConstraints = childConstraints,
            pagerItemProvider = pagerItemProvider,
            visualPageOffset = visualPageOffset,
            orientation = orientation,
            horizontalAlignment = horizontalAlignment,
            verticalAlignment = verticalAlignment,
            afterContentPadding = afterContentPadding,
            beforeContentPadding = beforeContentPadding,
            layoutDirection = layoutDirection,
            reverseLayout = reverseLayout,
            pageAvailableSize = pageAvailableSize
        )
    }

    // Update maxCrossAxis with extra pages
    extraPagesBefore.fastForEach {
        maxCrossAxis = maxOf(maxCrossAxis, it.crossAxisSize)
    }

    // Compose pages after last page
    val extraPagesAfter = createPagesAfterList(
        currentLastPage = visiblePages.last().index,
        pagesCount = pageCount,
        beyondBoundsPageCount = beyondBoundsPageCount,
    ) {
        getAndMeasure(
            index = it,
            childConstraints = childConstraints,
            pagerItemProvider = pagerItemProvider,
            visualPageOffset = visualPageOffset,
            orientation = orientation,
            horizontalAlignment = horizontalAlignment,
            verticalAlignment = verticalAlignment,
            layoutDirection = layoutDirection,
            pageAvailableSize = pageAvailableSize,
            afterContentPadding = afterContentPadding,
            beforeContentPadding = beforeContentPadding,
            reverseLayout = false
        )
    }

    // Update maxCrossAxis with extra pages
    extraPagesAfter.fastForEach {
        maxCrossAxis = maxOf(maxCrossAxis, it.crossAxisSize)
    }

    val noExtraPages = firstPage == visiblePages.first() &&
            extraPagesBefore.isEmpty() &&
            extraPagesAfter.isEmpty()

    val layoutWidth = constraints
        .constrainWidth(
            if (orientation == Orientation.Vertical)
                maxCrossAxis
            else
                currentMainAxisOffset
        )

    val layoutHeight = constraints
        .constrainHeight(
            if (orientation == Orientation.Vertical)
                currentMainAxisOffset
            else
                maxCrossAxis
        )

    val positionedPages = calculatePageOffsets(
        density = this,
        layoutWidth = layoutWidth,
        layoutHeight = layoutHeight,
        visiblePages = visiblePages,
        extraPagesBefore = extraPagesBefore,
        extraPagesAfter = extraPagesAfter,
        mainAxisOffset = currentMainAxisOffset,
        maxOffset = maxOffset,
        visiblePagesScrollOffset = visiblePagesScrollOffset,
        orientation = orientation,
        reverseLayout = reverseLayout,
        pageAvailableSize = pageAvailableSize,
        spaceBetweenPages = spaceBetweenPages,
    )

    val visiblePagesInfo = if (noExtraPages) positionedPages else positionedPages.fastFilter {
        (it.index >= visiblePages.first().index && it.index <= visiblePages.last().index)
    }
    val viewPortSize = if (orientation == Orientation.Vertical) layoutHeight else layoutWidth

    val closestPageToSnapPosition = visiblePagesInfo.fastMaxBy {
        -abs(
            calculateDistanceToDesiredSnapPosition(
                mainAxisViewPortSize = viewPortSize,
                beforeContentPadding = beforeContentPadding,
                afterContentPadding = afterContentPadding,
                itemSize = pageAvailableSize,
                itemOffset = it.offset,
                itemIndex = it.index,
                snapPositionInLayout = SnapAlignmentStartToStart
            )
        )
    }

    return PlaybackPagerMeasureResult(
        measureResult = layoutPlacement(
            childConstraints.maxWidth,
            childConstraints.maxHeight
        ) {
            positionedPages.fastForEach { placeable -> placeable.place(this) }
        },
        orientation = orientation,
        consumedScroll = consumedScroll,
        pageCount = timeline.windows.size,
        pageSize = pageAvailableSize,
        firstVisiblePage = visiblePages.first(),
        firstVisiblePageOffset = currentFirstPageScrollOffset,
        canScrollForward = index < pageCount || currentMainAxisOffset > maxOffset,
        closestPageToSnapPosition = closestPageToSnapPosition,
        visiblePagesInfo = visiblePagesInfo,
        afterContentPadding = afterContentPadding,
        viewportStartOffset = -beforeContentPadding,
        viewportEndOffset = maxOffset + afterContentPadding,
    )
}

private fun createPagesAfterList(
    currentLastPage: Int,
    pagesCount: Int,
    beyondBoundsPageCount: Int,
    getAndMeasure: (Int) -> PlaybackPagerMeasuredPage
): List<PlaybackPagerMeasuredPage> {
    var list: MutableList<PlaybackPagerMeasuredPage>? = null

    val end = minOf(currentLastPage + beyondBoundsPageCount, pagesCount - 1)

    for (i in currentLastPage + 1..end) {
        if (list == null) list = mutableListOf()
        list.add(getAndMeasure(i))
    }

    return list ?: emptyList()
}

private fun createPagesBeforeList(
    currentFirstPage: Int,
    beyondBoundsPageCount: Int,
    getAndMeasure: (Int) -> PlaybackPagerMeasuredPage
): List<PlaybackPagerMeasuredPage> {
    var list: MutableList<PlaybackPagerMeasuredPage>? = null

    val start = maxOf(0, currentFirstPage - beyondBoundsPageCount)

    for (i in currentFirstPage - 1 downTo start) {
        if (list == null) list = mutableListOf()
        list.add(getAndMeasure(i))
    }

    return list ?: emptyList()
}

private fun PlaybackPagerMeasureScope.calculatePageOffsets(
    density: Density,
    layoutWidth: Int,
    layoutHeight: Int,
    visiblePages: List<PlaybackPagerMeasuredPage>,
    extraPagesBefore: List<PlaybackPagerMeasuredPage>,
    extraPagesAfter: List<PlaybackPagerMeasuredPage>,
    visiblePagesScrollOffset: Int,
    maxOffset: Int,
    mainAxisOffset: Int,
    spaceBetweenPages: Int,
    pageAvailableSize: Int,
    orientation: Orientation,
    reverseLayout: Boolean
): List<PlaybackPagerPositionedPage> {

    Timber.d("PlaybackPagerMeasure: PlaybackPagerMeasureScope.calculateItemPositions(visibleItemsScrollOffset=$visiblePagesScrollOffset)")

    val pageSizeWithSpacing = (pageAvailableSize + spaceBetweenPages)
    val mainAxisLayoutSize = if (orientation == Orientation.Vertical) layoutHeight else layoutWidth

    val hasSpareSpace = mainAxisOffset < minOf(mainAxisLayoutSize, maxOffset)

    if (hasSpareSpace) {
        check(visiblePagesScrollOffset == 0)
    }

    val positionedPages = ArrayList<PlaybackPagerPositionedPage>(visiblePages.size)

    if (hasSpareSpace) {
        require(extraPagesBefore.isEmpty() && extraPagesAfter.isEmpty())

        val pagesCount = visiblePages.size
        fun Int.reverseAware() =
            if (!reverseLayout) this else pagesCount - this - 1

        val sizes = IntArray(pagesCount) { pageAvailableSize }
        val offsets = IntArray(pagesCount) { 0 }

        val arrangement = Arrangement.Absolute.spacedBy(pageAvailableSize.toDp())
        if (orientation == Orientation.Vertical) {
            with(arrangement) { density.arrange(mainAxisLayoutSize, sizes, offsets) }
        } else {
            with(arrangement) {
                // Enforces Ltr layout direction as it is mirrored with placeRelative later.
                density.arrange(mainAxisLayoutSize, sizes, LayoutDirection.Ltr, offsets)
            }
        }

        val reverseAwareOffsetIndices =
            if (!reverseLayout) offsets.indices else offsets.indices.reversed()
        for (index in reverseAwareOffsetIndices) {
            val absoluteOffset = offsets[index]
            // when reverseLayout == true, offsets are stored in the reversed order to pages
            val page = visiblePages[index.reverseAware()]
            val relativeOffset = if (reverseLayout) {
                // inverse offset to align with scroll direction for positioning
                mainAxisLayoutSize - absoluteOffset - page.size
            } else {
                absoluteOffset
            }
            positionedPages.add(page.position(relativeOffset, layoutWidth, layoutHeight))
        }
    } else {
        var currentMainAxis = visiblePagesScrollOffset
        extraPagesBefore.fastForEach {
            currentMainAxis -= pageSizeWithSpacing
            positionedPages.add(it.position(currentMainAxis, layoutWidth, layoutHeight))
        }

        currentMainAxis = visiblePagesScrollOffset
        visiblePages.fastForEach {
            positionedPages.add(it.position(currentMainAxis, layoutWidth, layoutHeight))
            currentMainAxis += pageSizeWithSpacing
        }

        extraPagesAfter.fastForEach {
            positionedPages.add(it.position(currentMainAxis, layoutWidth, layoutHeight))
            currentMainAxis += pageSizeWithSpacing
        }
    }

    return positionedPages
}