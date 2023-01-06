package com.flammky.musicplayer.domain.musiclib.entity

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.flammky.android.medialib.temp.player.LibraryPlayer.PlaybackState.Companion.toPlaybackStateInt
import com.flammky.android.medialib.temp.player.playback.RepeatMode.Companion.toRepeatModeInt
import com.flammky.common.kotlin.comparable.clamp
import com.flammky.musicplayer.core.common.sync
import com.flammky.musicplayer.domain.musiclib.media3.mediaitem.MediaItemFactory
import kotlinx.coroutines.InternalCoroutinesApi
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

	override fun toString(): String {
		return """
			PlaybackState:
			mediaItem: $mediaItem
			mediaItems: ${mediaItems.joinToString()}
			playWhenReady: $playWhenReady
			playing: $playing
			duration: $duration
			repeatMode: $playerRepeatMode
			playerState: $playerState
		""".trimIndent()
	}

	class StateFlow(player: com.flammky.android.medialib.temp.player.LibraryPlayer) : kotlinx.coroutines.flow.StateFlow<PlaybackState> {
		private val mStateFlow = MutableStateFlow(EMPTY)

		private val listenerImpl = mStateFlow.byPlayerListener(player)

		private var mPlayer: com.flammky.android.medialib.temp.player.LibraryPlayer? = null
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

		@InternalCoroutinesApi
		override suspend fun collect(collector: FlowCollector<PlaybackState>): Nothing {
			mStateFlow.collect(collector)
			error("")
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
			player: com.flammky.android.medialib.temp.player.LibraryPlayer
		): com.flammky.android.medialib.temp.player.event.LibraryPlayerEventListener {
			return object : com.flammky.android.medialib.temp.player.event.LibraryPlayerEventListener {
				val flow = this@byPlayerListener

				override fun onMediaItemTransition(
					old: MediaItem?,
					new: MediaItem?,
					reason: com.flammky.android.medialib.temp.player.event.MediaItemTransitionReason
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
					reason: com.flammky.android.medialib.temp.player.event.PlayWhenReadyChangedReason
				) {
					flow.update { it.copy(playWhenReady = playWhenReady, playing = player.isPlaying) }
					Timber.d("onIsPlayWhenReadyChanged MC, $playWhenReady : ${player.isPlaying}")
				}

				override fun onIsPlayingChanged(isPlaying: Boolean, reason: com.flammky.android.medialib.temp.player.event.IsPlayingChangedReason) {
					flow.update { it.copy(playing = isPlaying) }
					Timber.d("onIsPlayingChanged MC, $isPlaying, onPlayer: ${player.isPlaying}, reason: $reason")
				}

				override fun onRepeatModeChanged(old: com.flammky.android.medialib.temp.player.playback.RepeatMode, new: com.flammky.android.medialib.temp.player.playback.RepeatMode) {
					flow.update { it.copy(playerRepeatMode = new.toRepeatModeInt) }
				}

				override fun onPlaybackStateChanged(state: com.flammky.android.medialib.temp.player.LibraryPlayer.PlaybackState) {
					flow.update { it.copy(playerState = state.toPlaybackStateInt) }
					Timber.d("onPlaybackStateChanged MC, $state isPlaying: ${player.isPlaying}")
				}
			}
		}
	}
}
