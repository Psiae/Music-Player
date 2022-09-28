package com.flammky.android.medialib.player

import kotlinx.coroutines.Deferred

/**
 * root interface for Player with asynchronous behavior.
 *
 * as we extend [Player] interface, it may block accessor thread on non-async getters if required.
 */
interface AsyncPlayer : Player {

	fun prepareAsync(): Deferred<Unit>
	fun playAsync(): Deferred<Unit>
	fun pauseAsync(): Deferred<Unit>
	fun stopAsync(): Deferred<Unit>
}
