package dev.dexsr.klio.player.android.presentation.root.main

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.unit.Density
import dev.dexsr.klio.android.base.checkInMainLooper
import dev.dexsr.klio.base.compose.SnapshotRead
import dev.dexsr.klio.player.android.presentation.root.main.pager.AwaitFirstLayoutModifier
import dev.dexsr.klio.player.android.presentation.root.main.pager.PlaybackPagerScrollableState
import dev.dexsr.klio.player.android.presentation.root.main.pager.SnapAlignmentStartToStart
import dev.dexsr.klio.player.android.presentation.root.main.pager.calculateDistanceToDesiredSnapPosition
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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

    internal var density by mutableStateOf<Density>(Density(1f, 1f))

    private val scrollPosition = PlaybackPagerScrollPosition()

    private val _pagerLayoutInfoState = mutableStateOf<PlaybackPagerLayoutInfo>(
        PlaybackPagerLayoutInfo.UNSET
    )

    internal val pagerLayoutInfo
        @SnapshotRead get() = _pagerLayoutInfoState.value

    /**
     * The [Remeasurement] object associated with our layout. It allows us to remeasure
     * synchronously during scroll.
     */
    internal var remeasurement: Remeasurement? by mutableStateOf(null)
        private set

    /**
     * The modifier which provides [remeasurement].
     */
    internal val remeasurementModifier = object : RemeasurementModifier {
        override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
            this@PlaybackPagerController.remeasurement = remeasurement
        }
    }

    internal val interactionSource = object : MutableInteractionSource {

        override val interactions: Flow<Interaction> = emptyFlow()

        override suspend fun emit(interaction: Interaction) {
            Timber.d("DEBUG: interactionSource_emit=$interaction")
        }

        override fun tryEmit(interaction: Interaction): Boolean {
            Timber.d("DEBUG: interactionSource_tryEmit=$interaction")
            return false
        }
    }

    internal val awaitLayoutModifier = AwaitFirstLayoutModifier()

    private var settledPageState = mutableIntStateOf(0)

    /**
     * Only used for testing to confirm that we're not making too many measure passes
     */
    internal var numMeasurePasses: Int = 0
        private set

    val gestureScrollableState = PlaybackPagerScrollableState(
        pagerController = this
    )

    val isScrollInProgress: Boolean
        // TODO: @SnapshotRead
        get() = correctingTimeline or userDragScrolling or userDragFlinging or correctingFling

    val currentPage: Int
        @SnapshotRead get() = scrollPosition.currentPage

    var scrollToBeConsumed = 0f
        private set

    var canScrollForward: Boolean by mutableStateOf(false)
        private set
    var canScrollBackward: Boolean by mutableStateOf(false)
        private set

    val renderData
        @SnapshotRead get() = _renderData.value

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
        @SnapshotRead get() = scrollPosition.firstVisiblePage

    val firstVisiblePageOffset: Int
        @SnapshotRead get() = scrollPosition.scrollOffset

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
        scrollPosition.requestPosition(timeline.currentIndex, 0)
        remeasure()
    }

    private fun onNewTimelineUpdate(
        timeline: PlaybackPagerTimeline,
        previousTimeline: PlaybackPagerTimeline,
        stepHint: Int?,
        skipAnimate: Boolean = false,
        swipeUpdate: Boolean = false,
        debugSourceName: String = ""
    ) {
        Timber.d("PlaybackPagerController_DEBUG: onNewTimelineUpdate(timeline=${timeline.toDebugString()}, previousTimeline=${previousTimeline.toDebugString()}, debugSourceName=$debugSourceName)")
        checkInMainLooper()
        if (timeline.windows.isEmpty() || timeline.currentIndex !in timeline.windows.indices) {
            _renderData.value = PlaybackPagerRenderData(timeline = PlaybackPagerTimeline.UNSET)
            cancelAllInteraction()
            remeasure()
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
                                    if (timeline.windows[i] != previousTimeline.windows[stage + i]) return@stage
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
                                    if (previousTimeline.windows[previousTimeline.windows.lastIndex - i] != timeline.windows[timeline.windows.lastIndex - stage - i]) return@stage
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
                                    if (previousTimeline.windows[previousTimeline.windows.lastIndex - stage - i] != timeline.windows[timeline.windows.lastIndex - i]) return@stage
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

                    if (
                        !rightShift && !leftShift &&
                        cCutHeadCount == 0 && cCutTailCount == -1 &&
                        pCutHeadCount == 0 && pCutTailCount == -1
                    ) {
                        check(timeline.windows == previousTimeline.windows)
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
        // the current scroll position is more than the target, we don't want to animate left
        if (scrollPosition.currentPage > timeline.currentIndex + stepCount &&
            latestUserDragInstance?.dragEnded != false &&
            latestUserDragFlingInstance?.flingEnded != false
        ) {
            latestUserDragInstance?.reset()
            latestUserDragFlingInstance?.cancel()
            timelineShiftSnap(timeline)
            return
        }
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
            correction = TimelineCorrection(
                timeline,
                previousTimeline
            )
        )
    }

    private fun timelineShiftLeft(
        timeline: PlaybackPagerTimeline,
        previousTimeline: PlaybackPagerTimeline,
        stepCount: Int,
        skipAnimate: Boolean
    ) {

        // the current scroll position is more than the target, we don't want to animate right
        if (scrollPosition.currentPage < timeline.currentIndex - stepCount &&
            latestUserDragInstance?.dragEnded != false &&
            latestUserDragFlingInstance?.flingEnded != false
        ) {
            timelineShiftSnap(timeline)
            latestUserDragInstance?.reset()
            latestUserDragFlingInstance?.cancel()
            return
        }
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
            correction = TimelineCorrection(
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
        // TODO:
        // previousTimeline=PlaybackPagerTimeline@ab24173(windows=[MediaStore_28_AUDIO_28, MediaStore_28_AUDIO_36, MediaStore_28_AUDIO_37, MediaStore_28_AUDIO_38, MediaStore_28_AUDIO_39, MediaStore_28_AUDIO_40, MediaStore_28_AUDIO_41, MediaStore_28_AUDIO_42, MediaStore_28_AUDIO_43, MediaStore_28_AUDIO_44], currentIndex=4),
        // timeline=PlaybackPagerTimeline@24bb3ec(windows=[MediaStore_28_AUDIO_39, MediaStore_28_AUDIO_40, MediaStore_28_AUDIO_41, MediaStore_28_AUDIO_42, MediaStore_28_AUDIO_43, MediaStore_28_AUDIO_44, MediaStore_28_AUDIO_45, MediaStore_28_AUDIO_46, MediaStore_28_AUDIO_73, MediaStore_28_AUDIO_74, MediaStore_28_AUDIO_75], currentIndex=5)
        // stepCount=5, direction=1, beforePageDiff=1, firstVisiblePage=2, currentPage=3, scrollOffset=363, skipAnimate=false
        check(direction == 1 || direction == -1) {
            "timelineShiftRemeasure direction must be either 1 or -1"
        }
        val beforePageDiff = timeline.currentIndex - previousTimeline.currentIndex
        Timber.d("PlaybackPagerController_DEBUG: timelineShiftRemeasure(timeline=${timeline.toDebugString()}, previousTimeline=${previousTimeline.toDebugString()}, stepCount=$stepCount, direction=$direction, beforePageDiff=$beforePageDiff), firstVisiblePage=${scrollPosition.firstVisiblePage}, currentPage=${scrollPosition.currentPage}, scrollOffset=${scrollPosition.scrollOffset}, skipAnimate=$ov")
        _renderData.value = PlaybackPagerRenderData(
            timeline = timeline
        )
        var ignoreScrollOffset = false
        scrollPosition.requestPosition(
            firstVisiblePageIndex = run {
                val firstVisiblePage = scrollPosition.firstVisiblePage
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
                    if (scrollPosition.currentPage == timeline.currentIndex + (stepCount * direction) - beforePageDiff) {
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
            scrollOffset = if (!ignoreScrollOffset) scrollPosition.scrollOffset else 0
        )
        remeasure()
        userDragInstance?.remeasureUpdateCurrentPage(scrollPosition.currentPage)
        userDragFlingInstance?.remeasureUpdateCurrentPage(scrollPosition.currentPage)
    }

    private fun applyTimelineShiftAnimated(
        correction: TimelineCorrection
    ) {
        checkInMainLooper()
        _timelineAnimate?.cancel()
        latestUserDragFlingInstance?.cancel()
        val drag = latestUserDragInstance?.apply {
            newCorrectionOverride()
        }
        animateToTimelineCurrentWindow(correction.timeline).also { anim ->
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
        Timber.d("PlaybackPagerController_DEBUG: timelineShiftSnap(timeline=$timeline, currentPage=${scrollPosition.currentPage})")
        checkInMainLooper()
        _timelineAnimate?.cancel()
        latestUserDragInstance?.reset()
        latestUserDragFlingInstance?.cancel()
        _renderData.value = PlaybackPagerRenderData(timeline = timeline)
        snapToTimelineCurrentWindow(timeline)
    }

    private var _timelineAnimate: Job? = null
    private var _flingCorrection: Job? = null
    private fun animateToTimelineCurrentWindow(
        timeline: PlaybackPagerTimeline,
    ): Job {
        checkInMainLooper()
        _timelineAnimate?.cancel()
        _flingCorrection?.cancel()
        return coroutineScope.launch(Dispatchers.Main, CoroutineStart.UNDISPATCHED) {
            animateToTimelineCurrentWindowSuspend(timeline)
        }.also { _timelineAnimate = it }
    }

    private fun animateFlingCorrection(
        page: Int
    ) {
        _timelineAnimate?.let {
            if (it.isActive) return
        }
        _flingCorrection?.let {
            it.cancel()
        }
        coroutineScope.launch(Dispatchers.Main, CoroutineStart.UNDISPATCHED) {
            animateToTimelineCurrentWindowSuspend(
                PlaybackPagerTimeline(
                    windows = (_renderData.value?.timeline?.windows ?: PlaybackPagerTimeline.UNSET.windows),
                    currentIndex = page.coerceIn(_renderData.value?.timeline?.windows?.indices ?: 0..0)
                ),
                debugPerformerName = "animateFlingCorrection(page=$page)"
            )
        }.also { _flingCorrection = it }
    }

    private var animateToTimelineCurrentWindowSuspend_c = 0
    private suspend fun animateToTimelineCurrentWindowSuspend(
        timeline: PlaybackPagerTimeline,
        debugPerformerName: String? = null
    ) {
        animateToTimelineCurrentWindowSuspend_c++
        checkInMainLooper()
        val MaxPagesForAnimateScroll = 3
        val currentPage = scrollPosition.currentPage
        val page = timeline.currentIndex
        val targetPage = page
        val pageOffsetFraction = 0
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
            scrollPosition.requestPosition(preJumpPosition, 0)
            remeasurement?.forceRemeasure()
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

        withContext(AndroidUiDispatcher.Main) {
            var p = 0f
            animate(0f, displacement, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { currentValue, _ ->
                ensureActive()
                p += -performScroll(-(currentValue - p), "animateToTimelineCurrentWindowSuspend(c=$animateToTimelineCurrentWindowSuspend_c, performer=$debugPerformerName)")
            }
        }
    }

    private fun snapToTimelineCurrentWindow(timeline: PlaybackPagerTimeline) {
        scrollPosition.requestPosition(
            firstVisiblePageIndex = timeline.currentIndex.coerceAtLeast(0),
            scrollOffset = 0
        )
        remeasure()
    }

    private fun remeasure(): Boolean? {
        remeasurement?.forceRemeasure()
            ?: return null
        return true
    }

    private fun cancelAllInteraction() {
        latestUserDragInstance?.cancel()
        latestUserDragFlingInstance?.cancel()
    }

    private fun performScroll(
        distance: Float,
        debugPerformerName: String? = null
    ): Float {
        Timber.d("PlaybackPagerController_DEBUG_s: performScroll(distance=$distance, currentScrollToBeConsumed=$scrollToBeConsumed, canScrollForward=$canScrollForward, performer=$debugPerformerName)")
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
            val preScrollToBeConsumed = scrollToBeConsumed
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

    fun userPerformDragScroll(
        distance: Float,
        connection: UserDragInstance
    ): Float {
        connection.ensureActive()
        val beforePage = scrollPosition.currentPage
        val centerPage = _renderData.value?.timeline?.currentIndex?.coerceAtLeast(0) ?: return 0f
        val scroll = -performScroll(-distance, "userPerformDragScroll")
        connection.apply {
            onScrollBy(pixels = distance, centerPage = centerPage, beforePage = beforePage, afterPage = scrollPosition.currentPage)
        }
        return scroll
    }

    fun acceptFlingScroll(
        distance: Float,
        connection: UserDragFlingInstance
    ): Boolean {
        connection.ensureActive()
        connection.dragScrollDirection?.let { direction ->
            val flingDirection = sign(distance).toInt()
            connection
        }

        return false
    }

    fun performFlingScroll(
        distance: Float,
        connection: UserDragFlingInstance
    ): Float {
        connection.ensureActive()
        val beforePage = scrollPosition.currentPage
        val centerPage = _renderData.value?.timeline?.currentIndex?.coerceAtLeast(0) ?: return 0f
        val scroll = -performScroll(-distance, "performFlingScroll, source=${connection.source.toString()}")
        connection
            .apply {
                onScrollBy(pixels = -distance, centerPage = centerPage, beforePage = beforePage, afterPage = scrollPosition.currentPage)
            }
            .run {
                consumeNewPage()?.let { (center, beforePage, afterPage) ->
                    val pageChangeDirection = (afterPage - beforePage).sign
                    if (pageChangeDirection != connection.dragScrollDirection) {
                        connection.onUnexpectedDirection(beforePage)
                        // the fling can bounce, we don't want that, temporary fix
                        throw CancellationException("other fling direction expected (expected=${connection.dragScrollDirection}, got=${pageChangeDirection})")
                    }
                    userPerformScrollFlingChangePage(beforePage, afterPage)
                }
            }
        return scroll
    }

    fun userDragScrollEnd(
        instance: UserDragInstance
    ) {
        userPerformScrollDone(scrollInstance = instance, swipeInstance = null)
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
        scrollInstance.ensureActive()
        Timber.d("PlaybackPagerController_DEBUG: userPerformScrollDone(currentPage=${scrollPosition.currentPage}, swipePageOverride=$swipePageOverride, timeline.currentIndex=${_renderData.value?.timeline?.currentIndex}, firstDragPage=${scrollInstance.firstDragPage}, latestDragPage=${scrollInstance.latestDragResultPage}, dragPageShift=${scrollInstance.dragPageShift})")
        val firstDragPage = scrollInstance.firstDragPage
            ?: run {
                // no firstDragPage means that we are not dragging anywhere but is considered a scroll
                // assume that this is down gesture over an ongoing scroll
                if (scrollInstance.isDragOverridingScroll) {
                    // maybe: resume velocity decay ?
                    // todo: decide whether this is considered a swipe
                    animateToTimelineCurrentWindow(_renderData.value?.timeline ?: PlaybackPagerTimeline.UNSET)
                } else {
                    // we don't expect this, but we can just recover
                    snapToTimelineCurrentWindow(_renderData.value?.timeline ?: PlaybackPagerTimeline.UNSET)
                }
                return
            }
        when (scrollInstance.latestDragResultPage) {
            // expect fling to correct this
            // maybe: timeout
            firstDragPage -> {}
            firstDragPage + 1 -> userSwipeNextPage().also { scrollInstance.markConsumedSwipe() }
            firstDragPage - 1 -> userSwipePreviousPage().also { scrollInstance.markConsumedSwipe() }
            // if the drag end up at more than we expect,
            else -> animateToTimelineCurrentWindow(_renderData.value?.timeline ?: PlaybackPagerTimeline.UNSET)
        }
    }



    private fun userPerformFlingDone(
        instance: UserDragFlingInstance
    ) {
        if (!instance.isCancelled) {
            if (scrollPosition.scrollOffset != 0) {
                snapToTimelineCurrentWindow(_renderData.value?.timeline ?: PlaybackPagerTimeline.UNSET)
            }
        } else if (instance.shouldCorrect) {
            animateFlingCorrection(instance.shouldCorrectToPage ?: _renderData.value?.timeline?.currentIndex ?: 0)
        }
    }

    private fun userPerformScrollFlingChangePage(
        startPage: Int,
        endPage: Int,
    ) {
        Timber.d("PlaybackPagerController_DEBUG: userPerformScrollFlingChangePage(startPage=$startPage, endPage=$endPage, currentPage=${scrollPosition.currentPage}, timeline=${_renderData.value?.timeline?.toDebugString()}, pageOverride=$swipePageOverride)")
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
    private fun userSwipeNextPage() {
        Timber.d("PlaybackPagerController_DEBUG: userSwipeToNextPage")
        checkInMainLooper()
        val token = ++_ac
        val swipeInstance = UserSwipeInstance()
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
                    !swipeInstance.correction.isActive,
                    swipeUpdate = true,
                    debugSourceName = "userSwipeNextPage"
                )
            } else {
                snapToTimelineCurrentWindow(_renderData.value!!.timeline)
            }
            initTimelineUpdater()
            Timber.d("PlaybackPagerController_DEBUG: userSwipeToNextPage_end")
        }
    }

    private fun userSwipePreviousPage() {

        Timber.d("PlaybackPagerController_DEBUG: userSwipeToPrevPage")
        checkInMainLooper()
        val token = ++_ac
        val swipeInstance = UserSwipeInstance()
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
                    !swipeInstance.correction.isActive,
                    swipeUpdate = true,
                    debugSourceName = "userSwipePreviousPage"
                )
            } else {
                snapToTimelineCurrentWindow(_renderData.value!!.timeline)
            }

            initTimelineUpdater()
            Timber.d("PlaybackPagerController_DEBUG: userSwipeToPreviousPage_end")
        }
    }

    /**
     *  Updates the state with the new calculated scroll position and consumed scroll.
     */
    internal fun onMeasureResult(result: PlaybackPagerMeasureResult) {
        Timber.d("PlaybackPagerController: onMeasureResult(scrollToBeConsumed=${scrollToBeConsumed}, consumedScroll=${result.consumedScroll})")
        scrollPosition.updateFromMeasureResult(result)
        scrollToBeConsumed -= result.consumedScroll
        _pagerLayoutInfoState.value = result
        canScrollForward = result.canScrollForward
        canScrollBackward = (result.firstVisiblePage?.index ?: 0) != 0 ||
                result.firstVisiblePageOffset != 0
        numMeasurePasses++
        if (!isScrollInProgress) {
            settledPageState.value = currentPage
        }
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

        var flingCorrectionOverride = false
            private set

        var consumedSwipe = false
            private set

        // maybe: kindSet
        var isDragOverridingScroll = false
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

        var dragEnded = false
            private set

        val isActive: Boolean
            get() = lifetime.isActive

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

        fun cancel() {
            lifetime.cancel()
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
            firstDragPage ?: return
            checkNotNull(latestDragResultPage)
            checkNotNull(latestDragCenterPage)
            this.latestDragResultPage = page
        }

        fun initKind(
            isOverridingScroll: Boolean
        ) {
            check(kindInit) {
                "UserDragInstance.initKind is called multiple times"
            }
            kindInit = false
            this.isDragOverridingScroll = isOverridingScroll
        }

        fun ignoreUserDrag(): Boolean {
            return correctionOverride
        }

        fun markConsumedSwipe() {
            consumedSwipe = true
        }

        fun newUserDragOverride() {
            cancel()
        }

        private var cvr = 0
        private var cvr_s = 0
        fun newCorrectionOverride() {
            Timber.d("PlaybackPagerController_DEBUG: UserDragInstance_newCorrectionOverride")
            // end must be called when the correction is completed before starting a new one
            check(++cvr == 1) {
                "Playback Pager: CorrectionOverride imbalance, check internal coroutines API, info=(point=new, cvr=$cvr)"
            }
            correctionOverride = true
            if (dragEnded) {
                flingCorrectionOverride = true
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

        var source: Any? = null
            private set

        var firstScrollPage: Int? = null
            private set

        var latestScrollResultPage: Int? = null
            private set

        var latestScrollCenterPage: Int? = null
            private set

        var flingEnded = false
            private set

        var shouldCorrect = false
            private set

        var shouldCorrectToPage: Int? = null
            private set

        var dragScrollDirection: Int? = null
            private set

        var consumedSwipe: Boolean? = null
            private set

        val isCancelled: Boolean
            get() = lifetime.isCancelled

        val isActive: Boolean
            get() = lifetime.isActive

        fun initKind(
            source: UserDragInstance
        ) {
            if (!init) return
            init = false
            this.source = source
            sourceApply(source)
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

        fun cancel() {
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
            beforePage: Int,
        ) {
            val source = this.source
            if (source is UserDragInstance) {
                shouldCorrect = true
                if (source.consumedSwipe) {
                    shouldCorrectToPage = beforePage
                }
            }
        }

        fun consumeNewPage(): PageMove? {
            if (isPageChanged() && newPageConsumable == true) {
                newPageConsumable = false
                return PageMove(
                    center = requireNotNull(latestScrollCenterPage),
                    beforePage = requireNotNull(firstScrollPage),
                    afterPage = requireNotNull(latestScrollResultPage)
                )
            }
            return null
        }

        fun newUserDragOverride() {
            cancel()
        }

        fun allowFlingBack(): Boolean {
            val dragInstance = (source as? UserDragInstance)?.let { drag ->
                !drag.consumedSwipe
            } == true
            return dragInstance && consumedSwipe != true
        }

        fun expectedDragVelocitySign(): Int? {
            (source as? UserDragInstance)?.let { drag ->
                drag.latestScrollDirection?.let { return -it }
            }
            return null
        }

        private fun sourceApply(
            source: UserDragInstance
        ) {
            if (source.flingCorrectionOverride) {
                cancel()
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

        val correction = Job()

        fun newUserDragOverride() {
            correction.cancel()
        }
    }

    //

    val correctingTimeline
        get() = _timelineAnimate?.isActive == true

    val userDragScrolling
        get() = latestUserDragInstance?.dragEnded == false

    val userDragFlinging
        get() = latestUserDragFlingInstance?.flingEnded == false

    val correctingFling
        get() = _flingCorrection?.isActive == true

    // maybe: join them
    private var latestUserDragInstance: UserDragInstance? = null
    private var latestUserDragFlingInstance: UserDragFlingInstance? = null
    private var latestUserSwipeInstance: UserSwipeInstance? = null

    fun newUserDragScroll(): UserDragInstance {
        checkInMainLooper()
        latestUserDragInstance?.newUserDragOverride()
        latestUserDragFlingInstance?.newUserDragOverride()
        latestUserSwipeInstance?.newUserDragOverride()
        val correcting = correctingTimeline
        _timelineAnimate?.cancel()
        return UserDragInstance(lifetime = SupervisorJob())
            .apply {
                initKind(
                    isOverridingScroll = correcting
                )
                if (correcting) {
                    // gesture override correction
                    // down gesture when we are currently scrolling is treated as a "scroll"
                    startCenter(_renderData.value?.timeline?.currentIndex?.coerceAtLeast(0) ?: 0)
                }
            }
            .also {
                latestUserDragInstance = it
            }
    }

    fun newUserDragFlingScroll(
        key: Any,
        // ask to report the initial velocity, and check if that aligns with the drag
        // reason: there's an issue with the velocity tracker
    ): UserDragFlingInstance? {
        if (key !is UserDragInstance) return null
        if (latestUserDragInstance != key) return null
        return UserDragFlingInstance(lifetime = SupervisorJob(), debugName = "")
            .apply {
                initKind(source = key)
                // TODO
                _timelineAnimate?.let { if (it.isActive) cancel() }
            }
            .also {
                latestUserDragFlingInstance = it
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

    class TimelineCorrection(
        val timeline: PlaybackPagerTimeline,
        val previousTimeline: PlaybackPagerTimeline,
    ) {


    }
}

// TODO: make so that we can change the range dynamically
const val PlaybackPagerEagerRangePlaceholder = 5

private fun PlaybackPagerController.dragGestureDelta() = if (pagerLayoutInfo.orientation == Orientation.Horizontal) {
    upDownDifference.x
} else {
    upDownDifference.y
}