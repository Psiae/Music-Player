package com.flammky.musicplayer.domain.media

import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.player.Player
import com.flammky.common.kotlin.coroutines.safeCollect
import com.flammky.musicplayer.base.media.mediaconnection.MediaConnectionDelegate
import com.flammky.musicplayer.base.media.mediaconnection.MediaConnectionPlayback
import com.flammky.musicplayer.media.R
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.media.playback.*
import com.google.common.util.concurrent.ListenableFuture
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
) : MediaConnection, /* Temp */ PlaybackConnection, PlaybackConnection.Controller {

	private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

	override val playback: MediaConnection.Playback = Playback()
	override val repository: MediaConnection.Repository = Repository()

	//
	// Temp Start
	//

	override suspend fun <R> withControllerContext(block: suspend PlaybackConnection.Controller.() -> R): R {
		return delegate.playback.joinDispatcher { block() }
	}

	override suspend fun <R> withControllerImmediateContext(block: suspend PlaybackConnection.Controller.() -> R): R {
		return delegate.playback.joinDispatcher { block() }
	}

	override fun postController(block: suspend PlaybackConnection.Controller.() -> Unit) {
		TODO("Not yet implemented")
	}

	override fun immediatePostController(block: suspend PlaybackConnection.Controller.() -> Unit) {
		TODO("Not yet implemented")
	}

	override fun <R> postControllerCallback(block: suspend PlaybackConnection.Controller.() -> R): ListenableFuture<R> {
		TODO("Not yet implemented")
	}

	override fun <R> immediatePostControllerCallback(block: suspend PlaybackConnection.Controller.() -> R): ListenableFuture<R> {
		TODO("Not yet implemented")
	}

	override suspend fun observeQueueChange(): Flow<PlaybackEvent.QueueChange> {
		TODO("Not yet implemented")
	}

	override suspend fun setRepeatMode(mode: RepeatMode): Boolean {
		TODO("Not yet implemented")
	}

	override suspend fun setShuffleMode(mode: ShuffleMode): Boolean {
		TODO("Not yet implemented")
	}

	override suspend fun observeQueue(): Flow<PlaybackQueue> {
		TODO("Not yet implemented")
	}

	override suspend fun seekProgress(progress: Duration): Boolean {
		return withControllerContext { playback.seekToPosition(progress.inWholeMilliseconds)}
	}

	override suspend fun seekIndex(index: Int, startPosition: Duration): Boolean {
		return withControllerContext { playback.seekIndex(index, startPosition.inWholeMilliseconds) }
	}

	override suspend fun getQueue(): PlaybackQueue {
		TODO("Not yet implemented")
	}

	override suspend fun setQueue(queue: PlaybackQueue): Boolean {
		TODO("Not yet implemented")
	}

	override suspend fun getRepeatMode(): RepeatMode = delegate.playback.joinDispatcher {
		when (delegate.playback.repeatMode) {
			Player.RepeatMode.OFF -> RepeatMode.OFF
			Player.RepeatMode.ONE -> RepeatMode.ONE
			Player.RepeatMode.ALL -> RepeatMode.ALL
		}
	}

	override suspend fun observeRepeatMode(): Flow<RepeatMode> {
		return callbackFlow {

			delegate.playback.joinDispatcher {
				send(getRepeatMode())
				observeRepeatModeChange().collect {
					send(getRepeatMode())
				}
			}

			awaitClose()
		}
	}

	override suspend fun getShuffleMode(): ShuffleMode = delegate.playback.joinDispatcher {
		if (delegate.playback.shuffleEnabled) {
			ShuffleMode.ON
		} else {
			ShuffleMode.OFF
		}
	}

	override suspend fun observeShuffleMode(): Flow<ShuffleMode> {
		return callbackFlow {

			delegate.playback.joinDispatcher {
				send(getShuffleMode())
				observeShuffleEnabledChange().collect {
					send(getShuffleMode())
				}
			}

			awaitClose()
		}
	}



	override suspend fun getProgress(): Duration = delegate.playback.joinDispatcher {
		position
	}

	override suspend fun observeProgressDiscontinuity(): Flow<PlaybackEvent.ProgressDiscontinuity> {
		return callbackFlow {

			delegate.playback.joinDispatcher {

				delegate.playback.observeDiscontinuityEvent().collect {
					send(PlaybackEvent.ProgressDiscontinuity(
						oldProgress = it.oldPosition,
						newProgress = it.newPosition,
						reason = when (it.reason) {
							MediaConnectionPlayback.Events.Discontinuity.Reason.UNKNOWN -> ProgressDiscontinuityReason.UNKNOWN
							MediaConnectionPlayback.Events.Discontinuity.Reason.USER_SEEK -> ProgressDiscontinuityReason.USER_SEEK
						}
					))
				}
			}

			awaitClose()
		}
	}

	override suspend fun getBufferedProgress(): Duration = delegate.playback.joinDispatcher {
		bufferedPosition()
	}

	override suspend fun getIsPlaying(): Boolean = delegate.playback.joinDispatcher {
		playing
	}

	override suspend fun observeIsPlaying(): Flow<Boolean> {
		return callbackFlow {

			delegate.playback.joinDispatcher {
				send(playing)
				observeIsPlayingChange().collect {
					send(playing)
				}
			}

			awaitClose()
		}
	}

	override suspend fun getDuration(): Duration = delegate.playback.joinDispatcher {
		duration()
	}

	override suspend fun observeDuration(): Flow<Duration> {
		return callbackFlow {

			delegate.playback.joinDispatcher {
				send(duration())
				observeDurationChange().collect {
					send(duration())
				}
			}

			awaitClose()
		}
	}

	override suspend fun getPlaybackSpeed(): Float = delegate.playback.joinDispatcher {
		playbackSpeed()
	}

	override suspend fun observePlaybackSpeed(): Flow<Float> {
		TODO("Not yet implemented")
	}

	//
	// Temp end
	//

	private inner class Playback : MediaConnection.Playback {

		override val currentIndex: Int
			get() = delegate.playback.index

		override val currentPosition: Duration
			get() = delegate.playback.position

		override val currentDuration: Duration
			get() = delegate.playback.duration()

		override val mediaItemCount: Int
			get() = delegate.playback.mediaItemCount

		override val hasNextMediaItem: Boolean
			get() = delegate.playback.hasNextMediaItem

		override val hasPreviousMediaItem: Boolean
			get() = delegate.playback.hasPreviousMediaItem

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

		override fun seekNextMedia() {
			delegate.playback.seekNextMedia()
		}

		override fun seekPrevious() {
			delegate.playback.seekPrevious()
		}

		override fun seekPreviousMedia() {
			delegate.playback.seekPreviousMedia()
		}

		override fun stop() {
			delegate.playback.stop()
		}

		override suspend fun <R> joinSuspend(block: suspend MediaConnection.Playback.() -> R): R {
			return delegate.playback.joinDispatcher { block() }
		}

		override fun prepare() = delegate.playback.prepare()
		override fun playWhenReady() = delegate.play()
		override fun pause() = delegate.pause()

		override fun seekIndex(index: Int, startPosition: Long): Boolean {
			return delegate.playback.index != index && delegate.playback.seekToIndex(index, startPosition)
		}

		override fun postSeekPosition(position: Long) {
			delegate.playback.postSeekToPosition(position)
		}

		override fun seekToPosition(position: Long): Boolean {
			return delegate.playback.seekToPosition(position)
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

		override fun observePositionStream(interval: Duration): Flow<MediaConnection.Playback.PositionStream> = callbackFlow {

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
						delay(interval.inWholeMilliseconds)
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

			val transitionObserverJob = launch {
				delegate.playback.observeMediaItemTransition().safeCollect {
					restartPositionCollector(MediaConnection.Playback.PositionStream.PositionChangeReason.AUTO)
				}
			}

			val timelineObserverJob = launch {
				delegate.playback.observeTimelineChange().safeCollect {
					sendUpdatedPositions(MediaConnection.Playback.PositionStream.PositionChangeReason.AUTO)
				}
			}

			val discontinuityJob = launch {
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

			val isPlayingJob = launch {
				delegate.playback.observeIsPlayingChange().safeCollect {
					if (!it) {
						stopPositionCollector()
						sendUpdatedPositions(MediaConnection.Playback.PositionStream.PositionChangeReason.UNKN0WN)
					} else {
						startPositionCollector(MediaConnection.Playback.PositionStream.PositionChangeReason.UNKN0WN)
					}
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
						Timber.d("onPlaylistChange safeCollect $it")
						return@safeCollect send(MediaConnection.Playback.TracksInfo())
					}
					sendNew(1, currentList = it.toPersistentList())
				}
			}

			val currentObserver = ioScope.launch {
				delegate.playback.observeMediaItemTransition().safeCollect {
					if (it == null || it is MediaItem.UNSET) {
						Timber.d("onMediaItemTransition safeCollect $it")
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
		override suspend fun getMetadata(id: String): MediaMetadata? = delegate.repository.getMetadata(id)
		override suspend fun getArtwork(id: String): Any? = delegate.repository.getArtwork(id)
		override suspend fun observeMetadata(id: String): Flow<MediaMetadata?> = delegate.repository.observeMetadata(id)
		override suspend fun observeArtwork(id: String): Flow<Any?> = delegate.repository.observeArtwork(id)
		override suspend fun provideMetadata(id: String, metadata: MediaMetadata) = delegate.repository.provideMetadata(id, metadata)
		override suspend fun provideArtwork(id: String, artwork: Any?) = delegate.repository.provideArtwork(id, artwork)
	}
}
