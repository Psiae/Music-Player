package com.flammky.musicplayer.base.media.mediaconnection.playback

import com.flammky.musicplayer.base.media.playback.PlaybackConstants
import kotlin.time.Duration

data class PositionInfo(
	val progress: Duration,
	val bufferedProgress: Duration,
	val duration: Duration,
	val changeReason: ChangeReason
) {
	companion object {
		val UNSET = PositionInfo(
			progress = PlaybackConstants.POSITION_UNSET,
			bufferedProgress = PlaybackConstants.POSITION_UNSET,
			duration = PlaybackConstants.DURATION_UNSET,
			changeReason = ChangeReason.UNKNOWN
		)
	}

	sealed interface ChangeReason {
		object UNKNOWN : ChangeReason
		object PERIODIC : ChangeReason
		object SEEK_REQUEST : ChangeReason
		object MEDIA_TRANSITION : ChangeReason
		object PROGRESS_DISCONTINUITY : ChangeReason
	}
}
