package com.flammky.musicplayer.domain.musiclib.session

import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.flammky.android.medialib.temp.player.event.IsPlayingChangedReason
import com.flammky.common.kotlin.comparable.clamp
import com.flammky.musicplayer.domain.musiclib.entity.PlaybackState
import com.flammky.musicplayer.domain.musiclib.interactor.LibraryAgent
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class MusicSession(private val agent: LibraryAgent) {

	val player: com.flammky.android.medialib.temp.api.player.MediaController = com.flammky.android.medialib.temp.MediaLibrary.API.sessions.manager.findSessionById("DEBUG")!!.mediaController

	val sessionInfo = object : SessionInfo {
		private val scope = CoroutineScope(Dispatchers.Main)

		private val mLoading = MutableStateFlow(false)
		private val mPlaybackItem = MutableStateFlow<com.flammky.android.medialib.common.mediaitem.MediaItem>(com.flammky.android.medialib.common.mediaitem.MediaItem.UNSET)
		private val mPlaybackPosition = MutableStateFlow(-1L)
		private val mPlaybackDuration = MutableStateFlow(-1L)
		private val mPlaybackBufferedPosition = MutableStateFlow(-1L)

		private var positionCollectorJob = Job().apply { complete() }.job
			set(value) {
				if (field.isActive) throw IllegalStateException()
				field = value
			}

		private var bufferedPositionCollectorJob = Job().apply { complete() }.job
			set(value) {
				if (field.isActive) throw IllegalStateException()
				field = value
			}

		override val playbackState: StateFlow<PlaybackState> = PlaybackState.StateFlow(player)

		override val playbackItem: StateFlow<com.flammky.android.medialib.common.mediaitem.MediaItem>
			get() = mPlaybackItem

		override val playbackPosition: StateFlow<Long>
			get() = mPlaybackPosition

		override val playbackBufferedPosition: StateFlow<Long>
			get() = mPlaybackBufferedPosition

		private val playerListener = object :
			com.flammky.android.medialib.temp.player.event.LibraryPlayerEventListener {


			override fun onMediaItemTransition(
				old: MediaItem?,
				new: MediaItem?,
				reason: com.flammky.android.medialib.temp.player.event.MediaItemTransitionReason
			) {
				cancelPositionCollector()
				cancelBufferedPositionCollector()

				if (new != null) {
					if (!positionCollectorJob.isActive) {
						startPositionCollector(
							startPosition = 0L,
							context = Handler(Looper.myLooper()!!).asCoroutineDispatcher()
						)
					}
					if (!bufferedPositionCollectorJob.isActive) {
						startBufferedPositionCollector(
							startPosition = 0L,
							context = Handler(Looper.myLooper()!!).asCoroutineDispatcher()
						)
					}
				} else {
					updatePlaybackPosition(0L)
					updatePlaybackBufferedPosition(0L)
				}

				mPlaybackItem.update { player.currentActualMediaItem ?: com.flammky.android.medialib.common.mediaitem.MediaItem.UNSET }
			}

			override fun onIsPlayingChanged(
				isPlaying: Boolean,
				reason: IsPlayingChangedReason
			) {
				if (isPlaying && !positionCollectorJob.isActive) {
					startPositionCollector(context = Handler(Looper.myLooper()!!).asCoroutineDispatcher())
				}
			}

			override fun onIsLoadingChanged(isLoading: Boolean) {
				updatePlaybackBufferedPosition(player.bufferedPositionMs)
				mLoading.value = isLoading

				if (!bufferedPositionCollectorJob.isActive) {
					startBufferedPositionCollector(context = Handler(Looper.myLooper()!!).asCoroutineDispatcher())
				}
			}

			override fun onPositionDiscontinuity(
				oldPos: Player.PositionInfo,
				newPos: Player.PositionInfo,
				reason: Int
			) {
				updatePlaybackPosition(newPos.positionMs)
			}
		}

		init {
			player.addListener(playerListener)
		}

		private fun updatePlaybackPosition(pos: Long = player.positionMs) {
			mPlaybackPosition.value = pos.clamp(0L, playbackState.value.duration)
		}

		private fun updatePlaybackBufferedPosition(pos: Long = player.bufferedPositionMs) {
			mPlaybackBufferedPosition.value = pos.clamp(0L, playbackState.value.duration)
		}

		private fun startPositionCollector(
			startPosition: Long = player.positionMs,
			context: CoroutineContext = scope.coroutineContext
		) {
			updatePlaybackPosition(startPosition)
			positionCollectorJob = scope.launch(context) {
				delay(1000)
				collectPosition()
			}
		}

		private fun cancelPositionCollector() {
			positionCollectorJob.cancel()
		}

		private fun startBufferedPositionCollector(
			startPosition: Long = player.bufferedPositionMs,
			context: CoroutineContext = scope.coroutineContext
		) {
			updatePlaybackBufferedPosition(startPosition)
			bufferedPositionCollectorJob = scope.launch(context) {
				delay(500)
				collectBufferedPosition()
			}
		}

		private fun cancelBufferedPositionCollector() {
			bufferedPositionCollectorJob.cancel()
		}


		private suspend fun collectPosition() {
			while (coroutineContext.isActive) {
				updatePlaybackPosition(player.positionMs)
				if (playbackPosition.value >= playbackState.value.duration) break
				delay(1000)
			}
		}

		private suspend fun collectBufferedPosition() {
			while (coroutineContext.isActive) {
				updatePlaybackBufferedPosition(player.bufferedPositionMs)
				if (playbackBufferedPosition.value >= playbackState.value.duration) break
				delay(500)
			}
		}
	}

	interface SessionInfo {
		val playbackItem: StateFlow<com.flammky.android.medialib.common.mediaitem.MediaItem>
		val playbackState: StateFlow<PlaybackState>
		val playbackPosition: StateFlow<Long>
		val playbackBufferedPosition: StateFlow<Long>
	}
}
