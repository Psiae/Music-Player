package com.flammky.musicplayer.playbackcontrol.ui

import android.os.Looper
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.common.mediaitem.AudioFileMetadata
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.providers.metadata.VirtualFileMetadata
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackProperties
import com.flammky.musicplayer.media.mediaconnection.playback.PlaylistInfo
import com.flammky.musicplayer.media.mediaconnection.playback.PositionInfo
import com.flammky.musicplayer.media.mediaconnection.tracks.Track
import com.flammky.musicplayer.playbackcontrol.domain.usecase.PlaybackInfoUseCase
import com.flammky.musicplayer.playbackcontrol.domain.usecase.TrackUseCase
import com.flammky.musicplayer.playbackcontrol.ui.model.PlaybackInfo
import com.flammky.musicplayer.playbackcontrol.ui.model.PositionInfoChangeReason
import com.flammky.musicplayer.playbackcontrol.ui.model.TrackDescription
import kotlinx.coroutines.flow.*

/**
 * The actual role of ViewModel here is to bridge our Composable with our Presenter,
 */
internal class PlaybackControlViewModel(
	private val coroutineDispatchers: AndroidCoroutineDispatchers,
	private val playbackInfoUseCase: PlaybackInfoUseCase,
	private val trackUseCase: TrackUseCase,
	private val presenter: PlaybackControlPresenter
) : ViewModel() {
	private val _trackDescriptionStateFlows = mutableMapOf<String, StateFlow<TrackDescription>>()

	val playlistInfoStateFlow = playbackInfoUseCase.observePlaylist()
		.map { playlistInfo -> playlistInfo.toLocalPlaylistInfo() }
		.stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackInfo.Playlist.UNSET)

	val positionInfoStateFlow = playbackInfoUseCase.observePosition()
		.map { positionInfo -> positionInfo.toLocalPositionInfo() }
		.stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackInfo.Position.UNSET)

	val propertiesInfoStateFlow = playbackInfoUseCase.observeProperties()
		.map { propertiesInfo -> propertiesInfo.toLocalPropertiesInfo() }
		.stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackInfo.Properties.UNSET)

	val currentTrackDisplayInfoStateFlow: StateFlow<TrackDescription> = playlistInfoStateFlow
		.map { it.currentTrackId ?: "" }
		.distinctUntilChanged()
		.transform {id ->
			observeTrackDescription(id).collect(this)
		}
		.stateIn(viewModelScope, SharingStarted.Eagerly, TrackDescription.UNSET)

	@MainThread
	fun observeTrackDescription(id: String): StateFlow<TrackDescription> {
		checkInMainThread {
			"Trying to observe TrackDescription ($id) on worker Thread"
		}
		return findOrCreateTrackDescriptionStateFlow(id)
	}

	@MainThread
	private fun findOrCreateTrackDescriptionStateFlow(id: String): StateFlow<TrackDescription> {
		_trackDescriptionStateFlows[id]?.let { return it }

		val state = MutableStateFlow(TrackDescription.UNSET)

		// Presenter

		return state
	}

	private inline val PlaybackInfo.Playlist.currentTrackId: String?
		get() {
			return list.getOrNull(currentIndex)
		}

	private inline fun checkInMainThread(lazyMsg: () -> Any) {
		check(Looper.myLooper() == Looper.getMainLooper(), lazyMsg)
	}

	private fun toTrackDescription(track: Track): TrackDescription {
		return if (track === Track.UNSET) {
			TrackDescription.UNSET
		} else {
			TrackDescription(
				id = track.id,
				title = track.findTitle(),
				subtitle = track.findSubtitle()
			)
		}
	}

	private fun Track.findTitle(): String {
		return metadata.mediaMetadata?.title
			?: (metadata.mediaMetadata as? AudioFileMetadata)?.let { afm ->
				afm.file.fileName
					?: afm.file.absolutePath
					?: (afm.file as? VirtualFileMetadata)?.uri?.toString()
			}
			?: ""
	}

	private fun Track.findSubtitle(): String {
		return (metadata.mediaMetadata as? AudioMetadata)?.let { am ->
			am.artistName ?: am.albumArtistName ?: am.albumTitle
		} ?: ""
	}

	private fun PositionInfo.toLocalPositionInfo(): PlaybackInfo.Position {
		return PlaybackInfo.Position(
			progress = progress,
			bufferedProgress = bufferedProgress,
			duration = duration,
			infoChangeReason = when (changeReason) {
				PositionInfo.ChangeReason.PERIODIC -> PositionInfoChangeReason.PERIODIC
				PositionInfo.ChangeReason.MEDIA_TRANSITION -> PositionInfoChangeReason.MEDIA_TRANSITION
				PositionInfo.ChangeReason.SEEK_REQUEST -> PositionInfoChangeReason.SEEK_REQUEST
				PositionInfo.ChangeReason.PROGRESS_DISCONTINUITY -> PositionInfoChangeReason.PROGRESS_DISCONTINUITY
				PositionInfo.ChangeReason.UNKNOWN -> PositionInfoChangeReason.UNKNOWN
			}
		)
	}

	private fun PlaylistInfo.toLocalPlaylistInfo(): PlaybackInfo.Playlist {
		return PlaybackInfo.Playlist(
			currentIndex = index,
			list = playlist,
			infoChangeReason = when (changeReason) {
				PlaylistInfo.ChangeReason.MEDIA_TRANSITION -> PlaybackInfo.Playlist.ChangeReason.MEDIA_TRANSITION
				PlaylistInfo.ChangeReason.PLAYLIST_CHANGE -> PlaybackInfo.Playlist.ChangeReason.PLAYLIST_CHANGE
				PlaylistInfo.ChangeReason.UNKNOWN -> PlaybackInfo.Playlist.ChangeReason.UNKNOWN
			}
		)
	}

	private fun PlaybackProperties.toLocalPropertiesInfo(): PlaybackInfo.Properties {
		return PlaybackInfo.Properties(
			playWhenReady = playWhenReady,
			playing = playing,
			shuffleEnabled = shuffleEnabled,
			hasNextMediaItem = hasNextMediaItem,
			hasPreviousMediaItem = hasPreviousMediaItem,
			repeatMode = when (repeatMode) {
				PlaybackProperties.RepeatMode.OFF -> PlaybackInfo.RepeatMode.OFF
				PlaybackProperties.RepeatMode.ALL -> PlaybackInfo.RepeatMode.ALL
				PlaybackProperties.RepeatMode.ONE -> PlaybackInfo.RepeatMode.ONE
			},
			playbackState = when (playbackState) {
				PlaybackProperties.PlaybackState.READY -> PlaybackInfo.PlaybackState.READY
				PlaybackProperties.PlaybackState.BUFFERING -> PlaybackInfo.PlaybackState.BUFFERING
				PlaybackProperties.PlaybackState.ENDED -> PlaybackInfo.PlaybackState.ENDED
				PlaybackProperties.PlaybackState.IDLE -> PlaybackInfo.PlaybackState.IDLE
				PlaybackProperties.PlaybackState.ERROR -> PlaybackInfo.PlaybackState.ERROR
			}
		)
	}
}
