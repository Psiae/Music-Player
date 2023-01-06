package com.flammky.musicplayer.domain.media

import android.os.Handler
import android.os.Looper
import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.player.Player
import com.flammky.common.kotlin.coroutines.safeCollect
import com.flammky.musicplayer.base.media.playback.*
import com.flammky.musicplayer.base.media.r.MediaConnectionDelegate
import com.flammky.musicplayer.base.media.r.MediaConnectionPlayback
import com.flammky.musicplayer.core.common.sync
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import kotlin.time.Duration

class RealMediaConnection(
	private val delegate: MediaConnectionDelegate
) : MediaConnection, /* Temp */ PlaybackController {

	private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

	override val playback: MediaConnection.Playback = Playback()
	override val repository: MediaConnection.Repository = Repository()

	//
	// Temp Start
	//

	override val looper: Looper
		get() = delegate.playback.publicLooper

	override suspend fun isPlayWhenReady(): Boolean {
		return delegate.playback.playWhenReady
	}

	override suspend fun isPlaying(): Boolean {
		return delegate.playback.playing
	}

	override suspend fun getPlaybackSpeed(): Float = delegate.playback.playbackSpeed()

	override suspend fun getDuration(): Duration = delegate.playback.duration()

	override suspend fun getBufferedPosition(): Duration = delegate.playback.bufferedPosition()

	override suspend fun getPosition(): Duration = delegate.playback.position()

	override suspend fun getShuffleMode(): ShuffleMode = when (delegate.playback.shuffleEnabled) {
		true -> ShuffleMode.ON
		else -> ShuffleMode.OFF
	}

	override suspend fun getRepeatMode(): RepeatMode = when (delegate.playback.repeatMode) {
		Player.RepeatMode.OFF -> RepeatMode.OFF
		Player.RepeatMode.ONE -> RepeatMode.ONE
		Player.RepeatMode.ALL -> RepeatMode.ALL
	}

	override suspend fun getQueue(): PlaybackQueue {
		val (l: List<String>, i: Int) = withLooperContext {
			delegate.playback.getPlaylist().map { it.mediaId } to delegate.playback.index
		}
		return PlaybackQueue(l.toPersistentList(), i)
	}

	override suspend fun getPlaybackProperties(): PlaybackProperties {
		val player = delegate.playback
		val repeatMode = when (player.repeatMode) {
			Player.RepeatMode.OFF -> RepeatMode.OFF
			Player.RepeatMode.ONE -> RepeatMode.ONE
			Player.RepeatMode.ALL -> RepeatMode.ALL
		}
		return PlaybackProperties(
			playbackState = when (player.playerState) {
				is Player.State.READY -> PlaybackProperties.PlaybackState.READY
				is Player.State.BUFFERING -> PlaybackProperties.PlaybackState.BUFFERING
				is Player.State.ENDED -> PlaybackProperties.PlaybackState.ENDED
				is Player.State.ERROR, is Player.State.IDLE -> PlaybackProperties.PlaybackState.IDLE
			},
			playWhenReady = player.playWhenReady,
			canPlayWhenReady = !player.playWhenReady,
			canPlay = player.mediaItemCount > 0,
			playing = player.playing,
			loading = player.loading,
			speed = player.playbackSpeed(),
			hasNextMediaItem = player.hasNextMediaItem,
			canSeekNext = player.hasNextMediaItem,
			hasPreviousMediaItem = player.hasPreviousMediaItem,
			canSeekPrevious = true /* position > 0 */,
			shuffleMode = if (player.shuffleEnabled) { ShuffleMode.ON } else ShuffleMode.OFF,
			canShuffleOff = false,
			canShuffleOn = false,
			repeatMode = repeatMode,
			canRepeatOne = true,
			canRepeatAll = true,
			canRepeatOff = true,
			playbackSuppression = persistentListOf()
		)
	}

	override suspend fun setQueue(queue: PlaybackQueue): Boolean {
		TODO("Not yet implemented")
	}

	override suspend fun setRepeatMode(mode: RepeatMode): Boolean {
		delegate.playback.setRepeatMode(
			when (mode) {
				RepeatMode.ALL -> Player.RepeatMode.ALL
				RepeatMode.OFF -> Player.RepeatMode.OFF
				RepeatMode.ONE -> Player.RepeatMode.ONE
			}
		)
		return true
	}

	override suspend fun setShuffleMode(mode: ShuffleMode): Boolean {
		delegate.playback.setShuffleMode(
			when (mode) {
				ShuffleMode.OFF -> false
				ShuffleMode.ON -> true
			}
		)
		return true
	}

	override suspend fun seekPosition(progress: Duration): Boolean {
		return delegate.playback.seekToPosition(progress.inWholeMilliseconds)
	}

	override suspend fun seekIndex(index: Int, startPosition: Duration): Boolean {
		return delegate.playback.index != index && delegate.playback.seekToIndex(index, startPosition.inWholeMilliseconds)
	}

	override suspend fun play(): Boolean {
		delegate.playback.play()
		return true
	}

	override suspend fun seekNext(): Boolean {
		return delegate.playback.seekToIndex(delegate.playback.index + 1, 0)
	}

	override suspend fun seekPrevious(): Boolean {
		delegate.playback.seekPrevious()
		return true
	}

	override suspend fun seekPreviousMediaItem(): Boolean {
		return delegate.playback.seekToIndex(delegate.playback.index - 1, 0)
	}

	override suspend fun setPlayWhenReady(playWhenReady: Boolean): Boolean {
		delegate.playback.playWhenReady = playWhenReady
		return true
	}

	private inner class OBS() : PlaybackController.Observer {

		private val dispatcher = Handler(delegate.playback.publicLooper).asCoroutineDispatcher()
		private val scope = CoroutineScope(dispatcher + SupervisorJob())

		override fun observeRepeatMode(): Flow<RepeatMode> {
			TODO("Not yet implemented")
		}

		override fun observeShuffleMode(): Flow<ShuffleMode> {
			TODO("Not yet implemented")
		}

		override fun observeQueue(): Flow<PlaybackQueue> {
			TODO("Not yet implemented")
		}

		override fun observeIsPlaying(): Flow<Boolean> {
			TODO("Not yet implemented")
		}

		override fun observePlaybackSpeed(): Flow<Float> {
			TODO("Not yet implemented")
		}

		override fun observePositionDiscontinuityEvent(): Flow<PlaybackEvent.PositionDiscontinuity> {
			TODO("Not yet implemented")
		}

		override fun observeDuration(): Flow<Duration> {
			TODO("Not yet implemented")
		}

		override fun observePlaybackProperties(): Flow<PlaybackProperties> {
			TODO("Not yet implemented")
		}

		fun release() {
			scope.cancel()
		}
	}

	private val owners = mutableMapOf<Any, OBS>()

	override fun acquireObserver(owner: Any): PlaybackController.Observer {
		return owners.sync { getOrPut(owner) { OBS() } }
	}

	override fun releaseObserver(owner: Any) {
		return owners.sync { remove(owner)?.release() }
	}

	override fun hasObserver(owner: Any): Boolean {
		return owners.sync { contains(owner) }
	}

	override fun inLooper(): Boolean {
		return Looper.myLooper() == delegate.playback.publicLooper
	}

	override suspend fun <R> withLooperContext(block: suspend PlaybackController.() -> R): R {
		return delegate.playback.joinDispatcher { block() }
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

				val job1 = launch {
					delegate.playback.observeMediaItemTransition().safeCollect { sendNew() }
				}

				val job2 = launch {
					delegate.playback.observePlayWhenReadyChange().safeCollect { sendNew() }
				}

				val job3 = launch {
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

			if (delegate.playback.playing) {
				startPositionCollector(MediaConnection.Playback.PositionStream.PositionChangeReason.PERIODIC)
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

			val playlistObserver = launch {
				delegate.playback.observePlaylistChange().safeCollect {
					if (it.isEmpty()) {
						Timber.d("onPlaylistChange safeCollect $it")
						return@safeCollect send(MediaConnection.Playback.TracksInfo())
					}
					sendNew(1, currentList = it.toPersistentList())
				}
			}

			val currentObserver = launch {
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

			val job1 = launch { delegate.playback.observePlayWhenReadyChange().safeCollect { sendCurrent() } }
			val job2 = launch { delegate.playback.observeIsPlayingChange().safeCollect { sendCurrent() } }
			val job3 = launch { delegate.playback.observeShuffleEnabledChange().safeCollect { sendCurrent() } }
			val job4 = launch { delegate.playback.observeMediaItemTransition().safeCollect { sendCurrent() } }
			val job5 = launch { delegate.playback.observeTimelineChange().safeCollect { sendCurrent() } }
			val job6 = launch { delegate.playback.observeRepeatModeChange().safeCollect { sendCurrent() } }
			val job7 = launch { delegate.playback.observePlayerStateChange().safeCollect { sendCurrent() } }

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
