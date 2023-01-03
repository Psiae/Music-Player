package com.flammky.android.medialib.temp.cache

interface CacheProvider<K: Any, V: Any> {
	fun get(key: K): V?
}
