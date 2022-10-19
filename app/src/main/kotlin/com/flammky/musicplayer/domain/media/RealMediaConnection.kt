package com.flammky.musicplayer.domain.media

import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.common.kotlin.coroutines.safeCollect
import com.flammky.musicplayer.base.media.MediaConnectionDelegate
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import kotlin.time.Duration

class RealMediaConnection(
	private val delegate: MediaConnectionDelegate
) : MediaConnection {

	private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

	override val playback: MediaConnection.Playback = Playback()
	override val repository: MediaConnection.Repository = Repository()


	private inner class Playback : MediaConnection.Playback {
		override fun playWhenReady() = delegate.play()
		override fun pause() = delegate.pause()

		override fun observeInfo(): Flow<MediaConnection.PlaybackInfo> {
			return callbackFlow {

				val mutex = Mutex()
				var remember = MediaConnection.PlaybackInfo.UNSET

				val job1 = ioScope.launch {
					delegate.playback.observeMediaItem().safeCollect { mediaItem ->
						Timber.d("observeMediaItemConnection $mediaItem ${mediaItem?.mediaId}")
						mutex.withLock {
							if (remember.id != mediaItem?.mediaId) {
								remember = MediaConnection.PlaybackInfo(
									id = mediaItem?.mediaId ?: "",
									playWhenReady = delegate.playback.playWhenReady,
									playing = delegate.playback.playing,
									playlist = delegate.playback.getPlaylist().map { it.mediaId }.toPersistentList(),
									currentIndex = delegate.playback.index
								)
								send(remember)
							}
						}
					}
				}

				val job4 = ioScope.launch {
					delegate.playback.observePlayWhenReady().safeCollect {
						mutex.withLock {
							remember = remember.copy(playWhenReady = it)
							send(remember)
						}
					}
				}

				val job5 = ioScope.launch {
					delegate.playback.observeIsPlaying().safeCollect {
						mutex.withLock {
							remember = remember.copy(playing = it)
							send(remember)
						}
					}
				}

				awaitClose {
					job1.cancel();job4.cancel();job5.cancel()
				}
			}
		}

		override fun observePositionStream(): Flow<MediaConnection.Playback.PositionStream> = callbackFlow {

			val mutex = Mutex()
			var remember = MediaConnection.Playback.PositionStream()
			var positionCollectorJob: Job? = null

			suspend fun updateDuration(duration: Duration) = mutex.withLock {
				Timber.d("observePositionStream $duration")
				if (remember.duration != duration) {
					remember = remember.copy(duration = duration)
					send(remember)
				}
			}

			suspend fun collectPosition() {
				while (isActive) {
					mutex.withLock {
						remember = remember.copy(
							position = delegate.playback.position(),
							bufferedPosition = delegate.playback.bufferedPosition()
						)
						send(remember)
					}
					delay(500)
				}
			}

			suspend fun restartPositionCollector() = mutex.withLock {
				positionCollectorJob?.cancel()
				remember = MediaConnection.Playback.PositionStream()
				send(remember)
				positionCollectorJob = launch { collectPosition() }
			}

			send(remember)

			val transitionObserverJob = ioScope.launch {
				delegate.playback.observeMediaItem().safeCollect { restartPositionCollector() }
			}

			val timelineObserverJob = ioScope.launch {
				delegate.playback.observeTimeline().safeCollect { updateDuration(it.duration) }
			}

			awaitClose {
				positionCollectorJob?.cancel()
				timelineObserverJob.cancel()
				transitionObserverJob.cancel()
			}
		}

		override fun observePlaylistStream(): Flow<MediaConnection.Playback.PlaylistStream> = callbackFlow {
			val mutex = Mutex()
			var remember = MediaConnection.Playback.PlaylistStream()

			val playlistObserver = ioScope.launch {
				delegate.playback.observePlaylist().safeCollect {
					mutex.withLock {
						remember = remember.copy(
							reason = 1,
							currentIndex = delegate.playback.index,
							list = it.toPersistentList()
						)
						send(remember)
					}
				}
			}

			val currentObserver = ioScope.launch {
				delegate.playback.observeMediaItem().safeCollect {
					mutex.withLock {
						remember = remember.copy(
							reason = 0,
							currentIndex = delegate.playback.index,
							list = delegate.playback.getPlaylist().map { it.mediaId }.toPersistentList()
						)
						send(remember)
					}
				}
			}

			awaitClose {
				playlistObserver.cancel()
				currentObserver.cancel()
			}
		}
	}

	private inner class Repository : MediaConnection.Repository {
		// We should localize them
		override fun observeMetadata(id: String): Flow<MediaMetadata?> = delegate.repository.observeMetadata(id)
		override fun observeArtwork(id: String): Flow<Any?> = delegate.repository.observeArtwork(id)
		override fun provideMetadata(id: String, metadata: MediaMetadata) = delegate.repository.provideMetadata(id, metadata)
		override fun provideArtwork(id: String, artwork: Any?) = delegate.repository.provideArtwork(id, artwork)
	}
}
