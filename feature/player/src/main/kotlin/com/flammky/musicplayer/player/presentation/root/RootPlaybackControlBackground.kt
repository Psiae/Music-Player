package com.flammky.musicplayer.player.presentation.root

import android.annotation.SuppressLint
import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.palette.graphics.Palette
import com.flammky.musicplayer.base.compose.SubCompose
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundColorAsState
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.coroutines.EmptyCoroutineContext

internal class RootPlaybackControlBackgroundState(
    private val observeQueue: () -> Flow<OldPlaybackQueue>,
    private val observePalette: (String) -> Flow<Palette?>
) {

    class Applier(private val state: RootPlaybackControlBackgroundState) {

        companion object {

            @Composable
            fun Applier.ComposeLayout(
                content: @Composable CompositionScope.() -> Unit
            ) {
                observeForScope()
                    .run {
                        content()
                        DoLayoutComposedWork(composition = this)
                    }
            }

            @Composable
            private fun Applier.observeForScope(): CompositionScope {
                val composableCoroutineScope = rememberCoroutineScope()
                val latestScope = remember {
                    mutableStateOf(
                        CompositionScope(
                            CoroutineScope(EmptyCoroutineContext),
                            OldPlaybackQueue.UNSET,
                            { emptyFlow() }
                        )
                    )
                }
                DisposableEffect(
                    key1 = this,
                    effect = {
                        val supervisor = SupervisorJob()

                        composableCoroutineScope.launch {
                            state.observeQueue()
                                .collect {
                                    latestScope.value.onReplaced()
                                    latestScope.value = CompositionScope(
                                        lifetimeCoroutineScope = CoroutineScope(
                                            currentCoroutineContext() + SupervisorJob(),
                                        ),
                                        queueData = it,
                                        observePalette = state.observePalette
                                    )
                                }
                        }

                        onDispose {
                            latestScope.value.onDetachedFromComposition()
                            supervisor.cancel()
                        }
                    }
                )
                return latestScope.value
            }

            @Composable
            private fun Applier.DoLayoutComposedWork(composition: CompositionScope) {

            }
        }
    }

    class CompositionScope(
        private val lifetimeCoroutineScope: CoroutineScope,
        private val queueData: OldPlaybackQueue,
        private val observePalette: (String) -> Flow<Palette?>
    ) {

        fun onReplaced() {
            lifetimeCoroutineScope.cancel()
        }

        fun onDetachedFromComposition() {
            lifetimeCoroutineScope.cancel()
        }

        companion object {

            @SuppressLint("ModifierFactoryExtensionFunction")
            fun CompositionScope.animatedRadialBackgroundModifier(
                spec: AnimationSpec<Color>,
                dark: Boolean,
                center: Offset,
                radiusPx: Float,
            ): Modifier {
                return Modifier.composed {
                    val backgroundColor = Theme.backgroundColorAsState().value
                    val scope = this@animatedRadialBackgroundModifier

                    val animatable = remember {
                        Animatable(
                            initialValue = backgroundColor,
                            typeConverter = Color.VectorConverter(backgroundColor.colorSpace)
                        )
                    }

                    SubCompose {
                        val id = remember(scope) {
                            with(scope) {
                                queueData.list
                                    .getOrNull(queueData.currentIndex)
                            }
                        }
                        val palette = id?.let {
                            remember(scope, id) {
                                scope.observePalette(id)
                            }.collectAsState(initial = null).value
                        }
                        val paletteColor = palette.run {
                            if (this is Palette) {
                                remember(this, dark) {
                                    val argb = if (dark) {
                                        getDarkVibrantColor(getMutedColor(getDominantColor(-1)))
                                    } else {
                                        getVibrantColor(getLightMutedColor(getDominantColor(-1)))
                                    }
                                    Color(argb)
                                }
                            } else {
                                backgroundColor
                            }
                        }
                        val updatedAnimSpec = rememberUpdatedState(newValue = spec)
                        val channel = remember { Channel<Color>(Channel.CONFLATED) }
                        LaunchedEffect(paletteColor) {
                            channel.send(paletteColor)
                        }
                        LaunchedEffect(channel) {
                            for (target in channel) {
                                val newTarget = channel.tryReceive().getOrNull() ?: target
                                launch {
                                    if (newTarget != animatable.targetValue) {
                                        animatable.animateTo(newTarget, updatedAnimSpec.value)
                                    }
                                }
                            }
                        }
                    }

                    remember(backgroundColor, animatable.value, center, radiusPx) {
                        Modifier.background(
                            brush = Brush.radialGradient(
                                colors = run {
                                    val radialColorBase = animatable.value
                                    val compositeBase = backgroundColor
                                    listOf(
                                        radialColorBase.copy(alpha = 0.55f).compositeOver(compositeBase),
                                        radialColorBase.copy(alpha = 0.45f).compositeOver(compositeBase),
                                        radialColorBase.copy(alpha = 0.35f).compositeOver(compositeBase),
                                        radialColorBase.copy(alpha = 0.2f).compositeOver(compositeBase),
                                        radialColorBase.copy(alpha = 0.15f).compositeOver(compositeBase),
                                        radialColorBase.copy(alpha = 0.1f).compositeOver(compositeBase),
                                        radialColorBase.copy(alpha = 0.05f).compositeOver(compositeBase),
                                        radialColorBase.copy(alpha = 0.0f).compositeOver(compositeBase)
                                    )
                                },
                                center = center,
                                radius = radiusPx
                            )
                        )
                    }
                }
            }
        }
    }
}