package com.flammky.musicplayer.player.presentation.root

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.media.playback.PlaybackConstants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RootPlaybackControlTimeBarState(
    private val observeQueue: () -> Flow<OldPlaybackQueue>,
    private val onRequestSeek: (
        expectFromId: String,
        expectDuration: Duration,
        percent: Float
    ) -> Deferred<Result<Boolean>>,
    private val observeDuration: () -> Flow<Duration>,
    private val observeProgressWithIntervalHandle: (
        getInterval: (
            event: Boolean,
            position: Duration,
            bufferedPosition: Duration,
            duration: Duration,
            speed: Float
        ) -> Duration
    ) -> Flow<Duration>
) {

    val applier: Applier = Applier(this)


    class Applier(
        private val state: RootPlaybackControlTimeBarState
    ) {

        init {
            @Suppress("SENSELESS_COMPARISON")
            check(state.applier == null) {
                "Applier should only be created by the state"
            }
        }

        companion object {

            @Composable
            fun Applier.ComposeLayout(
                content: @Composable CompositionScope.() -> Unit
            ) {
                observeForScope().run {
                    content()
                }
            }

            @Composable
            fun Applier.observeForScope(): CompositionScope {
                val composableCoroutineScope = rememberCoroutineScope()
                val latestScopeState = remember {
                    mutableStateOf(
                        CompositionScope(
                            lifetimeCoroutineScope = CoroutineScope(
                                composableCoroutineScope.coroutineContext + SupervisorJob()
                            ),
                            queueData = OldPlaybackQueue.UNSET,
                            onRequestSeek = state.onRequestSeek,
                            observeDuration = state.observeDuration,
                            observeProgressWithIntervalHandle = state.observeProgressWithIntervalHandle
                        )
                    )
                }
                DisposableEffect(
                    key1 = this,
                    effect = {
                        composableCoroutineScope.launch {
                            state.observeQueue()
                                .collect {
                                    latestScopeState.value.onSucceeded()
                                    latestScopeState.value = CompositionScope(
                                        lifetimeCoroutineScope = CoroutineScope(
                                            composableCoroutineScope.coroutineContext + SupervisorJob()
                                        ),
                                        queueData = it,
                                        onRequestSeek = state.onRequestSeek,
                                        observeDuration = state.observeDuration,
                                        observeProgressWithIntervalHandle = state.observeProgressWithIntervalHandle
                                    )
                                }
                        }
                        onDispose {
                            latestScopeState.value.onDetachedFromComposition()
                        }
                    }
                )
                return latestScopeState.value
            }
        }
    }

    class ScrubbingInstance(
        val startPosition: Float,
        val startDuration: Duration,
    ) {

        init {
            positionSanityCheck(startPosition, startDuration)
        }

        private var locked = false

        var latestPosition by mutableStateOf(startPosition)
            private set

        // reconsider this
        var latestDuration by mutableStateOf(startDuration)
            private set

        val finished
            get() = locked

        fun newPosition(
            position: Float,
            duration: Duration
        ) {
            mutationSanityCheck()
            positionSanityCheck(position, duration)
            latestPosition = position
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
            check(!locked)
            locked = true
        }
    }

    class CompositionScope(
        val lifetimeCoroutineScope: CoroutineScope,
        val queueData: OldPlaybackQueue,
        private val onRequestSeek: (
            expectFromId: String,
            expectDuration: Duration,
            percent: Float
        ) -> Deferred<Result<Boolean>>,
        private val observeDuration: () -> Flow<Duration>,
        private val observeProgressWithIntervalHandle: (
            getInterval: (
                event: Boolean,
                position: Duration,
                bufferedPosition: Duration,
                duration: Duration,
                speed: Float
            ) -> Duration
        ) -> Flow<Duration>
    ) {

        val scrubResultStack = mutableStateListOf<ScrubbingInstance>()
        var currentScrubbingInstance by mutableStateOf<ScrubbingInstance?>(null)

        var latestComposedScrubBarPosition by mutableStateOf<Float?>(null)
            private set

        var latestComposedScrubBarDuration by mutableStateOf<Duration?>(null)
            private set

        var latestComposedScrubBarWidth by mutableStateOf<Dp?>(null)
            private set

        fun onDetachedFromComposition() {
            lifetimeCoroutineScope.cancel()
        }

        fun onSucceeded(/* next: CompositionScope */) {
            lifetimeCoroutineScope.cancel()
        }

        fun onUserScrubToPosition(
            newPosition: Float,
        ) {
            // introduce Scrub slot ?
            if (currentScrubbingInstance == null) {
                currentScrubbingInstance = ScrubbingInstance(
                    newPosition,
                    latestComposedScrubBarDuration
                        ?: error(
                            "sanity check: can only scrub on composed duration"
                        )
                )
                return
            }
            currentScrubbingInstance!!.newPosition(
                newPosition,
                latestComposedScrubBarDuration!!
            )
        }

        fun onScrubFinished(
            // expect ..
        ) {
            val scrubInstance = currentScrubbingInstance
                ?: error(
                    "sanity check: no scrub instance to finish"
                )
            scrubInstance.finish()
            currentScrubbingInstance = null
            lifetimeCoroutineScope.launch {
                scrubResultStack.add(scrubInstance)
                runCatching {
                    onRequestSeek(
                        queueData.list[queueData.currentIndex],
                        scrubInstance.latestDuration,
                        scrubInstance.latestPosition * 100
                    ).await()
                }
                check(scrubResultStack.remove(scrubInstance)) {
                    "sanity check: scrubResult was removed prematurely, " +
                            "don't forget to remove this check"
                }
            }
        }

        companion object {

            @Composable
            fun CompositionScope.animatedSliderValue(sliderWidth: Dp): Float {
                val value = remember {
                    mutableStateOf(0f)
                }
                val duration = remember {
                    mutableStateOf(Duration.ZERO)
                }
                val latestWidth = rememberUpdatedState(sliderWidth)
                (currentScrubbingInstance ?: scrubResultStack.lastOrNull())
                    ?.let { instance ->
                        value.value = instance.latestPosition
                        duration.value = instance.latestDuration
                    }
                    ?: run {
                        val lAnimatable = remember {
                            Animatable(value.value)
                        }
                        val lDuration = remember {
                            mutableStateOf(duration.value)
                        }
                        DisposableEffect(
                            key1 = this,
                            effect = {
                                val supervisor = SupervisorJob()
                                lifetimeCoroutineScope.launch(supervisor) {
                                    var latestDurationCollector: Job? = null
                                    snapshotFlow { latestWidth.value }
                                        .collect { width ->
                                            latestDurationCollector?.cancel()
                                            latestDurationCollector = launch {
                                                var latestPositionCollector: Job? = null
                                                observeDuration()
                                                    .collect { duration ->
                                                        latestPositionCollector?.cancel()
                                                        lDuration.value = duration
                                                        latestPositionCollector = launch {
                                                            observeProgressWithIntervalHandle { e, p, b, d, s ->
                                                                if (d == Duration.ZERO ||
                                                                    s == 0f
                                                                ) {
                                                                    null
                                                                } else {
                                                                    (d.inWholeMilliseconds / width.value / s)
                                                                        .toLong()
                                                                        .takeIf { it > 100 }?.milliseconds

                                                                } ?: PlaybackConstants.DURATION_UNSET
                                                            }.collect {
                                                                val target =
                                                                    it.inWholeMilliseconds.toFloat() / duration.inWholeMilliseconds.coerceAtLeast(1)
                                                              lAnimatable.animateTo(target, tween(200))
                                                            }
                                                        }
                                                    }
                                            }
                                        }

                                }
                                onDispose {
                                    supervisor.cancel()
                                }
                            }
                        )
                        value.value = lAnimatable.value
                        duration.value = lDuration.value
                    }
                SideEffect {
                    latestComposedScrubBarWidth = sliderWidth
                    latestComposedScrubBarPosition = value.value
                    latestComposedScrubBarDuration = duration.value
                }
                return value.value
            }

            @Composable
            fun CompositionScope.timeBarTextDurationDisplayValue(): Duration {
                val value = remember {
                    mutableStateOf(Duration.ZERO)
                }
                (currentScrubbingInstance ?: scrubResultStack.lastOrNull())
                    ?.let { instance ->
                        value.value = instance.latestDuration
                    }
                    ?: run {
                        DisposableEffect(
                            key1 = this,
                            effect = {
                                val supervisor = SupervisorJob()
                                lifetimeCoroutineScope.launch(supervisor) {
                                    observeDuration().collect { duration -> value.value = duration }
                                }
                                onDispose {
                                    supervisor.cancel()
                                }
                            }
                        )
                    }
                return value.value
            }

            @Composable
            fun CompositionScope.timeBarTextPositionDisplayValue(): Duration {
                val value = remember {
                    mutableStateOf(Duration.ZERO)
                }
                (currentScrubbingInstance ?: scrubResultStack.lastOrNull())
                    ?.let { instance ->
                        value.value = (instance.latestDuration.inWholeMilliseconds * instance.latestPosition)
                            .toDouble()
                            .milliseconds
                    }
                    ?: run {
                        DisposableEffect(
                            key1 = this,
                            effect = {
                                val supervisor = SupervisorJob()
                                lifetimeCoroutineScope.launch(supervisor) {
                                    observeProgressWithIntervalHandle { e, p, b, d, s ->
                                        if (d == Duration.ZERO || s == 0f) {
                                            PlaybackConstants.DURATION_UNSET
                                        } else {
                                            1.seconds
                                        }
                                    }.collect {
                                        value.value = it
                                    }
                                }
                                onDispose {
                                    supervisor.cancel()
                                }
                            }
                        )
                    }
                return value.value
            }
        }
    }
}