package com.flammky.musicplayer.media.playback

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object PlaybackConstants {
	val PROGRESS_UNSET: Duration = (-1).milliseconds
	val DURATION_UNSET: Duration = (-1).seconds
	val INDEX_UNSET: Int = -1
}
