package com.kylentt.musicplayer.medialib.cache.listenable

import com.kylentt.musicplayer.medialib.cache.lru.LruCache

interface ListenableLruCache<K: Any, V: Any> : ListenableCacheStorage<K, V>, LruCache<K, V> {
	fun onMaxSizeChanged(old: Long, new: Long)
}
