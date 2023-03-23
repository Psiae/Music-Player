package com.flammky.musicplayer.player.presentation.root

import androidx.compose.runtime.*
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.player.presentation.root.RootPlaybackControlSliderState.CompositionScope.Companion.OnLayoutComposed
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration

class RootPlaybackControlSliderState(
    private val observeQueue: () -> Flow<OldPlaybackQueue>,
    private val observeDuration: () -> Flow<Duration>,
    private val observePositionWithIntervalHandle: (
        getInterval: (
            event: Boolean,
            position: Duration,
            bufferedPosition: Duration,
            duration: Duration,
            speed: Float
        ) -> Duration
    ) -> Flow<Duration>
) {

    class Applier(
        private val state: RootPlaybackControlSliderState
    ) {

        companion object {

            @Composable
            fun Applier.ComposeLayout(
                content: @Composable CompositionScope.() -> Unit
            ) {
                observeForCompositionScope()
                    .run {
                        content()
                        OnLayoutComposed()
                    }
            }

            @Composable
            fun Applier.observeForCompositionScope(): CompositionScope {
                val latestScopeState = remember(this) {
                    mutableStateOf(CompositionScope.UNSET)
                }
                val coroutineScope = rememberCoroutineScope()
                DisposableEffect(
                    key1 = latestScopeState,
                    effect = {
                        val supervisor = SupervisorJob()

                        coroutineScope.launch(supervisor) {
                            state.observeQueue()
                                .collect { queue ->
                                    val current = latestScopeState.value
                                    val next = CompositionScope(
                                        state.observeDuration,
                                        state.observePositionWithIntervalHandle
                                    )

                                }
                        }

                        onDispose { supervisor.cancel() }
                    }
                )
                return latestScopeState.value
            }
        }
    }

    class CompositionScope(
        private val observeDuration: () -> Flow<Duration>,
        private val observePositionWithIntervalHandle: (
            getInterval: (
                event: Boolean,
                position: Duration,
                bufferedPosition: Duration,
                duration: Duration,
                speed: Float
            ) -> Duration
        ) -> Flow<Duration>
    ) {

        var ancestor: CompositionScope? = null
        var successor: CompositionScope? = null

        companion object {

            val UNSET = CompositionScope(
                observeDuration = { emptyFlow() },
                observePositionWithIntervalHandle = { emptyFlow() }
            )

            @Composable
            fun CompositionScope.OnLayoutComposed() {

            }
        }
    }
}