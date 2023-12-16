package dev.dexsr.klio.player.android.presentation.root.main

import dev.dexsr.klio.base.UNSET

data class PlaybackProgressionState(
    val isPlaying: Boolean,
    val canPlay: Boolean,
    val playWhenReady: Boolean,
    val canPlayWhenReady: Boolean,
    val repeatMode: Int,
    val canToggleRepeat: Boolean,
    val shuffleMode: Int,
    val canToggleShuffleMode: Boolean,
    val canSeekNext: Boolean,
    val canSeekPrevious: Boolean,
): UNSET<PlaybackProgressionState> by Companion {

    companion object : UNSET<PlaybackProgressionState> {

        override val UNSET: PlaybackProgressionState = PlaybackProgressionState(
            isPlaying = false,
            canPlay = false,
            playWhenReady = false,
            canPlayWhenReady = false,
            repeatMode = -1,
            shuffleMode = -1,
            canToggleRepeat = false,
            canToggleShuffleMode = false,
            canSeekNext = false,
            canSeekPrevious = false,
        )
    }
}
