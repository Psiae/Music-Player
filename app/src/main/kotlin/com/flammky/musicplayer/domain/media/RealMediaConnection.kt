package com.flammky.musicplayer.domain.media

import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.player.Player
import com.flammky.common.kotlin.coroutines.safeCollect
import com.flammky.musicplayer.base.media.mediaconnection.MediaConnectionDelegate
import com.flammky.musicplayer.base.media.mediaconnection.MediaConnectionPlayback
import kotlinx.collections.immutable.ImmutableList
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

		override val currentIndex: Int
			get() = delegate.playback.index

		override val mediaItemCount: Int
			get() = delegate.playback.mediaItemCount

		override fun setMediaItems(items: List<MediaItem>, startIndex: Int, startPosition: Duration) {
			delegate.playback.setMediaItems(items, startIndex, startPosition)
		}

		override fun setRepeatMode(repeatMode: Player.RepeatMode) {
			delegate.playback.setRepeatMode(repeatMode)
		}

		override fun setShuffleMode(enabled: Boolean) {
			delegate.playback.setShuffleMode(enabled)
		}

		override fun seekNext() {
			delegate.playback.seekNext()
		}

		override fun seekPrevious() {
			delegate.playback.seekPrevious()
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

		override fun seekIndex(index: Int, startPosition: Long) {
			if (delegate.playback.index != index) {
				delegate.playback.seekToIndex(index, startPosition)
			}
		}

		override fun seekPosition(position: Long) {
			delegate.playback.seekToPosition(position)
		}

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

				sendNew()

				val job1 = ioScope.launch {
					delegate.playback.observeMediaItemTransition().safeCollect { sendNew() }
				}

				val job2 = ioScope.launch {
					delegate.playback.observePlayWhenReadyChange().safeCollect { sendNew() }
				}

				val job3 = ioScope.launch {
					delegate.playback.observeIsPlayingChange().safeCollect { sendNew() }
				}

				awaitClose {
					job1.cancel();job2.cancel();job3.cancel()
				}
			}
		}

		override fun observePositionStream(): Flow<MediaConnection.Playback.PositionStream> = callbackFlow {

			var positionCollectorJob: Job? = null
			val mutex = Mutex()

			suspend fun sendUpdatedPositions(
				reason: MediaConnection.Playback.PositionStream.PositionChangeReason
			) {
				val new = delegate.playback.joinDispatcher {
					MediaConnection.Playback.PositionStream(
						positionChangeReason = reason,
						position = delegate.playback.position(),
						bufferedPosition = delegate.playback.bufferedPosition(),
						duration = delegate.playback.duration()
					)
				}
				send(new)
			}

			suspend fun collectPosition(
				startReason: MediaConnection.Playback.PositionStream.PositionChangeReason
			) {
				if (isActive) {
					sendUpdatedPositions(startReason)
					while (isActive) {
						sendUpdatedPositions(
							reason = MediaConnection.Playback.PositionStream.PositionChangeReason.PERIODIC
						)
						delay(500)
					}
				}
			}

			fun stopPositionCollector() = positionCollectorJob?.cancel()

			suspend fun startPositionCollector(
				reason: MediaConnection.Playback.PositionStream.PositionChangeReason
			) = mutex.withLock {
				if (positionCollectorJob?.isActive != true) {
					positionCollectorJob = launch { collectPosition(reason) }
				}
			}

			suspend fun restartPositionCollector(
				reason: MediaConnection.Playback.PositionStream.PositionChangeReason
			) {
				stopPositionCollector()
				send(MediaConnection.Playback.PositionStream(reason))
				startPositionCollector(MediaConnection.Playback.PositionStream.PositionChangeReason.PERIODIC)
			}

			sendUpdatedPositions(MediaConnection.Playback.PositionStream.PositionChangeReason.PERIODIC)

			val transitionObserverJob = ioScope.launch {
				delegate.playback.observeMediaItemTransition().safeCollect {
					restartPositionCollector(MediaConnection.Playback.PositionStream.PositionChangeReason.AUTO)
				}
			}

			val timelineObserverJob = ioScope.launch {
				delegate.playback.observeTimelineChange().safeCollect {
					sendUpdatedPositions(MediaConnection.Playback.PositionStream.PositionChangeReason.AUTO)
				}
			}

			val discontinuityJob = ioScope.launch {
				delegate.playback.observeDiscontinuityEvent().safeCollect {
					val updateReason =
						if (it.reason == MediaConnectionPlayback.Events.Discontinuity.Reason.USER_SEEK) {
							MediaConnection.Playback.PositionStream.PositionChangeReason.USER_SEEK
						} else {
							MediaConnection.Playback.PositionStream.PositionChangeReason.UNKN0WN
						}
					sendUpdatedPositions(updateReason)
				}
			}

			val isPlayingJob = ioScope.launch {
				delegate.playback.observeIsPlayingChange().safeCollect {
					if (!it) stopPositionCollector() else startPositionCollector(
						MediaConnection.Playback.PositionStream.PositionChangeReason.UNKN0WN
					)
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

		override fun observePlaylistStream(): Flow<MediaConnection.Playback.TracksInfo> = callbackFlow {

			suspend fun sendNew(
				reason: Int,
				currentIndex: Int? = null,
				currentList: ImmutableList<String>? = null
			) {
				val new = delegate.playback.joinDispatcher {
					MediaConnection.Playback.TracksInfo(
						changeReason = reason,
						currentIndex = currentIndex ?: this.index,
						list = currentList ?: this.getPlaylist().map { it.mediaId }.toPersistentList()
					)
				}
				send(new)
				Timber.d("observePlaylistStream sent $new")
			}

			sendNew(0)

			val playlistObserver = ioScope.launch {
				delegate.playback.observePlaylistChange().safeCollect {
					if (it.isEmpty()) {
						return@safeCollect send(MediaConnection.Playback.TracksInfo())
					}
					sendNew(1, currentList = it.toPersistentList())
				}
			}

			val currentObserver = ioScope.launch {
				delegate.playback.observeMediaItemTransition().safeCollect {
					if (it == null || it is MediaItem.UNSET) {
						return@safeCollect send(MediaConnection.Playback.TracksInfo())
					}
					sendNew(0)
				}
			}
			awaitClose {
				currentObserver.cancel()
				playlistObserver.cancel()
			}
		}

		override fun observePropertiesInfo(): Flow<MediaConnection.Playback.PropertiesInfo> = callbackFlow {

			suspend fun sendCurrent() {
				val new = delegate.playback.joinDispatcher {
					MediaConnection.Playback.PropertiesInfo(
						playWhenReady = playWhenReady,
						playing = playing,
						shuffleEnabled = shuffleEnabled,
						hasNextMediaItem = hasNextMediaItem,
						hasPreviousMediaItem = hasPreviousMediaItem,
						repeatMode = repeatMode,
						playerState = playerState
					)
				}
				send(new)
			}

			sendCurrent()

			val job1 = ioScope.launch { delegate.playback.observePlayWhenReadyChange().safeCollect { sendCurrent() } }
			val job2 = ioScope.launch { delegate.playback.observeIsPlayingChange().safeCollect { sendCurrent() } }
			val job3 = ioScope.launch { delegate.playback.observeShuffleEnabledChange().safeCollect { sendCurrent() } }
			val job4 = ioScope.launch { delegate.playback.observeMediaItemTransition().safeCollect { sendCurrent() } }
			val job5 = ioScope.launch { delegate.playback.observePlaylistChange().safeCollect { sendCurrent() } }
			val job6 = ioScope.launch { delegate.playback.observeRepeatModeChange().safeCollect { sendCurrent() } }
			val job7 = ioScope.launch { delegate.playback.observePlayerStateChange().safeCollect { sendCurrent() } }

			awaitClose {
				job1.cancel();job2.cancel();job3.cancel();job4.cancel();job5.cancel();job6.cancel();job7.cancel()
			}
		}
	}



	private inner class Repository : MediaConnection.Repository {
		// We should localize them
		override suspend fun observeMetadata(id: String): Flow<MediaMetadata?> = delegate.repository.observeMetadata(id)
		override suspend fun observeArtwork(id: String): Flow<Any?> = delegate.repository.observeArtwork(id)
		override suspend fun provideMetadata(id: String, metadata: MediaMetadata) = delegate.repository.provideMetadata(id, metadata)
		override suspend fun provideArtwork(id: String, artwork: Any?) = delegate.repository.provideArtwork(id, artwork)
	}
}
