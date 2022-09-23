package com.flammky.android.medialib.async

import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.player.Player
import kotlinx.coroutines.Deferred

/**
 * root interface for Player with asynchronous functions.
 *
 * may block accessor thread on non-async getters if required.
 */
interface AsyncPlayer : Player {

	fun prepareAsync(): Deferred<Unit>
	fun playAsync(): Deferred<Unit>
	fun pauseAsync(): Deferred<Unit>
	fun stopAsync(): Deferred<Unit>
}
