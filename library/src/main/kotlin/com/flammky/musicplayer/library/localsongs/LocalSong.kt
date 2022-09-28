package com.flammky.musicplayer.library.localsongs

import com.flammky.android.medialib.common.mediaitem.MediaItem

/** Local Song entity */
class LocalSong(

	/** The Id  */
	val id: String,

	/** The MediaItem used for playback interaction */
	val mediaItem: MediaItem
)
