package com.flammky.musicplayer.domain.media

import androidx.compose.runtime.Immutable
import com.flammky.android.medialib.common.Contract
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
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
		fun playWhenReady()
		fun pause()
		fun observePositionStream(): Flow<PositionStream>
		fun observePlaylistStream(): Flow<PlaylistStream>

		fun observeInfo(): Flow<PlaybackInfo>

		data class PlaylistStream(
			val reason: Int = -1,
			val currentIndex: Int = Contract.INDEX_UNSET,
			val list: ImmutableList<String> = persistentListOf()
		)
		data class PositionStream(
			val position: Duration = Contract.POSITION_UNSET,
			val bufferedPosition: Duration = Contract.POSITION_UNSET,
			val duration: Duration = Contract.DURATION_INDEFINITE
		)
	}

	/**
	 * Repository Interactor
	 */
	interface Repository {
		fun observeMetadata(id: String): Flow<MediaMetadata?>
		fun observeArtwork(id: String): Flow<Any?>
		fun provideMetadata(id: String, metadata: MediaMetadata)
		fun provideArtwork(id: String, artwork: Any?)
	}

	/**
	 * Data class containing Information about current Playback,
	 * all fields must correlate to the [id] given
	 */
	@Immutable
	data class PlaybackInfo(
		val id: String = "",
		val playlist: ImmutableList<String> = persistentListOf(),
		val currentIndex: Int = Contract.INDEX_UNSET,
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
