package com.flammky.android.medialib.temp.cache.lru

import com.flammky.android.medialib.temp.cache.CacheStorage
import timber.log.Timber
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock


class DefaultLruCache<K: Any, V: Any>(
	maxSize: Long,
	private val sizeResolver: SizeResolver<K, V> = { _, _ -> 1 }
) : LruCache<K, V> {

	/**
	 * The Occupied size, never exceeds [_maxSize]
	 */
	private var _occupiedSize: Long = 0
		set(value) {
			check(write.isHeldByCurrentThread) { "Tried to update occupiedSize without writeLock" }
			require(value in 0.._maxSize) { "$value was not in 0..maxSize bounds" }
			field = value
		}

	/**
	 * The Maximum size
	 */
	private var _maxSize: Long = maxSize
		set(value) {
			check(write.isHeldByCurrentThread) { "Tried to update maxSize without writeLock" }
			require(value > 0) { "$value was < 0" }
			field = value
		}

	// Entries
	private val lruEntries: LinkedHashMap<K, V> = LinkedHashMap(0, 0.75f, true)

	private val rwl = ReentrantReadWriteLock()
	private val read = rwl.readLock()
	private val write = rwl.writeLock()

	/**
	 * The Maximum size
	 */
	override val maxSize: Long
		get() = read.withLock { _maxSize }

	/**
	 * The current size occupied
	 *
	 * @see _occupiedSize
	 * @see safeSizeOf
	 * @see [CacheStorage.occupiedSize]
	 */
	override val occupiedSize: Long
		get() = read.withLock { _occupiedSize }

	init {
		checkMaxSize(maxSize)
	}

	private fun checkMaxSize(maxSize: Long) {
		require(maxSize > 0) { "Invalid Argument, maxSize($maxSize) <= 0" }
	}

	override fun put(key: K, value: V): V? {
		return write.withLock {
			val size = checkCandidateSize(key, value)
			trimToSize(maxSize - size)
			val old = lruEntries.put(key, value)
			_occupiedSize += size
			old
		}
	}

	override fun putIfKeyAbsent(key: K, value: V): V? {
		return write.withLock {
			if (!lruEntries.containsKey(key)) put(key, value) else value
		}
	}

	override fun get(key: K): V? {
		return read.withLock { lruEntries[key] }
	}

	override fun remove(key: K): V? {
		return write.withLock {
			Timber.d("DefaultLruCache removing $key")
			lruEntries.remove(key)?.let { old ->
				val oldSize = safeSizeOf(key, old)
				Timber.d("DefaultLruCache removed $key from lru, currentSize $_occupiedSize, removedSize: $oldSize, trimming to: ${_occupiedSize - oldSize}")
				_occupiedSize -= oldSize
				old
			}
		}
	}

	override fun resize(size: Long) {
		return write.withLock {
			checkMaxSize(size)
			_maxSize = size
			trimToSize(_maxSize)
		}
	}

	private fun checkCandidateSize(key: K, value: V): Long {
		val size = safeSizeOf(key, value)
		require( size < _maxSize) {
			"""
				Invalid Argument, candidate: $key | $value
				size($size) was too big to insert
			"""
		}
		return size
	}

	private fun trimToSize(size: Long) {
		while (_occupiedSize > size) if (!evictVictim()) {
			check(_occupiedSize <= size) {
				"No remaining element to evict (${lruEntries.size}), leftover size: $size" +
						"\n ensure that SizeResolver is reporting correct and consistent size"
			}
			break
		}
	}

	private fun evictVictim(): Boolean {
		return try {
			val victim = lruEntries.entries.first()
			remove(victim.key)
			true
		} catch (nse: NoSuchElementException) {
			// fallback?
			false
		}
	}

	private fun safeSizeOf(key: K, value: V): Long {
		val size = sizeResolver(key, value)
		require(size > -1) { "Invalid Size, $size" }
		return size
	}

	fun getSnapshot(): Map<K, V> = LinkedHashMap(lruEntries)
}
