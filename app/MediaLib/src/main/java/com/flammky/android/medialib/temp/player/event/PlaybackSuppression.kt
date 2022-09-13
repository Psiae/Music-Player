package com.flammky.android.medialib.temp.player.event

sealed class PlaybackSuppressionReason {

	object NONE : PlaybackSuppressionReason()

	object AUDIO_REROUTE : PlaybackSuppressionReason()
}
