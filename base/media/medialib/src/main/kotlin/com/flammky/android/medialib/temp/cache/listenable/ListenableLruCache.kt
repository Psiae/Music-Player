package com.flammky.android.medialib.temp.cache.listenable

import com.flammky.android.medialib.temp.cache.lru.LruCache

interface ListenableLruCache<K: Any, V: Any> : ListenableCacheStorage<K, V>, LruCache<K, V> {
	fun onMaxSizeChanged(old: Long, new: Long)
}
