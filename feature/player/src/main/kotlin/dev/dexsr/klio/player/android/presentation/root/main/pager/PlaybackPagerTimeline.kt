package dev.dexsr.klio.player.android.presentation.root.main.pager

import dev.dexsr.klio.base.UNSET

class PlaybackPagerTimeline(
    val windows: List<String>,
    val currentIndex: Int
): UNSET<PlaybackPagerTimeline> by Companion {

    companion object : UNSET<PlaybackPagerTimeline> {

        override val UNSET: PlaybackPagerTimeline = PlaybackPagerTimeline(
            listOf(),
            -1
        )
    }

    fun toDebugString(): String = "PlaybackPagerTimeline@${Integer.toHexString(System.identityHashCode(this))}(windows=$windows, currentIndex=$currentIndex)"
}