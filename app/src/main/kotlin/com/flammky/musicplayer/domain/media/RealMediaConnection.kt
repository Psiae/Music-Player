package com.flammky.musicplayer.domain.media

import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.common.kotlin.coroutines.safeCollect
import com.flammky.musicplayer.base.media.mediaconnection.MediaConnectionDelegate
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
		override fun setMediaItems(items: List<MediaItem>, startIndex: Int, startPosition: Duration) {
			delegate.playback.setMediaItems(items, startIndex, startPosition)
		}

		override fun stop() {
			delegate.playback.stop()
		}

		override suspend fun <R> joinSuspend(block: MediaConnection.Playback.() -> R): R {
			return delegate.playback.joinDispatcher { block() }
		}

		override fun prepare() = delegate.playback.prepare()
		override fun playWhenReady() = delegate.play()
		override fun pause() = delegate.pause()

		override fun observeInfo(): Flow<MediaConnection.PlaybackInfo> {
			return callbackFlow {

				suspend fun sendNew() {
					val new = delegate.playback.joinDispatcher {
						MediaConnection.PlaybackInfo(
							id = delegate.playback.getCurrentMediaItem()?.mediaId ?: "",
							playWhenReady = delegate.playback.playWhenReady,
							playing = delegate.playback.playing
						)
					}
					send(new)
				}

				val job1 = ioScope.launch {
					delegate.playback.observeMediaItem().safeCollect { sendNew() }
				}

				val job2 = ioScope.launch {
					delegate.playback.observePlayWhenReady().safeCollect { sendNew() }
				}

				val job3 = ioScope.launch {
					delegate.playback.observeIsPlaying().safeCollect { sendNew() }
				}

				awaitClose {
					job1.cancel();job2.cancel();job3.cancel()
				}
			}
		}

		override fun observePositionStream(): Flow<MediaConnection.Playback.PositionStream> = callbackFlow {

			var positionCollectorJob: Job? = null
			val mutex = Mutex()

			suspend fun sendUpdatedPositions() {
				val new = delegate.playback.joinDispatcher {
					MediaConnection.Playback.PositionStream(
						position = delegate.playback.position(),
						bufferedPosition = delegate.playback.bufferedPosition(),
						duration = delegate.playback.duration()
					)
				}
				send(new)
			}

			suspend fun collectPosition() {
				while (isActive) {
					sendUpdatedPositions()
					delay(500)
				}
			}

			fun stopPositionCollector() = positionCollectorJob?.cancel()
			suspend fun startPositionCollector() = mutex.withLock {
				if (positionCollectorJob?.isActive != true) {
					positionCollectorJob = launch { collectPosition() }
				}
			}

			suspend fun restartPositionCollector() {
				stopPositionCollector()
				send(MediaConnection.Playback.PositionStream())
				startPositionCollector()
			}

			sendUpdatedPositions()

			val transitionObserverJob = ioScope.launch {
				delegate.playback.observeMediaItem().safeCollect { restartPositionCollector() }
			}

			val timelineObserverJob = ioScope.launch {
				delegate.playback.observeTimeline().safeCollect { sendUpdatedPositions() }
			}

			val discontinuityJob = ioScope.launch {
				delegate.playback.observeDiscontinuity().safeCollect { sendUpdatedPositions() }
			}

			val isPlayingJob = ioScope.launch {
				delegate.playback.observeIsPlaying().safeCollect {
					if (!it) stopPositionCollector() else startPositionCollector()
				}
			}

			awaitClose {
				transitionObserverJob.cancel()
				discontinuityJob.cancel()
				timelineObserverJob.cancel()
				positionCollectorJob?.cancel()
				isPlayingJob.cancel()
			}
		}

		override fun observePlaylistStream(): Flow<MediaConnection.Playback.PlaylistStream> = callbackFlow {

			suspend fun sendNew(reason: Int) {
				val new = delegate.playback.joinDispatcher {
					MediaConnection.Playback.PlaylistStream(
						reason = reason,
						currentIndex = delegate.playback.index,
						list = delegate.playback.getPlaylist().map { it.mediaId }.toPersistentList()
					)
				}
				send(new)
				Timber.d("observePlaylistStream sent $new")
			}

			val playlistObserver = ioScope.launch {
				delegate.playback.observePlaylist().safeCollect {
					if (it.isEmpty()) {
						return@safeCollect send(MediaConnection.Playback.PlaylistStream())
					}
					sendNew(1)
				}
			}

			val currentObserver = ioScope.launch {
				delegate.playback.observeMediaItem().safeCollect {
					if (it == null || it is MediaItem.UNSET) {
						return@safeCollect send(MediaConnection.Playback.PlaylistStream())
					}
					sendNew(0)
				}
			}
			awaitClose {
				currentObserver.cancel()
				playlistObserver.cancel()
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
