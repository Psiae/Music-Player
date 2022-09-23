package com.flammky.android.medialib.concurrent

/**
 * Instance is not Thread-safe and must only be accessed from certain Thread or Event Loop
 */
interface PublicThreadLocked {

	/**
	 * Queue the block to be executed internally without blocking,
	 *
	 * in this case returning result is impossible without callback
	 *
	 * @see postListen
	 * @see joinBlocking
	 */
	fun post(block: () -> Unit)

	/**
	 * Queue the block to be executed internally without blocking,
	 *
	 * in this case result will be returned with callback
	 *
	 * @see post
	 * @see joinBlocking
	 */
	fun <R> postListen(block: () -> R, listener: (R) -> Unit)

	/**
	 * Block the current Thread until block is done executed internally,
	 *
	 * allows the caller to get results with cost of being blocked.
	 *
	 * this function must be re-entrant
	 *
	 * @see post
	 * @see postListen
	 */
	fun <R> joinBlocking(block: () -> R): R
}
