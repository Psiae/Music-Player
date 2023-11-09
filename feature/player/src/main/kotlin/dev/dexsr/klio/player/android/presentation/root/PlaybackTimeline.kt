package dev.dexsr.klio.player.android.presentation.root

import kotlinx.collections.immutable.ImmutableList

data class PlaybackTimeline(
    val currentIndex: Int,
    val items: ImmutableList<String>,
)
