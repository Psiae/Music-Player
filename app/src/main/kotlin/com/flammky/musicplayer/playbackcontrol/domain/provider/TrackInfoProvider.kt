package com.flammky.musicplayer.playbackcontrol.domain.provider

import com.flammky.musicplayer.playbackcontrol.domain.model.TrackInfo
import kotlinx.coroutines.flow.Flow

interface TrackInfoProvider {
	suspend fun observeTrack(id: String): Flow<TrackInfo>
}
