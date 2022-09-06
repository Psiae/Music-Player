package com.kylentt.musicplayer.medialib.player.event

import androidx.media3.common.Player
import com.kylentt.musicplayer.medialib.player.playback.RepeatMode

/**
 * Reason for onMediaItemTransition callback
 *
 * @see LibraryPlayerEventListener.onMediaItemTransition
 */
sealed class MediaItemTransitionReason() {

	/**
	 * Playback has automatically transitioned to the next media item.
	 */
	object AUTO : MediaItemTransitionReason()

	/**
	 * The media item has been repeated. Because of [RepeatMode.CURRENT]
	 */
	object REPEAT : MediaItemTransitionReason()

	/**
	 * Seek request to another MediaItem
	 */
	object SEEK : MediaItemTransitionReason()

	/**
	 * The current media item has changed because of a change in the playlist. This can either be if
	 * the media item previously being played has been removed, or when the playlist becomes non-empty
	 * after being empty.
	 */
	object PLAYLIST_CHANGED : MediaItemTransitionReason()

	companion object {
		inline val @Player.MediaItemTransitionReason Int.asMediaItemTransitionReason
			get() = when(this) {
				Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> AUTO
				Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> REPEAT
				Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> SEEK
				Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> PLAYLIST_CHANGED
				else -> throw IllegalArgumentException("Tried to cast $this as ${MediaItemTransitionReason::class}")
			}
	}
}
