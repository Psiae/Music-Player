package com.flammky.musicplayer.base.coroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Dispatchers for non blocking operations
 */
@OptIn(ExperimentalCoroutinesApi::class)
object NonBlockingDispatcherPool {

	fun get(parallelism: Int): CoroutineDispatcher {
		// Dispatchers.Default is reserved for fast non-blocking operation that is expected to not block
		// for more than 100 ms, but may be divided to smaller message chunks to yield for others,
		// suitable for Services and should generally be limited to 1 parallelism
		return Dispatchers.Default.limitedParallelism(parallelism)
	}

	// Looper ?
}
