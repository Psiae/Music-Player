package com.flammky.musicplayer.media.playback

import kotlin.time.Duration

interface PlaybackController {
	val playing: Boolean
	val playWhenReady: Boolean
	val queue: PlaybackQueue
	val repeatMode: RepeatMode
	val shuffleMode: ShuffleMode
	val progress: Duration
	val bufferedProgress: Duration
	val duration: Duration
	val playbackSpeed: Float

	fun setPlayWhenReady(playWhenReady: Boolean): Boolean
	fun setQueue(queue: PlaybackQueue): Boolean
	fun setRepeatMode(mode: RepeatMode): Boolean
	fun setShuffleMode(mode: ShuffleMode): Boolean
	fun seekProgress(progress: Duration): Boolean
	fun seekIndex(index: Int, progress: Duration): Boolean


	fun observeRepeatModeChange(
		onChange: (RepeatMode, /* reason */) -> Unit
	)

	fun observeShuffleModeChange(
		onChange: (ShuffleMode, /* reason */) -> Unit
	)

	fun observePlayWhenReadyChange(
		onChange: (Boolean, /* reason */) -> Unit
	)

	fun observeQueueChange(
		onChange: (PlaybackEvent.QueueChange) -> Unit
	)

	fun inLooper(): Boolean
}
