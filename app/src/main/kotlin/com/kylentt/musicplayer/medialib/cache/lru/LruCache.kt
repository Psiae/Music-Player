package com.kylentt.musicplayer.medialib.cache.lru

import androidx.annotation.IntRange
import com.kylentt.musicplayer.medialib.cache.CacheProvider
import com.kylentt.musicplayer.medialib.cache.CacheStorage

/**
 * LruCache base class
 */

interface LruCache <K: Any, V: Any> : CacheStorage<K, V>, CacheProvider<K, V> {
	fun resize(@IntRange(from = 1, to = Long.MAX_VALUE) size: Long)
}

