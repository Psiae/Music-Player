package com.kylentt.musicplayer.medialib.player.event

sealed class PlaybackSuppressionReason {

	object NONE : PlaybackSuppressionReason()

	object AUDIO_REROUTE : PlaybackSuppressionReason()
}
