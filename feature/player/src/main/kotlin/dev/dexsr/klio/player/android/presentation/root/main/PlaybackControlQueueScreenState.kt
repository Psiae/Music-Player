package dev.dexsr.klio.player.android.presentation.root.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class PlaybackControlQueueScreenState {
}

@Composable
fun rememberPlaybackControlQueueScreenState(
    container: PlaybackControlScreenState,
    transitionState: PlaybackControlScreenTransitionState
): PlaybackControlQueueScreenState {

    return remember(container) {
        PlaybackControlQueueScreenState()
    }
}