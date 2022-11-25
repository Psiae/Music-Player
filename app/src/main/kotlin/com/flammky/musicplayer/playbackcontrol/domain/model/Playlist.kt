package com.flammky.musicplayer.playbackcontrol.domain.model

import kotlinx.collections.immutable.ImmutableList

data class Playlist(
	val index: Int,
	val list: ImmutableList<String>
) {

}
