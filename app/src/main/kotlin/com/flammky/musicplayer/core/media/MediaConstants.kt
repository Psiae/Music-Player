package com.flammky.musicplayer.core.media

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object MediaConstants {
	val INDEX_UNSET = -1
	val POSITION_UNSET = (-1).milliseconds
	val DURATION_UNSET = (-1).seconds
	val DURATION_INDEFINITE = Duration.INFINITE
}
