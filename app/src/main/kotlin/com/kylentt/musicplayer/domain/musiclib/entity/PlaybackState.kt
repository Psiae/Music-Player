package com.kylentt.musicplayer.domain.musiclib.entity

import androidx.media3.common.*
import com.kylentt.musicplayer.common.generic.sync
import com.kylentt.musicplayer.common.kotlin.comparable.clamp
import com.kylentt.musicplayer.domain.musiclib.core.media3.mediaitem.MediaItemFactory
import com.kylentt.musicplayer.domain.musiclib.session.LibraryPlayer
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

data class PlaybackState(
	val mediaItem: MediaItem,
	val mediaItems: List<MediaItem>,
	val playWhenReady: Boolean,
	val playing: Boolean,
	val duration: Long,
	@Player.RepeatMode val playerRepeatMode: Int,
	@Player.State val playerState: Int
) {

	class StateFlow(player: LibraryPlayer) : kotlinx.coroutines.flow.StateFlow<PlaybackState> {
		private val mStateFlow = MutableStateFlow(EMPTY)

		private val listenerImpl = mStateFlow.byPlayerListener(player)

		private var mPlayer: LibraryPlayer? = null
			set(value) = sync {
				if (field === value) return@sync
				field?.removeListener(listenerImpl)
				value?.addListener(listenerImpl)
				field = value
			}

		init {
			mPlayer = player
		}

		override val replayCache: List<PlaybackState>
			get() = mStateFlow.replayCache

		override val value: PlaybackState
			get() = mStateFlow.value

		override suspend fun collect(collector: FlowCollector<PlaybackState>): Nothing {
			mStateFlow.collect(collector)
		}
	}

	companion object {
		inline val PlaybackState.isEmpty: Boolean
			get() = this === EMPTY


		val EMPTY = PlaybackState(
			mediaItem = MediaItemFactory.EMPTY,
			mediaItems = emptyList(),
			playWhenReady = false,
			playing = false,
			playerRepeatMode = Player.REPEAT_MODE_OFF,
			playerState = Player.STATE_IDLE,
			duration = 0L
		)

		fun MutableStateFlow<PlaybackState>.byPlayerListener(
			player: LibraryPlayer
		): Player.Listener {
			return object : Player.Listener {
				val flow = this@byPlayerListener

				override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
					val item = mediaItem ?: MediaItem.EMPTY
					val get = flow.value.mediaItem
					if (item.mediaId == get.mediaId && get.localConfiguration != null) return
					flow.update { it.copy(mediaItem = item) }
				}

				override fun onTimelineChanged(timeline: Timeline, reason: Int) {
					if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
						flow.update {
							it.copy(mediaItems = player.getMediaItems())
						}
					}

					flow.update { it.copy(duration = player.duration) }
				}

				override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
					flow.update { it.copy(playWhenReady = playWhenReady) }
				}

				override fun onIsPlayingChanged(isPlaying: Boolean) {
					flow.update { it.copy(playing = isPlaying) }
				}

				override fun onRepeatModeChanged(repeatMode: Int) {
					flow.update { it.copy(playerRepeatMode = repeatMode) }
				}

				override fun onPlaybackStateChanged(playbackState: Int) {
					flow.update { it.copy(playerState = playbackState) }
				}
			}
		}
	}
}
