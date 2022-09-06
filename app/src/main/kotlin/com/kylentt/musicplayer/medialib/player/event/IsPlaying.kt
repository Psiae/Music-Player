package com.kylentt.musicplayer.medialib.player.event

// TODO
sealed class IsPlayingChangedReason {

	object UNKNOWN : IsPlayingChangedReason()

	/*data class*/ object PLAYBACK_STATE_CHANGED : IsPlayingChangedReason()
	data class PLAY_WHEN_READY_CHANGED(val playWhenReady: Boolean) : IsPlayingChangedReason()
	data class PLAYBACK_SUPPRESSED(val reason: PlaybackSuppressionReason) : IsPlayingChangedReason()
}
