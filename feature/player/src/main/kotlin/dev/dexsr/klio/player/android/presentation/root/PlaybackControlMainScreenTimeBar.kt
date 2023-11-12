package dev.dexsr.klio.player.android.presentation.root

import androidx.annotation.MainThread
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.flammky.musicplayer.player.presentation.main.compose.Slider
import com.flammky.musicplayer.player.presentation.main.compose.SliderDefaults
import dev.dexsr.klio.android.base.checkInMainLooper
import dev.dexsr.klio.base.theme.md3.MD3Theme
import dev.dexsr.klio.base.theme.md3.compose.MaterialTheme3
import dev.dexsr.klio.base.theme.md3.compose.backgroundContentColorAsState
import dev.dexsr.klio.base.theme.md3.compose.dpPaddingIncrementsOf
import dev.dexsr.klio.base.theme.md3.compose.surfaceContentColorAsState
import kotlinx.coroutines.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PlaybackControlMainScreenTimeBar(
    modifier: Modifier,
    containerState: PlaybackControlMainScreenState
) {
    BoxWithConstraints {
        val state = rememberPlaybackControlMainScreenTimeBarState(
            state = containerState
        ).apply {
            SideEffect {
                initParams(maxWidth.value)
            }
        }
        Column(modifier) {
            val thumbSize = 12.dp
            val trackHeight = 6.dp
            val thumbColor = MD3Theme.backgroundContentColorAsState().value
            val trackColor = MD3Theme.surfaceContentColorAsState().value
            if (!state.prepared) return@Column
            Slider(
                modifier = Modifier.fillMaxWidth().heightIn(max = max(thumbSize, trackHeight)),
                value = playbackControlMainScreenTimeBarAnimatedValue(
                    scrub = state.scrub,
                    position = state.position,
                    duration = state.duration,
                    animationSpec = remember { tween(200) },
                ),
                onValueChange = state::userScrub,
                onValueChangeFinished = state::userScrubFinished,
                trackHeight = trackHeight,
                thumbSize = thumbSize,
                enabled = state.userCanScrub,
                colors = SliderDefaults.colors(
                    thumbColor = thumbColor,
                    activeTrackColor = trackColor
                )
            )
            Spacer(
                modifier = Modifier
                    .height(MD3Theme.dpPaddingIncrementsOf(1))
            )
            Row(modifier = Modifier.padding(horizontal = thumbSize / 2)) {
                val duration = state.duration
                val position = state.scrub
                    ?.let {
                        (it * duration.inWholeMilliseconds).toLong().milliseconds
                    }
                    ?: state.position
                Text(
                    modifier = Modifier,
                    text = remember(position) {
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
                    },
                    color = MD3Theme.backgroundContentColorAsState().value,
                    style = MaterialTheme3.typography.bodySmall
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    modifier = Modifier,
                    text = remember(duration) {
                        val seconds =
                            if (duration.isNegative()) 0
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
                    },
                    color = MD3Theme.backgroundContentColorAsState().value,
                    style = MaterialTheme3.typography.bodySmall
                )
            }
        }
    }
}


@Composable
private fun playbackControlMainScreenTimeBarAnimatedValue(
    scrub: Float?,
    position: Duration,
    duration: Duration,
    animationSpec: FiniteAnimationSpec<Float>,
): Float {
    if (scrub != null) return scrub
    val anim = remember {
        Animatable(
            position.inWholeMilliseconds.toFloat()
                .div(duration.inWholeMilliseconds.coerceAtLeast(1))
        )
    }
    val upPosition = rememberUpdatedState(newValue = position)
    val upDuration = rememberUpdatedState(newValue = duration)
    val upAnimationSpec = rememberUpdatedState(newValue = animationSpec)
    DisposableEffect(
        Unit,
        effect = {
            val coroutineScope = CoroutineScope(SupervisorJob())

            coroutineScope.launch(AndroidUiDispatcher.Main) {
                var initial = true
                var positionCollector: Job? = null
                snapshotFlow { upDuration.value }
                    .collect { duration ->
                        positionCollector?.cancel()
                        positionCollector = launch {
                            var animator: Job? = null
                            snapshotFlow { upPosition.value }
                                .collect { position ->
                                    animator?.cancel()
                                    animator = launch {
                                        val target =
                                            position.inWholeMilliseconds.toFloat()
                                                .div(duration.inWholeMilliseconds.coerceAtLeast(1))
                                        if (initial) {
                                            anim.snapTo(target)
                                            initial = false
                                        } else {
                                            anim.animateTo(target, upAnimationSpec.value)
                                        }
                                    }
                                }
                        }
                    }
            }

            onDispose { coroutineScope.cancel() }
        }
    )
    return anim.value
}

@Composable
private fun rememberPlaybackControlMainScreenTimeBarState(
    state: PlaybackControlMainScreenState
): PlaybackControlMainScreenTimeBarState {
    return remember(state) {
        PlaybackControlMainScreenTimeBarState(state.playbackController)
    }.apply {
        freeze = state.freeze
    }
}

private class PlaybackControlMainScreenTimeBarState(
    private val playbackController: PlaybackController
) {

    private var initiated = false
    private val coroutineScope = CoroutineScope(SupervisorJob())

    var freeze by mutableStateOf(false)

    private var scrubResultOverrides = mutableStateListOf<Float>()
        private set

    private var scrubbing by mutableStateOf<Float?>(null)

    private val skipCollectUpdate
        get() = scrubResultOverrides.isNotEmpty()

    val scrub by derivedStateOf { scrubbing ?: scrubResultOverrides.lastOrNull() }

    private var updatedPosition by mutableStateOf(Duration.ZERO)
    private var updatedDuration by mutableStateOf(Duration.ZERO)

    val position by derivedStateOf { updatedPosition }

    val duration by derivedStateOf { updatedDuration }

    var prepared by mutableStateOf(false)
        private set

    val userCanScrub by derivedStateOf { prepared }

    @MainThread
    fun initParams(
        sliderWidthDp: Float
    ) {
        checkInMainLooper()
        invalidateProgressCollector(sliderWidthDp)
    }

    @MainThread
    fun userScrub(
        value: Float
    ) {
        checkInMainLooper()
        check(value in 0f..1f) {
            "Invalid User Scrub value=$$value"
        }
        if (!userCanScrub) return
        scrubbing = value
    }

    private var lastScrubDispatch: Job? = null
    @MainThread
    fun userScrubFinished(
    ) {
        checkInMainLooper()
        val scrub = this.scrubbing ?: return
        scrubbing = null
        lastScrubDispatch?.let {
            it.cancel()
            scrubResultOverrides.removeFirstOrNull()
        }
        scrubResultOverrides.add(scrub)
        lastScrubDispatch = coroutineScope.launch(Dispatchers.Main) {

            stopProgressCollector()

            runCatching {
                playbackController
                    .requestSeekAsync(scrub * 100)
                    .await()

                playbackController
                    .getPlaybackProgressAsync()
                    .await().let { progress ->
                        updatedPosition = progress.position
                        updatedDuration = progress.duration
                    }
            }
            ensureActive()
            if (scrubResultOverrides.remove(scrub) && scrubResultOverrides.isEmpty()) {
                restartProgressCollector()
            }
        }
    }

    // TODO: decide the expected behavior if the width is ever change mid-delay
    private var progressCollectorConstraint = 0f
    private var progressCollector: Job? = null
    private fun invalidateProgressCollector(
        uiWidthDp: Float
    ) {
        if (uiWidthDp == progressCollectorConstraint) return
        progressCollectorConstraint = uiWidthDp
        if (progressCollector?.isActive != true) {
            restartProgressCollector()
        }
    }

    private fun stopProgressCollector() {
        progressCollector?.cancel()
    }

    private fun restartProgressCollector() {
        progressCollector?.cancel()
        progressCollector = coroutineScope.launch(Dispatchers.Main) {
            playbackController
                .playbackProgressAsFlow(
                    getUiWidthDp = ::progressCollectorConstraint
                )
                .collect { progress ->
                    if (skipCollectUpdate) return@collect
                    updatedPosition = progress.position
                    updatedDuration = progress.duration
                    prepared = true
                }
        }
    }
}