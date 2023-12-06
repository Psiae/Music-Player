package dev.dexsr.klio.player.android.presentation.root

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.dexsr.klio.player.android.presentation.root.bw.OldRootPlaybackControlScreen

@Composable
fun RootPlaybackControlScreen(
    state: PlaybackControlScreenState
) {
    // TODO
    if (false) {
        OldRootPlaybackControlScreen(state)
    } else {
        TransitioningPlaybackControlScreen(
            modifier = Modifier,
            state = state
        )
    }
}

