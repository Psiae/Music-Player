package com.flammky.android.medialib.player

import android.os.Handler
import android.os.Looper
import com.flammky.android.medialib.concurrent.PublicThreadLocked

typealias PlayerThreadLocked = ThreadLockedPlayer<Player>

/**
 * The Player is only accessible from certain Thread, doing otherwise will result in
 * either an exception or thread blocking
 *
 * @see [PublicThreadLocked] for extra convenience function
 */
interface ThreadLockedPlayer<out P: Player> : Player, PublicThreadLocked<P> {

	/**
	 * The [Looper] to access this Player
	 *
	 * @see [Handler]
	 */
	val publicLooper: Looper
}
