package com.flammky.musicplayer.base.media.r

import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider.ContentObserver.Flag.Companion.isDelete
import com.flammky.musicplayer.base.Playback
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.base.media.playback.PlaybackConstants
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.core.common.sync
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

// Watcher should be started by service and have direct access instead
class MediaContentWatcher(
	private val authService: AuthService,
	private val connection: PlaybackConnection,
	private val mediaStoreProvider: MediaStoreProvider,
) {
	private val stateLock = Any()
	private var started: Boolean = false
	private val coroutineScope = CoroutineScope(SupervisorJob())

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
		if (flag.isDelete) notifyUnplayableMedia(id)
	}

	private fun notifyUnplayableMedia(id: String) {
		authService.currentUser
			?.let { user ->
				coroutineScope.launch(Playback.DISPATCHER) {
					connection.requestUserSessionAsync(user).await().controller.withLooperContext {
						val gq = getQueue()
						val filtered = mutableListOf<String>()
						var removed = 0
						gq.list.forEachIndexed { i, item ->
							if (item != id) filtered.add(item) else if (gq.currentIndex >= i) removed++
						}
						val q = OldPlaybackQueue(
							list = filtered.toPersistentList(),
							currentIndex = (gq.currentIndex - removed).takeIf { it >= 0 }
								?: PlaybackConstants.INDEX_UNSET
						)
						setQueue(q)
					}
				}
			}
	}
}
