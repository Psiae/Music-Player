package com.flammky.musicplayer.player.presentation.root.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Composable
fun TransitioningPlaybackControl(
    state: PlaybackControlState
) = state.coordinator.ComposeLayout(
    screen = {

    }
)

class PlaybackControlState() {

    val coordinator = PlaybackControlCoordinator()
}

class PlaybackControlCoordinator() {

    private val layoutCoordinator = PlaybackControlLayoutCoordinator()

    interface PlaybackControlScreenScope {

    }

    private class PlaybackControlScreenScopeImpl(

    ) : PlaybackControlScreenScope {


    }

    @Composable
    fun ComposeLayout(
        screen: PlaybackControlScreenScope.() -> Unit
    ) {
        val upScreen = rememberUpdatedState(screen)
        val scope = remember(this) {
            val impl = PlaybackControlScreenScopeImpl()
            derivedStateOf { impl.apply(upScreen.value) }
        }
    }
}

class PlaybackControlLayoutCoordinator() {

}