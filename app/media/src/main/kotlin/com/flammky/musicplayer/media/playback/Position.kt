package com.flammky.musicplayer.media.playback

sealed interface ProgressDiscontinuityReason {

	@Deprecated("remove")
	object UNKNOWN : ProgressDiscontinuityReason

	object USER_SEEK : ProgressDiscontinuityReason

	object MEDIA_TRANSITION : ProgressDiscontinuityReason

	object MEDIA_REMOVAL : ProgressDiscontinuityReason
}
