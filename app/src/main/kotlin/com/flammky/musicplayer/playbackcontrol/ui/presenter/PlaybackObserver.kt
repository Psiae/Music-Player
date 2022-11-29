package com.flammky.musicplayer.playbackcontrol.ui.presenter

import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

@MainThread
interface PlaybackObserver {

	// should they be `Composable` getter ?

	val progressStateFlow: StateFlow<Duration>
	val bufferedProgressStateFlow: StateFlow<Duration>
	val durationStateFlow: StateFlow<Duration>

	fun collectProgress(
		collectorScope: CoroutineScope,
		startInterval: Duration?,
		// event array instead ?
		includeEvent: Boolean,
		nextInterval: suspend (isEvent: Boolean, progress: Duration, duration: Duration, speed: Float) -> Duration?
	): StateFlow<Duration>

	suspend fun updatePosition()
}
