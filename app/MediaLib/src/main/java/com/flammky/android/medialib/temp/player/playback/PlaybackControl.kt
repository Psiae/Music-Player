package com.flammky.android.medialib.temp.player.playback

import androidx.annotation.IntRange
import com.flammky.common.kotlin.time.annotation.DurationValue
import java.util.concurrent.TimeUnit
import com.flammky.android.medialib.temp.player.LibraryPlayer

interface PlaybackController {
	val controlInfo: PlaybackControlInfo

	fun play()
	fun pause()
	fun stop()
}

/**
 * class that stores the information on how the player should decide
 * when processing certain playback control request
 */
class PlaybackControlInfo private constructor(
	val pauseOnMediaItemEnded: Boolean,
	val skipSilence: Boolean,
	val seekBackwardIncrementMs: Int,
	val seekForwardIncrementMs: Int,
	val seekToPreviousPositionThreshold: Long,
) {

	class Builder() {

		private var mPauseOnMediaItemEnded: Boolean = false
		private var mSkipSilence: Boolean = false

		private var mSeekBackwardIncrementMs: Int = 15_000 // 15 seconds defaulted to ExoPlayer
		private var mSeekForwardIncrementMs: Int = 15_000 // 15 seconds defaulted to ExoPlayer
		private var mSeekToPreviousThresholdMs: Long = 3_000 // 3 seconds defaulted to ExoPlayer

		fun setPauseOnMediaItemEnded(pauseOnMediaItemEnded: Boolean): Builder {
			mPauseOnMediaItemEnded = pauseOnMediaItemEnded
			return this
		}

		@DurationValue(unit = TimeUnit.MILLISECONDS)
		fun setSeekBackwardIncrementMs(@IntRange(from = 0) ms: Int): Builder {
			require(ms > 0)
			mSeekForwardIncrementMs = ms
			return this
		}

		@DurationValue(unit = TimeUnit.MILLISECONDS)
		fun setSeekForwardIncrementMs(@IntRange(from = 0) ms: Int): Builder {
			require(ms > 0)
			mSeekForwardIncrementMs = ms
			return this
		}

		/**
		 * The position threshold in which case if [LibraryPlayer.seekToPrevious] is called when
		 * [LibraryPlayer.positionMs] is lower than or equal [threshold],
		 * it will seek to previous MediaItem otherwise it will reset to default position
		 *
		 * ```
		 * LibraryPlayer.positionMs <= threshold -> LibraryPlayer.seekToPreviousMediaItem()
		 * LibraryPlayer.positionMs > threshold -> LibraryPlayer.seekToDefaultPosition()
		 *
		 * ```
		 */

		@DurationValue(unit = TimeUnit.MILLISECONDS)
		fun setSeekToPreviousThresholdMs(@IntRange(from = -1) threshold: Long): Builder {
			mSeekToPreviousThresholdMs = threshold
			return this
		}

		fun build(): PlaybackControlInfo = PlaybackControlInfo(
			pauseOnMediaItemEnded = mPauseOnMediaItemEnded,
			skipSilence = mSkipSilence,
			seekBackwardIncrementMs = mSeekBackwardIncrementMs,
			seekForwardIncrementMs = mSeekForwardIncrementMs,
			seekToPreviousPositionThreshold = mSeekToPreviousThresholdMs
		)
	}

	companion object {
		val DEFAULT = Builder().build()
	}
}
