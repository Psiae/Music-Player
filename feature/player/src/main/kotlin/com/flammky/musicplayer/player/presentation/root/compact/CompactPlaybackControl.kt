package com.flammky.musicplayer.player.presentation.root.compact

import androidx.compose.runtime.Composable

// should the compact variant be part of [TransitioningPlaybackControl] composition ?

@Composable
fun CompactPlaybackControl(
    state: CompactPlaybackControlState
) = state.coordinator.ComposeContent(
    content = {}
)

class CompactPlaybackControlState(

) {
    val coordinator = CompactPlaybackControlCoordinator()
}

class CompactPlaybackControlCoordinator(

) {
    val layoutCoordinator = CompactPlaybackControlLayoutCoordinator()

    @Composable
    fun ComposeContent(
        content: @Composable () -> Unit
    ) {

    }
}

class CompactPlaybackControlLayoutCoordinator(

) {

    @Composable
    fun PlaceLayout() {

    }
}