package dev.dexsr.klio.player.android.presentation.root

import dev.dexsr.klio.base.UNSET

data class PlaybackProgressionState(
    val isPlaying: Boolean,
    val canPlay: Boolean,
    val playWhenReady: Boolean,
    val canPlayWhenReady: Boolean,
): UNSET<PlaybackProgressionState> by Companion {

    companion object : UNSET<PlaybackProgressionState> {

        override val UNSET: PlaybackProgressionState = PlaybackProgressionState(
            isPlaying = false,
            canPlay = false,
            playWhenReady = false,
            canPlayWhenReady = false
        )
    }
}
