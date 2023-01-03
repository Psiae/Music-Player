package com.flammky.musicplayer.library.localmedia.domain

import com.flammky.musicplayer.base.media.mediaconnection.tracks.TracksConnection
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Deferred

internal class TracksProvider : TracksConnection.TrackProvider(persistentListOf("")) {

	override suspend fun requestTrackAsync(id: String): Deferred<TracksConnection.Track?> {
		error("")
	}
}
