package com.flammky.android.medialib.temp.cache

import com.flammky.android.common.io.exception.RejectionException
import com.flammky.android.medialib.temp.cache.lru.DefaultLruCache
import java.io.IOException

/**
 * Interface for class that stores cached value: [V], retrievable
 */
interface CacheStorage<K: Any, V: Any> {

	/**
	 * The maximum size it can hold,
	 * usually size representation for [V] is used, as [Long]. Must be in accordance with [occupiedSize]
	 *
	 * e.g: [android.graphics.Bitmap] is usually represented by:
	 *
	 * ```
	 * android.graphics.Bitmap.getAllocationByteCount.toLong()
	 * ```
	 *
	 * @see occupiedSize
	 */
	val maxSize: Long

	/**
	 * The current size occupied, must never ( publicly ) exceed maxSize under any circumstances.
	 *
	 * &nbsp
	 *
	 * Usually size representation for [V] is used, as [Long], must be in accordance with [maxSize]
	 *
	 * &nbsp
	 *
	 * e.g. [android.graphics.Bitmap] is usually represented by:
	 *
	 * ```
	 * android.graphics.Bitmap.getAllocationByteCount.toLong()
	 * ```
	 *
	 * usually this kind of measurement is configurable externally.
	 *
	 * @see [maxSize]
	 * @see [DefaultLruCache] for default implementation of LruCache
	 */
	val occupiedSize: Long

	/**
	 * Put the value: [V] to this storage.
	 *
	 * &nbsp
	 *
	 * request may be rejected,
	 *
	 * in that case the given @param [value] itself is returned,
	 *
	 * &nbsp
	 *
	 * may also throw [RejectionException] which extends [IOException] for convenience
	 *
	 * @return the replaced value for the given key
	 */
	fun put(key: K, value: V): V?

	fun putIfKeyAbsent(key: K, value: V): V?

	/**
	 * Remove the value: [V] from this storage.
	 *
	 * &nbsp
	 *
	 * request may be rejected,
	 *
	 * in that case [RejectionException] which extends [IOException] for convenience will be thrown
	 *
	 * @return the value: [V] that is removed, may be null if there was none
	 */
	fun remove(key: K): V?

	/**
	 * Get the value: [V] from this storage by the given @param [key]
	 *
	 * &nbsp
	 *
	 * request may be rejected if there's no stored value for the given key,
	 * or the value: [V] is not retrievable
	 *
	 * &nbsp
	 *
	 * on those cases null is usually returned, but may also
	 * throw [RejectionException] which extends [IOException] for convenience
	 */
    operator fun get(key: K): V?
}
