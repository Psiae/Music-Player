package com.kylentt.musicplayer.medialib.cache.lru

open class NonBlockingLruCache<K: Any, V: Any>(maxSize: Long) : DefaultLruCache<K, V>(maxSize) {


}
