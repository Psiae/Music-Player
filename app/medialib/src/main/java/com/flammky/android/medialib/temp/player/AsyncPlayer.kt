package com.flammky.android.medialib.temp.player

import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.common.mediaitem.RealMediaItem
import kotlinx.coroutines.Deferred

interface AsyncPlayer {
	val currentMediaItem: Deferred<MediaItem>
	val realMediaItems: Deferred<List<MediaItem>>

	suspend fun play(item: MediaItem)
	suspend fun pause()
	suspend fun stop()
	suspend fun prepare()
}
