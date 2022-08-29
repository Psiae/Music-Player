package com.kylentt.musicplayer.domain.musiclib.session

import androidx.media3.common.AudioAttributes
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

		private val mPlaybackPosition = MutableStateFlow(0L)
		private val mPlaybackDuration = MutableStateFlow(0L)
		private val mPlaybackBufferedPosition = MutableStateFlow(0L)

		private var allowUpdatePosition = true

		private var positionCollectorJob = collectPosition()


		override val playbackState: StateFlow<PlaybackState> = PlaybackState.StateFlow(player)

		override val playbackPosition: StateFlow<Long>
			get() = mPlaybackPosition

		override val playbackBufferedPosition: StateFlow<Long>
			get() = mPlaybackBufferedPosition

		private val playerListener = object : Player.Listener {

			override fun onIsPlayingChanged(isPlaying: Boolean) {
				Timber.d("player callback: onIsPlayingChanged $isPlaying, pos: ${player.position}, ${player.bufferedPosition}")
			}

			override fun onEvents(player: Player, events: Player.Events) {
				for (i in 0 until events.size()) {
					Timber.d("player callback: onEvents[$i]: ${events[i]}")
				}
			}

			override fun onPositionDiscontinuity(
				oldPosition: Player.PositionInfo,
				newPosition: Player.PositionInfo,
				reason: Int
			) {
				Timber.d("player callback: onPositionDiscontinuity: ${newPosition.positionMs}, reason: $reason. playerPosition: ${player.position}, ${player.bufferedPosition}")
				/*if (!positionCollectorJob.isActive) {
					startPositionCollector()
				}*/
			}

			override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
				Timber.d("player callback: onMediaItemTransition $reason. playerPosition: ${player.position}, ${player.bufferedPosition}")
				cancelPositionCollector()
			}

			override fun onTimelineChanged(timeline: Timeline, reason: Int) {
				Timber.d("player callback: onTimelineChanged $reason")
				if (!positionCollectorJob.isActive) {
					startPositionCollector()
				}
			}
		}

		init {
			player.addListener(playerListener)
		}

		private fun updatePlaybackPosition(pos: Long) {
			mPlaybackPosition.value = pos.clamp(0L, playbackState.value.duration)
		}

		private fun updatePlaybackBufferedPosition(pos: Long) {
			mPlaybackBufferedPosition.value = pos.clamp(0L, playbackState.value.duration)
		}

		private fun cancelPositionCollector() {
			positionCollectorJob.cancel()
			Timber.d("positionCollectorJob is cancelled")
		}

		private fun startPositionCollector() {
			positionCollectorJob = collectPosition()
			Timber.d("positionCollectorJob is started")
		}

		private fun collectPosition() = mainScope.launch {
			while (isActive) {
				updatePlaybackPosition(player.position)
				updatePlaybackBufferedPosition(player.bufferedPosition)
				delay(1000)
			}
		}
	}

	interface SessionInfo {
		val playbackState: StateFlow<PlaybackState>
		val playbackPosition: StateFlow<Long>
		val playbackBufferedPosition: StateFlow<Long>
	}
}
