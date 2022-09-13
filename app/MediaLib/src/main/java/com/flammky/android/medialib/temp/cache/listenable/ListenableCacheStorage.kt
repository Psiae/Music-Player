package com.flammky.android.medialib.temp.cache.listenable

import com.flammky.android.medialib.temp.cache.CacheStorage

interface ListenableCacheStorage<K: Any, V: Any> : CacheStorage<K, V> {
	fun onItemAdded(key: K, item: V)
	fun onItemRemoved(key: K, item: V)
}
