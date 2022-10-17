package com.flammky.musicplayer.base.media

import com.flammky.android.medialib.common.mediaitem.MediaItem

interface MediaConnectionDelegate {
	fun play(item: MediaItem)
}
