package com.kylentt.musicplayer.domain.musiclib.entity

import androidx.media3.common.*
import com.kylentt.musicplayer.common.generic.sync
import com.kylentt.musicplayer.common.kotlin.comparable.clamp
import com.kylentt.musicplayer.domain.musiclib.media3.mediaitem.MediaItemFactory
import com.kylentt.musicplayer.medialib.player.LibraryPlayer
import com.kylentt.musicplayer.medialib.player.LibraryPlayer.PlaybackState.Companion.toPlaybackStateInt
import com.kylentt.musicplayer.medialib.player.event.IsPlayingChangedReason
import com.kylentt.musicplayer.medialib.player.event.LibraryPlayerEventListener
import com.kylentt.musicplayer.medialib.player.event.MediaItemTransitionReason
import com.kylentt.musicplayer.medialib.player.event.PlayWhenReadyChangedReason
import com.kylentt.musicplayer.medialib.player.playback.RepeatMode
import com.kylentt.musicplayer.medialib.player.playback.RepeatMode.Companion.toRepeatModeInt
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
		): LibraryPlayerEventListener {
			return object : LibraryPlayerEventListener {
				val flow = this@byPlayerListener

				override fun onMediaItemTransition(
					old: MediaItem?,
					new: MediaItem?,
					reason: MediaItemTransitionReason
				) {
					val item = new ?: MediaItem.EMPTY
					val get = flow.value.mediaItem
					if (item.mediaId == get.mediaId && get.localConfiguration != null) return
					flow.update { it.copy(mediaItem = item) }
				}

				override fun onTimelineChanged(old: Timeline, new: Timeline, reason: Int) {
					if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
						flow.update {
							it.copy(mediaItems = player.getAllMediaItems())
						}
					}

					flow.update { it.copy(duration = player.durationMs.clamp(0, Long.MAX_VALUE)) }
				}

				override fun onPlayWhenReadyChanged(
					playWhenReady: Boolean,
					reason: PlayWhenReadyChangedReason
				) {
					flow.update { it.copy(playWhenReady = playWhenReady, playing = player.isPlaying) }
					Timber.d("onIsPlayWhenReadyChanged MC, $playWhenReady : ${player.isPlaying}")
				}

				override fun onIsPlayingChanged(isPlaying: Boolean, reason: IsPlayingChangedReason) {
					flow.update { it.copy(playing = isPlaying) }
					Timber.d("onIsPlayingChanged MC, $isPlaying")
				}

				override fun onRepeatModeChanged(old: RepeatMode, new: RepeatMode) {
					flow.update { it.copy(playerRepeatMode = new.toRepeatModeInt) }
				}

				override fun onPlaybackStateChanged(state: LibraryPlayer.PlaybackState) {
					flow.update { it.copy(playerState = state.toPlaybackStateInt) }
					Timber.d("onPlaybackStateChanged MC, isPlaying: ${player.isPlaying}")
				}
			}
		}
	}
}
