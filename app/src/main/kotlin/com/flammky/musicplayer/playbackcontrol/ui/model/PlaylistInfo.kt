package com.flammky.musicplayer.playbackcontrol.ui.model

import com.flammky.musicplayer.media.PlaybackConstants
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class PlaylistInfo(
	val list: ImmutableList<String>,
	val index: Int
) {
	companion object {
		val UNSET = PlaylistInfo(persistentListOf(), PlaybackConstants.INDEX_UNSET)
	}
}
