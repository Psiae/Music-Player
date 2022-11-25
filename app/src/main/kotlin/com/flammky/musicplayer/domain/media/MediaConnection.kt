package com.flammky.musicplayer.domain.media

import androidx.compose.runtime.Immutable
import com.flammky.android.medialib.common.Contract
import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.player.Player
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/**
 * Media Service Interactor
 */
interface MediaConnection {
	val playback: Playback
	val repository: Repository

	/**
	 * Playback Interactor
	 */
	interface Playback {
		val currentIndex: Int
		val currentPosition: Duration
		val currentDuration: Duration
		val mediaItemCount: Int
		val hasNextMediaItem: Boolean
		val hasPreviousMediaItem: Boolean

		fun setMediaItems(items: List<MediaItem>, startIndex: Int, startPosition: Duration)
		fun prepare()
		fun playWhenReady()
		fun pause()
		fun stop()

		fun setRepeatMode(repeatMode: Player.RepeatMode)
		fun setShuffleMode(enabled: Boolean)
		fun seekNext()
		fun seekNextMedia()
		fun seekPrevious()
		fun seekPreviousMedia()

		//
		// I think we should add `shared` option on these observable
		//

		fun observePositionStream(interval: Duration): Flow<PositionStream>
		fun observePlaylistStream(): Flow<TracksInfo>
		fun observePropertiesInfo(): Flow<PropertiesInfo>
		fun seekIndex(index: Int, startPosition: Long)

		fun postSeekPosition(position: Long)

		fun observeInfo(): Flow<PlaybackInfo>

		suspend fun seekToPosition(position: Long): Boolean
		suspend fun <R> joinSuspend(block: suspend MediaConnection.Playback.() -> R): R

		data class PropertiesInfo(
			val playWhenReady: Boolean = false,
			val playing: Boolean = false,
			val shuffleEnabled: Boolean = false,
			val hasNextMediaItem: Boolean = false,
			val hasPreviousMediaItem: Boolean = false,
			val repeatMode: Player.RepeatMode = Player.RepeatMode.OFF,
			val playerState: Player.State = Player.State.IDLE
		) {
			companion object {
				val UNSET = PropertiesInfo()
				inline val PropertiesInfo.isUnset: Boolean
					get() = this == UNSET
				inline val PropertiesInfo.actuallyUnset: Boolean
					get() = this === UNSET
			}
		}
		data class TracksInfo(
			val changeReason: Int = -1,
			val currentIndex: Int = Contract.INDEX_UNSET,
			val list: ImmutableList<String> = persistentListOf()
		) {
			companion object {
				val UNSET = TracksInfo()
				inline val TracksInfo.isUnset: Boolean
					get() = this == UNSET
				inline val TracksInfo.actuallyUnset: Boolean
					get() = this === UNSET
			}
		}
		data class PositionStream(
			val positionChangeReason: PositionChangeReason = PositionChangeReason.UNKN0WN,
			val position: Duration = Contract.POSITION_UNSET,
			val bufferedPosition: Duration = Contract.POSITION_UNSET,
			val duration: Duration = Contract.DURATION_INDEFINITE
		) {
			sealed interface PositionChangeReason {
				object UNKN0WN : PositionChangeReason
				object USER_SEEK : PositionChangeReason
				object AUTO : PositionChangeReason
				object PERIODIC : PositionChangeReason
			}

			companion object {
				val UNSET = PositionStream()
				inline val PositionStream.isUnset: Boolean
					get() = this == UNSET
				inline val PositionStream.actuallyUnset: Boolean
					get() = this === UNSET
			}
		}
	}

	/**
	 * Repository Interactor
	 */
	interface Repository {
		suspend fun getMetadata(id: String): MediaMetadata?
		suspend fun getArtwork(id: String): Any?
		suspend fun observeMetadata(id: String): Flow<MediaMetadata?>
		suspend fun observeArtwork(id: String): Flow<Any?>
		suspend fun provideMetadata(id: String, metadata: MediaMetadata)
		suspend fun provideArtwork(id: String, artwork: Any?)

		data class MetadataChangeInfo(val id: String, val metadata: MediaMetadata?)
		data class ArtworkChangeInfo(val id: String, val artwork: Any?)
	}

	/**
	 * Data class containing Information about current Playback,
	 * all fields must correlate to the [id] given
	 */
	@Immutable
	data class PlaybackInfo(
		val id: String = "",
		val playing: Boolean = false,
		val playWhenReady: Boolean = false,
	) {
		companion object {
			val UNSET = PlaybackInfo()
			inline val PlaybackInfo.isUnset
				get() = this == UNSET
			inline val PlaybackInfo.actuallyUnset
				get() = this === UNSET
		}
	}
}
