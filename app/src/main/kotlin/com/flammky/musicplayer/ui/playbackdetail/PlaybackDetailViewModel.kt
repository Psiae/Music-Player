package com.flammky.musicplayer.ui.playbackdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flammky.android.medialib.common.Contract
import com.flammky.android.medialib.common.mediaitem.AudioFileMetadata
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.player.Player
import com.flammky.musicplayer.domain.media.MediaConnection
import com.flammky.musicplayer.ui.playbackdetail.PlaybackDetailPositionStream.Companion.asPlaybackDetails
import com.flammky.musicplayer.ui.playbackdetail.PlaybackDetailPositionStream.PositionChangeReason.Companion.asPlaybackDetails
import com.flammky.musicplayer.ui.playbackdetail.PlaybackDetailPropertiesInfo.Companion.asPlaybackDetails
import com.flammky.musicplayer.ui.playbackdetail.PlaybackDetailTracksInfo.Companion.asPlaybackDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import kotlin.time.Duration

@HiltViewModel
internal class PlaybackDetailViewModel @Inject constructor(
	private val mediaConnection: MediaConnection
) : ViewModel() {
	private val _positionStreamFlow = mediaConnection.playback.observePositionStream()
	private val _trackStreamFlow = mediaConnection.playback.observePlaylistStream()
	private val _playbackPropertiesFlow = mediaConnection.playback.observePropertiesInfo()

	// Inject as Dependency instead
	val positionStreamStateFlow = _positionStreamFlow
		.map { it.asPlaybackDetails }
		.stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackDetailPositionStream())

	// Inject as Dependency instead
	val trackStreamStateFlow = _trackStreamFlow
		.map { it.asPlaybackDetails }
		.stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackDetailTracksInfo())

	// Inject as Dependency instead
	val playbackPropertiesStateFlow = _playbackPropertiesFlow
		.map { it.asPlaybackDetails }
		.stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackDetailPropertiesInfo())

	@OptIn(ExperimentalCoroutinesApi::class)
	val currentMetadataStateFlow = _trackStreamFlow.flatMapLatest { tracksInfo ->
		val id = tracksInfo.takeIf { it.currentIndex >= 0 && it.list.isNotEmpty() }
			?.let { safeTrackInfo -> safeTrackInfo.list[safeTrackInfo.currentIndex] }
			?: ""
		Timber.d("PlaybackDetailViewModel currentMetadata observing $id, $tracksInfo")
		observeMetadata(id)
	}.stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackDetailMetadata())

	suspend fun seek(currentIndex: Int, position: Long): Boolean {
		return mediaConnection.playback.joinSuspend {
			if (this@joinSuspend.currentIndex == currentIndex) {
				this@joinSuspend.seekPosition(position)
				true
			} else false
		}
	}

	suspend fun seek(currentIndex: Int, toIndex: Int): Boolean {
		return mediaConnection.playback.joinSuspend playback@ {
			(this@playback.currentIndex == currentIndex && this@playback.mediaItemCount >= toIndex)
				.also { if (it) seekIndex(toIndex, 0L) }
		}
	}

	override fun onCleared() {
		super.onCleared()
	}

	// Inject as Dependency
	fun observeMetadata(id: String): StateFlow<PlaybackDetailMetadata> = flow {
		val combined = combine(
			flow = mediaConnection.repository.observeArtwork(id),
			flow2 = mediaConnection.repository.observeMetadata(id)
		) { art: Any?, metadata: MediaMetadata? ->
			val title = metadata?.title
				?: (metadata as? AudioFileMetadata)?.file?.fileName
			val subtitle = (metadata as? AudioMetadata)
				?.let { it.albumArtistName ?: it.artistName }
			PlaybackDetailMetadata(id, art, title, subtitle)
		}
		emitAll(combined)
	}.stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackDetailMetadata())

	fun playWhenReady() {
		mediaConnection.playback.playWhenReady()
	}
	fun pause() {
		mediaConnection.playback.pause()
	}
	fun seekNext() {
		mediaConnection.playback.seekNext()
	}
	fun seekPrevious() {
		mediaConnection.playback.seekPrevious()
	}
	fun enableShuffleMode() {
		mediaConnection.playback.setShuffleMode(true)
	}
	fun disableShuffleMode() {
		mediaConnection.playback.setShuffleMode(false)
	}
	fun enableRepeatMode() {
		mediaConnection.playback.setRepeatMode(Player.RepeatMode.ONE)
	}
	fun enableRepeatAllMode() {
		mediaConnection.playback.setRepeatMode(Player.RepeatMode.ALL)
	}
	fun disableRepeatMode() {
		mediaConnection.playback.setRepeatMode(Player.RepeatMode.OFF)
	}
}

@Immutable
data class PlaybackDetailMetadata(
	val id: String = "",
	val artwork: Any? = null,
	val title: String? = null,
	val subtitle: String? = null,
)

@Immutable
data class PlaybackDetailPropertiesInfo(
	val playWhenReady: Boolean = false,
	val playing: Boolean = false,
	// should be hasNext instead, it's our UI properties
	val hasNextMediaItem: Boolean = false,
	// should be hasPrevious instead, it's our UI properties
	val hasPreviousMediaItem: Boolean = false,
	val shuffleOn: Boolean = false,
	val repeatMode: Player.RepeatMode = Player.RepeatMode.OFF,
	val playerState: Player.State = Player.State.IDLE
	// later suppressionInfo
) {
	companion object {
		inline val MediaConnection.Playback.PropertiesInfo.asPlaybackDetails
			get() = PlaybackDetailPropertiesInfo(
				playWhenReady = playWhenReady,
				playing = playing,
				hasNextMediaItem = hasNextMediaItem,
				hasPreviousMediaItem = hasPreviousMediaItem,
				shuffleOn = shuffleEnabled,
				repeatMode = repeatMode,
				playerState = playerState
			)
	}
}

@Immutable
data class PlaybackDetailTracksInfo(
	val currentIndex: Int = Contract.INDEX_UNSET,
	val tracks: ImmutableList<String> = persistentListOf(),
) {
	companion object {
		inline val MediaConnection.Playback.TracksInfo.asPlaybackDetails
			get() = PlaybackDetailTracksInfo(
				currentIndex = currentIndex,
				tracks = list
			)
	}
}

@Immutable
data class PlaybackDetailPositionStream(
	val positionChangeReason: PositionChangeReason = PositionChangeReason.UNKNOWN,
	val position: Duration = Contract.POSITION_UNSET,
	val bufferedPosition: Duration = Contract.POSITION_UNSET,
	val duration: Duration = Contract.DURATION_INDEFINITE
) {
	companion object {
		inline val MediaConnection.Playback.PositionStream.asPlaybackDetails
			get() = PlaybackDetailPositionStream(
				positionChangeReason = positionChangeReason.asPlaybackDetails,
				position = position,
				bufferedPosition = bufferedPosition,
				duration = duration
			)
	}

	sealed interface PositionChangeReason {
		object AUTO : PositionChangeReason
		object USER_SEEK : PositionChangeReason
		object PERIODIC : PositionChangeReason
		object UNKNOWN : PositionChangeReason

		companion object {
			inline val MediaConnection.Playback.PositionStream.PositionChangeReason.asPlaybackDetails
				get() = when (this) {
					MediaConnection.Playback.PositionStream.PositionChangeReason.AUTO -> AUTO
					MediaConnection.Playback.PositionStream.PositionChangeReason.PERIODIC -> PERIODIC
					MediaConnection.Playback.PositionStream.PositionChangeReason.UNKN0WN -> UNKNOWN
					MediaConnection.Playback.PositionStream.PositionChangeReason.USER_SEEK -> USER_SEEK
				}
		}
	}
}
