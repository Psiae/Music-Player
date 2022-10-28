package com.flammky.android.medialib.temp.player

import androidx.annotation.IntRange
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Timeline.Period
import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.temp.player.component.VolumeManager
import com.flammky.android.medialib.temp.player.event.LibraryPlayerEventListener
import com.flammky.android.medialib.temp.player.playback.RepeatMode
import kotlin.time.Duration

interface LibraryPlayer {

	// TODO: wrap
	val availableCommands: Player.Commands
	val playbackParameters: PlaybackParameters

	// TODO: wrap
	val playWhenReady: Boolean
	val playbackState: PlaybackState
	val repeatMode: RepeatMode
	val shuffleEnabled: Boolean
	val hasNextMediaItem: Boolean
	val hasPreviousMediaItem: Boolean

	// TODO: wrap
	val isLoading: Boolean
	val isPlaying: Boolean

	// TODO: wrap
	val currentPeriod: Period?
	val currentPeriodIndex: Int
	val timeLine: Timeline
	val mediaItemCount: Int
	val currentMediaItem: androidx.media3.common.MediaItem?

	val currentMediaItemIndex: Int
	val nextMediaItemIndex: Int
	val previousMediaItemIndex: Int

	// TODO: wrap
	val positionMs: Long
	val bufferedPositionMs: Long
	val bufferedDurationMs: Long
	val durationMs: Long

	val contextInfo: PlayerContextInfo
	val volumeManager: VolumeManager
	val released: Boolean

	// TODO: wrap
	val seekable: Boolean
	fun seekToDefaultPosition()
	fun seekToDefaultPosition(index: Int)
	fun seekToPosition(position: Long)
	@Throws(IndexOutOfBoundsException::class)
	fun seekToMediaItem(index: Int, startPosition: Long)
	fun seekToMediaItem(index: Int)
	fun seekToPrevious()
	fun seekToNext()
	fun seekToPreviousMediaItem()
	fun seekToNextMediaItem()
	fun setRepeatMode(repeatMode: com.flammky.android.medialib.player.Player.RepeatMode)
	fun setShuffleMode(enabled: Boolean)

	fun removeMediaItem(item: androidx.media3.common.MediaItem)
	fun removeMediaItems(items: List<androidx.media3.common.MediaItem>)
	fun removeMediaItem(index: Int)
	fun setMediaItems(items: List<MediaItem>)
	fun setMediaItems(items: List<MediaItem>, startIndex: Int)
	fun setMediaItems(items: List<MediaItem>, startIndex: Int, startPosition: Duration)
	fun play()

	fun play(item: androidx.media3.common.MediaItem)
	fun play(item: MediaItem)

	fun pause()
	fun prepare()
	fun stop()


	fun addListener(listener: LibraryPlayerEventListener)
	fun removeListener(listener: LibraryPlayerEventListener)

	fun release()

	@Throws(IndexOutOfBoundsException::class)
	fun getMediaItemAt(@IntRange(from = 0, to = 2147483647) index: Int): androidx.media3.common.MediaItem

	fun getAllMediaItems(@IntRange(from = 0, to = 2147483647) limit: Int = Int.MAX_VALUE): List<androidx.media3.common.MediaItem>
	fun getAllMediaItem(): List<MediaItem>

	sealed class PlaybackState {
		object IDLE : PlaybackState()
		object BUFFERING : PlaybackState()
		object READY : PlaybackState()
		object ENDED : PlaybackState()

		companion object {
			inline val @Player.State Int.asPlaybackState
				get() = when(this) {
					Player.STATE_IDLE -> IDLE
					Player.STATE_BUFFERING -> BUFFERING
					Player.STATE_READY -> READY
					Player.STATE_ENDED -> ENDED
					else -> throw IllegalArgumentException("Tried to cast invalid: $this to: ${PlaybackState::class}")
				}

			inline val PlaybackState.toPlaybackStateInt
				get() = when(this) {
					IDLE -> Player.STATE_IDLE
					BUFFERING -> Player.STATE_BUFFERING
					READY -> Player.STATE_READY
					ENDED -> Player.STATE_ENDED
				}
		}

		fun isIDLE() = this === IDLE
		fun isBUFFERING() = this === BUFFERING
		fun isREADY() = this === READY
		fun isENDED() = this === ENDED

		fun shouldPrepare() = isIDLE()
		fun shouldSeekDefault() = isENDED()
	}
}
