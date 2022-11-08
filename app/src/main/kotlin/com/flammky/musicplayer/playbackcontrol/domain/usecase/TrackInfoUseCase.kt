package com.flammky.musicplayer.playbackcontrol.domain.usecase

import com.flammky.musicplayer.playbackcontrol.domain.model.TrackInfo
import com.flammky.musicplayer.playbackcontrol.domain.provider.TrackInfoProvider
import kotlinx.coroutines.flow.Flow

internal class TrackInfoUseCase(
	private val provider: TrackInfoProvider
) {
	suspend fun observe(id: String): Flow<TrackInfo> = provider.observeTrack(id)
}
