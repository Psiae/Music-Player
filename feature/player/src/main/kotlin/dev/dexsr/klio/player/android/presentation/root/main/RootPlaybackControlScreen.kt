package dev.dexsr.klio.player.android.presentation.root.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.flammky.musicplayer.base.compose.LocalLayoutVisibility
import dev.dexsr.klio.player.android.presentation.root.bw.OldRootPlaybackControlScreen

@Composable
fun RootPlaybackControlScreen(
    state: PlaybackControlScreenState,
    bottomVisibilitySpacing: Dp
) {
    CompositionLocalProvider(
        LocalLayoutVisibility.Bottom provides bottomVisibilitySpacing
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
}

