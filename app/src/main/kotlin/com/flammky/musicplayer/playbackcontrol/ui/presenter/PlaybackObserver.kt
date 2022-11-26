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
	 * Change the Preferred interval, the absolute interval will be recalculated.
	 *
	 * @param interval the interval, null for `DEFAULT`
	 */
	@MainThread
	fun setPreferredProgressCollectionDelay(interval: Duration?)

	suspend fun updatePosition()

	/**
	 * release the Observer
	 */
	@MainThread
	fun release()
}
