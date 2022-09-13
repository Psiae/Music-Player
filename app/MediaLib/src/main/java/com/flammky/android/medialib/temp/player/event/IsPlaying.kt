package com.flammky.android.medialib.temp.player.event

// TODO
sealed class IsPlayingChangedReason {

	object UNKNOWN : IsPlayingChangedReason()

	/*data class*/ object PLAYBACK_STATE_CHANGED : IsPlayingChangedReason()
	object PLAY_WHEN_READY_CHANGED : IsPlayingChangedReason()
	data class PLAYBACK_SUPPRESSED(val reason: PlaybackSuppressionReason) : IsPlayingChangedReason()
}
