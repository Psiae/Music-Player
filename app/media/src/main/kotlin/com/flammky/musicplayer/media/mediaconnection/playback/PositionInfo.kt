package com.flammky.musicplayer.media.mediaconnection.playback

import com.flammky.musicplayer.media.PlaybackConstants
import kotlin.time.Duration

data class PositionInfo(
	val progress: Duration,
	val bufferedProgress: Duration,
	val duration: Duration
) {
	companion object {
		val UNSET = PositionInfo(
			progress = PlaybackConstants.POSITION_UNSET,
			bufferedProgress = PlaybackConstants.POSITION_UNSET,
			duration = PlaybackConstants.DURATION_UNSET
		)
	}
}
