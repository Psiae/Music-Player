package com.flammky.musicplayer.base.media.mediaconnection

import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider.ContentObserver.Flag.Companion.isDelete
import com.flammky.kotlin.common.sync.sync
import timber.log.Timber

class MediaConnectionWatcher(
	private val mediaStoreProvider: MediaStoreProvider,
	private val mediaConnectionDelegate: MediaConnectionDelegate
) {
	private val stateLock = Any()
	private var started: Boolean = false

	fun start() {
		sync(stateLock) {
			if (started) return
			mediaStoreProvider.audio.observe(_audioObserver)
			started = true
		}
	}

	fun stop() {
		sync(stateLock) {
			if (!started) return
			mediaStoreProvider.audio.removeObserver(_audioObserver)
			started = false
		}
	}

	private val _audioObserver = MediaStoreProvider.ContentObserver { id, uri, flag ->
		Timber.d("MediaConnectionWatcher audioObserver $id, $uri, $flag")
		if (flag.isDelete) mediaConnectionDelegate.playback.notifyUnplayableMedia(id)
	}
}
