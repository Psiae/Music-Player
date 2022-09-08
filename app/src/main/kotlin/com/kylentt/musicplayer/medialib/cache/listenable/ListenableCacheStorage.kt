package com.kylentt.musicplayer.medialib.cache.listenable

import com.kylentt.musicplayer.medialib.cache.CacheStorage

interface ListenableCacheStorage<K: Any, V: Any> : CacheStorage<K, V> {
	fun onItemAdded(key: K, item: V)
	fun onItemRemoved(key: K, item: V)
}
