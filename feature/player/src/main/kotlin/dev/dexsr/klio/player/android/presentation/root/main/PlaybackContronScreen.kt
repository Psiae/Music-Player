package dev.dexsr.klio.player.android.presentation.root.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import dev.dexsr.klio.base.compose.Stack
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun TransitioningPlaybackControlScreen(
    modifier: Modifier,
    state: PlaybackControlScreenState
) {
    BoxWithConstraints(modifier) {
        val transitionState = rememberAnimatedTransitionState(
            state = state,
            height = constraints.maxHeight,
        )
        val stagedOffsetPx = transitionState.stagedOffsetPx
        Stack(
            modifier = Modifier
                .offset { IntOffset(x = 0, y = stagedOffsetPx) }
                .onGloballyPositioned { transitionState.renderedOffsetPx = stagedOffsetPx }
        ) {
            if (transitionState.shouldShowScreen()) {
                PlaybackControlScreen(
                    state = state,
                    transitionState = transitionState
                )
            }
        }
    }
}

@Composable
fun PlaybackControlScreen(
    state: PlaybackControlScreenState,
    transitionState: PlaybackControlScreenTransitionState
) {
    SubcomposeLayout { constraints ->

        val main = subcompose("MAIN") {
            PlaybackControlMainScreen(
                state = rememberPlaybackControlMainScreenState(
                    container = state,
                    transitionState = transitionState
                )
            )
        }.fastMap { it.measure(constraints) }

        val queue = subcompose("QUEUE") {
            PlaybackControlQueueScreen(
                state = rememberPlaybackControlQueueScreenState(
                    container = state,
                    transitionState = transitionState
                )
            )
        }.fastMap { it.measure(constraints) }

        layout(
            width = constraints.maxWidth,
            height = constraints.maxHeight
        ) {
            main.fastForEach { it.place(0,0, 0f) }
            queue.fastForEach { it.place(0,0, 0f) }
        }
    }
}



@Composable
private fun rememberAnimatedTransitionState(
    state: PlaybackControlScreenState,
    height: Int,
    showAnimationSpec: FiniteAnimationSpec<Int> = tween(350),
    hideAnimationSpec: FiniteAnimationSpec<Int> = tween(200),
    savedSnapAnimationSpec: FiniteAnimationSpec<Int> = snap(0)
): PlaybackControlScreenTransitionState {
    // TODO: savable
    val rememberState = remember(state) {
        PlaybackControlScreenTransitionState(
            screenState = state,
        )
    }
    val animatable = remember(state) {
        Animatable(
            initialValue = height,
            typeConverter = Int.VectorConverter
        )
    }
    val heightState = rememberUpdatedState(newValue = height)
    DisposableEffect(
        key1 = state,
        key2 = animatable,
        effect = {
            val coroutineScope = CoroutineScope(SupervisorJob())
            coroutineScope.launch(Dispatchers.Main) {
                var task: Job? = null
                snapshotFlow { state.freeze }
                    .distinctUntilChanged()
                    .collect { freeze ->
                        task?.cancel()
                        if (freeze) return@collect
                        task = launch {
                            var animator: Job? = null
                            snapshotFlow { state.showSelf }
                                .collect { animateToShow ->
                                    animator?.cancel()
                                    animator = launch {
                                        var animateToTarget: Job? = null
                                        snapshotFlow { if (animateToShow) 0 else heightState.value }
                                            .distinctUntilChanged()
                                            .collect { target ->
                                                animateToTarget?.cancel()
                                                animateToTarget = launch(AndroidUiDispatcher.Main) {
                                                    animatable.animateTo(
                                                        target,
                                                        animationSpec = if (animateToShow) {
                                                            if (rememberState.consumeShowSnap) {
                                                                rememberState.consumeShowSnap = false
                                                                savedSnapAnimationSpec
                                                            } else {
                                                                showAnimationSpec
                                                            }
                                                        } else {
                                                            hideAnimationSpec
                                                        }
                                                    )
                                                    rememberState.consumeShowSnap = animateToShow
                                                }
                                            }
                                    }
                                }
                        }
                    }
            }


            onDispose { coroutineScope.cancel() }
        }
    )
    return rememberState.apply {
        targetHeightPx = heightState.value
        stagedOffsetPx = animatable.value
    }
}