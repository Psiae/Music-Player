package com.flammky.musicplayer.ui.playbackdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flammky.android.medialib.common.Contract
import com.flammky.android.medialib.player.Player
import com.flammky.musicplayer.domain.media.MediaConnection
import com.flammky.musicplayer.ui.playbackdetail.PlaybackDetailPositionStream.Companion.asPlaybackDetails
import com.flammky.musicplayer.ui.playbackdetail.PlaybackDetailPositionStream.PositionChangeReason.Companion.asPlaybackDetails
import com.flammky.musicplayer.ui.playbackdetail.PlaybackDetailPropertiesInfo.Companion.asPlaybackDetails
import com.flammky.musicplayer.ui.playbackdetail.PlaybackDetailTracksInfo.Companion.asPlaybackDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import kotlin.time.Duration

@HiltViewModel
internal class PlaybackDetailViewModel @Inject constructor(
	private val mediaConnection: MediaConnection
) : ViewModel() {

	// Inject as Dependency instead
	val positionStreamFlow = mediaConnection.playback.observePositionStream()
		.map { it.asPlaybackDetails }
		.stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackDetailPositionStream())


	// Inject as Dependency instead
	val trackStreamFlow = mediaConnection.playback.observePlaylistStream()
		.map { it.asPlaybackDetails }
		.stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackDetailTracksInfo())


	// Inject as Dependency instead
	val playbackPropertiesFlow = mediaConnection.playback.observePropertiesInfo()
		.map { it.asPlaybackDetails }
		.stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackDetailPropertiesInfo())

	suspend fun seek(currentIndex: Int, position: Long): Boolean {
		return mediaConnection.playback.joinSuspend {
			if (this@joinSuspend.currentIndex == currentIndex) {
				this@joinSuspend.seekPosition(position)
				true
			} else false
		}
	}
}

@Immutable
data class PlaybackDetailPropertiesInfo(
	val playWhenReady: Boolean = false,
	val playing: Boolean = false,
	val hasNextMediaItem: Boolean = false,
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
