package com.flammky.musicplayer.base.media.mediaconnection.playback

import com.flammky.musicplayer.base.media.playback.PlaybackSessionConnector
import com.flammky.musicplayer.base.user.User
import kotlinx.coroutines.Deferred
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


/**
 * Playback interface to be used outside of domain
 */
interface PlaybackConnection {

	/**
	 * Request PlaybackSession instance for the given user.
	 */
	// TODO: more descriptive return value
	fun requestUserSessionAsync(
		user: User,
		coroutineContext: CoroutineContext = EmptyCoroutineContext
	): Deferred<PlaybackSessionConnector>
}
