package com.flammky.musicplayer.base.media

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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class RealMediaConnectionPlayback : MediaConnectionPlayback {

	// TEMP
	private val s = MediaLibrary.API.sessions.manager.findSessionById("DEBUG")!!

	private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
	private val playerScope = CoroutineScope(SupervisorJob() + Handler(s.mediaController.publicLooper).asCoroutineDispatcher())

	override var playWhenReady: Boolean
		get() = s.mediaController.playWhenReady
		set(value) {
			if (value) s.mediaController.play() else s.mediaController.pause()
		}

	override val playing: Boolean
		get() = s.mediaController.isPlaying

	override val index: Int
		get() = s.mediaController.currentMediaItemIndex

	override fun getPlaylist(): List<MediaItem> {
		return s.mediaController.getAllMediaItem()
	}

	override fun play(mediaItem: MediaItem) {
		s.mediaController.play(mediaItem)
	}

	override fun pause() {
		s.mediaController.pause()
	}

	override fun bufferedPosition(): Duration {
		return s.mediaController.bufferedPositionMs.milliseconds
	}

	override fun position(): Duration {
		return s.mediaController.positionMs.milliseconds
	}

	override fun observeMediaItem(): Flow<MediaItem?> {
		return callbackFlow {
			val listener = object : LibraryPlayerEventListener {
				override fun onLibMediaItemTransition(
					oldLib: MediaItem?,
					newLib: MediaItem?
				) {
					trySend(newLib)
				}
			}
			playerScope.launch { send(s.mediaController.currentActualMediaItem) }
			s.mediaController.addListener(listener)
			awaitClose {
				s.mediaController.removeListener(listener)
			}
		}
	}

	override fun observePlaylist(): Flow<List<String>> {
		return callbackFlow {
			val timelineObserver = object : LibraryPlayerEventListener {
				override fun onTimelineChanged(old: Timeline, new: Timeline, reason: Int) {
					if (reason == 0) trySend(s.mediaController.getAllMediaItem().map { it.mediaId })
				}
			}

			playerScope.launch {
				send(s.mediaController.getAllMediaItem().map { it.mediaId })
			}
			s.mediaController.addListener(timelineObserver)
			awaitClose {
				s.mediaController.removeListener(timelineObserver)
			}
		}
	}

	override fun observePosition(): Flow<Duration> {
		return callbackFlow {
			var collectorJob: Job = Job()
			fun startCollectorJob() {
				collectorJob.cancel()
				collectorJob = coroutineScope.launch {
					s.mediaController.joinSuspend {
						while (collectorJob.isActive) {
							send(s.mediaController.positionMs.milliseconds)
							delay(500)
						}
					}
				}
			}

			observeMediaItem().safeCollect {
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

	override fun observeDuration(): Flow<Duration> {
		return callbackFlow {
			var bool = false
			val job1 = coroutineScope.launch {
				observeMediaItem().safeCollect {
					send(Contract.POSITION_UNSET)
					bool = true
				}
			}
			val job2 = coroutineScope.launch {
				var rememberDuration = Contract.DURATION_UNSET
				observeTimeline().safeCollect {
					if (bool) {
						send(it.duration)
						rememberDuration = it.duration
						bool = false
					} else {
						if (rememberDuration != it.duration) {
							send(it.duration)
							rememberDuration = it.duration
						}
					}
				}
			}
			send(s.mediaController.durationMs.milliseconds)
			awaitClose {
				job1.cancel()
				job2.cancel()
			}
		}
	}

	override fun observeIsPlaying(): Flow<Boolean> {
		return callbackFlow {
			val listener = object : LibraryPlayerEventListener {
				override fun onIsPlayingChanged(isPlaying: Boolean, reason: IsPlayingChangedReason) {
					trySend(isPlaying)
				}
			}
			send(s.mediaController.isPlaying)
			s.mediaController.addListener(listener)
			awaitClose {
				s.mediaController.removeListener(listener)
			}
		}
	}

	override fun observePlayWhenReady(): Flow<Boolean> {
		return callbackFlow {
			val listener = object : LibraryPlayerEventListener {
				override fun onPlayWhenReadyChanged(
					playWhenReady: Boolean,
					reason: PlayWhenReadyChangedReason
				) {
					trySend(playWhenReady)
				}
			}
			s.mediaController.addListener(listener)
			awaitClose {
				s.mediaController.removeListener(listener)
			}
		}
	}

	override fun observeTimeline(): Flow<Timeline> {
		return callbackFlow {
			val timelineObserver = object : LibraryPlayerEventListener {
				override fun onTimelineChanged(old: Timeline, new: Timeline, reason: Int) {
					trySend(new)
				}
			}
			s.mediaController.addListener(timelineObserver)
			awaitClose {
				s.mediaController.removeListener(timelineObserver)
			}
		}
	}
}
