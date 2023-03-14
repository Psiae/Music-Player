package com.flammky.musicplayer.player.presentation.root

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import com.flammky.kotlin.common.time.Durations
import com.flammky.musicplayer.base.compose.SnapshotRead
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.media.playback.PlaybackConstants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class SliderLayoutComposition(
    val queueData: OldPlaybackQueue,
    val coroutineScope: CoroutineScope,
    val onRequestSeek: suspend (
        expectDuration: Duration,
        percent: Float
    ) -> Deferred<Result<Boolean>>,
    val observePositionWithIntervalHandle: (
        getInterval: (
            event: Boolean,
            position: Duration,
            bufferedPosition: Duration,
            duration: Duration,
            speed: Float
        ) -> Duration
    ) -> Flow<Nothing>
) {

    var readyForScrub by mutableStateOf(false)
    var layoutWidthDp by mutableStateOf(Dp.Infinity)

    var remoteSliderPlaybackPosition by mutableStateOf<Duration>(PlaybackConstants.POSITION_UNSET)
        private set
    var remoteSliderTextPlaybackPosition by mutableStateOf<Duration>(PlaybackConstants.POSITION_UNSET)
        private set
    var remotePlaybackDuration by mutableStateOf<Duration>(PlaybackConstants.DURATION_UNSET)
        private set
    var remoteSliderTextPlaybackDuration by mutableStateOf<Duration>(PlaybackConstants.DURATION_UNSET)
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

    fun setNewSliderDuration(duration: Duration) {
        remotePlaybackDuration = duration
    }

    fun setNewSliderPosition(duration: Duration) {
        remoteSliderPlaybackPosition = duration
    }

    fun setNewSliderTextPosition(duration: Duration) {
        remoteSliderTextPlaybackPosition = duration
    }

    fun setNewSliderTextDuration(duration: Duration) {
        remoteSliderTextPlaybackDuration = duration
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
                if (
                    instance.currentProgress > 0f &&
                    instance.startInDuration.isPositive()
                ) {
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
    private val mainComposition: RootPlaybackControlComposition,
    val observePositionWithIntervalHandle: (
        getInterval: (
            event: Boolean,
            position: Duration,
            bufferedPosition: Duration,
            duration: Duration,
            speed: Float
        ) -> Duration
    ) -> Flow<Nothing>
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
    ): Deferred<Result<Boolean>> = mainComposition.requestSeekPositionAsync(
        expectId,
        expectDuration,
        percent
    )
}

@Composable
internal fun rememberRootPlaybackControlSliderState(
    parentComposition: RootPlaybackControlComposition
): RootPlaybackControlSliderState {
    return remember(parentComposition) {
        RootPlaybackControlSliderState(parentComposition, parentComposition.observePositionWithIntervalHandle)
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
                                    },
                                    observePositionWithIntervalHandle = state.observePositionWithIntervalHandle
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
                val supervisor = SupervisorJob()
                composition.run {
                    coroutineScope.launch(supervisor) {
                        snapshotFlow { composition.layoutWidthDp }
                            .collect { sliderWidth ->
                                observePositionWithIntervalHandle { _, position, _, duration, speed ->
                                    setNewSliderPosition(position)
                                    setNewSliderDuration(duration)
                                    if (position == Duration.ZERO ||
                                        duration == Duration.ZERO ||
                                        speed == 0f
                                    ) {
                                        null
                                    } else {
                                        (duration.inWholeMilliseconds / sliderWidth.value / speed)
                                            .toLong()
                                            .takeIf { it > 100 }?.milliseconds

                                    } ?: PlaybackConstants.DURATION_UNSET
                                }.collect {}
                            }
                    }
                    coroutineScope.launch(supervisor) {
                        observePositionWithIntervalHandle { _, position, _, duration, speed ->
                            setNewSliderTextPosition(position)
                            setNewSliderTextDuration(duration)
                            Durations.ONE_SECOND / speed.toDouble()
                        }
                    }
                    coroutineScope.launch(supervisor) {
                        snapshotFlow { composition.remotePlaybackDuration }
                            .first { it.isPositive() }
                        composition.readyForScrub = true
                    }
                }
                onDispose { supervisor.cancel() }
            }
        )
    }
}