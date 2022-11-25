package com.flammky.musicplayer.playbackcontrol.ui.presenter

import androidx.annotation.MainThread
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

@MainThread
interface PlaybackObserver {
	val progressStateFlow: StateFlow<Duration>
	val bufferedProgressStateFlow: StateFlow<Duration>
	val durationStateFlow: StateFlow<Duration>

	/**
	 * Change the Preferred interval, the presenter will recalculate the absolute interval.
	 */
	@MainThread
	fun setPreferredProgressCheckInterval(interval: Duration?)

	suspend fun updatePosition()
}
