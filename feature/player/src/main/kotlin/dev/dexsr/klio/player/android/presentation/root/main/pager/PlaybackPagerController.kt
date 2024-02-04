package dev.dexsr.klio.player.android.presentation.root.main.pager

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.unit.Density
import dev.dexsr.klio.android.base.checkInMainLooper
import dev.dexsr.klio.base.composeui.SnapshotRead
import dev.dexsr.klio.base.composeui.SnapshotWrite
import dev.dexsr.klio.base.kt.castOrNull
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

// use impl from androidx.compose.foundation-android:1.5.0-beta01

class PlaybackPagerController(
    private val player: PlaybackPagerPlayer,
) {

    //
    private var _initiated = false
    private var _disposed = false

    private val coroutineScope = CoroutineScope(SupervisorJob())

    private val _renderData = mutableStateOf<PlaybackPagerRenderData?>(null, neverEqualPolicy())

    internal var upDownDifference: Offset by mutableStateOf(Offset.Zero)

    internal var density: Density
        @SnapshotRead get() = layoutState.density
        @SnapshotWrite set(value) { layoutState.density = value }


    internal val pagerLayoutInfo
        @SnapshotRead get() = layoutState.pagerLayoutInfo

    val gestureScrollableState = PlaybackPagerScrollableState(
        pagerController = this
    )

    val isScrollInProgress: Boolean
        @SnapshotRead
        get() = correctingTimeline or userDragScrolling or userDragFlinging or correctingFling

    val currentPage: Int
        @SnapshotRead get() = layoutState.currentPage

    val renderData
        @SnapshotRead get() = _renderData.value

    internal val scrollToBeConsumed
        @SnapshotRead get() = layoutState.scrollToBeConsumed

    fun init() {
        checkInMainLooper()
        if (_initiated || _disposed) return
        initTimelineUpdater()
        _initiated = true
    }

    fun dispose() {
        checkInMainLooper()
        if (_disposed) return
        _disposed = true
        coroutineScope.cancel()
    }

    val firstVisiblePage: Int
        @SnapshotRead get() = layoutState.firstVisiblePage

    val firstVisiblePageOffset: Int
        @SnapshotRead get() = layoutState.firstVisiblePageOffset

    private var timelineUpdateCollector: Job? = null
    private fun initTimelineUpdater() {
        timelineUpdateCollector = coroutineScope.launch(Dispatchers.Main) {
            player
                .timelineAndStepAsFlow(PlaybackPagerEagerRangePlaceholder)
                .collect { (timeline, step) ->
                    if (_renderData.value == null) {
                        onFirstTimelineCandidate(
                            timeline = PlaybackPagerTimeline(
                                timeline.items,
                                timeline.currentIndex
                            )
                        )
                        return@collect
                    }
                    onNewTimelineUpdate(
                        PlaybackPagerTimeline(
                            timeline.items,
                            timeline.currentIndex
                        ),
                        _renderData.value!!.timeline,
                        stepHint = step,
                        debugSourceName = "timelineUpdater"
                    )
                }
        }
    }
    private fun stopTimelineUpdater() {
        timelineUpdateCollector?.cancel()
    }

    private fun onFirstTimelineCandidate(
        timeline: PlaybackPagerTimeline
    ) {
        checkInMainLooper()
        if (timeline.windows.isEmpty()|| timeline.currentIndex !in timeline.windows.indices) {
            return
        }
        _renderData.value = PlaybackPagerRenderData(timeline = timeline)
        layoutState.scrollToPosition(timeline.currentIndex, 0)
    }

    private fun onNewTimelineUpdate(
        timeline: PlaybackPagerTimeline,
        previousTimeline: PlaybackPagerTimeline,
        stepHint: Int?,
        skipAnimate: Boolean = false,
        swipeUpdate: Boolean = false,
        debugSourceName: String = ""
    ) {
        Timber.d("PlaybackPagerController_DEBUG: onNewTimelineUpdate(timeline=${timeline.toDebugString()}, previousTimeline=${previousTimeline.toDebugString()}, skipAnimate=$skipAnimate, swipeUpdate=$swipeUpdate, debugSourceName=$debugSourceName)")
        checkInMainLooper()
        if (timeline.windows.isEmpty() || timeline.currentIndex !in timeline.windows.indices) {
            _renderData.value = PlaybackPagerRenderData(timeline = PlaybackPagerTimeline.UNSET)
            emptyTimelineCancelInteractions()
            layoutState.scrollToPosition(0, 0)
            return
        }
        if (previousTimeline.windows.isEmpty() || previousTimeline.currentIndex !in previousTimeline.windows.indices) {
            timelineShiftUnknown(timeline = timeline, previousTimeline = previousTimeline)
            return
        }

        // check if the new timeline is a continuation of previous timeline
        var rightShift = false
        var leftShift = false
        var shiftCount = 0
        var shiftNone = false

        // TODO: tests
        run findShift@ {
            when {
                timeline.windows.size == previousTimeline.windows.size -> {
                    Timber.d("PlaybackPagerController_DEBUG: onNewTimelineUpdate_eqSize")

                    if (timeline.currentIndex != previousTimeline.currentIndex) {
                        if (timeline.windows == previousTimeline.windows) {
                            if (timeline.currentIndex > previousTimeline.currentIndex) {
                                rightShift = true
                            } else {
                                leftShift = true
                            }
                            shiftCount = abs(timeline.currentIndex - previousTimeline.currentIndex)
                        }
                        // invalid index, reject
                        return@findShift
                    }

                    var pCutHeadCount: Int = -1
                    var cCutTailCount: Int = -1
                    run rightShift@ {
                        val pCutHead = run pCutHead@ {
                            repeat(
                                timeline.currentIndex + 1
                            ) stage@ { stage ->

                                repeat(timeline.currentIndex + 1 - stage) { i ->
                                    if (timeline.windows[i] != previousTimeline.windows[stage + i]) {
                                        if (i == (timeline.currentIndex + 1 - stage) - 1) {
                                            pCutHeadCount = 0
                                            return@rightShift
                                        }
                                        return@stage
                                    }
                                    if (i == (timeline.currentIndex + 1 - stage) - 1) {
                                        pCutHeadCount = stage
                                        return@pCutHead previousTimeline.windows.subList(stage, previousTimeline.windows.size)
                                    }
                                }
                            }
                            return@rightShift
                        }
                        if (pCutHead.isEmpty()) return@rightShift
                        if (pCutHeadCount == 0) return@rightShift

                        val cCutTail = run cCutTail@ {
                            repeat(
                                (pCutHeadCount + 1)
                                    .coerceAtMost(timeline.windows.size - timeline.currentIndex)
                            ) stage@ { stage ->

                                repeat(timeline.windows.size - timeline.currentIndex + stage) { i ->
                                    if (previousTimeline.windows[previousTimeline.windows.lastIndex - i] != timeline.windows[timeline.windows.lastIndex - stage - i]) {
                                        if (i == timeline.windows.size - timeline.currentIndex - stage - 1) {
                                            cCutTailCount = 0
                                            return@rightShift
                                        }
                                        return@stage
                                    }
                                    if (i == timeline.windows.size - timeline.currentIndex - stage - 1) {
                                        cCutTailCount = stage
                                        return@cCutTail previousTimeline.windows.subList(fromIndex = 0, toIndex = timeline.windows.size - stage)
                                    }
                                }
                            }
                            return@rightShift
                        }
                        if (cCutTail.isEmpty()) return@rightShift
                        if (cCutTailCount == 0) return@rightShift

                        rightShift = true
                        shiftCount = max(pCutHeadCount, cCutTailCount)
                    }

                    var cCutHeadCount: Int = -1
                    var pCutTailCount: Int = -1
                    if (!rightShift) run leftShift@ {

                        val cCutHead = run cCutHead@ {
                            repeat(
                                timeline.currentIndex + 1
                            ) stage@ { stage ->
                                repeat(timeline.currentIndex + 1 - stage) { i ->
                                    if (timeline.windows[stage + i] != previousTimeline.windows[i]) return@stage
                                    if (i == (timeline.currentIndex + 1 - stage) - 1) {
                                        cCutHeadCount = stage
                                        return@cCutHead timeline.windows.subList(stage, timeline.windows.size)
                                    }
                                }
                            }
                            return@leftShift
                        }
                        if (cCutHead.isEmpty()) return@leftShift
                        if (cCutHeadCount == 0) return@leftShift

                        val pCutTail = run pCutTail@ {
                            repeat(
                                (cCutHeadCount + 1)
                                    .coerceAtMost(timeline.windows.size - timeline.currentIndex)
                            ) stage@ { stage ->
                                repeat(timeline.windows.size - timeline.currentIndex - stage) { i ->
                                    if (previousTimeline.windows[previousTimeline.windows.lastIndex - stage - i] != timeline.windows[timeline.windows.lastIndex - i]) {
                                        if (i == timeline.windows.size - timeline.currentIndex - stage - 1) {
                                            pCutTailCount = 0
                                            return@leftShift
                                        }
                                        return@stage
                                    }
                                    if (i == timeline.windows.size - timeline.currentIndex - stage - 1) {
                                        pCutTailCount = stage
                                        return@pCutTail previousTimeline.windows.subList(fromIndex = 0, toIndex = previousTimeline.windows.size - stage)
                                    }
                                }
                            }
                            return@leftShift
                        }

                        if (pCutTail.isEmpty()) return@leftShift
                        if (pCutTailCount == 0) return@leftShift

                        leftShift = true
                        shiftCount = max(cCutHeadCount, pCutTailCount)
                    }

                    Timber.d("PlaybackPagerController_DEBUG: onNewTimelineUpdate_eqSize_result($cCutHeadCount, $cCutTailCount, $pCutHeadCount, $pCutTailCount)")

                    if (
                        !rightShift && !leftShift &&
                        cCutHeadCount == 0 && pCutHeadCount == 0 &&
                        // we know that the head is the same, we can just compare tail and current
                        run {
                            timeline.windows.subList(timeline.currentIndex, timeline.windows.size) ==
                                    previousTimeline.windows.subList(timeline.currentIndex, timeline.windows.size)
                        }
                    ) {
                        shiftNone = true
                    }
                }
                timeline.windows.size > previousTimeline.windows.size -> {
                    Timber.d("PlaybackPagerController_DEBUG: onNewTimelineUpdate_cbSize")
                    // assume that we are moving towards the center, from the edge
                    run rightShift@ {
                        if (previousTimeline.currentIndex >= timeline.currentIndex) return@rightShift
                        val sizeDiff = timeline.windows.size - previousTimeline.windows.size
                        val pCutHeadCount: Int
                        val pCutHead = run pCutHead@ {
                            repeat(
                                previousTimeline.currentIndex + 1
                            ) stage@ { stage ->
                                repeat(previousTimeline.currentIndex + 1 - stage) { i ->
                                    if (timeline.windows[i] != previousTimeline.windows[stage + i]) return@stage
                                    if (i == (previousTimeline.currentIndex + 1 - stage) - 1) {
                                        pCutHeadCount = stage
                                        return@pCutHead previousTimeline.windows.subList(fromIndex = stage, toIndex = previousTimeline.windows.size)
                                    }
                                }
                            }
                            return@rightShift
                        }
                        if (pCutHead.isEmpty()) return@rightShift

                        val cCutTailCount: Int
                        val cCutTail = run cCutTail@ {
                            repeat(
                                (pCutHeadCount + sizeDiff + 1)
                                    .coerceAtMost(previousTimeline.windows.size - previousTimeline.currentIndex)
                            ) stage@ { stage ->
                                repeat(previousTimeline.windows.size - previousTimeline.currentIndex - stage) { i ->
                                    if (previousTimeline.windows[previousTimeline.windows.lastIndex - i] != timeline.windows[timeline.windows.lastIndex - stage - i]) return@stage
                                    if (i == previousTimeline.windows.size - previousTimeline.currentIndex - stage - 1) {
                                        cCutTailCount = stage
                                        return@cCutTail timeline.windows.subList(fromIndex = 0, toIndex = timeline.windows.size - stage)
                                    }
                                }
                            }
                            return@rightShift
                        }

                        if (cCutTail.isEmpty()) return@rightShift
                        rightShift = true
                        shiftCount = max(pCutHeadCount, cCutTailCount)
                    }

                    if (!rightShift) run leftShift@ {
                        if (previousTimeline.currentIndex != timeline.currentIndex) return@leftShift
                        val sizeDiff = timeline.windows.size - previousTimeline.windows.size
                        val cCutHeadCount: Int
                        val cCutHead = run cCutHead@ {
                            repeat(
                                previousTimeline.currentIndex + 1
                            ) stage@ { stage ->
                                repeat(previousTimeline.currentIndex + 1 - stage) { i ->
                                    if (timeline.windows[stage + i] != previousTimeline.windows[i]) return@stage
                                    if (i == (previousTimeline.currentIndex + 1 - stage) - 1) {
                                        cCutHeadCount = stage
                                        return@cCutHead timeline.windows.subList(fromIndex = stage, toIndex = timeline.windows.size)
                                    }
                                }
                            }
                            return@leftShift
                        }
                        if (cCutHead.isEmpty()) return@leftShift

                        val pCutTailCount: Int
                        val pCutTail = run pCutTail@ {
                            repeat(
                                (cCutHeadCount + sizeDiff + 1)
                                    .coerceAtMost(previousTimeline.windows.size - previousTimeline.currentIndex)
                            ) stage@ { stage ->
                                repeat(previousTimeline.windows.size - previousTimeline.currentIndex - stage) { i ->
                                    if (previousTimeline.windows[previousTimeline.windows.lastIndex - stage - i] != timeline.windows[timeline.windows.lastIndex - i]) return@stage
                                    if (i == previousTimeline.windows.size - previousTimeline.currentIndex - stage - 1) {
                                        pCutTailCount = stage
                                        return@pCutTail previousTimeline.windows.subList(fromIndex = 0, toIndex = previousTimeline.windows.size - stage)
                                    }
                                }
                            }
                            return@leftShift
                        }
                        if (pCutTail.isEmpty()) return@leftShift

                        leftShift = true
                        shiftCount = max(cCutHeadCount, pCutTailCount)
                    }
                }
                timeline.windows.size < previousTimeline.windows.size -> {
                    Timber.d("PlaybackPagerController_DEBUG: onNewTimelineUpdate_pbSize")
                    // assume that we are moving towards the edge

                    // TODO: test-cases
                    run rightShift@ {
                        if (previousTimeline.currentIndex != timeline.currentIndex) return@rightShift
                        val sizeDiff = previousTimeline.windows.size - timeline.windows.size
                        val pCutHeadCount: Int
                        val pCutHead = run pCutHead@ {
                            repeat(
                                timeline.currentIndex + 1
                            ) stage@ { stage ->
                                repeat(timeline.currentIndex + 1 - stage) { i ->
                                    if (timeline.windows[i] != previousTimeline.windows[stage + i]) return@stage
                                    if (i == (timeline.currentIndex + 1 - stage) - 1) {
                                        pCutHeadCount = stage
                                        return@pCutHead previousTimeline.windows.subList(fromIndex = stage, toIndex = timeline.windows.size)
                                    }
                                }
                            }
                            return@rightShift
                        }
                        if (pCutHead.isEmpty()) return@rightShift

                        val cCutTailCount: Int
                        val cCutTail = run cCutTail@ {
                            repeat(
                                (pCutHeadCount + sizeDiff + 1)
                                    .coerceAtMost(timeline.windows.size - timeline.currentIndex)
                            ) stage@ { stage ->
                                repeat(
                                    timeline.windows.size - timeline.currentIndex - stage
                                ) { i ->
                                    if (previousTimeline.windows[previousTimeline.windows.lastIndex - i] != timeline.windows[timeline.windows.lastIndex - stage - i]) return@stage
                                    if (i == timeline.windows.size - timeline.currentIndex - stage - 1) {
                                        cCutTailCount = stage
                                        return@cCutTail timeline.windows.subList(fromIndex = 0, toIndex = timeline.windows.size - stage)
                                    }
                                }
                            }
                            return@rightShift
                        }
                        if (cCutTail.isEmpty()) return@rightShift

                        rightShift = true
                        shiftCount = max(pCutHeadCount, cCutTailCount)
                    }
                    // TODO: test-cases
                    if (!rightShift) run leftShift@ {
                        // if previous timeline index is equal or less, assume we are moving to the right.
                        if (previousTimeline.currentIndex <= timeline.currentIndex) return@leftShift
                        val sizeDiff = previousTimeline.windows.size - timeline.windows.size
                        val indexDiff = previousTimeline.currentIndex - timeline.currentIndex
                        if (indexDiff != sizeDiff) return@leftShift
                        if (sizeDiff > timeline.windows.size) return@leftShift
                        val cCutHeadCount: Int
                        val cCutHead = run cCutHead@ {
                            repeat(timeline.currentIndex + 1) stage@ { stage ->
                                repeat(timeline.currentIndex + 1 - stage) { i ->
                                    if (timeline.windows[stage + i] != previousTimeline.windows[i]) return@stage
                                    if (i == (timeline.currentIndex + 1 - stage) - 1) {
                                        cCutHeadCount = stage
                                        return@cCutHead timeline.windows.subList(fromIndex = stage, toIndex = timeline.windows.size)
                                    }
                                }
                            }
                            return@leftShift
                        }
                        if (cCutHead.isEmpty()) return@leftShift
                        val pCutTailCount: Int
                        val pCutTail = run pCutTail@ {
                            repeat(
                                (cCutHeadCount + sizeDiff + 1)
                                    .coerceAtMost(timeline.windows.size - timeline.currentIndex)
                            ) stage@ { stage ->
                                repeat(timeline.windows.size - timeline.currentIndex - stage) { i ->
                                    if (previousTimeline.windows[previousTimeline.windows.lastIndex - stage - i] != timeline.windows[timeline.windows.lastIndex - i]) return@stage
                                    if (i == timeline.windows.size - timeline.currentIndex - stage - 1) {
                                        pCutTailCount = stage
                                        return@pCutTail timeline.windows.subList(fromIndex = 0, toIndex = timeline.windows.size - stage)
                                    }
                                }
                            }
                            return@leftShift
                        }
                        if (pCutTail.isEmpty()) return@leftShift
                        leftShift = true
                        shiftCount = max(cCutHeadCount, pCutTailCount)
                    }
                }
            }
        }

        when {
            shiftNone -> {
                // no structural change, usually from timeline observer
                Timber.d("PlaybackPagerController_DEBUG: onNewTimelineUpdate_noShift")
                if (swipeUpdate) {
                    snapToTimelineCurrentWindow(timeline)
                }
            }
            rightShift -> {
                // moving forward
                Timber.d("PlaybackPagerController_DEBUG: onNewTimelineUpdate_rightShift_$shiftCount")
                timelineShiftRight(
                    timeline,
                    previousTimeline,
                    shiftCount,
                    skipAnimate
                )
            }
            leftShift -> {
                // moving backward
                Timber.d("PlaybackPagerController_DEBUG: onNewTimelineUpdate_leftShift_$shiftCount")
                timelineShiftLeft(
                    timeline,
                    previousTimeline,
                    shiftCount,
                    skipAnimate
                )
            }
            else -> {
                // can't deduce.
                Timber.d("PlaybackPagerController_DEBUG: onNewTimelineUpdate_unknownShift")
                timelineShiftUnknown(timeline, previousTimeline, stepHint)
            }
        }
    }

    private fun timelineShiftRight(
        timeline: PlaybackPagerTimeline,
        previousTimeline: PlaybackPagerTimeline,
        stepCount: Int,
        skipAnimate: Boolean
    ) {
         timelineShiftRemeasure(
            timeline = timeline,
            previousTimeline = previousTimeline,
            stepCount = stepCount,
            direction = +1,
            ov = skipAnimate,
            userDragInstance = latestUserDragInstance,
            userDragFlingInstance = latestUserDragFlingInstance
        )
        if (skipAnimate) {
            return
        }
        applyTimelineShiftAnimated(
            correction = NewTimelineCorrection(
                timeline,
                previousTimeline,
            )
        )
    }

    private fun timelineShiftLeft(
        timeline: PlaybackPagerTimeline,
        previousTimeline: PlaybackPagerTimeline,
        stepCount: Int,
        skipAnimate: Boolean
    ) {
        timelineShiftRemeasure(
            timeline = timeline,
            previousTimeline = previousTimeline,
            stepCount = stepCount,
            direction = -1,
            ov = skipAnimate,
            userDragInstance = latestUserDragInstance,
            userDragFlingInstance = latestUserDragFlingInstance
        )
        if (skipAnimate) {
            return
        }
        applyTimelineShiftAnimated(
            correction = NewTimelineCorrection(
                timeline,
                previousTimeline
            )
        )
    }

    private fun timelineShiftRemeasure(
        timeline: PlaybackPagerTimeline,
        previousTimeline: PlaybackPagerTimeline,
        stepCount: Int,
        direction: Int,
        ov: Boolean,
        userDragInstance: UserDragInstance?,
        userDragFlingInstance: UserDragFlingInstance?
    ) {
        check(direction == 1 || direction == -1) {
            "timelineShiftRemeasure direction must be either 1 or -1"
        }
        val beforePageDiff = timeline.currentIndex - previousTimeline.currentIndex
        Timber.d("PlaybackPagerController_DEBUG: timelineShiftRemeasure(timeline=${timeline.toDebugString()}, previousTimeline=${previousTimeline.toDebugString()}, stepCount=$stepCount, direction=$direction, beforePageDiff=$beforePageDiff), firstVisiblePage=${layoutState.firstVisiblePage}, currentPage=${layoutState.currentPage}, scrollOffset=${layoutState.scrollOffset}, skipAnimate=$ov")
        _renderData.value = PlaybackPagerRenderData(
            timeline = timeline
        )
        var ignoreScrollOffset = false
        layoutState.scrollToPosition(
            firstVisiblePageIndex = run {
                val firstVisiblePage = layoutState.firstVisiblePage
                val firstVisiblePageDiff = timeline.currentIndex - firstVisiblePage + (stepCount * direction) - beforePageDiff
                if (ov) {
                    if (direction == 1) {
                        userDragInstance?.shiftRight(stepCount, timeline.currentIndex)
                        userDragFlingInstance?.shiftRight(stepCount, timeline.currentIndex)
                    } else {
                        userDragInstance?.shiftLeft(stepCount, timeline.currentIndex)
                        userDragFlingInstance?.shiftLeft(stepCount, timeline.currentIndex)
                    }
                    val page = timeline.currentIndex - firstVisiblePageDiff
                    if (page < 0 && -page < previousTimeline.currentIndex) {
                        ignoreScrollOffset = true
                        return@run timeline.currentIndex
                    }
                    return@run page
                }
                val index = run index@ {
                    if (firstVisiblePageDiff.sign == direction.sign) {
                        val page = timeline.currentIndex - firstVisiblePageDiff
                        if (page < 0 && -page < previousTimeline.currentIndex) {
                            ignoreScrollOffset = true
                            return@index timeline.currentIndex
                        }
                        return@index page
                    }
                    if (layoutState.currentPage == timeline.currentIndex + (stepCount * direction) - beforePageDiff) {
                        return@index timeline.currentIndex
                    }
                    timeline.currentIndex - (1 * direction)
                }
                userDragInstance?.apply {
                    moveCenter(timeline.currentIndex)
                }
                userDragFlingInstance?.apply {
                    moveCenter(timeline.currentIndex)
                }
                index
            },
            scrollOffset = if (!ignoreScrollOffset) layoutState.scrollOffset else 0
        )
        userDragInstance?.remeasureUpdateCurrentPage(layoutState.currentPage)
        userDragFlingInstance?.remeasureUpdateCurrentPage(layoutState.currentPage)
    }

    private fun applyTimelineShiftAnimated(
        correction: NewTimelineCorrection
    ) {
        checkInMainLooper()
        _scrollCorrection?.let { current ->
            if (current.isActive) {
                current.newCorrectionOverride(correction)
            }
            if (!correction.isActive) {
                return
            }
        }
        // TODO: maybe we can let the fling instance to continue
        latestUserDragFlingInstance?.newTimelineCorrectionOverride()
        val drag = latestUserDragInstance?.apply {
            newCorrectionOverride()
        }
        animateToTimelineCurrentWindow(
            correction,
            debugPerformerName = "timelineShiftAnimated"
        ).also { anim ->
            drag?.let {
                // invoke immediately on cancelling state
                @OptIn(InternalCoroutinesApi::class)
                anim.invokeOnCompletion(onCancelling = true) { ex ->
                    drag.correctionOverrideEnd(interrupted = ex != null)
                }
            }
        }
    }

    private fun timelineShiftUnknown(
        timeline: PlaybackPagerTimeline,
        previousTimeline: PlaybackPagerTimeline,
        directionHint: Int? = null
    ) {
        directionHint?.let {
            timelineShiftUnknownFade(timeline, previousTimeline, it)
            return
        }
        timelineShiftSnap(timeline = timeline)
    }

    private fun timelineShiftUnknownFade(
        timeline: PlaybackPagerTimeline,
        previousTimeline: PlaybackPagerTimeline,
        direction: Int
    ) {
        // TODO
        timelineShiftSnap(timeline = timeline)
    }

    private fun timelineShiftSnap(
        timeline: PlaybackPagerTimeline,
    ) {
        Timber.d("PlaybackPagerController_DEBUG: timelineShiftSnap(timeline=$timeline, currentPage=${layoutState.currentPage})")
        checkInMainLooper()
        val correction = NewTimelineCorrection(
            timeline,
            _renderData.value?.timeline ?: PlaybackPagerTimeline.UNSET
        )
        _scrollCorrection?.let { current ->
            if (current.isActive) {
                current.newCorrectionOverride(correction)
            }
            if (!correction.isActive) {
                return
            }
        }
        _scrollCorrection = correction
        latestUserDragInstance?.reset()
        latestUserDragFlingInstance?.newTimelineCorrectionOverride()
        _renderData.value = PlaybackPagerRenderData(timeline = timeline)
        snapToTimelineCurrentWindow(timeline)
        correction.initScroll(Job().apply { complete() })
    }

    private var _scrollCorrection: ScrollCorrection? by mutableStateOf(null)

    private fun animateToTimelineCurrentWindow(
        correction: NewTimelineCorrection,
        debugPerformerName: String?
    ): Job {
        _scrollCorrection?.let { current ->
            if (current.isActive) {
                current.newCorrectionOverride(correction)
            }
            if (!correction.isActive) {
                return requireNotNull(correction.cancellationAsJob())
            }
        }
        _scrollCorrection = correction
        return coroutineScope.launch(Dispatchers.Main, CoroutineStart.UNDISPATCHED) {
            animateToTimelineCurrentWindowSuspend(correction.timeline, correction.targetPage, debugPerformerName)
        }.also { correction.initScroll(worker = it) }
    }

    private fun animateToTimelineCurrentWindow(
        correction: FlingCorrection,
        debugPerformerName: String?
    ): Job {
        _scrollCorrection?.let { current ->
            if (current.isActive) {
                current.newCorrectionOverride(correction)
            }
            if (!correction.isActive) {
                return requireNotNull(correction.cancellationAsJob())
            }
        }
        _scrollCorrection = correction
        return coroutineScope.launch(Dispatchers.Main, CoroutineStart.UNDISPATCHED) {
            animateToTimelineCurrentWindowSuspend(correction.timeline, correction.targetPage, debugPerformerName)
        }.also { correction.initScroll(worker = it) }
    }


    private fun animateToTimelineCurrentWindow(
        correction: ScrollCorrection,
        debugPerformerName: String?
    ): Job? {
        if (correction is NewTimelineCorrection) {
            return animateToTimelineCurrentWindow(correction, debugPerformerName)
        }
        if (correction is FlingCorrection) {
            return animateToTimelineCurrentWindow(correction, debugPerformerName)
        }
        return null
    }

    private fun dispatchAnimatedFlingCorrection(
        page: Int
    ) {
        val tl = _renderData.value?.timeline ?: PlaybackPagerTimeline.UNSET
        val correction = FlingCorrection(
            tl,
            page.coerceIn(tl.windows.indices)
        )
        animateToTimelineCurrentWindow(
            correction,
            debugPerformerName = "animateFlingCorrection(page=$page, ci=${tl.currentIndex})"
        )
    }

    private var animateToTimelineCurrentWindowSuspend_c = 0
    private suspend fun animateToTimelineCurrentWindowSuspend(
        timeline: PlaybackPagerTimeline,
        currentWindow: Int? = null,
        debugPerformerName: String? = null
    ) {
        animateToTimelineCurrentWindowSuspend_c++
        checkInMainLooper()
        val MaxPagesForAnimateScroll = 3
        val currentPage = layoutState.currentPage
        val page = (currentWindow ?: timeline.currentIndex).coerceIn(timeline.windows.indices)
        val targetPage = page
        val pageOffsetFraction = 0f
        // If our future page is too far off, that is, outside of the current viewport
        var currentPosition = currentPage
        val visiblePages = pagerLayoutInfo.visiblePagesInfo
        val firstVisiblePageIndex = visiblePages.first().index
        val lastVisiblePageIndex = visiblePages.last().index
        if (((page > currentPage && page > lastVisiblePageIndex) ||
                    (page < currentPage && page < firstVisiblePageIndex)) &&
            abs(page - currentPage) >= MaxPagesForAnimateScroll
        ) {

            val preJumpPosition = if (page > currentPage) {
                (page - visiblePages.size).coerceAtLeast(currentPosition)
            } else {
                page + visiblePages.size.coerceAtMost(currentPosition)
            }

            // Pre-jump to 1 viewport away from destination page, if possible
            layoutState.scrollToPosition(preJumpPosition, 0)
            currentPosition = preJumpPosition
        }

        val pagerLayoutInfo = pagerLayoutInfo

        val pageAvailableSpace = pagerLayoutInfo.pageSize
        val distanceToSnapPosition = pagerLayoutInfo.closestPageToSnapPosition?.let {
            @OptIn(ExperimentalFoundationApi::class)
            density.calculateDistanceToDesiredSnapPosition(
                mainAxisViewPortSize = pagerLayoutInfo.mainAxisViewportSize,
                beforeContentPadding = pagerLayoutInfo.beforeContentPadding,
                afterContentPadding = pagerLayoutInfo.afterContentPadding,
                itemSize = pagerLayoutInfo.pageSize,
                itemOffset = it.offset,
                itemIndex = it.index,
                snapPositionInLayout = SnapAlignmentStartToStart
            )
        } ?: 0f

        val targetOffset = targetPage * pageAvailableSpace
        val currentOffset = currentPosition * pageAvailableSpace
        val pageOffsetToSnappedPosition =
            distanceToSnapPosition + pageOffsetFraction * pageAvailableSpace

        val displacement = targetOffset - currentOffset + pageOffsetToSnappedPosition

        Timber.d("PlaybackPagerController_DEBUG: @${Integer.toHexString(System.identityHashCode(this))}_animateToTimelineCurrentWindowSuspend, displacement=$displacement, pageOffsetToSnappedPosition=$pageOffsetToSnappedPosition, targetOffset=$targetOffset, currentOffset=$currentOffset")

        withContext(AndroidUiDispatcher.Main) {
            var p = 0f
            animate(0f, displacement, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { currentValue, _ ->
                Timber.d("PlaybackPagerController_DEBUG: @${Integer.toHexString(System.identityHashCode(this@PlaybackPagerController))}_animateToTimelineCurrentWindowSuspend, onFrame, active=$isActive")
                ensureActive()
                p += performScroll((currentValue - p), "animateToTimelineCurrentWindowSuspend(c=$animateToTimelineCurrentWindowSuspend_c, performer=$debugPerformerName)")
            }
        }
    }

    private fun snapToTimelineCurrentWindow(timeline: PlaybackPagerTimeline) {
        layoutState.scrollToPosition(
            firstVisiblePageIndex = timeline.currentIndex.coerceAtLeast(0),
            scrollOffset = 0
        )
    }

    private fun emptyTimelineCancelInteractions() {
        latestUserDragInstance?.emptyTimelineOverride()
        latestUserDragFlingInstance?.emptyTimelineOverride()
        _scrollCorrection?.emptyTimelineOverride()
    }

    private fun performScroll(
        distance: Float,
        debugPerformerName: String? = null
    ): Float {
        return layoutState.scrollBy(distance, debugPerformerName)
    }


    fun userPerformDragScroll(
        distance: Float,
        connection: UserDragInstance
    ): Float {
        connection.ensureActive()
        val beforePage = layoutState.currentPage
        val centerPage = _renderData.value?.timeline?.currentIndex?.coerceAtLeast(0) ?: return 0f
        val scroll = performScroll(distance, "userPerformDragScroll")
        connection.apply {
            Timber.d("userPerformDragScroll, pre firstDragPage=$firstDragPage, beforePage=$beforePage, centerPage=$centerPage")
            onScrollBy(pixels = distance, centerPage = centerPage, beforePage = beforePage, afterPage = layoutState.currentPage)
            Timber.d("userPerformDragScroll, post firstDragPage=$firstDragPage, beforePage=$beforePage, centerPage=$centerPage")
        }
        return scroll
    }

    fun performFlingScroll(
        distance: Float,
        connection: UserDragFlingInstance
    ): Float {
        connection.ensureActive()
        val beforePage = layoutState.currentPage
        val centerPage = _renderData.value?.timeline?.currentIndex?.coerceAtLeast(0) ?: return 0f
        // TODO: if the fling already consumed newPage, coerce the distance or throw exception
        connection.flingTracker?.let { tracker ->
            tracker.onFlingBy(distance)
            if (tracker.incorrect) {
                connection.onUnexpectedDirection(beforePage)
                throw CancellationException("fling tracker: unexpected direction, alreadyForward=${tracker.alreadyForward}, alreadyBackward=${tracker.alreadyBackward}, snapped=${tracker.alreadySnapped}, passedSnap=${tracker.passedSnapOffset}, initialVelocity=${tracker.initialVelocity}, snapOffset=${tracker.currentSnapOffset}, startCenter=${tracker.startCenter} ")
            }
        }
        val scroll = performScroll(
            distance,
            "performFlingScroll(beforePage=$beforePage, centerPage=$centerPage), source=${connection.source.toString()}"
        )
        connection
            .apply {
                onScrollBy(pixels = distance, centerPage = centerPage, beforePage = beforePage, afterPage = layoutState.currentPage)
            }
            .run {
                consumeNewPage()
                    ?.let { (center, beforePage, afterPage) ->
                        val pageChangeDirection = (afterPage - beforePage).sign
                        if (pageChangeDirection != connection.dragScrollDirection) {
                            connection.onUnexpectedDirection(beforePage)
                            // the fling can bounce, we don't want that, temporary fix
                            throw CancellationException("other fling direction expected (expected=${connection.dragScrollDirection}, got=${pageChangeDirection})")
                        }
                        userPerformScrollFlingChangePage(beforePage, afterPage)
                    }
                    ?: run {
                        if (isScrolledOverConsumedPage()) {
                            onPageChangedNonConsumable(beforePage)
                        }
                    }
            }
        return scroll
    }

    fun userDragScrollEnd(
        instance: UserDragInstance
    ) {
        userPerformScrollDone(scrollInstance = instance, swipeInstance = latestUserSwipeInstance)
    }

    fun userDragFlingScrollEnd(
        instance: UserDragFlingInstance
    ) {
        userPerformFlingDone(instance = instance)
    }

    private var scrollInstanceSkip = 0

    private var swipePageOverride: Int? = null
    private fun userPerformScrollDone(
        scrollInstance: UserDragInstance,
        swipeInstance: UserSwipeInstance?
    ) {
        swipeInstance?.endUserDragOverride()
        Timber.d("PlaybackPagerController_DEBUG: userPerformScrollDone(instance=$scrollInstance, currentPage=${layoutState.currentPage}, swipePageOverride=$swipePageOverride, timeline.currentIndex=${_renderData.value?.timeline?.currentIndex}, firstDragPage=${scrollInstance.firstDragPage}, latestDragPage=${scrollInstance.latestDragResultPage}, dragPageShift=${scrollInstance.dragPageShift})")
        if (!scrollInstance.isActive) {
            return
        }
        val firstDragPage = scrollInstance.firstDragPage
            ?: run {

                // no firstDragPage means that we are not dragging anywhere but is considered a scroll
                scrollInstance.apply { markNoDragMovement() }

                if (scrollInstance.isDragOverridingTimelineCorrector) {
                    // maybe: resume velocity decay ?
                    // todo: decide whether this is considered a swipe, YT-M bugged when tried
                    animateToTimelineCurrentWindow(
                        FlingCorrection(_renderData.value?.timeline ?: PlaybackPagerTimeline.UNSET),
                        "userScrollDone_noDragMovement_overTlCorrector"
                    )
                } else if (scrollInstance.isDragOverridingFlingScroll) {
                    // maybe: resume velocity decay ?
                    // todo: decide whether this is considered a swipe, YT-M bugged when tried

                    val timeline = _renderData.value?.timeline ?: PlaybackPagerTimeline.UNSET
                    animateToTimelineCurrentWindow(
                        FlingCorrection(
                            timeline,
                            targetPage = layoutState.currentPage.coerceIn(timeline.windows.indices)
                        ),
                        "userScrollDone_noDragMovement_overFlCorrector"
                    )
                } else {
                    // we don't expect this, but we can just recover
                    snapToTimelineCurrentWindow(_renderData.value?.timeline ?: PlaybackPagerTimeline.UNSET)
                }
                return
            }
        val latestDragResultPage = requireNotNull(scrollInstance.latestDragResultPage) {
            "UserDragInstance latestDragResultPage was null when firstDragPage was not"
        }
        // maybe: timeout fling
        when (latestDragResultPage) {
            // expect fling to correct this
            firstDragPage -> {}
            firstDragPage + 1 -> {
                scrollInstance.apply {
                    markConsumedSwipe(1)
                }
            }
            firstDragPage - 1 -> {
                scrollInstance.apply {
                    markConsumedSwipe(-1)
                }
            }
            // if the drag end up at more than we expect,
            else -> {
                scrollInstance.apply {
                    markOverBoundDrag(latestDragResultPage - firstDragPage)
                }
            }
        }
    }

    private fun userPerformFlingDone(
        instance: UserDragFlingInstance
    ) {
        Timber.d("PlaybackPagerController_DEBUG: userPerformFlingDone(instance=$instance, cancelled=${instance.isCancelled}, firstVisiblePage=$firstVisiblePage, scrollOffset=$firstVisiblePageOffset, currentPage=$currentPage, shouldCorrect=${instance.shouldCorrect}, shouldCorrectToPage=${instance.shouldCorrectToPage})")
        if (!instance.isCancelled) {
            if (layoutState.scrollOffset != 0) {
                snapToTimelineCurrentWindow(_renderData.value?.timeline ?: PlaybackPagerTimeline.UNSET)
            }
        } else if (instance.shouldCorrect) {
            dispatchAnimatedFlingCorrection(
                instance.shouldCorrectToPage
                    ?: instance.shouldCorrectToPageBy?.let { by ->
                        layoutState.currentPage + by
                    }
                    // no specific page
                    // TODO: be more explicit on where it should settle,
                    ?: layoutState.currentPage
            )
        }
    }

    private fun userPerformScrollFlingChangePage(
        startPage: Int,
        endPage: Int,
    ) {
        Timber.d("PlaybackPagerController_DEBUG: userPerformScrollFlingChangePage(startPage=$startPage, endPage=$endPage, currentPage=${layoutState.currentPage}, timeline=${_renderData.value?.timeline?.toDebugString()}, pageOverride=$swipePageOverride)")
        when (endPage) {
            startPage + 1 -> userSwipeNextPage()
            startPage - 1 -> userSwipePreviousPage()
            // if the scroll end up at more than we expect
            // we don't expect this, fallback
            // maybe: log
            else -> snapToTimelineCurrentWindow(_renderData.value?.timeline?: PlaybackPagerTimeline.UNSET)
        }
    }

    private var _ac = 0
    private var _pageSwipeBy = 0
    private fun userSwipeNextPage() {
        Timber.d("PlaybackPagerController_DEBUG: userSwipeToNextPage(token=$_ac)")
        checkInMainLooper()
        val token = ++_ac
        val swipeInstance = UserSwipeInstance()
            .apply {
                latestUserSwipeInstance?.let { derive(it) }
            }
        latestUserSwipeInstance = swipeInstance
        if (token == 1) {
            stopTimelineUpdater()
        }
        coroutineScope.launch(Dispatchers.Main, CoroutineStart.UNDISPATCHED) {
            val seekResult = runCatching {
                player.seekToNextMediaItemAsync(PlaybackPagerEagerRangePlaceholder).await().getOrThrow()
            }
            if (token != _ac) {
                return@launch
            }
            _ac = 0
            if (seekResult.isSuccess) {
                onNewTimelineUpdate(
                    PlaybackPagerTimeline(windows = seekResult.getOrThrow().items, currentIndex = seekResult.getOrThrow().currentIndex),
                    _renderData.value!!.timeline,
                    1,
                    swipeInstance.ignoreAnimate,
                    swipeUpdate = true,
                    debugSourceName = "userSwipeNextPage"
                )
            } else {
                snapToTimelineCurrentWindow(_renderData.value!!.timeline)
            }
            latestUserSwipeInstance = null
            initTimelineUpdater()
            Timber.d("PlaybackPagerController_DEBUG: userSwipeToNextPage_end")
        }
    }

    private fun userSwipePreviousPage() {

        Timber.d("PlaybackPagerController_DEBUG: userSwipeToPrevPage(token=$_ac)")
        checkInMainLooper()
        val token = ++_ac
        val swipeInstance = UserSwipeInstance()
            .apply {
                latestUserSwipeInstance?.let { derive(it) }
            }
        latestUserSwipeInstance = swipeInstance
        if (token == 1) {
            stopTimelineUpdater()
        }
        coroutineScope.launch(Dispatchers.Main, CoroutineStart.UNDISPATCHED) {
            val seekResult = runCatching {
                player.seekToPreviousMediaItemAsync(PlaybackPagerEagerRangePlaceholder).await().getOrThrow()
            }
            if (token != _ac) {
                return@launch
            }
            _ac = 0
            if (seekResult.isSuccess) {
                onNewTimelineUpdate(
                    PlaybackPagerTimeline(windows = seekResult.getOrThrow().items, currentIndex = seekResult.getOrThrow().currentIndex),
                    _renderData.value!!.timeline,
                    -1,
                    swipeInstance.ignoreAnimate,
                    swipeUpdate = true,
                    debugSourceName = "userSwipePreviousPage"
                )
            } else {
                snapToTimelineCurrentWindow(_renderData.value!!.timeline)
            }
            latestUserSwipeInstance = null
            initTimelineUpdater()
            Timber.d("PlaybackPagerController_DEBUG: userSwipeToPreviousPage_end")
        }
    }

    /**
     *  Updates the state with the new calculated scroll position and consumed scroll.
     */
    internal fun onMeasureResult(result: PlaybackPagerMeasureResult) {
        Timber.d("PlaybackPagerLayoutState_DEBUG: onMeasureResult(currentPage=${layoutState.currentPage}, thread=${Thread.currentThread()})")
        layoutState.onMeasureResult(result)
        Timber.d("PlaybackPagerLayoutState_DEBUG: onMeasureResult_post(currentPage=${layoutState.currentPage})")
    }

    class UserScrollInstance(
        val timeline: PlaybackPagerTimeline
    ) {



    }

    class UserDragInstance(
        private val lifetime: Job
    ) {

        private var kindInit = true

        var correctionOverride = false
            private set

        var flingTimelineCorrectionOverride = false
            private set

        var consumedSwipe = false
            private set

        var consumedSwipeDirection: Int? = null
            private set

        var overBound = false
            private set

        var overBoundBy: Int? = null
            private set

        var noMovement = false
            private set

        // maybe: kindSet
        var isDragOverridingTimelineCorrector = false
            private set

        var isDragOverridingFlingScroll = false
            private set

        var firstDragPage: Int? = null
            private set

        var dragPageShift: Int? = null
            private set

        var latestDragResultPage: Int? = null
            private set

        var latestDragCenterPage: Int? = null
            private set

        var latestScrollDirection: Int? = null
            private set

        var dragEnded by mutableStateOf(false)
            private set

        val isActive: Boolean
            get() = lifetime.isActive

        val noFling: Boolean
            get() = lifetime.isCancelled || flingTimelineCorrectionOverride || noMovement

        fun onScrollBy(
            pixels: Float,
            centerPage: Int,
            beforePage: Int,
            afterPage: Int
        ) {
            if (firstDragPage == null) {
                firstDragPage = beforePage
            }
            latestDragCenterPage = centerPage
            latestDragResultPage = afterPage
            if (pixels != 0f) {
                latestScrollDirection = sign(pixels).toInt()
            }
        }

        fun dragEnd(): Any {
            dragEnded = true
            return this
        }

        fun ensureActive() {
            lifetime.ensureActive()
        }

        private fun cancel() {
            lifetime.cancel()
        }

        fun emptyTimelineOverride() {
            cancel()
        }

        // [g, h, i, j, k, l]

        fun shiftRight(
            stepCount: Int,
            center: Int
        ) {
            Timber.d("PlaybackPagerController_DEBUG: userDragInstance@${Integer.toHexString(System.identityHashCode(this))}_shiftRight(stepCount=$stepCount, center=$center), firstDragPage=$firstDragPage, latestDragResultPage=$latestDragResultPage, latestDragCenterPage=$latestDragCenterPage")
            val firstDragPage = firstDragPage ?: return
            val currentDragPage = requireNotNull(latestDragResultPage)
            val currentCenter = requireNotNull(latestDragCenterPage)
            val centerDiff = center - currentCenter
            val diff = stepCount
            this.firstDragPage = (firstDragPage - diff).coerceAtLeast(center)
            this.latestDragResultPage = (currentDragPage - diff).coerceAtLeast(center)
            this.latestDragCenterPage = center
            Timber.d("PlaybackPagerController_DEBUG: userDragInstance@${Integer.toHexString(System.identityHashCode(this))}_shiftRight(stepCount=$stepCount, center=$center)_END, firstDragPage=${this.firstDragPage}, latestDragResultPage=$latestDragResultPage, latestDragCenterPage=$latestDragCenterPage")
        }

        fun shiftLeft(
            stepCount: Int,
            center: Int
        ) {
            Timber.d("PlaybackPagerController_DEBUG: userDragInstance@${Integer.toHexString(System.identityHashCode(this))}_shiftLeft(stepCount=$stepCount, center=$center), firstDragPage=$firstDragPage, latestDragResultPage=$latestDragResultPage, latestDragCenterPage=$latestDragCenterPage")
            val firstDragPage = firstDragPage ?: return
            val currentDragPage = checkNotNull(latestDragResultPage)
            val currentCenter = checkNotNull(latestDragCenterPage)
            val centerDiff = center - currentCenter
            val diff = stepCount
            this.firstDragPage = (firstDragPage + diff).coerceAtMost(center)
            this.latestDragResultPage = (currentDragPage + diff).coerceAtMost(center)
            this.latestDragCenterPage = center
            Timber.d("PlaybackPagerController_DEBUG: userDragInstance@${Integer.toHexString(System.identityHashCode(this))}_shiftLeft(stepCount=$stepCount, center=$center)_END, firstDragPage=${this.firstDragPage}, latestDragResultPage=$latestDragResultPage, latestDragCenterPage=$latestDragCenterPage")
        }

        fun reset() {
            firstDragPage = null
            latestDragResultPage = null
            latestDragCenterPage = null
        }

        fun startCenter(
            center: Int
        ) {
            check(firstDragPage == null) {
                "startCenter when firstDragPage is not NULL"
            }
            firstDragPage = center
            latestDragCenterPage = center
            latestDragResultPage = center
        }

        fun moveCenter(
            center: Int
        ) {
            firstDragPage ?: return
            checkNotNull(latestDragResultPage)
            checkNotNull(latestDragCenterPage)
            this.firstDragPage = center
            this.latestDragResultPage = center
            this.latestDragCenterPage = center
        }

        fun remeasureUpdateCurrentPage(
            page: Int
        ) {
            Timber.d("PlaybackPagerController_DEBUG: userDragInstance@${Integer.toHexString(System.identityHashCode(this))}_remeasureUpdateCurrentPage(page=$page), firstDragPage=$firstDragPage, latestDragResultPage=$latestDragResultPage, latestDragCenterPage=$latestDragCenterPage")
            firstDragPage ?: return
            checkNotNull(latestDragResultPage)
            checkNotNull(latestDragCenterPage)
            this.latestDragResultPage = page
        }

        fun initKind(
            isOverridingTimelineCorrector: Boolean,
            isOverridingFlingCorrector: Boolean,
        ) {
            check(kindInit) {
                "UserDragInstance.initKind is called multiple times"
            }
            kindInit = false
            this.isDragOverridingTimelineCorrector = isOverridingTimelineCorrector
            this.isDragOverridingFlingScroll = isOverridingFlingCorrector
        }

        fun ignoreUserDrag(): Boolean {
            return correctionOverride
        }

        fun markConsumedSwipe(direction: Int) {
            require(direction == 1 || direction == -1)
            consumedSwipe = true
            consumedSwipeDirection = direction
        }

        fun markOverBoundDrag(
            countByDirection: Int
        ) {
            overBound = true
            overBoundBy = countByDirection
        }

        fun markNoDragMovement() {
            noMovement = true
        }

        fun newUserDragOverride() {
            cancel()
        }

        private var cvr = 0
        fun newCorrectionOverride() {
            Timber.d("PlaybackPagerController_DEBUG: UserDragInstance_newCorrectionOverride")
            // end must be called when the correction is completed before starting a new one
            check(++cvr == 1) {
                "Playback Pager: CorrectionOverride imbalance, check internal coroutines API, info=(point=new, cvr=$cvr)"
            }
            correctionOverride = true
            if (dragEnded) {
                flingTimelineCorrectionOverride = true
            }
        }

        fun correctionOverrideEnd(
            interrupted: Boolean
            // maybe: expect only cancelling,
        ) {
            Timber.d("PlaybackPagerController_DEBUG: UserDragInstance_correctionOverrideEnd(interrupted=$interrupted)")
            // must be called when the correction is completed before starting a new one
            check(--cvr == 0) {
                "Playback Pager: CorrectionOverride imbalance, check internal coroutines API, info=(point=end, cvr=$cvr)"
            }
            if (!correctionOverride) {
                // maybe: log
                return
            }
            correctionOverride = false
        }

        fun flingKey(): Any {
            return if (dragEnded) this else Any()
        }
    }

    class UserDragFlingInstance(
        private val lifetime: Job,
        val debugName: String
    ) {

        private var init = true

        private var newPageConsumable: Boolean? = null

        private var pageConsumed: Int? = null

        var source: Any? = null
            private set

        var firstScrollPage: Int? = null
            private set

        var latestScrollResultPage: Int? = null
            private set

        var latestScrollCenterPage: Int? = null
            private set

        var flingEnded by mutableStateOf(false)
            private set

        var unexpectedDirection = false
            private set

        var unexpectedPageChange = false
            private set

        var disallowDirection = false
            private set

        var flingBack = false
            private set

        var shouldCorrect = false
            private set

        var shouldCorrectToPage: Int? = null
            private set

        var shouldCorrectToPageBy: Int? = null
            private set

        var dragScrollDirection: Int? = null
            private set

        var flingTracker: DragFlingTracker? = null
            private set

        val isCancelled: Boolean
            get() = lifetime.isCancelled

        val isActive: Boolean
            get() = lifetime.isActive

        val dragSwipeDirection: Int?
            get() {
                source.castOrNull<UserDragInstance>()?.let { drag ->
                    drag.consumedSwipeDirection?.let { return it }
                }
                return null
            }

        fun initKind(
            source: UserDragInstance,
            initialVelocity: Float,
            currentSnapOffset: Float,
        ) {
            if (!init) return
            init = false
            this.source = source
            sourceApply(source)
            if (!isActive) return
            this.flingTracker = DragFlingTracker(
                initialVelocity,
                currentSnapOffset,
                dragSwipeDirection == null
            )
        }

        fun onScrollBy(
            pixels: Float,
            centerPage: Int,
            beforePage: Int,
            afterPage: Int
        ) {
            Timber.d("PlaybackPagerController_DEBUG_v: flingScroll@${debugName}_onScrollBy(pixels=$pixels, beforePage=$beforePage, afterPage=$afterPage)")
            if (firstScrollPage == null) {
                Timber.d("PlaybackPagerController_DEBUG: flingScroll@${debugName}_onScrollBy(pixels=$pixels, beforePage=$beforePage, afterPage=$afterPage)_newFirstScrollPage")
                firstScrollPage = beforePage
            }
            if (latestScrollResultPage != afterPage) {
                Timber.d("PlaybackPagerController_DEBUG: flingScroll@${debugName}_onScrollBy(pixels=$pixels, beforePage=$beforePage, afterPage=$afterPage)_newLatestScrollPage")
                latestScrollResultPage = afterPage
                onPageChanged()
            }
            if (latestScrollCenterPage != centerPage) {
                latestScrollCenterPage = centerPage
            }
        }


        fun flingEnd() {
            flingEnded = true
        }

        fun flingEndedExceptionally() {
            flingEnded = true
            cancel()
        }

        fun ensureActive() {
            lifetime.ensureActive()
        }

        fun isPageChanged(): Boolean {
            return firstScrollPage
                ?.let { start ->
                    start != requireNotNull(latestScrollResultPage)
                }
                ?: false
        }

        // make cancel private, make reason explicit
        private fun cancel() {
            lifetime.cancel()
        }

        fun shiftRight(
            stepCount: Int,
            center: Int
        ) {
            val firstDragPage = firstScrollPage ?: return
            val currentDragPage = checkNotNull(latestScrollResultPage)
            val currentCenter = checkNotNull(latestScrollCenterPage)
            val diff = stepCount
            this.firstScrollPage = (firstDragPage - diff).coerceAtLeast(center)
            this.latestScrollResultPage = (currentDragPage - diff).coerceAtLeast(center)
            this.latestScrollCenterPage = center
        }

        fun shiftLeft(
            stepCount: Int,
            center: Int
        ) {
            val firstDragPage = firstScrollPage ?: return
            val currentDragPage = checkNotNull(latestScrollResultPage)
            val currentCenter = checkNotNull(latestScrollCenterPage)
            val diff = stepCount
            this.firstScrollPage = (firstDragPage + diff).coerceAtMost(center)
            this.latestScrollResultPage = (currentDragPage + diff).coerceAtMost(center)
            this.latestScrollCenterPage = center
        }

        fun reset() {
            firstScrollPage = null
            latestScrollResultPage = null
            latestScrollCenterPage = null
        }

        fun startCenter(
            center: Int
        ) {
            firstScrollPage = center
            latestScrollResultPage = center
            latestScrollCenterPage = center
        }

        fun moveCenter(
            center: Int
        ) {
            firstScrollPage ?: return
            checkNotNull(latestScrollResultPage)
            checkNotNull(latestScrollCenterPage)
            this.firstScrollPage = center
            this.latestScrollResultPage = center
            this.latestScrollCenterPage = center
        }

        fun remeasureUpdateCurrentPage(
            page: Int
        ) {
            firstScrollPage ?: return
            checkNotNull(latestScrollResultPage)
            checkNotNull(latestScrollCenterPage)
            this.latestScrollResultPage = page
        }

        fun onUnexpectedDirection(
            beforePage: Int? = null
        ) {
            val source = this.source
            if (source is UserDragInstance) {
                shouldCorrect = true
                if (source.consumedSwipe) {
                    shouldCorrectToPage = source.latestDragResultPage ?: beforePage
                }
            }
            this.unexpectedDirection = true
            cancel()
        }

        fun onDisallowDirection() {
            val source = this.source
            if (source is UserDragInstance) {
                shouldCorrect = true
                if (source.consumedSwipe) {
                    shouldCorrectToPage = source.latestDragResultPage
                }
            }
            this.disallowDirection = true
            cancel()
        }

        fun onFlingBack() {
            val source = this.source
            if (source is UserDragInstance) {
                shouldCorrect = true
                if (source.consumedSwipe) {
                    shouldCorrectToPage = source.firstDragPage
                }
            }
            this.flingBack = true
            cancel()
        }

        fun onPageChangedNonConsumable(
            beforePage: Int
        ) {
            val source = this.source
            if (source is UserDragInstance) {
                shouldCorrect = true
                if (source.consumedSwipe) {
                    shouldCorrectToPage = source.latestDragResultPage ?: beforePage
                }
            }
            this.unexpectedPageChange
            cancel()
        }

        fun consumeNewPage(): PageMove? {
            if (isPageChanged() && newPageConsumable == true) {
                newPageConsumable = false
                return PageMove(
                    center = requireNotNull(latestScrollCenterPage),
                    beforePage = requireNotNull(firstScrollPage),
                    afterPage = requireNotNull(latestScrollResultPage).also { pageConsumed = it }
                )
            }
            return null
        }

        fun isScrolledOverConsumedPage(): Boolean {
            return newPageConsumable == false &&
                    requireNotNull(latestScrollResultPage) != requireNotNull(pageConsumed)
        }

        fun newUserDragOverride() {
            cancel()
            shouldCorrect = false
        }

        fun newUserDragFlingOverride() {
            cancel()
            shouldCorrect = false
        }

        fun newTimelineCorrectionOverride() {
            cancel()
            shouldCorrect = false
        }

        fun emptyTimelineOverride() {
            cancel()
            shouldCorrect = false
        }

        fun expectedDragScrollVelocitySign(): Int? {
            (source as? UserDragInstance)?.let { drag ->
                drag.latestScrollDirection?.let { return -it }
            }
            return null
        }

        private fun sourceApply(
            source: UserDragInstance,
        ) {
            if (source.flingTimelineCorrectionOverride || source.noMovement) {
                cancel()
                return
            }
            if (source.overBound) {
                cancel()
                shouldCorrect = true
                shouldCorrectToPageBy = -requireNotNull(source.overBoundBy)
                return
            }
            source.latestScrollDirection?.let {
                check(it == 1 || it == -1) {
                    "sourceApply: unexpected drag scroll direction, direction=$it"
                }
                dragScrollDirection = it
            }
        }

        private fun onPageChanged() {
            if (newPageConsumable == null) {
                newPageConsumable = true
            }
        }
    }

    class UserSwipeInstance(

    ) {
        private var drag: Int = 0

        val ignoreAnimate
            get() = drag > 0

        fun newUserDragOverride() {
            check(++drag == 1) {
                "PlaybackPagerController_newUserDragOverride: drag imbalance, expected end to be called before any new drag"
            }
        }

        private fun deriveUserDragOverride() {
            check(++drag == 1) {
                "PlaybackPagerController_deriveUserDragOverride: drag imbalance, expected end to be called before any new drag"
            }
        }

        fun endUserDragOverride() {
            check(--drag == 0) {
                "PlaybackPagerController_endUserDragOverride: drag imbalance, expected end to not be called twice"
            }
        }

        fun derive(swipeInstance: UserSwipeInstance) {
            if (swipeInstance.drag > 0) {
                deriveUserDragOverride()
            }
        }
    }

    // TODO: check for side-page pass
    class DragFlingTracker(
        val initialVelocity: Float,
        val currentSnapOffset: Float,
        val startCenter: Boolean
    ) {


        private var initialFlingDirection: Int? = null
        private var flingAcc = 0f

        // whether or not it passed the snap position
        var passedSnapOffset = false
            private set

        var alreadySnapped = false
            private set

        // maybe: change to int ?
        var alreadyForward = false
            private set

        var alreadyBackward = false
            private set

        var incorrect: Boolean = false
            private set

        // TODO
        fun onFlingBy(
            pixels: Float
        ) {

            if (!startCenter) {
                onFlingByEdge(pixels)
                return
            }

            if (incorrect) {
                return
            }

            val displacement = -pixels

            val flingDistanceSign = sign(displacement).toInt()
            val snapOffsetSign = sign(currentSnapOffset).toInt()

            fun checkSnapped() {
                // TODO: check clamped <0.5f
                this.alreadySnapped == this.alreadySnapped || currentSnapOffset == flingAcc
            }

            if (initialFlingDirection == null || initialFlingDirection == 0) {
                initialFlingDirection = flingDistanceSign
            }
            if (initialVelocity == 0f) {
                // if this instance starts with 0 velocity, it can only go backward to snap-position,
                // TODO: unless specified otherwise, in that case the `pixels` must gradually decay into the snap-position
                flingAcc += displacement
                if (snapOffsetSign == flingDistanceSign) {
                    // sign is the same, forward or stay
                    if (currentSnapOffset == 0f) {
                        if (displacement == 0f) {
                            // stay
                            checkSnapped()
                            return
                        }
                    }
                    // zero-velocity, no going forward
                    this.incorrect = true
                    return
                }
                // sign is different, backward

                if (alreadySnapped) {
                    // already snapped, no going anywhere
                    this.incorrect = true
                    return
                }
                checkSnapped()
                if (alreadySnapped) {
                    // snapped as a result of this fling
                    return
                }
                val postFlingSign = sign(currentSnapOffset - flingAcc).toInt()
                if (postFlingSign != snapOffsetSign) {
                    // we passed through
                    passedSnapOffset = true
                    this.incorrect = true
                    return
                }
                return
            }
            // non-zero velocity
            if (initialFlingDirection == 0) {
                checkSnapped()
                return
            }
            if (snapOffsetSign == 0) {
                if (flingDistanceSign != 0) {
                    this.incorrect = true
                    return
                }
                checkSnapped()
                return
            }
            if (flingDistanceSign == 0) {
                // not moving
                checkSnapped() // ?
                return
            }
            flingAcc += displacement

            if (initialFlingDirection == snapOffsetSign) {
                // we started forward, non zero

                val forward = flingDistanceSign == snapOffsetSign

                if (alreadyBackward) {
                    if (forward) {
                        // already backward, no forward
                        this.incorrect = true
                        return
                    }
                    val postFlingSign = sign(currentSnapOffset - flingAcc).toInt()
                    if (postFlingSign != snapOffsetSign) {
                        // we passed through
                        passedSnapOffset = true
                        this.incorrect = true
                        return
                    }
                    checkSnapped()
                    return
                }

                // hasn't go backward yet

                if (!alreadyForward) {
                    // this is first fling,
                    check(forward)
                    alreadyForward = true
                    return
                }

                // already go forward, no backward

                if (forward) {
                    // still going forward
                    return
                }

                // goes backward

                alreadyBackward = true
                checkSnapped()
            }
        }


        private fun onFlingByEdge(
            pixels: Float
        ) {

            if (incorrect) {
                return
            }

            val displacement = -pixels

            val flingDistanceSign = sign(displacement).toInt()
            val snapOffsetSign = sign(currentSnapOffset).toInt()

            fun checkSnapped() {
                // TODO: check clamped <0.5f
                this.alreadySnapped == this.alreadySnapped || currentSnapOffset == flingAcc
            }

            if (initialFlingDirection == null || initialFlingDirection == 0) {
                initialFlingDirection = flingDistanceSign
            }

            // it can only go forward to snap position,
            // TODO: unless specified otherwise, in that case the `pixels` must gradually decay into the snap-position
            flingAcc += displacement
            if (
                flingDistanceSign == 0 ||
                // inverse snapOffsetSign as forward is edge to center
                -snapOffsetSign == flingDistanceSign
            ) {
                // forward or stay

                // maybe: disallow multiple stay after forward ?
                if (currentSnapOffset == 0f) {
                    if (displacement == 0f) {
                        // stay
                        checkSnapped()
                        return
                    }
                }
                if (alreadySnapped) {
                    // already snapped, no going anywhere
                    this.incorrect = true
                    return
                }
                checkSnapped()
                if (alreadySnapped) {
                    // snapped as a result of this fling
                    return
                }
                val postFlingSign = sign(currentSnapOffset - flingAcc).toInt()
                if (postFlingSign != snapOffsetSign) {
                    // we passed through
                    passedSnapOffset = true
                    this.incorrect = true
                    return
                }
                return
            }
            // sign is different, no going backward
            this.incorrect = true
            return
        }
    }



    //

    val correctingTimeline: Boolean
        @SnapshotRead get() {
            return _scrollCorrection
                ?.castOrNull<NewTimelineCorrection>()
                ?.isActive == true
        }

    val userDragScrolling
        @SnapshotRead get() = latestUserDragInstance?.dragEnded == false

    val userDragFlinging
        @SnapshotRead get() = latestUserDragFlingInstance?.flingEnded == false

    val correctingFling: Boolean
        @SnapshotRead get() {
            return _scrollCorrection
                ?.castOrNull<FlingCorrection>()
                ?.isActive == true
        }

    private val layoutState = PlaybackPagerLayoutState()

    val modifier
        get() = layoutState.modifier

    // maybe: join them
    private var latestUserDragInstance: UserDragInstance? by mutableStateOf(null)
    private var latestUserDragFlingInstance: UserDragFlingInstance? by mutableStateOf(null)
    private var latestUserSwipeInstance: UserSwipeInstance? by mutableStateOf(null)

    fun newUserDragScroll(): UserDragInstance {
        Timber.d("PlaybackPagerController_DEBUG: newUserDragScroll")
        checkInMainLooper()
        latestUserDragInstance?.newUserDragOverride()
        latestUserDragFlingInstance?.newUserDragOverride()
        latestUserSwipeInstance?.newUserDragOverride()
        val currentScrollCorrection = _scrollCorrection
        val wasCorrectingTimeline = currentScrollCorrection is NewTimelineCorrection &&
                currentScrollCorrection.isActive
        val wasCorrectingFling = !wasCorrectingTimeline &&
                currentScrollCorrection is FlingCorrection && currentScrollCorrection.isActive
        currentScrollCorrection?.newUserDragOverride()
        return UserDragInstance(lifetime = SupervisorJob())
            .also {
                latestUserDragInstance = it
                Timber.d("PlaybackPagerController_DEBUG: newUserDragScroll(instance=$it, wasCorrectingTimeline=$wasCorrectingTimeline, wasCorrectingFling=$wasCorrectingFling)")
            }
            .apply {
                initKind(
                    isOverridingTimelineCorrector = wasCorrectingTimeline,
                    isOverridingFlingCorrector = wasCorrectingFling,
                )
                if (wasCorrectingTimeline || wasCorrectingFling) {
                    // gesture override correction
                    // down gesture when we are currently scrolling is treated as a "scroll"
                    startCenter(checkNotNull(currentScrollCorrection).targetPage)
                }
            }
    }

    fun newUserDragFlingScroll(
        key: Any,
        // ask to report the initial velocity, and check if that aligns with the drag
        // reason: there's an issue with the velocity tracker
        scrollAxisVelocity: Float
    ): UserDragFlingInstance? {
        Timber.d("PlaybackPagerController_DEBUG: newUserDragFlingScroll(key=$key, vel=$scrollAxisVelocity)")
        if (key !is UserDragInstance) return null
        if (latestUserDragInstance != key) return null
        return UserDragFlingInstance(lifetime = SupervisorJob(), debugName = "")
            .also {
                latestUserDragFlingInstance = it
                Timber.d("PlaybackPagerController_DEBUG: newUserDragFlingScroll(instance=$it)")
            }
            .apply {
                initKind(
                    source = key,
                    initialVelocity = scrollAxisVelocity,
                    currentSnapOffset = layoutState.distanceToSnapPosition,
                )
                if (isActive) {

                    dragSwipeDirection?.let { direction ->
                        // if the user already dragged to nearby pages but then fling back,
                        if (scrollAxisVelocity != 0f && direction != sign(-scrollAxisVelocity).toInt()) {
                            // treat it as going back
                            onFlingBack()
                            return@apply
                        }
                        if (direction == 1) userSwipeNextPage() else userSwipePreviousPage()
                    }

                    if (scrollAxisVelocity != 0f) {
                        // non-zero velocity must fling forward of latest drag-direction before going backward
                        expectedDragScrollVelocitySign()?.let { vel ->
                            if (vel != sign(scrollAxisVelocity).toInt()) {
                                onUnexpectedDirection()
                            }
                        }
                    }
                }
            }
    }

    class PageMove(
        val center: Int,
        val beforePage: Int,
        val afterPage: Int
    ) {
        operator fun component1() = center
        operator fun component2() = beforePage
        operator fun component3() = afterPage
    }

    class NewTimelineCorrection(
        timeline: PlaybackPagerTimeline,
        val previousTimeline: PlaybackPagerTimeline,
        targetPage: Int = timeline.currentIndex.coerceAtLeast(0),
    ) : ScrollCorrection(timeline, targetPage) {

        override fun newCorrectionOverride(correction: ScrollCorrection) {
            if (correction is NewTimelineCorrection) {
                timelineCorrectionOverride(correction)
            } else if (correction is FlingCorrection) {
                flingCorrectionOverride(correction)
            }
        }

        fun timelineCorrectionOverride(
            timelineCorrection: NewTimelineCorrection,
            debugStr: String? = null
        ) {
            checkInMainLooper()
            if (scroller == null || !scroller!!.isActive) {
                return
            }
            scroller?.cancel(CancellationException("newTimelineCorrectionOverride, debugStr=$debugStr"))
            checkScrollerCancellationEffect()
        }

        private fun flingCorrectionOverride(
            fling: FlingCorrection,
        ) {
            checkInMainLooper()
            if (scroller?.isActive != true) {
                return
            }
            // TODO: do we really want to do this ?
            fling.denyOverride("")
        }
    }

    class FlingCorrection(
        timeline: PlaybackPagerTimeline,
        targetPage: Int = timeline.currentIndex.coerceAtLeast(0)
    ) : ScrollCorrection(timeline, targetPage) {

        override fun newCorrectionOverride(
            correction: ScrollCorrection
        ) {
            if (correction is NewTimelineCorrection) {
                correctionOverride("")
            } else if (correction is FlingCorrection) {
                correctionOverride("")
            }
        }

        fun correctionOverride(
            debugStr: String? = null
        ) {
            checkInMainLooper()
            if (scroller?.isActive != true) {
                return
            }
            scroller?.cancel(CancellationException("newTimelineCorrectionOverride, debugStr=$debugStr"))
            checkScrollerCancellationEffect()
        }


        fun denyOverride(
            debugStr: String? = null
        ) {
            val worker = Job()
            worker.cancel(CancellationException("Deny Override, debugStr=$debugStr"))
            initScrollPreCancelled(worker)
        }

        override fun newUserDragOverride() {
            // TODO decide what to actually do
            super.newUserDragOverride()
        }
    }

    abstract class ScrollCorrection internal constructor(
        val timeline: PlaybackPagerTimeline,
        val targetPage: Int
    ) {

        var isScrolling by mutableStateOf(false)
            protected set

        var cancelled: Boolean = false
            private set

        var completed: Boolean = false
            private set

        val isActive: Boolean
            get() = !cancelled && !completed

        protected var cancellationEx: Exception? = null
            private set

        protected var scroller: Job? = null
            private set

        private var scrollPreConsumed = false

        fun ensureScrollActive() {
            checkInMainLooper()
            if (cancelled) {
                throw requireNotNull(cancellationEx) {
                    "TimelineCorrection was cancelled without exception"
                }
            }
            if (completed) {
                throw CancellationException("Completed")
            }
        }

        fun cancellationAsJob(): Job? {
            if (!cancelled) return null
            val job = Job()
            val cause = requireNotNull(cancellationEx)
            job.cancel(CancellationException(cause.message, cause))
            return job
        }

        protected fun initScrollPreCancelled(
            worker: Job
        ) = initScroll(worker, preCancelled = true)

        fun initScroll(worker: Job) = initScroll(worker, preCancelled = false)

        private fun initScroll(
            worker: Job,
            preCancelled: Boolean
        ) {
            checkInMainLooper()
            check(scroller == null) {
                "initScroll was already called"
            }

            if (!isActive) {
                worker.cancel()
                return
            }

            scrollPreConsumed = preCancelled
            isScrolling = true

            scroller = worker

            if (preCancelled) worker.cancel()

            @OptIn(InternalCoroutinesApi::class)
            worker.invokeOnCompletion(onCancelling = true) { ex ->
                scrollEnd(
                    ex = ex.castOrNull<CancellationException>() ?: CancellationException(null, ex),
                )
            }

            if (preCancelled) checkScrollerCancellationEffect()
        }

        private fun scrollEnd(
            ex: CancellationException? = null,
        ) {
            checkInMainLooper()
            ex?.let {
                cancellationEx = ex
                cancelled = true
            }
            isScrolling = false
            completed = true
        }

        abstract fun newCorrectionOverride(
            correction: ScrollCorrection
        )

        open fun newUserDragOverride() {
            scroller?.let {
                if (it.isActive) {
                    it.cancel()
                    checkScrollerCancellationEffect()
                    return
                }
            }
            cancellationEx = CancellationException("NewUserDragOverride")
            cancelled = true
        }

        open fun emptyTimelineOverride() {
            scroller?.let {
                if (it.isActive) {
                    it.cancel()
                    checkScrollerCancellationEffect()
                    return
                }
            }
            cancellationEx = CancellationException("NewUserDragOverride")
            cancelled = true
        }

        protected fun checkScrollerCancellationEffect() {
            check(cancelled && cancellationEx != null) {
                "Cancelling Scroller did not apply changes immediately, check internal coroutines API"
            }
        }
    }
}

// TODO: make so that we can change the range dynamically
const val PlaybackPagerEagerRangePlaceholder = 5