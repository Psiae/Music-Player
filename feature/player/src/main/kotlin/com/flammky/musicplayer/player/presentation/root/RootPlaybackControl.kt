package com.flammky.musicplayer.player.presentation.root

import android.content.pm.ActivityInfo
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flammky.androidx.content.context.findActivity
import com.flammky.musicplayer.base.compose.NoInline
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.absoluteBackgroundColorAsState
import com.flammky.musicplayer.player.presentation.PlaybackControlViewModel
import com.flammky.musicplayer.player.presentation.controller.PlaybackController

@Composable
internal fun RootPlaybackControl(
    modifier: Modifier = Modifier,
    state: RootPlaybackControlState
) {
    RootPlaybackControl(
        modifier = modifier,
        state = state,
        content = {
            ContentTransition()
        },
    )
}

@Composable
private fun RootPlaybackControl(
    modifier: Modifier,
    state: RootPlaybackControlState,
    content: @Composable RootPlaybackControlMainScope.() -> Unit,
) {
    if (state.showMainState.value) {
        LockScreenOrientation(landscape = false)
    }

    val viewModel: PlaybackControlViewModel = hiltViewModel()

    val controllerState = remember {
        mutableStateOf<PlaybackController?>(null)
    }

    DisposableEffect(
        state,
        viewModel,
        effect = {
            val controller = viewModel.createUserPlaybackController(state.user)
            controllerState.value = controller
            onDispose { controller.dispose() }
        }
    )
    NoInline {
        val controller = controllerState.value
            ?: return@NoInline
        BoxWithConstraints(
            modifier = modifier
        ) {
            val mainScope = remember(state, controller) {
                RootPlaybackControlMainScope(state, controller)
            }
            // Main Content
            mainScope
                .apply {
                    fullyVisibleHeightTarget = constraints.maxHeight
                }
                .run {
                    state.currentComposition = this
                    content()
                }
        }
    }
}

@Composable
private fun RootPlaybackControlMainScope.ContentTransition() {
    // Think if there is somewhat better way to do this
    val transitionHeightState = updateTransition(targetState = state.showMainState.value, label = "")
        .animateInt(
            label = "Playback Control Transition",
            targetValueByState = { targetShowSelf ->
                if (targetShowSelf) fullyVisibleHeightTarget else 0
            },
            transitionSpec = {
                remember(targetState) {
                    tween(
                        durationMillis = if (targetState) {
                            if (state.rememberMainFullyTransitionedState.value) {
                                0
                            } else {
                                350
                            }
                        } else {
                            250
                        },
                        easing = if (targetState) FastOutSlowInEasing else LinearOutSlowInEasing
                    )
                }
            }
        ).apply {
            LaunchedEffect(
                this@ContentTransition, this.value, fullyVisibleHeightTarget,
                block = {
                    visibleHeight = value
                    if (visibleHeight == fullyVisibleHeightTarget) {
                        state.rememberMainFullyTransitionedState.value = true
                    } else if (visibleHeight == 0) {
                        state.rememberMainFullyTransitionedState.value = false
                    }
                }
            )
        }

    val blockerFraction =
        if (state.showMainState.value || visibleHeight > 0) 1f else 0f

    // Inline Box to immediately consume input during transition
    Box(
        modifier = Modifier
            .fillMaxSize(blockerFraction)
            .clickable(
                enabled = true,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        if (visibleHeight > 0) {
            val localDensity = LocalDensity.current
            val absBackgroundColor = Theme.absoluteBackgroundColorAsState().value
            Layout(
                modifier = Modifier
                    .runRemember {
                        Modifier.fillMaxWidth()
                    }
                    .runRemember(
                        localDensity,
                        fullyVisibleHeightTarget,
                        transitionHeightState.value
                    ) {
                        this
                            .height(
                                height = with(localDensity) {
                                    fullyVisibleHeightTarget.toDp()
                                }
                            )
                            .offset(
                                y = with(localDensity) {
                                    (fullyVisibleHeightTarget - transitionHeightState.value).toDp()
                                }
                            )
                    }
                    .runRemember(absBackgroundColor) {
                        this
                            .background(
                                color = absBackgroundColor.copy(alpha = 0.97f)
                            )
                    },
                background = { RadialPlaybackBackground(composition = it) },
                toolbar = { Toolbar(composition = it) },
                pager = { Pager(composition = it) },
                description = { Pager(composition = it) },
                seekbar = { Seekbar(composition = it) },
                primaryControlRow = { PrimaryControlRow(composition = it) },
                secondaryControlRow = { SecondaryControlRow(composition = it) }
            )
        }
    }
}

@Composable
private fun RootPlaybackControlMainScope.Layout(
    modifier: Modifier,
    background: @Composable BoxScope.(composition: RootPlaybackControlMainScope) -> Unit,
    toolbar: @Composable BoxScope.(composition: RootPlaybackControlMainScope) -> Unit,
    pager: @Composable BoxScope.(composition: RootPlaybackControlMainScope) -> Unit,
    description: @Composable BoxScope.(composition: RootPlaybackControlMainScope) -> Unit,
    seekbar: @Composable BoxScope.(composition: RootPlaybackControlMainScope) -> Unit,
    primaryControlRow: @Composable RowScope.(composition: RootPlaybackControlMainScope) -> Unit,
    secondaryControlRow: @Composable RowScope.(composition: RootPlaybackControlMainScope) -> Unit,
    // TODO: Lyrics ?
) {
    val composition = this
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        background(composition)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                toolbar(composition)
            }
            Spacer(modifier = Modifier.height(15.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                pager(composition)
            }
            Spacer(modifier = Modifier.height(15.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                description(composition)
            }
            Spacer(modifier = Modifier.height(15.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                seekbar(composition)
            }
            Spacer(modifier = Modifier.height(15.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                primaryControlRow(composition)
            }
            Spacer(modifier = Modifier.height(15.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                secondaryControlRow(composition)
            }
        }
    }
}

@Composable
private fun BoxScope.RadialPlaybackBackground(
    composition: RootPlaybackControlMainScope
) {

}

@Composable
private fun BoxScope.Toolbar(
    composition: RootPlaybackControlMainScope
) {

}

@Composable
private fun BoxScope.Pager(
    composition: RootPlaybackControlMainScope
) {

}

@Composable
private fun BoxScope.Description(
    composition: RootPlaybackControlMainScope
) {

}

@Composable
private fun BoxScope.Seekbar(
    composition: RootPlaybackControlMainScope
) {

}

@Composable
private fun BoxScope.Queue(
    composition: RootPlaybackControlMainScope
) {

}

@Composable
private fun RowScope.PrimaryControlRow(
    composition: RootPlaybackControlMainScope
) {

}

@Composable
private fun RowScope.SecondaryControlRow(
    composition: RootPlaybackControlMainScope
) {

}


@Composable
private fun LockScreenOrientation(landscape: Boolean) {
    val activity = LocalContext.current.findActivity()
        ?: error("cannot Lock Screen Orientation, LocalContext is not an Activity")
    DisposableEffect(key1 = Unit) {
        val original = activity.requestedOrientation
        activity.requestedOrientation =
            if (landscape) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        onDispose { activity.requestedOrientation = original }
    }
}

@Composable
inline fun <T, R> T.runRemember(
    vararg keys: Any?,
    crossinline block: @DisallowComposableCalls T.() -> R
): R {
    return remember(this, *keys) {
        block()
    }
}