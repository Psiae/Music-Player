package com.flammky.musicplayer.base.media.mediaconnection

import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.temp.MediaLibrary

class RealMediaConnectionDelegate(
	private val mediaStore: MediaStoreProvider,
	override val playback: MediaConnectionPlayback,
	override val repository: MediaConnectionRepository,
) : MediaConnectionDelegate {
	// Temp
	private val s = MediaLibrary.API.sessions.manager.findSessionById("DEBUG")!!

	override fun play(item: MediaItem) {
		s.mediaController.play(item)
	}

	override fun play(items: List<MediaItem>, index: Int) {
		if (index in items.indices) {
			s.mediaController.setMediaItems(items, index)
			s.mediaController.play()
		}
	}

	override fun play() {
		s.mediaController.play()
	}

	override fun pause() {
		s.mediaController.pause()
	}

	private val watcher = MediaConnectionWatcher(mediaStore, this)

	init {
		watcher.start()
	}
}
