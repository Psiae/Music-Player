package com.flammky.musicplayer.base.media.mediaconnection

import android.net.Uri
import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.player.Player
import com.flammky.android.medialib.temp.media3.Timeline
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface MediaConnectionPlayback {
	var playWhenReady: Boolean
	val playing: Boolean
	val index: Int
	val shuffleEnabled: Boolean

	val hasNextMediaItem: Boolean
	val hasPreviousMediaItem: Boolean
	val repeatMode: Player.RepeatMode
	val playerState: Player.State

	val mediaItemCount: Int

	val position: Duration

	fun seekToIndex(int: Int, startPosition: Long)
	fun postSeekToPosition(position: Long)
	fun seekNext()
	fun seekNextMedia()
	fun seekPrevious()
	fun seekPreviousMedia()

	fun prepare()
	fun stop()

	fun bufferedPosition(): Duration
	fun position(): Duration
	fun duration(): Duration
	fun playbackSpeed(): Float

	fun setMediaItems(items: List<MediaItem>, startIndex: Int, startPosition: Duration)

	fun setRepeatMode(repeatMode: Player.RepeatMode)
	fun setShuffleMode(enabled: Boolean)


	fun getCurrentMediaItem(): MediaItem?
	fun getPlaylist(): List<MediaItem>

	fun play(mediaItem: MediaItem)
	fun pause()

	fun observeMediaItemTransition(): Flow<MediaItem?>
	fun observePlaylistChange(): Flow<List<String>>
	fun observePositionChange(): Flow<Duration>
	fun observeDurationChange(): Flow<Duration>
	fun observeIsPlayingChange(): Flow<Boolean>
	fun observePlayWhenReadyChange(): Flow<Boolean>
	fun observeTimelineChange(): Flow<Timeline>
	fun observeDiscontinuityEvent(): Flow<Events.Discontinuity>
	fun observeShuffleEnabledChange(): Flow<Boolean>
	fun observeRepeatModeChange(): Flow<Player.RepeatMode>
	fun observePlayerStateChange(): Flow<Player.State>

	suspend fun seekToPosition(position: Long): Boolean
	suspend fun <R> joinDispatcher(block: suspend MediaConnectionPlayback.() -> R): R

	fun notifyUnplayableMedia(id: String)
	fun notifyUnplayableMedia(uri: Uri)


	sealed interface Events {
		data class Discontinuity(
			val oldPosition: Duration,
			val newPosition: Duration,
			val reason: Reason
		) {
			sealed interface Reason {
				object UNKNOWN : Reason
				object USER_SEEK : Reason
			}
		}



	}

	interface EditorScope {

	}
}
