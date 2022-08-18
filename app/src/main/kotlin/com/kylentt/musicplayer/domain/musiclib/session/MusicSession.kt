package com.kylentt.musicplayer.domain.musiclib.session

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

		private var positionCollectorJob = collectPosition()


		override val playbackState: StateFlow<PlaybackState> = PlaybackState.StateFlow(player)

		override val playbackPosition: StateFlow<Long>
			get() = mPlaybackPosition

		private val playerListener = object : Player.Listener {
			override fun onPositionDiscontinuity(
				oldPosition: Player.PositionInfo,
				newPosition: Player.PositionInfo,
				reason: Int
			) {
				Timber.d("sessionPlayerListener,onPositionDiscontinuity, reason: $reason")
				mPlaybackPosition.value = newPosition.positionMs
			}

			override fun onTimelineChanged(timeline: Timeline, reason: Int) {
				super.onTimelineChanged(timeline, reason)
				Timber.d("")
			}
		}

		init {
			player.addListener(playerListener)
		}

		private fun collectPosition() = mainScope.launch {
			while (isActive) {
				val pos = player.position.clamp(0L, playbackState.value.duration)
				mPlaybackPosition.value = pos
				delay(1000)
			}
		}
	}

	interface SessionInfo {
		val playbackState: StateFlow<PlaybackState>
		val playbackPosition: StateFlow<Long>
	}
}
