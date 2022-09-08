package com.kylentt.musicplayer.medialib.cache.lru

import com.kylentt.musicplayer.medialib.cache.CacheStorage
import timber.log.Timber

open class DefaultLruCache<K: Any, V: Any>(maxSize: Long) : LruCache<K, V> {

	/**
	 * The Occupied size, never exceeds [_maxSize]
	 *
	 * @see [occupiedSize]
	 */
	private var _size: Long = 0
		set(value) {
			require(value <= _maxSize)
			field = value
		}

	/**
	 * The Maximum size of this Lru cache can hold
	 */
	private var _maxSize: Long = maxSize

	// Entries
	private val lruEntries: LinkedHashMap<K, V> = LinkedHashMap(0, 0.75f, true)

	override val maxSize: Long
		@Synchronized get() = _maxSize

	/**
	 * The current size occupied, Synchronized
	 *
	 * @see _size
	 * @see sizeOf
	 * @see [CacheStorage.occupiedSize]
	 */
	override val occupiedSize: Long
		@Synchronized get() = _size

	init {
		checkMaxSize(maxSize)
	}

	private fun checkMaxSize(maxSize: Long) {
		require(maxSize > 0) { "Invalid Argument, maxSize <= 0" }
	}

	@Synchronized
	override fun put(key: K, value: V): V? {
		val size = checkCandidateSize(key, value)
		trimToSize(maxSize - size)
		val old = lruEntries.put(key, value)
		_size += size
		return old
	}

	@Synchronized
	override fun get(key: K): V? {
		return lruEntries[key]
	}

	@Synchronized
	override fun remove(key: K): V? {
		return lruEntries.remove(key)?.let { old ->
			_size -= safeSizeOf(key, old)
			old
		}
	}

	@Synchronized
	override fun resize(size: Long) {
		checkMaxSize(size)
		_maxSize = size
		trimToMaxSize()
	}

	private fun checkCandidateSize(key: K, value: V): Long {
		val size = safeSizeOf(key, value)
		require( size < maxSize) {
			"""
				Invalid Argument, candidate: $key | $value
				size($size) was too big to insert
			"""
		}
		return size
	}

	private fun trimToMaxSize() {
		while (_size > _maxSize) if (!removeVictim()) {
			check(_size < _maxSize) { "Should never reach here." }
			break
		}
	}

	private fun trimToSize(size: Long) {
		while (_size > size) if (!removeVictim()) {
			check(_size < size) { "Should never reach here." }
			break
		}
	}

	private fun removeVictim(): Boolean {
		val victim = lruEntries.entries.iterator().next()
		val victimSize = safeSizeOf(victim.key, victim.value)
		val result = remove(victim.key) != null
		_size -= victimSize
		Timber.d("victim: $victim will be removed, size: $victimSize" )
		return result
	}

	open fun sizeOf(key: K, value: V): Long {
		return 1
	}

	private fun safeSizeOf(key: K, value: V): Long {
		val size = sizeOf(key, value)
		require(size > -1) { "Invalid Size, $size" }
		return size
	}

	fun getSnapshot(): Map<K, V> = LinkedHashMap(lruEntries)
}
