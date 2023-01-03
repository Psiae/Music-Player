package com.flammky.musicplayer.playbackcontrol.domain.usecase

import com.flammky.musicplayer.base.media.mediaconnection.tracks.Track
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow

interface TrackUseCase {
	suspend fun getAsync(id: String): Deferred<Track?>
	fun observe(id: String): Flow<Track>
}
