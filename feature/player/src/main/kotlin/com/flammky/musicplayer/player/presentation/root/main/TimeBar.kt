package com.flammky.musicplayer.player.presentation.root.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.media.playback.PlaybackConstants
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundContentColorAsState
import com.flammky.musicplayer.base.theme.compose.surfaceContentColorAsState
import com.flammky.musicplayer.player.presentation.main.compose.Slider
import com.flammky.musicplayer.player.presentation.main.compose.SliderDefaults
import com.flammky.musicplayer.player.presentation.root.runRemember
import dev.flammky.compose_components.core.SnapshotRead
import dev.flammky.compose_components.core.SnapshotReader
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun rememberPlaybackControlTimeBarState(
    key: Any,
    observeQueue: () -> Flow<OldPlaybackQueue>,
    onRequestSeek: (
        expectFromId: String,
        expectDuration: Duration,
        percent: Float
    ) -> Deferred<Result<Boolean>>,
    observeDuration: () -> Flow<Duration>,
    observeProgressWithIntervalHandle: (
        getInterval: (
            event: Boolean,
            position: Duration,
            bufferedPosition: Duration,
            duration: Duration,
            speed: Float
        ) -> Duration
    ) -> Flow<Duration>
): PlaybackControlTimeBarState {
    return remember(key) {
        PlaybackControlTimeBarState(
            observeQueue,
            onRequestSeek,
            observeDuration,
            observeProgressWithIntervalHandle
        )
    }
}

@Composable
fun PlaybackTimeBar(
    state: PlaybackControlTimeBarState
) = state.coordinator.ComposeContent(
    slider = @SnapshotRead {
        provideSliderRenderFactory { modifier ->
            val trackColor = trackColor()
            val thumbColor = thumbColor()
            val thumbSize = thumbSize()
            Slider(
                modifier = modifier.width(trackWidth() + thumbSize),
                value = animatedValue(),
                onValueChange = eventSink.onUserScrubToRatio,
                onValueChangeFinished = eventSink.onUserScrubFinished,
                trackHeight = trackHeight(),
                thumbSize = thumbSize,
                enabled = scrubEnabled(),
                colors = SliderDefaults.colors(
                    thumbColor = thumbColor,
                    activeTrackColor = trackColor
                )
            )
        }
    },
    progressText = @SnapshotRead {
        // TODO: Coordinator should provide the text color and the text property
        providePositionTextRenderFactory { modifier ->
            Text(
                modifier = modifier,
                text = positionString(),
                color = Theme.backgroundContentColorAsState().value,
                style = MaterialTheme.typography.bodySmall
            )
        }
        provideDurationTextRenderFactory { modifier ->
            Text(
                modifier = modifier,
                text = durationString(),
                color = Theme.backgroundContentColorAsState().value,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
)

class PlaybackControlTimeBarState(
    val observeQueue: () -> Flow<OldPlaybackQueue>,
    val onRequestSeek: (
        expectFromId: String,
        expectDuration: Duration,
        percent: Float
    ) -> Deferred<Result<Boolean>>,
    val observeDuration: () -> Flow<Duration>,
    val observeProgressWithIntervalHandle: (
        getInterval: (
            event: Boolean,
            position: Duration,
            bufferedPosition: Duration,
            duration: Duration,
            speed: Float
        ) -> Duration
    ) -> Flow<Duration>
) {
    val coordinator = PlaybackControlTimeBarCoordinator(this)
}

class PlaybackControlTimeBarCoordinator(
    private val state: PlaybackControlTimeBarState
) {

    init {
        @Suppress("SENSELESS_COMPARISON")
        check(state.coordinator == null)
    }

    val layoutCoordinator = TimeBarLayoutCoordinator()

    data class SliderEventSink(
        val onUserScrubToRatio: ( /* @FloatRange(from = 0.0, to = 1.0) */ ratio: Float) -> Unit,
        val onUserScrubFinished: () -> Unit
    )

    class ScrubConsumer() {

        private var locked by mutableStateOf(false)

        var started by mutableStateOf(false)
            private set

        var startPosition: Float? = null
            private set(value) {
                check(field == null)
                field = value
            }

        var startDuration: Duration? = null
            private set(value) {
                check(field == null)
                field = value
            }

        var latestRatio by mutableStateOf(startPosition)
            private set

        // reconsider this
        var latestDuration by mutableStateOf(startDuration)
            private set

        val finished
            get() = locked

        fun consumePosition(
            ratio: Float,
            duration: Duration
        ) {
            mutationSanityCheck()
            positionSanityCheck(ratio, duration)
            if (startPosition == null) {
                startPosition = ratio
                startDuration = duration
                started = true
            }
            latestRatio = ratio
            latestDuration = duration
        }

        private fun mutationSanityCheck() {
            check(!locked) {
                "sanity check: scrub instance is already locked"
            }
        }

        private fun positionSanityCheck(
            position: Float,
            duration: Duration
        ) {
            check(position >= 0f) {
                "sanity check: should not be able to scrub to negative position"
            }
            check(duration >= Duration.ZERO || duration == Duration.INFINITE) {
                "sanity check: Scrub duration must be positive, " +
                        "negative duration must not be scrub-able, " +
                        "for unknown duration but is valid in any case use Infinite instead"
            }
        }

        fun finish() {
            mutationSanityCheck()
            locked = true
        }
    }

    interface SliderScope {
        fun provideSliderRenderFactory(
            content: @Composable SliderRenderScope.(Modifier) -> Unit
        )
    }

    interface SliderRenderScope {
        val eventSink: SliderEventSink

        @Composable
        fun animatedValue(): Float

        @Composable
        fun trackWidth(): Dp

        @Composable
        fun trackHeight(): Dp

        @Composable
        fun thumbSize(): Dp

        @Composable
        fun scrubEnabled(): Boolean

        @Composable
        fun trackColor(): Color

        @Composable
        fun thumbColor(): Color
    }

    interface ProgressTextScope {

        fun providePositionTextRenderFactory(
            content: @Composable TextPositionRenderScope.(Modifier) -> Unit
        )

        fun provideDurationTextRenderFactory(
            content: @Composable TextDurationRenderScope.(Modifier) -> Unit
        )
    }

    interface TextPositionRenderScope {
        @Composable
        fun positionString(): String
    }

    interface TextDurationRenderScope {
        @Composable
        fun durationString(): String
    }

    private class TextPositionRenderScopeImpl(
        private val positionFlow: () -> Flow<Duration>,
        private val positionOverrideState: State<Duration?>
    ) : TextPositionRenderScope {

        @Composable
        override fun positionString(): String {
            val position = run {
                val remote = remember(this){
                    positionFlow()
                }.collectAsState(initial = Duration.ZERO).value
                positionOverrideState.value ?: remote
            }
            return remember(position) {
                val seconds =
                    if (position.isNegative()) 0
                    else position.inWholeSeconds
                if (seconds > 3600) {
                    String.format(
                        "%02d:%02d:%02d",
                        seconds / 3600,
                        seconds % 3600 / 60,
                        seconds % 60
                    )
                } else {
                    String.format(
                        "%02d:%02d",
                        seconds / 60,
                        seconds % 60
                    )
                }
            }
        }
    }

    private class TextDurationRenderScopeImpl(
        private val durationFlow: () -> Flow<Duration>,
        private val durationOverrideState: State<Duration?>
    ): TextDurationRenderScope {

        @Composable
        override fun durationString(): String {
            val duration = run {
                val remote = remember(this) {
                    durationFlow()
                }.collectAsState(initial = Duration.ZERO).value
                durationOverrideState.value ?: remote
            }
            return remember(duration) {
                val seconds =
                    if (duration.isNegative() || duration.isInfinite()) 0
                    else duration.inWholeSeconds
                if (seconds > 3600) {
                    String.format(
                        "%02d:%02d:%02d",
                        seconds / 3600,
                        seconds % 3600 / 60,
                        seconds % 60
                    )
                } else {
                    String.format(
                        "%02d:%02d",
                        seconds / 60,
                        seconds % 60
                    )
                }
            }
        }
    }

    private class SliderRenderScopeImpl(
        override val eventSink: SliderEventSink,
        private val positionContinuationKey: Any,
        private val canScrub: Boolean,
        private val currentScrubbingRatio: Float?,
        private val latestQueuedScrubbingRatio: Float?,
        private val trackWidth: Dp,
        private val trackHeight: Dp,
        private val thumbSize: Dp,
        private val position: Duration,
        private val duration: Duration
    ) : SliderRenderScope {

        @Composable
        override fun animatedValue(): Float {

            Timber.d(
                "RootMainTimeBar@${System.identityHashCode(this)}," +
                        "animateValue#$currentCompositeKeyHash: " +
                        "$eventSink $positionContinuationKey $canScrub $currentScrubbingRatio " +
                        "$latestQueuedScrubbingRatio $trackWidth $trackHeight $thumbSize $position " +
                        "$$duration"
            )


            val updatedSelf = rememberUpdatedState(newValue = this)

            val returns = remember {
                mutableStateOf(0f)
            }.apply {

                if (currentScrubbingRatio != null) {
                    value = currentScrubbingRatio
                    return@apply
                }

                if (latestQueuedScrubbingRatio != null) {
                    value = latestQueuedScrubbingRatio
                    return@apply
                }

                val anim = remember {
                    Animatable(value)
                }

                val coroutineScope = rememberCoroutineScope()

                DisposableEffect(
                    key1 = positionContinuationKey,
                    effect = {
                        val supervisor = SupervisorJob()

                        Timber.d("RootMainTimeBar, animateValue, newDisposableEffect")

                        coroutineScope.launch(supervisor) {
                            var initial = true
                            snapshotFlow { updatedSelf.value }
                                .collect { self ->
                                    val target =
                                        self.position.inWholeMilliseconds
                                            .toFloat() /
                                                self.duration.inWholeMilliseconds
                                                    .coerceAtLeast(1)
                                    if (initial) {
                                        Timber.d("RootMainTimeBar, animateValue, snapTo=$target")
                                        anim.snapTo(target)
                                        initial = false
                                    } else {
                                        Timber.d("RootMainTimeBar, animateValue, animateTo=$target")
                                        anim.animateTo(target, tween(200))
                                    }
                                }
                        }

                        onDispose { supervisor.cancel() }
                    }
                )

                value = anim.value
            }
            return returns.value
        }

        @Composable
        override fun trackWidth(): Dp = trackWidth

        @Composable
        override fun trackHeight(): Dp = trackHeight

        @Composable
        override fun thumbSize(): Dp = thumbSize

        @Composable
        override fun scrubEnabled(): Boolean = canScrub

        @Composable
        override fun trackColor(): Color = Theme.surfaceContentColorAsState().value

        @Composable
        override fun thumbColor(): Color = Theme.backgroundContentColorAsState().value
    }

    private class SliderScopeImpl(
        private val observeQueue: () -> Flow<OldPlaybackQueue>,
        private val positionFlow: (trackWidth: Dp) -> Flow<Duration>,
        private val durationFlow: () -> Flow<Duration>,
        private val onRequestSeek: (
            expectFromId: String,
            expectDuration: Duration,
            percent: Float
        ) -> Deferred<Result<Boolean>>
    ) : SliderScope {

        var sliderContent by mutableStateOf<@Composable () -> Unit>({})
            private set

        var overridingScrubConsumer by mutableStateOf<ScrubConsumer?>(null)
            private set

        private var getSliderModifier by mutableStateOf<Modifier.() -> Modifier>({ Modifier })

        private var getTrackWidth by mutableStateOf<@SnapshotRead () -> Dp>({ Dp.Unspecified })

        private var getTrackHeight by mutableStateOf<@SnapshotRead () -> Dp>({ Dp.Unspecified })

        private var getThumbSize by mutableStateOf<@SnapshotRead () -> Dp>({ Dp.Unspecified })

        fun attachLayoutHandle(
            getSliderModifier: @SnapshotRead Modifier.() -> Modifier,
            getTrackWidth: @SnapshotRead () -> Dp,
            getTrackHeight: @SnapshotRead () -> Dp,
            getThumbSize: @SnapshotRead () -> Dp
        ) {
            this.getSliderModifier = getSliderModifier
            this.getTrackWidth = getTrackWidth
            this.getTrackHeight = getTrackHeight
            this.getThumbSize = getThumbSize
        }

        override fun provideSliderRenderFactory(content: @Composable SliderRenderScope.(Modifier) -> Unit) {
            sliderContent = @Composable { observeForRenderScope().content(Modifier.getSliderModifier()) }
        }

        @Composable
        private fun observeForRenderScope(): SliderRenderScope {

            val positionContinuationKeyState = remember(this) {
                mutableStateOf<OldPlaybackQueue>(OldPlaybackQueue.UNSET)
            }

            // we can just use the continuation key as the remember key ?

            val renderDurationState = remember {
                mutableStateOf<Duration>(Duration.ZERO)
            }

            val renderPositionState = remember {
                mutableStateOf<Duration>(Duration.ZERO)
            }

            val coroutineScope = rememberCoroutineScope()

            val currentScrubConsumer = remember(this) {
                mutableStateOf(ScrubConsumer())
            }

            val queuedScrubConsumers = remember(this) {
                mutableStateListOf<ScrubConsumer>()
            }

            val renderDuration = renderDurationState.value

            SideEffect {
                this.overridingScrubConsumer =
                    queuedScrubConsumers.lastOrNull() ?: currentScrubConsumer.value
            }

            DisposableEffect(
                key1 = this,
                effect = {
                    val supervisor = SupervisorJob()

                    coroutineScope.launch(supervisor) {
                        var latestWidthCollector: Job? = null
                        var latestDurationCollector: Job? = null
                        var latestPositionCollector: Job? = null
                        observeQueue().collect { queue ->
                            latestWidthCollector?.cancel()
                            latestWidthCollector = launch {
                                snapshotFlow { getTrackWidth() }
                                    .collect { trackWidth ->
                                        latestDurationCollector?.cancel()
                                        latestDurationCollector = launch {
                                            durationFlow().collect { duration ->
                                                latestPositionCollector?.cancel()
                                                latestPositionCollector = launch {
                                                    positionFlow(trackWidth).collect { position ->
                                                        positionContinuationKeyState.value = queue
                                                        renderPositionState.value = position
                                                        renderDurationState.value = duration
                                                    }
                                                }
                                            }
                                        }
                                    }
                            }
                        }
                    }

                    onDispose {
                        supervisor.cancel()
                        if (
                            overridingScrubConsumer ==
                            (queuedScrubConsumers.lastOrNull() ?: currentScrubConsumer.value)
                        ) {
                            overridingScrubConsumer = null
                        }
                    }
                }
            )

            val pCont = positionContinuationKeyState.value

            return SliderRenderScopeImpl(
                eventSink = remember(
                    currentScrubConsumer.value,
                    renderDuration,
                    positionContinuationKeyState.value
                ) {
                    val id = pCont.let { it.list.getOrNull(it.currentIndex) }
                    SliderEventSink(
                        // there's chance that we might get additional scrub before new consumer is
                        // rendered
                        onUserScrubToRatio = { ratio ->
                            if (positionContinuationKeyState.value == pCont) {
                                currentScrubConsumer.value.consumePosition(ratio, renderDuration)
                            }
                        },
                        onUserScrubFinished = {
                            val consumer = if (positionContinuationKeyState.value == pCont) {
                                currentScrubConsumer.value.takeIf { it.started }
                                    ?: return@SliderEventSink
                            } else {
                                return@SliderEventSink
                            }
                            consumer.finish()
                            queuedScrubConsumers.add(consumer)
                            currentScrubConsumer.value = ScrubConsumer()
                            coroutineScope.launch {
                                onRequestSeek(
                                    id ?: "",
                                    consumer.latestDuration!!,
                                    consumer.latestRatio!! * 100
                                ).await()
                            }.invokeOnCompletion {
                                queuedScrubConsumers.remove(consumer)
                            }
                        }
                    )
                },
                positionContinuationKey = pCont,
                canScrub = remember {
                    mutableStateOf(pCont)
                }.run {
                    val enabled = renderDuration > Duration.ZERO &&
                            value == positionContinuationKeyState.value
                    value = positionContinuationKeyState.value
                    enabled
                },
                currentScrubbingRatio = currentScrubConsumer.value.let {
                    check(!it.finished)
                    if (it.started) it.latestRatio else null
                },
                latestQueuedScrubbingRatio = queuedScrubConsumers.lastOrNull()?.let {
                    check(it.finished)
                    it.latestRatio
                },
                trackWidth = getTrackWidth(),
                trackHeight = getTrackHeight(),
                thumbSize = getThumbSize(),
                position = renderPositionState.value,
                duration = renderDurationState.value
            )
        }
    }

    private class ProgressTextScopeImpl(
        private val positionFlow: () -> Flow<Duration>,
        private val durationFlow: () -> Flow<Duration>,
        private val positionOverrideState: State<Duration?>,
        private val durationOverrideState: State<Duration?>
    ) : ProgressTextScope {

        var positionTextContent by mutableStateOf<@Composable () -> Unit>({})
            private set

        var durationTextContent by mutableStateOf<@Composable () -> Unit>({})
            private set

        private var getPositionTextModifier by mutableStateOf<Modifier.() -> Modifier>({ Modifier })

        private var getDurationTextModifier by mutableStateOf<Modifier.() -> Modifier>({ Modifier })

        fun attachPositionTextLayoutHandle(
            positionTextModifier: Modifier.() -> Modifier,
        ) {
            this.getPositionTextModifier = positionTextModifier
        }

        fun attachDurationTextLayoutHandle(
            durationTextModifier: Modifier.() -> Modifier
        ) {
            this.getDurationTextModifier = durationTextModifier
        }

        override fun providePositionTextRenderFactory(
            content: @Composable TextPositionRenderScope.(Modifier) -> Unit
        ) {
            positionTextContent = @Composable {
                remember(this) {
                    TextPositionRenderScopeImpl(positionFlow, positionOverrideState)
                }.content(Modifier.getPositionTextModifier())
            }
        }

        override fun provideDurationTextRenderFactory(
            content: @Composable TextDurationRenderScope.(Modifier) -> Unit
        ) {
           durationTextContent = @Composable {
               remember(this) {
                   TextDurationRenderScopeImpl(durationFlow, durationOverrideState)
               }.content(Modifier.getDurationTextModifier())
           }
        }
    }

    @Composable
    fun ComposeContent(
        slider: @SnapshotReader SliderScope.() -> Unit,
        progressText: @SnapshotReader ProgressTextScope.() -> Unit
    ) {
        val upSlider = rememberUpdatedState(slider)
        val upProgressText = rememberUpdatedState(progressText)
        with(layoutCoordinator) {
            val sliderScopeState = rememberSliderScope()
                .runRemember { derivedStateOf { apply(upSlider.value) } }
            val textScopeState = rememberTextScope(sliderScopeState.value)
                .runRemember { derivedStateOf { apply(upProgressText.value) } }
            PlaceTimeBar(
                slider = @SnapshotRead {
                    setSliderContent {
                        val updatedTrackWidth = rememberUpdatedState(newValue = trackWidth())
                        val updatedTrackHeight = rememberUpdatedState(newValue = trackHeight())
                        val updatedThumbSize = rememberUpdatedState(newValue = thumbSize())
                        val sliderScope = sliderScopeState.value
                        sliderScope.attachLayoutHandle(
                            getSliderModifier = remember(this) {
                                { sliderModifier() }
                            },
                            getTrackWidth = remember(this) {
                                { updatedTrackWidth.value }
                            },
                            getTrackHeight = remember(this) {
                                { updatedTrackHeight.value }
                            },
                            getThumbSize = remember(this) {
                                { updatedThumbSize.value }
                            }
                        )
                        sliderScope.sliderContent()
                    }
                },
                progress = @SnapshotRead {
                    setPositionContent {
                        val textScope = textScopeState.value
                        textScope.attachPositionTextLayoutHandle(
                            positionTextModifier = remember(this) {
                                { positionTextModifier() }
                            }
                        )
                        textScope.positionTextContent()
                    }
                    setDurationContent {
                        val textScope = textScopeState.value
                        textScope.attachDurationTextLayoutHandle(
                            durationTextModifier = remember(this) {
                                { durationTextModifier() }
                            }
                        )
                        textScope.durationTextContent()
                    }
                }
            )
        }
    }

    @Composable
    private fun rememberSliderScope(): SliderScopeImpl {
        return remember(this, this@PlaybackControlTimeBarCoordinator) {
            SliderScopeImpl(
                positionFlow = { trackWidth ->
                    flow {
                        state.observeProgressWithIntervalHandle { e, p, b, d, s ->
                            if (d == Duration.ZERO ||
                                s == 0f
                            ) {
                                null
                            } else {
                                (d.inWholeMilliseconds / trackWidth.value / s)
                                    .toLong()
                                    .takeIf { it > 100 }?.milliseconds

                            } ?: PlaybackConstants.DURATION_UNSET
                        }.collect {
                            emit(it)
                        }
                    }
                },
                durationFlow = state.observeDuration,
                observeQueue = state.observeQueue,
                onRequestSeek = state.onRequestSeek
            )
        }
    }

    @Composable
    private fun rememberTextScope(
        slider: SliderScopeImpl
    ): ProgressTextScopeImpl {
        val updatedSlider = rememberUpdatedState(newValue = slider)
        return remember(this) {
            ProgressTextScopeImpl(
                positionFlow = {
                    flow {
                        state.observeProgressWithIntervalHandle { e, p, b, d, s ->
                            if (d == Duration.ZERO ||
                                s == 0f
                            ) {
                                null
                            } else {
                                1.seconds
                            } ?: PlaybackConstants.DURATION_UNSET
                        }.collect {
                            emit(it)
                        }
                    }
                },
                durationFlow = state.observeDuration,
                positionOverrideState = derivedStateOf {
                    updatedSlider.value.overridingScrubConsumer?.let { consumer ->
                        if (consumer.started) {
                            (consumer.latestDuration!!.inWholeMilliseconds *
                                    consumer.latestRatio!!).toDouble().milliseconds
                        } else {
                            null
                        }
                    }
                },
                durationOverrideState = derivedStateOf {
                    updatedSlider.value.overridingScrubConsumer?.let { consumer ->
                        if (consumer.started) {
                            consumer.latestDuration
                        } else {
                            null
                        }
                    }
                }
            )
        }
    }
}

class TimeBarLayoutCoordinator() {

    interface SliderLayoutScope {

        fun Modifier.sliderModifier(): Modifier

        @Composable
        fun trackWidth(): Dp

        @Composable
        fun trackHeight(): Dp

        @Composable
        fun thumbSize(): Dp

        fun setSliderContent(content: @Composable () -> Unit)
    }

    interface ProgressTextLayoutScope {
        fun Modifier.positionTextModifier(): Modifier
        fun Modifier.durationTextModifier(): Modifier

        fun setPositionContent(content: @Composable () -> Unit)
        fun setDurationContent(content: @Composable () -> Unit)
    }

    private class SliderScopeImpl(): SliderLayoutScope {

        var content by mutableStateOf<@Composable () -> Unit>({})
            private set

        private var constraints by mutableStateOf<Constraints>(Constraints.fixed(0, 0))


        override fun Modifier.sliderModifier(): Modifier {
            return composed {
                widthIn(
                    max = with(LocalDensity.current) { constraints.maxWidth.toDp() }
                ).heightIn(
                    max = 14.dp
                )
            }
        }

        @Composable
        override fun trackWidth(): Dp {
            return with(LocalDensity.current) {
                (constraints.maxWidth.toDp() * 0.8f)
            }
        }

        @Composable
        override fun trackHeight(): Dp {
            return 6.dp
        }

        @Composable
        override fun thumbSize(): Dp {
            return 12.dp
        }

        override fun setSliderContent(content: @Composable () -> Unit) {
            this.content = content
        }

        fun updateConstraints(constraints: Constraints) {
            this.constraints = constraints
        }
    }

    private class ProgressTextScopeImpl: ProgressTextLayoutScope {

        private var constraints by mutableStateOf<Constraints>(Constraints.fixed(0, 0))
            private set

        private var _positionContent by mutableStateOf<@Composable () -> Unit>({})
        private var _durationContent by mutableStateOf<@Composable () -> Unit>({})

        val positionContent
            get() = _positionContent

        val durationContent
            get() = _durationContent

        override fun Modifier.positionTextModifier(): Modifier {
            return composed {
                widthIn(
                    max = with(LocalDensity.current) {
                        constraints.maxWidth.toDp() / 2
                    }
                ).heightIn(
                    max = 20.dp
                )
            }
        }

        override fun Modifier.durationTextModifier(): Modifier {
            return composed {
                widthIn(
                    max = with(LocalDensity.current) {
                        constraints.maxWidth.toDp() / 2
                    }
                ).heightIn(
                    max = 20.dp
                )
            }
        }

        override fun setPositionContent(content: @Composable () -> Unit) {
            _positionContent = content
        }

        override fun setDurationContent(content: @Composable () -> Unit) {
            _durationContent = content
        }

        fun updateConstraints(constraints: Constraints) {
            this.constraints = constraints
        }
    }

    @Composable
    fun PlaceTimeBar(
        slider: SliderLayoutScope.() -> Unit,
        progress: ProgressTextLayoutScope.() -> Unit
    ) = BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val sliderScope = remember { SliderScopeImpl() }.apply(slider).apply {
            updateConstraints(constraints)
        }
        val progressScope = remember { ProgressTextScopeImpl() }.apply(progress).apply {
            updateConstraints(constraints)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            sliderScope.content()
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                progressScope.positionContent()
                progressScope.durationContent()
            }
        }
    }
}