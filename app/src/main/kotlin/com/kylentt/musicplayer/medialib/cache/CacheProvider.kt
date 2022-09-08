package com.kylentt.musicplayer.medialib.cache

interface CacheProvider<K: Any, V: Any> {
	fun get(key: K): V?
}
