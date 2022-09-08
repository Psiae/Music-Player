package com.kylentt.musicplayer.medialib.cache.lru

import com.kylentt.musicplayer.medialib.cache.CacheStorage
import timber.log.Timber

typealias SizeResolver<K, V> = (key: K, value: V) -> Long

open class DefaultLruCache<K: Any, V: Any>(
	maxSize: Long,
	protected val sizeResolver: SizeResolver<K, V> = { _, _ -> 1 }
) : LruCache<K, V> {

	/**
	 * The Occupied size, never exceeds [_maxSize]
	 */
	protected var _size: Long = 0
		set(value) {
			require(value in 0.._maxSize)
			field = value
		}

	/**
	 * The Maximum size of this Lru cache can hold
	 */
	protected var _maxSize: Long = maxSize
		set(value) {
			require(value > 0)
			field = value
		}

	// Entries
	protected val lruEntries: LinkedHashMap<K, V> = LinkedHashMap(0, 0.75f, true)

	/**
	 * The Maximum size of this Lru cache can hold
	 */
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
	override fun putIfKeyAbsent(key: K, value: V): V? {
		return if (!lruEntries.containsKey(key)) put(key, value) else value
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

	protected fun checkCandidateSize(key: K, value: V): Long {
		val size = safeSizeOf(key, value)
		require( size < _maxSize) {
			"""
				Invalid Argument, candidate: $key | $value
				size($size) was too big to insert
			"""
		}
		return size
	}

	private fun trimToMaxSize() = trimToSize(_maxSize)

	protected open fun trimToSize(size: Long) {
		while (_size > size) if (!evictVictim()) {
			check(_size <= size) { "Should never reach here." }
			break
		}
	}

	protected open fun evictVictim(): Boolean {
		val victim = lruEntries.entries.iterator().next()
		val victimSize = safeSizeOf(victim.key, victim.value)
		val result = remove(victim.key) != null
		_size -= victimSize
		Timber.d("victim: $victim will be removed, size: $victimSize" )
		return result
	}

	protected open fun safeSizeOf(key: K, value: V): Long {
		val size = sizeResolver(key, value)
		require(size > -1) { "Invalid Size, $size" }
		return size
	}

	fun getSnapshot(): Map<K, V> = LinkedHashMap(lruEntries)
}
