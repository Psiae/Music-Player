package com.kylentt.musicplayer.domain.musiclib.entity

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.kylentt.musicplayer.domain.musiclib.core.media3.mediaitem.MediaItemFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

data class PlaybackState(
	val mediaItem: MediaItem,
	val mediaItems: List<MediaItem>,
	val playWhenReady: Boolean,
	val playing: Boolean,
	@Player.RepeatMode val playerRepeatMode: Int,
	@Player.State val playerState: Int
) {

	companion object {
		val EMPTY = PlaybackState(
			mediaItem = MediaItemFactory.EMPTY,
			mediaItems = emptyList(),
			playWhenReady = false,
			playing = false,
			playerRepeatMode = Player.REPEAT_MODE_OFF,
			playerState = Player.STATE_IDLE
		)

		fun MutableStateFlow<PlaybackState>.byPlayerListener(
			getMediaItems: () -> List<MediaItem>
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
						flow.update { it.copy(mediaItems = getMediaItems()) }
					}
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
