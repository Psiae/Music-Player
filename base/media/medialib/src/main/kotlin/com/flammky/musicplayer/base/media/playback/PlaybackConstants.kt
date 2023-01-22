package com.flammky.musicplayer.base.media.playback

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Object Holding Global Playback-Constants, these Constants are used throughout entire app
 * with exception of external libraries in which are converted to it's corresponding value
 * when reaching our code
 */
object PlaybackConstants {

	/**
	 * Unset value of a PlaybackQueue
	 */
	val QUEUE_UNSET: OldPlaybackQueue
		// avoid circular reference on `INDEX_UNSET`
		get() = OldPlaybackQueue.UNSET

	/**
	 * Unset value of PlaybackProperties
	 */
	val PROPERTIES_UNSET: PlaybackProperties
		get() = PlaybackProperties.UNSET

	val POSITION_UNSET: Duration = (-1).milliseconds
	val DURATION_UNSET: Duration = (-1).seconds
	val INDEX_UNSET: Int = -1
}
