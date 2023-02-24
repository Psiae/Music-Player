package com.flammky.musicplayer.player.presentation.root

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import com.flammky.musicplayer.base.compose.SnapshotRead
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.media.playback.PlaybackConstants
import com.flammky.musicplayer.player.presentation.controller.PlaybackController
import com.flammky.musicplayer.player.presentation.presenter.PlaybackObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class SliderLayoutComposition(
    val queueData: OldPlaybackQueue,
    val coroutineScope: CoroutineScope,
    val onRequestSeek: suspend (
        expectDuration: Duration,
        percent: Float
    ) -> Deferred<PlaybackController.RequestResult>
) {

    var readyForScrub by mutableStateOf(false)
    var layoutWidthDp by mutableStateOf(Dp.Infinity)

    var remoteSliderPlaybackPosition by mutableStateOf<Duration>(PlaybackConstants.POSITION_UNSET)
        private set
    var remoteSliderTextPlaybackPosition by mutableStateOf<Duration>(PlaybackConstants.POSITION_UNSET)
        private set
    var remotePlaybackDuration by mutableStateOf<Duration>(PlaybackConstants.DURATION_UNSET)
        private set

    val scrubbingResultStack = mutableStateListOf<ScrubbingResult>()

    var consumeSliderValueAnimator = false

    var currentScrubbingInstance by mutableStateOf<ScrubbingInstance?>(null)

    class ScrubbingResult(
        val progress: Float,
        val duration: Duration
    )

    class ScrubbingInstance(
        val startInDuration: Duration,
        val startInPosition: Float
    ) {
        var currentProgress by mutableStateOf<Float>(startInPosition)
        var locked = false
        fun lock() { locked = true }
    }

    fun setNewPlaybackDuration(duration: Duration) {
        remotePlaybackDuration = duration
    }

    fun setNewSliderPosition(duration: Duration) {
        remoteSliderPlaybackPosition = duration
    }

    fun setNewSliderTextPosition(duration: Duration) {
        remoteSliderTextPlaybackPosition = duration
    }

    fun markToBeForgotten() {
        coroutineScope.cancel()
    }

    fun onValueChange(value: Float) {
        check(readyForScrub)
        currentScrubbingInstance
            ?.let {
                check(!it.locked)
                it.currentProgress = value
            }
            ?: run {
                val durationStart = remotePlaybackDuration
                    .takeIf { it.inWholeMilliseconds >= 0f }
                    ?: /* error ? */ return
                currentScrubbingInstance = ScrubbingInstance(durationStart, value)
            }
    }

    fun onValueChangeFinished() {
        currentScrubbingInstance
            ?.let { scrubbingInstance ->
                scrubbingInstance.lock()
                val result = ScrubbingResult(
                    scrubbingInstance.currentProgress,
                    scrubbingInstance.startInDuration
                )
                coroutineScope.launch {
                    scrubbingResultStack.add(result)
                    runCatching {
                        onRequestSeek(result.duration, result.progress * 100).await()
                    }
                    scrubbingResultStack.remove(result)
                }
            }
        currentScrubbingInstance = null
    }

    @Composable
    fun sliderDisplayValue(): Float {
        val switch = currentScrubbingInstance
            ?.let { instance ->
                consumeSliderValueAnimator = true
                if (instance.currentProgress > 0f && instance.startInDuration.isPositive()) {
                    instance.currentProgress
                } else {
                    0f
                }
            }
            ?: run {
                scrubbingResultStack.lastOrNull()
                    ?.let { scrubbingResult ->
                        consumeSliderValueAnimator = true
                        if (scrubbingResult.duration.isPositive()) {
                            scrubbingResult.progress
                        } else {
                            0f
                        }
                    }
            }
            ?: run {
                if (
                    remoteSliderPlaybackPosition.isPositive() &&
                    remotePlaybackDuration.isPositive()
                ) {
                    val target = remoteSliderPlaybackPosition.inWholeMilliseconds.toFloat() /
                            remotePlaybackDuration.inWholeMilliseconds
                    if (consumeSliderValueAnimator) {
                        consumeSliderValueAnimator = false
                        return target
                    }
                    animateFloatAsState(
                        targetValue = target,
                        animationSpec = remember { tween(200) }
                    ).value
                } else {
                    0f
                }
            }
        return switch.coerceIn(0f, 1f)
    }

    @Composable
    fun sliderTextPositionValue(): Duration {
        val switch = currentScrubbingInstance
            ?.let { instance ->
                if (
                    instance.currentProgress > 0f &&
                    instance.startInDuration.isPositive()
                ) {
                    (instance.startInDuration.inWholeMilliseconds * instance.currentProgress)
                        .toDouble()
                        .milliseconds
                } else {
                    Duration.ZERO
                }
            }
            ?: run {
                scrubbingResultStack.lastOrNull()
                    ?.let { scrubbingResult ->
                        if (scrubbingResult.duration.isPositive()) {
                            (scrubbingResult.duration.inWholeMilliseconds * scrubbingResult.progress)
                                .toDouble()
                                .milliseconds
                        } else {
                            Duration.ZERO
                        }
                    }
            }
            ?: run {
                if (
                    remoteSliderPlaybackPosition.isPositive() &&
                    remotePlaybackDuration.isPositive()
                ) {
                    remoteSliderPlaybackPosition
                } else {
                    Duration.ZERO
                }
            }
        return switch.coerceAtLeast(Duration.ZERO)
    }

    @Composable
    fun sliderTextDurationDisplayValue(): Duration {
        val switch = currentScrubbingInstance
            ?.startInDuration
            ?: run {
                scrubbingResultStack.lastOrNull()?.duration
            }
            ?: run {
                remotePlaybackDuration
            }
        return switch.coerceAtLeast(Duration.ZERO)
    }
}

internal class RootPlaybackControlSliderState(
    private val mainComposition: RootPlaybackControlMainScope
) {

    var layoutComposition by mutableStateOf<SliderLayoutComposition?>(null)

    val currentMainQueue: OldPlaybackQueue
        @SnapshotRead get() = mainComposition.currentQueue

    fun incrementQueueReader() {
        mainComposition.currentQueueReaderCount++
    }

    fun decrementQueueReader() {
        mainComposition.currentQueueReaderCount--
    }

    suspend fun requestSeekPositionAsync(
        expectId: String,
        expectDuration: Duration,
        percent: Float
    ): Deferred<PlaybackController.RequestResult> {
        return mainComposition.playbackController.requestSeekPositionAsync(
            expectId,
            expectDuration,
            percent
        )
    }

    fun createPlaybackObserver(): PlaybackObserver {
        return mainComposition.playbackController.createPlaybackObserver()
    }
}

@Composable
internal fun rememberRootPlaybackControlSliderState(
    parentComposition: RootPlaybackControlMainScope
): RootPlaybackControlSliderState {
    return remember(parentComposition) {
        RootPlaybackControlSliderState(parentComposition)
    }
}

internal class RootPlaybackControlSliderApplier(
    private val state: RootPlaybackControlSliderState,
    private val coroutineScope: CoroutineScope
) {

    fun prepareState() {
        state.layoutComposition = null
    }

    @Composable
    fun PrepareCompose() {
        DisposableEffect(
            key1 = this,
            effect = {
                state.incrementQueueReader()
                onDispose { state.decrementQueueReader() }
            }
        )
        LaunchedEffect(
            key1 = this,
            block = {
                snapshotFlow { state.currentMainQueue }
                    .collect { queueData ->
                        state.layoutComposition?.markToBeForgotten()
                        if (queueData === OldPlaybackQueue.UNSET ||
                            queueData.currentIndex == PlaybackConstants.INDEX_UNSET
                        ) {
                            state.layoutComposition = null
                        } else {
                            state.layoutComposition =
                                SliderLayoutComposition(
                                    queueData,
                                    CoroutineScope(
                                        currentCoroutineContext() +
                                                SupervisorJob(currentCoroutineContext().job)
                                    ),
                                    onRequestSeek = { expectDuration, percent ->
                                        state.requestSeekPositionAsync(
                                            expectId = queueData.list[queueData.currentIndex],
                                            expectDuration,
                                            percent
                                        )
                                    }
                                )
                        }
                    }
            }
        )
    }

    fun skipLayoutComposition() {

    }

    @Suppress("NOTHING_TO_INLINE")
    @Composable
    inline fun StartLayoutComposition(
        composition: SliderLayoutComposition,
    ) {

    }

    @Suppress("NOTHING_TO_INLINE")
    @Composable
    inline fun EndLayoutComposition(
        composition: SliderLayoutComposition
    ) {
        DisposableEffect(
            this, composition,
            effect = {
                val playbackObserver = state.createPlaybackObserver()
                val durationCollector = playbackObserver.createDurationCollector()
                val sliderPositionCollector = playbackObserver.createProgressionCollector()
                val sliderTextPositionCollector = playbackObserver.createProgressionCollector()

                durationCollector
                    .apply {
                        composition.coroutineScope.launch {
                            startCollect().join()
                            durationStateFlow.collect { composition.setNewPlaybackDuration(it) }
                        }
                    }

                sliderPositionCollector
                    .apply {
                        setCollectEvent(true)
                        composition.coroutineScope.launch {
                            snapshotFlow { composition.layoutWidthDp }
                                .collect { sliderWidth ->
                                    setIntervalHandler { _, progress, duration, speed ->
                                        if (progress == Duration.ZERO ||
                                            duration == Duration.ZERO ||
                                            speed == 0f
                                        ) {
                                            null
                                        } else {
                                            (duration.inWholeMilliseconds / sliderWidth.value / speed)
                                                .toLong()
                                                .takeIf { it > 100 }?.milliseconds
                                                ?: PlaybackConstants.DURATION_UNSET
                                        }.also {
                                            Timber.d("Playback_Slider_Debug: intervalHandler($it) param: $progress $duration $speed")
                                        }
                                    }
                                }
                        }
                        composition.coroutineScope.launch {
                            startCollectPosition().join()
                            positionStateFlow.collect {
                                composition.setNewSliderPosition(it)
                            }
                        }
                    }

                sliderTextPositionCollector
                    .apply {
                        setCollectEvent(true)
                        composition.coroutineScope.launch {
                            startCollectPosition().join()
                            positionStateFlow.collect {
                                composition.setNewSliderTextPosition(it)
                            }
                        }
                    }

                composition.coroutineScope.launch {
                    snapshotFlow { composition.remotePlaybackDuration }
                        .first { it.isPositive() }
                    composition.readyForScrub = true
                }

                onDispose { playbackObserver.dispose() }
            }
        )
    }
}