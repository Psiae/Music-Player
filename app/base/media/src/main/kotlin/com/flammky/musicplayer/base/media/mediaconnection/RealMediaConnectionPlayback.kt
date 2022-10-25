package com.flammky.musicplayer.base.media.mediaconnection

import android.net.Uri
import android.os.Handler
import com.flammky.android.medialib.common.Contract
import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.temp.MediaLibrary
import com.flammky.android.medialib.temp.media3.Timeline
import com.flammky.android.medialib.temp.player.event.IsPlayingChangedReason
import com.flammky.android.medialib.temp.player.event.LibraryPlayerEventListener
import com.flammky.android.medialib.temp.player.event.PlayWhenReadyChangedReason
import com.flammky.common.kotlin.coroutines.safeCollect
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class RealMediaConnectionPlayback : MediaConnectionPlayback {

	private val mediaItemTransitionListeners = mutableListOf<(MediaItem) -> Unit>()
	private val playlistChangeListeners = mutableListOf<() -> Unit>()

	// TEMP
	private val s = MediaLibrary.API.sessions.manager.findSessionById("DEBUG")!!

	private val playerDispatcher = Handler(s.mediaController.publicLooper).asCoroutineDispatcher()

	private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
	private val playerScope = CoroutineScope(SupervisorJob() + playerDispatcher)

	override var playWhenReady: Boolean
		get() = s.mediaController.playWhenReady
		set(value) {
			if (value) s.mediaController.play() else s.mediaController.pause()
		}

	override val playing: Boolean
		get() = s.mediaController.isPlaying

	override val index: Int
		get() = s.mediaController.currentMediaItemIndex

	override val shuffleEnabled: Boolean
		get() = s.mediaController.shuffleEnabled

	override val hasNextMediaItem: Boolean
		get() = s.mediaController.hasNextMediaItem

	override val hasPreviousMediaItem: Boolean
		get() = s.mediaController.hasPreviousMediaItem

	override fun getPlaylist(): List<MediaItem> {
		return s.mediaController.getAllMediaItem()
	}

	override fun stop() {
		s.mediaController.stop()
	}

	override fun setMediaItems(items: List<MediaItem>, startIndex: Int, startPosition: Duration) {
		s.mediaController.setMediaItems(items, startIndex, startPosition)
	}

	override fun play(mediaItem: MediaItem) {
		s.mediaController.play(mediaItem)
	}

	override fun pause() {
		s.mediaController.pause()
	}

	override fun prepare() {
		s.mediaController.prepare()
	}

	override fun seekToIndex(int: Int, startPosition: Long) {
		s.mediaController.seekToMediaItem(int, startPosition)
	}

	override fun seekToPosition(position: Long) {
		s.mediaController.seekToPosition(position)
	}

	override fun bufferedPosition(): Duration {
		return s.mediaController.bufferedPositionMs.milliseconds
	}

	override fun position(): Duration {
		return s.mediaController.positionMs.milliseconds
	}

	override fun duration(): Duration {
		return s.mediaController.durationMs.milliseconds
	}

	override fun getCurrentMediaItem(): MediaItem? {
		return s.mediaController.currentActualMediaItem
	}

	override fun observeMediaItemTransition(): Flow<MediaItem?> {
		return callbackFlow {
			val listener = object : LibraryPlayerEventListener {
				override fun onLibMediaItemTransition(
					oldLib: MediaItem?,
					newLib: MediaItem?
				) {
					Timber.d("onMediaItemTransition,\nold: $oldLib,\nnew: $newLib,\nduration: ${duration()}")
					playerScope.launch(playerDispatcher.immediate) {
						send(newLib)
					}
				}
			}
			s.mediaController.addListener(listener)
			awaitClose {
				s.mediaController.removeListener(listener)
			}
		}
	}

	override fun observePlaylistChange(): Flow<List<String>> {
		return callbackFlow {
			val timelineObserver = object : LibraryPlayerEventListener {
				override fun onTimelineChanged(old: Timeline, new: Timeline, reason: Int) {
					if (reason == 0) playerScope.launch(playerDispatcher.immediate) {
						send(s.mediaController.getAllMediaItem().map { it.mediaId })
					}
				}
			}
			s.mediaController.addListener(timelineObserver)
			awaitClose {
				s.mediaController.removeListener(timelineObserver)
			}
		}
	}

	override fun observePositionChange(): Flow<Duration> {
		return callbackFlow {
			var collectorJob: Job = Job()
			fun startCollectorJob() {
				collectorJob.cancel()
				collectorJob = playerScope.launch {
					while (collectorJob.isActive) {
						send(s.mediaController.positionMs.milliseconds)
						delay(500)
					}
				}
			}
			observeMediaItemTransition().safeCollect {
				if (it == null) {
					send(Contract.POSITION_UNSET)
					collectorJob.cancel()
				} else {
					startCollectorJob()
				}
			}
			awaitClose {
				collectorJob.cancel()
			}
		}
	}

	override fun observeDurationChange(): Flow<Duration> {
		return callbackFlow {
			val job1 = coroutineScope.launch {
				observeMediaItemTransition().safeCollect {
					// our first `UNSET` duration callback is given by MediaItemTransition
					send(Contract.POSITION_UNSET)
				}
			}
			val job2 = coroutineScope.launch {
				observeTimelineChange().safeCollect {
					// our first `SET` duration callback is given by TimeLineChanged
					send(it.duration)
				}
			}
			awaitClose {
				job1.cancel()
				job2.cancel()
			}
		}
	}

	override fun observeIsPlayingChange(): Flow<Boolean> {
		return callbackFlow {
			val listener = object : LibraryPlayerEventListener {
				override fun onIsPlayingChanged(isPlaying: Boolean, reason: IsPlayingChangedReason) {
					playerScope.launch(playerDispatcher.immediate) {
						send(isPlaying)
					}
				}
			}
			s.mediaController.addListener(listener)
			awaitClose {
				s.mediaController.removeListener(listener)
			}
		}
	}

	override fun observePlayWhenReadyChange(): Flow<Boolean> {
		return callbackFlow {
			val listener = object : LibraryPlayerEventListener {
				override fun onPlayWhenReadyChanged(
					playWhenReady: Boolean,
					reason: PlayWhenReadyChangedReason
				) {
					playerScope.launch(playerDispatcher.immediate) {
						send(playWhenReady)
					}
				}
			}
			s.mediaController.addListener(listener)
			awaitClose {
				s.mediaController.removeListener(listener)
			}
		}
	}

	override fun observeTimelineChange(): Flow<Timeline> {
		return callbackFlow {
			val timelineObserver = object : LibraryPlayerEventListener {
				override fun onTimelineChanged(old: Timeline, new: Timeline, reason: Int) {
					Timber.d("MediaConnectionPlayback onTimelineChanged,\n" +
						"old: $old,\nnew: $new,\nreason: $reason,\nduration: ${s.mediaController.durationMs}")
					playerScope.launch(playerDispatcher.immediate) {
						send(new)
					}
				}
			}
			s.mediaController.addListener(timelineObserver)
			awaitClose {
				s.mediaController.removeListener(timelineObserver)
			}
		}
	}

	override fun observeDiscontinuityEvent(): Flow<MediaConnectionPlayback.Events.Discontinuity> {
		return callbackFlow {
			val discontinuityObserver = object : LibraryPlayerEventListener {
				override fun libOnPositionDiscontinuity(
					oldPosition: Long,
					newPosition: Long,
					reason: Int
				) {
					Timber.d("MediaConnectionPlayback onPositionDiscontinuity,\n" +
						"oldPos: $oldPosition,\nnewPos: $newPosition,\nreason: $reason,\nduration: ${s.mediaController.durationMs}"
					)
					val localReason = when(reason) {
						1 -> MediaConnectionPlayback.Events.Discontinuity.Reason.USER_SEEK
						else -> MediaConnectionPlayback.Events.Discontinuity.Reason.UNKNOWN
					}
					val new = MediaConnectionPlayback.Events.Discontinuity(
						oldPosition = oldPosition.milliseconds,
						newPosition = newPosition.milliseconds,
						reason = localReason
					)
					playerScope.launch(playerDispatcher.immediate) {
						send(new)
					}
				}
			}
			s.mediaController.addListener(discontinuityObserver)
			awaitClose {
				s.mediaController.removeListener(discontinuityObserver)
			}
		}
	}

	override fun observeShuffleEnabledChange(): Flow<Boolean> {
		return callbackFlow {
			val shuffleListener = object : LibraryPlayerEventListener {
				override fun onShuffleModeEnabledChanged(enabled: Boolean) {
					trySend(enabled)
				}
			}
			s.mediaController.addListener(shuffleListener)
			awaitClose {
				s.mediaController.removeListener(shuffleListener)
			}
		}
	}

	override fun notifyUnplayableMedia(id: String) {
		playerScope.launch(playerDispatcher.immediate) {
			var removed = 0
			s.mediaController.getAllMediaItem().forEachIndexed { index, item ->
				if (item.mediaId == id) s.mediaController.removeMediaItem(index - removed++)
			}
		}
	}

	override fun notifyUnplayableMedia(uri: Uri) {
		playerScope.launch(playerDispatcher.immediate) {
			var removed = 0
			s.mediaController.getAllMediaItem().forEachIndexed { index, item ->
				if (item.mediaUri == uri) s.mediaController.removeMediaItem(index - removed++)
			}
		}
	}

	override suspend fun <R> joinDispatcher(block: suspend MediaConnectionPlayback.() -> R): R {
		return withContext(playerDispatcher.immediate) { block() }
	}
}
