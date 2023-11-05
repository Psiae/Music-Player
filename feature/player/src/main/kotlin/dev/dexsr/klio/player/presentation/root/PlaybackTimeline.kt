package dev.dexsr.klio.player.presentation.root

import kotlinx.collections.immutable.ImmutableList

data class PlaybackTimeline(
    val currentIndex: String,
    val items: ImmutableList<String>,
)
