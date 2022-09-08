package com.kylentt.musicplayer.medialib.cache.lru/*
package com.kylentt.musicplayer.medialib.cache

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import java.io.IOException

*/
/**
 * DiskLruCache that uses kotlin coroutines as means for synchronization
 * without blocking external context
 *//*

interface CoroutineDiskLruCache<V> : DiskLruCache<V> {

	*/
/**
	 * The current maximum size of this LruCache, this value is modifiable and is synchronized.
	 *//*

	override var maxSize: Long

	*/
/**
	 * update the maximum size,
	 * this function returns a [Job] to await for completion without blocking
	 *//*


	@Throws(IOException::class)
	suspend fun updateMaxSize(maxSize: Long): Job
	suspend fun suspendPut(): Job
	suspend fun suspendGet(): Deferred<V?>
}
*/
