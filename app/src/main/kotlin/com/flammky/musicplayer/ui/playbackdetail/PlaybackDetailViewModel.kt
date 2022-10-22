package com.flammky.musicplayer.ui.playbackdetail

import androidx.lifecycle.ViewModel
import com.flammky.musicplayer.domain.media.MediaConnection
import kotlinx.coroutines.flow.Flow

internal class PlaybackDetailViewModel constructor(
	val observableTrackStream: Flow<MediaConnection.Playback.TracksStream>,
	val observablePositionStream: Flow<MediaConnection.Playback.PositionStream>
) : ViewModel() {




}
