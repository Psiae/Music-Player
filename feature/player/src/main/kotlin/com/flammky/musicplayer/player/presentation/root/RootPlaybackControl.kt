package com.flammky.musicplayer.player.presentation.root

import android.content.pm.ActivityInfo
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import com.flammky.androidx.content.context.findActivity
import com.flammky.musicplayer.base.compose.NoInline
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

        },
    )
}

@Composable
private fun RootPlaybackControl(
    modifier: Modifier,
    state: RootPlaybackControlState,
    content: @Composable RootPlaybackControlMainScope.() -> Unit,
) {
    if (state.showSelfState.value) {
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
            val queueScope = remember(mainScope) {
                RootPlaybackControlQueueScope(mainScope)
            }
            val blockerFraction =
                if (state.showSelfState.value || state.mainScreenVisible) 1f else 0f

            // Inline Box to immediately consume input during transition
            Box(
                modifier = Modifier
                    .fillMaxSize(blockerFraction)
                    .pointerInput(Unit) {
                        forEachGesture {
                            awaitPointerEventScope { awaitFirstDown().consume() }
                        }
                    }
            )

            // Main Content
            mainScope
                .apply {
                    fullyVisibleHeightTarget = with(LocalDensity.current) {
                        this@BoxWithConstraints.maxHeight.roundToPx()
                    }
                }
                .run {
                    content()
                }
        }
    }
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