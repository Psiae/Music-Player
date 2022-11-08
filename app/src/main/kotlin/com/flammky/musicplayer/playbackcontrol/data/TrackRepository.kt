package com.flammky.musicplayer.playbackcontrol.data

import com.flammky.musicplayer.media.mediaconnection.tracks.TrackMetadata
import com.flammky.musicplayer.media.mediaconnection.tracks.TracksConnection
import com.flammky.musicplayer.playbackcontrol.domain.model.TrackInfo
import com.flammky.musicplayer.playbackcontrol.domain.provider.TrackInfoProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class TrackRepository(
	private val trackConnection: TracksConnection
) : TrackInfoProvider {

	override suspend fun observeTrack(id: String): Flow<TrackInfo> {
		return trackConnection.repository.observe(id).distinctUntilChanged().map { it.toTrackInfo() }
	}

	private fun TracksConnection.Track.toTrackInfo(): TrackInfo {
		return if (this === TracksConnection.Track.UNSET) {
			TrackInfo.UNSET
		} else {
			TrackInfo(
				id = id,
				artwork = metadata.artwork.takeIf { it !is TrackMetadata.NO_ARTWORK }
					?: TrackInfo.NO_ARTWORK,
				mediaMetadata = metadata.mediaMetadata
			)
		}
	}
}
