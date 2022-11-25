package com.flammky.musicplayer.media.mediaconnection.playback

import com.flammky.musicplayer.media.playback.ProgressDiscontinuityReason
import com.flammky.musicplayer.media.playback.RepeatMode
import com.flammky.musicplayer.media.playback.ShuffleMode
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface PlaybackConnection {

	//
	// keep these simple for now
	//

	suspend fun getRepeatMode(): RepeatMode
	suspend fun observeRepeatMode(): Flow<RepeatMode>

	suspend fun getShuffleMode(): ShuffleMode
	suspend fun observeShuffleMode(): Flow<ShuffleMode>

	suspend fun getProgress(): Duration
	suspend fun observeProgressDiscontinuity(): Flow<ProgressDiscontinuity>

	suspend fun getBufferedProgress(): Duration

	suspend fun getIsPlaying(): Boolean
	suspend fun observeIsPlaying(): Flow<Boolean>

	suspend fun getDuration(): Duration
	suspend fun observeDuration(): Flow<Duration>

	suspend fun getPlaybackSpeed(): Float

	suspend fun <R> joinContext(block: suspend PlaybackConnection.() -> R): R

	interface Properties {
		val repeat: Repeat
		val shuffle: Shuffle
		val progress: Progress

		interface Progress {
			suspend fun get(): Duration
			suspend fun getBuffered(): Duration

			suspend fun observeDiscontinuity(): Flow<Duration>
		}
		interface Shuffle {
			suspend fun get(): ShuffleMode
			suspend fun observeChange(): Flow<ShuffleMode>
		}
		interface Repeat {
			suspend fun get(): RepeatMode
			suspend fun observeChange(): Flow<RepeatMode>
		}
	}


	data class ProgressDiscontinuity(
		val old: Duration,
		val new: Duration,
		val reason: ProgressDiscontinuityReason
	)
}
