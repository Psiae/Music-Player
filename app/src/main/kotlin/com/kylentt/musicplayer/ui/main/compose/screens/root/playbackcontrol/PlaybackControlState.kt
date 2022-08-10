package com.kylentt.musicplayer.ui.main.compose.screens.root.playbackcontrol

import androidx.compose.runtime.Immutable
import androidx.media3.common.MediaItem

@Immutable
data class PlaybackControlState(
	val currentItem: MediaItem,
	val playList: List<MediaItem>,
	val isPlaying: Boolean
)
