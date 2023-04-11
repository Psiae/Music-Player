package com.flammky.musicplayer.player.presentation.root.queue

import androidx.compose.runtime.Composable
import com.flammky.musicplayer.base.compose.SnapshotReader

@Composable
fun PlaybackQueueScreen(
    state: PlaybackQueueScreenState
) = state.coordinator.ComposeLayout(
    playbackControl = {

    },
    queue = {

    }
)

class PlaybackQueueScreenState() {
    val coordinator = PlaybackQueueScreenCoordinator()
}

class PlaybackQueueScreenCoordinator() {
    val layoutCoordinator = PlaybackQueueScreenLayoutCoordinator()

    @Composable
    fun ComposeLayout(
        playbackControl: @SnapshotReader () -> Unit,
        queue: @SnapshotReader () -> Unit
    ) {

    }
}

class PlaybackQueueScreenLayoutCoordinator() {

}