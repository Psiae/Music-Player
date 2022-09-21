package com.flammky.android.medialib.temp.cache.lru

import androidx.annotation.IntRange
import com.flammky.android.medialib.temp.cache.CacheProvider
import com.flammky.android.medialib.temp.cache.CacheStorage

typealias SizeResolver<K, V> = (key: K, value: V) -> Long

/**
 * LruCache base class
 */

interface LruCache <K: Any, V: Any> : CacheStorage<K, V>, CacheProvider<K, V> {
	fun resize(@IntRange(from = 1, to = Long.MAX_VALUE) size: Long)
}

