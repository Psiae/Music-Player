package dev.dexsr.klio.player.android.presentation.root

import dev.dexsr.klio.base.UNSET
import kotlin.time.Duration

data class PlaybackProgress(
    val duration: Duration,
    val position: Duration,
    val bufferedPosition: Duration
): UNSET<PlaybackProgress> by Companion {

    companion object : UNSET<PlaybackProgress> {

        override val UNSET: PlaybackProgress = PlaybackProgress(
            duration = Duration.ZERO,
            position = Duration.ZERO,
            bufferedPosition = Duration.ZERO
        )
    }
}
