package com.kylentt.musicplayer.domain.musiclib.session

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.kylentt.musicplayer.common.kotlin.comparable.clamp
import com.kylentt.musicplayer.domain.musiclib.entity.PlaybackState
import com.kylentt.musicplayer.domain.musiclib.interactor.LibraryAgent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

class MusicSession(private val agent: LibraryAgent) {

	val player = LibraryPlayer(agent)

	val sessionInfo = object : SessionInfo {
		val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

		private val mLoading = MutableStateFlow(false)
		private val mPlaybackPosition = MutableStateFlow(0L)
		private val mPlaybackDuration = MutableStateFlow(0L)
		private val mPlaybackBufferedPosition = MutableStateFlow(0L)

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

		override val playbackPosition: StateFlow<Long>
			get() = mPlaybackPosition

		override val playbackBufferedPosition: StateFlow<Long>
			get() = mPlaybackBufferedPosition

		private val playerListener = object : Player.Listener {

			override fun onIsLoadingChanged(isLoading: Boolean) {
				updatePlaybackBufferedPosition(player.bufferedPosition)
				mLoading.value = isLoading
			}

			override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
				Timber.d("player callback: onMediaItemTransition $reason. playerPosition: ${player.position}, ${player.bufferedPosition}")
				cancelPositionCollector()
				cancelBufferedPositionCollector()

				updatePlaybackPosition(0L)
				updatePlaybackBufferedPosition(0L)

				if (mediaItem != null) {
					if (!positionCollectorJob.isActive) {
						startPositionCollector(0L)
					}
					if (!bufferedPositionCollectorJob.isActive) {
						startBufferedPositionCollector(0L)
					}
				}
			}

			override fun onTimelineChanged(timeline: Timeline, reason: Int) {
			}
		}

		init {
			player.addListener(playerListener)
		}

		private fun updatePlaybackPosition(pos: Long = player.position) {
			mPlaybackPosition.value = pos.clamp(0L, playbackState.value.duration)
		}

		private fun updatePlaybackBufferedPosition(pos: Long = player.bufferedPosition) {
			mPlaybackBufferedPosition.value = pos.clamp(0L, playbackState.value.duration)
		}

		private fun startPositionCollector(startPosition: Long = player.position) {
			updatePlaybackPosition(startPosition)
			positionCollectorJob = collectPosition()
		}

		private fun cancelPositionCollector() {
			positionCollectorJob.cancel()
		}

		private fun startBufferedPositionCollector(startPosition: Long = player.position) {
			updatePlaybackBufferedPosition(startPosition)
			bufferedPositionCollectorJob = collectBufferedPosition()
		}

		private fun cancelBufferedPositionCollector() {
			bufferedPositionCollectorJob.cancel()
		}

		private fun collectPosition() = mainScope.launch {
			while (isActive) {
				delay(1000)
				updatePlaybackPosition(player.position)
			}
		}

		private fun collectBufferedPosition() = mainScope.launch {
			while (isActive) {
				delay(if (mLoading.value) 100 else 1000)
				updatePlaybackBufferedPosition(player.bufferedPosition)
			}
		}
	}

	interface SessionInfo {
		val playbackState: StateFlow<PlaybackState>
		val playbackPosition: StateFlow<Long>
		val playbackBufferedPosition: StateFlow<Long>
	}
}
