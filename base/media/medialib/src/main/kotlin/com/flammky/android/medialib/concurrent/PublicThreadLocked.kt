package com.flammky.android.medialib.concurrent

/**
 * Instance is not Thread-safe and must only be accessed from certain Thread or Event Loop
 */
interface PublicThreadLocked<out T: Any> {

	/**
	 * Queue the block to be executed internally without blocking,
	 *
	 * in this case returning result is impossible without callback
	 *
	 * @see postListen
	 * @see joinBlocking
	 * @see joinBlockingSuspend
	 * @see joinSuspend
	 */
	fun post(block: T.() -> Unit)

	/**
	 * Queue the block to be executed internally without blocking,
	 *
	 * in this case result will be returned with callback
	 *
	 * @see post
	 * @see joinBlocking
	 * @see joinBlockingSuspend
	 * @see joinSuspend
	 */
	fun <R> postListen(block: T.() -> R, listener: (R) -> Unit)

	/**
	 * Block the current Thread until block is done executed internally,
	 *
	 * allows the caller to get results with cost of being blocked.
	 *
	 * this function must be re-entrant meaning recalling this function from within does not
	 * cause deadlocks
	 *
	 * @see post
	 * @see postListen
	 * @see joinBlockingSuspend
	 * @see joinSuspend
	 */
	fun <R> joinBlocking(block: T.() -> R): R

	/**
	 * Block the current Thread until block is done executed internally,
	 *
	 * allows the caller to get results with cost of being blocked.
	 *
	 * this function must be re-entrant meaning recalling this function from within
	 * does not cause deadlocks
	 *
	 * + no guarantees if you lock yourself externally
	 *
	 * @see post
	 * @see postListen
	 * @see joinBlocking
	 * @see joinSuspend
	 */

	// maybe we should just not include this.
	fun <R> joinBlockingSuspend(block: suspend T.() -> R): R

	/**
	 * Block the current Thread until block is done executed internally,
	 *
	 * allows the caller to get results with cost of being blocked.
	 *
	 * this function must be re-entrant meaning recalling this function from within does not
	 * cause deadlocks
	 *
	 * @see post
	 * @see postListen
	 * @see joinBlocking
	 * @see joinBlockingSuspend
	 */
	suspend fun <R> joinSuspend(block: suspend T.() -> R): R
}
