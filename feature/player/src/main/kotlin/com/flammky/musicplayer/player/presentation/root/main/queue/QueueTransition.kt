package com.flammky.musicplayer.player.presentation.root.main.queue

import android.os.Bundle
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.absoluteBackgroundColorAsState
import com.flammky.musicplayer.player.presentation.root.main.MainContainerTransitionState
import kotlinx.coroutines.*
import timber.log.Timber

class QueueContainerTransitionState(
    private val parent: MainContainerTransitionState,
    private val uiCoroutineScope: CoroutineScope,
    private val initialRememberFullTransitionRendered: Boolean = false,
    private val isRestoredInstance: Boolean = false
) {
    private var saveCount = 0

    private var stagingJob: Job = Job().apply { cancel() }
    private var constraints by mutableStateOf(Constraints.fixed(0,0))

    var show by mutableStateOf(false)
        private set

    var targetHeightPx by mutableStateOf(0)
        private set

    var stagedHeightPx by mutableStateOf(0)
        private set

    var renderedHeightPx by mutableStateOf(0)
        private set

    var targetWidthPx by mutableStateOf(0)
        private set

    var stagedWidthPx by mutableStateOf(0)
        private set

    var renderedWidthPx by mutableStateOf(0)
        private set

    var rememberFullTransitionRendered by mutableStateOf(false)
        private set

    init {
        if (initialRememberFullTransitionRendered) {
            rememberFullTransitionRendered = true
            show()
        }
    }

    fun show() {
        Timber.d(
            "player.root.main.queue.MainTransition.kt: QueueContainerTransitionState@${System.identityHashCode(this)}_show()"
        )
        if (show) {
            return
        }
        this.show = true
        onNewVisibility()
    }

    fun hide() {
        Timber.d(
            "player.root.queue.MainTransition.kt: QueueContainerTransitionState@${System.identityHashCode(this)}_hide()"
        )
        if (!show) {
            return
        }
        this.show = false
        onNewVisibility()
    }

    fun updateConstraints(
        constraints: Constraints
    ) {
        if (this.constraints == constraints) {
            return
        }
        this.constraints = constraints
        onNewConstraints()
    }

    fun onRender(
        heightPx: Int
    ) {
        this.renderedHeightPx = heightPx
        rememberFullTransitionRendered = this.renderedHeightPx == targetHeightPx
    }

    private fun onNewConstraints() {
        Timber.d("player.root.main.queue.MainTransition.kt: QueueContainerTransitionState new constraints=$constraints")
        this.targetHeightPx = constraints.maxHeight
        invalidateTarget()
    }

    private fun onNewVisibility() {
        invalidateTarget()
    }

    private fun invalidateTarget() {
        stagingJob.cancel()
        stagingJob = uiCoroutineScope
            .launch(SupervisorJob()) {
                val animatable = Animatable<Int, AnimationVector1D>(
                    stagedHeightPx,
                    Int.VectorConverter
                )
                launch(Dispatchers.Main.immediate) {
                    snapshotFlow { animatable.value }
                        .collect { value ->
                            Timber.d(
                                "player.root.main.queue.MainTransition.kt: " +
                                        "QueueContainerTransitionState@${System.identityHashCode(this@QueueContainerTransitionState)} " +
                                        "new staged height px=$value, show=$show, constraints=$constraints"
                            )
                            stagedHeightPx = value
                        }
                }
                animatable.animateTo(
                    targetValue = if (show) constraints.maxHeight else 0,
                    animationSpec = tween(
                        durationMillis = if (show) {
                            if (rememberFullTransitionRendered) {
                                0
                            } else {
                                350
                            }
                        } else {
                            250
                        },
                        easing = if (show) FastOutSlowInEasing else LinearOutSlowInEasing
                    )
                )
            }
    }

    companion object {

        @Suppress("FunctionName")
        fun Saver(
            parent: MainContainerTransitionState,
            uiCoroutineScope: CoroutineScope
        ): Saver<QueueContainerTransitionState, Bundle> = Saver(
            save = { self ->
                Timber.d("player.root.main.queue.MainTransition.kt: QueueContainerTransitionState.Companion_Saver save($self, ${self.saveCount})")
                Bundle()
                    .apply {
                        if (self.show) {
                            run rememberTransition@ {
                                val prop = self::rememberFullTransitionRendered
                                putBoolean(prop.name, prop.get())
                            }
                        }
                    }
            },
            restore = { bundledSelf ->
                Timber.d("player.root.main.queue.MainTransition.kt: QueueContainerTransitionState.Companion_Saver restore($bundledSelf})")
                QueueContainerTransitionState(
                    parent,
                    uiCoroutineScope = uiCoroutineScope,
                    initialRememberFullTransitionRendered = run rememberTransition@ {
                        val prop = MainContainerTransitionState::rememberFullTransitionRendered
                        bundledSelf.getBoolean(prop.name)
                    },
                    isRestoredInstance = true
                )
            }
        )
    }
}

@Composable
fun QueueTransition(
    state: QueueContainerTransitionState,
    content: @Composable () -> Unit
) = PlaceContents(
    container = { AnimatedTransitionContainer(state, content) }
)

@Composable
private fun PlaceContents(container: @Composable () -> Unit) = container()

@Composable
private fun InteractionBlocker(
    consume: Boolean
) {
    val blockerFraction = if (consume) 1f else 0f
    Box(
        modifier = Modifier
            .fillMaxSize(blockerFraction)
            .clickable(
                enabled = consume,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    )
}

@Composable
private fun AnimatedTransitionContainer(
    state: QueueContainerTransitionState,
    content: @Composable () -> Unit
) = BoxWithConstraints {
    state.apply {
        updateConstraints(constraints)
    }
    InteractionBlocker(
        consume = state.stagedHeightPx > 0 || state.renderedHeightPx > 0
    )
    val height = state.stagedHeightPx
    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset {
                IntOffset(
                    x = 0,
                    y = (constraints.maxHeight - height).coerceAtLeast(0)
                )
            }
            .background(Theme.absoluteBackgroundColorAsState().value)
            .onGloballyPositioned { state.onRender(height) }
    ) {
        if (height > 0) {
            content()
        }
    }

    DisposableEffect(
        key1 = state,
        effect = {

            onDispose { state.onRender(0) }
        }
    )
}