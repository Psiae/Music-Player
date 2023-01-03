package com.flammky.musicplayer.base.media.mediaconnection.playback

import com.flammky.musicplayer.base.media.playback.PlaybackSession
import kotlinx.coroutines.flow.Flow


/**
 * Playback interface to be used outside of domain
 */
interface PlaybackConnection {

	fun getSession(id: String? = null): PlaybackSession?
	fun observeCurrentSession(): Flow<PlaybackSession?>
	fun observeSession(id: String): Flow<PlaybackSession?>
}
