package com.flammky.android.medialib.player

import android.os.Handler
import android.os.Looper
import com.flammky.android.medialib.concurrent.PublicThreadLocked
import java.util.concurrent.locks.LockSupport

/**
 * Denotes that the Player is only accessible from certain Thread,
 *
 * [publicLooper] is public and `is` Thread-Safe.
 *
 * @see [PublicThreadLocked] for extra convenience function
 */
interface ThreadLockedPlayer<P: Player> : Player, PublicThreadLocked<P> {

	/**
	 * The [Looper] to access this Player
	 *
	 * @see [Handler]
	 */
	val publicLooper: Looper
}
