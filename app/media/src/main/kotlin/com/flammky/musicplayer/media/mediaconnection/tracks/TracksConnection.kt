package com.flammky.musicplayer.media.mediaconnection.tracks

import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow

interface TracksConnection {


	val repository: Repository
	val providers: Provider

	interface Repository {
		suspend fun getAsync(id: String): Deferred<Track?>
		suspend fun observe(id: String): Flow<Track>
	}

	interface Provider {
		suspend fun register(provider: TrackProvider): TrackProvider.Connection
	}

	data class Track(
		val id: String,
		val metadata: TrackMetadata
	) {
		companion object {
			val UNSET = Track(id = "", metadata = TrackMetadata.UNSET)
		}
	}

	abstract class TrackProvider(
		val prefixes: ImmutableList<String>
	) {
		abstract suspend fun requestTrackAsync(id: String): Deferred<Track?>

		interface Connection {
			suspend fun notifyTrackChange(id: String, track: Track)
			suspend fun close()
		}





	}

	class TrackRequest() {

	}
}
