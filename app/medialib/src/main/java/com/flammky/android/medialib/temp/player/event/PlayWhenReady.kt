package com.flammky.android.medialib.temp.player.event

import androidx.media3.common.Player
import com.flammky.android.medialib.temp.player.LibraryPlayer

sealed class PlayWhenReadyChangedReason {

	/**
	 * Playback has been started or paused because
	 * a call to [LibraryPlayer.play] or [LibraryPlayer.pause] was made respectively
	 */
	object USER_REQUEST : PlayWhenReadyChangedReason()

	/**
	 * Playback has been paused because of a loss of audio focus.
	 */
	object FOCUS_LOSS : PlayWhenReadyChangedReason()

	/**
	 * Playback has been paused to avoid becoming noisy because of device output change.
	 */
	object AUDIO_REROUTE : PlayWhenReadyChangedReason()

	/**
	 * Playback has been started or paused because of a remote change.
	 */
	object REMOTE : PlayWhenReadyChangedReason()

	/**
	 * Playback has been paused at the end of a media item.
	 */
	object END_OF_MEDIA_ITEM : PlayWhenReadyChangedReason()

	companion object {
		inline val @Player.PlayWhenReadyChangeReason Int.asPlayWhenReadyChangedReason
			get() = when(this) {
				Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> USER_REQUEST
				Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS -> FOCUS_LOSS
				Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY -> AUDIO_REROUTE
				Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE -> REMOTE
				Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM -> END_OF_MEDIA_ITEM
				else -> throw IllegalArgumentException("Tried to cast $this as ${PlayWhenReadyChangedReason::class}")
			}
	}
}
