package com.flammky.musicplayer.base.media

import com.flammky.android.medialib.common.mediaitem.MediaItem

/**
 * Delegator for Different MediaConnection on each Module
 */
interface MediaConnectionDelegate {



	fun play()
	fun play(item: MediaItem)
	fun pause()

	val playback: MediaConnectionPlayback
	val repository: MediaConnectionRepository
}
