package com.flammky.android.medialib.temp.cache.internal

import androidx.collection.LruCache
import com.flammky.android.medialib.temp.internal.AndroidContext

class CacheManager private constructor(androidContext: AndroidContext) {
	private val lru: LruCache<String, Any?> = LruCache(DEFAULT_LRU_MEMORY_CACHE_SIZE)

	companion object {

		const val DEFAULT_LRU_MEMORY_CACHE_SIZE = 100000000
		const val DEFAULT_LRU_DISK_CACHE_SIZE =  DEFAULT_LRU_MEMORY_CACHE_SIZE / 8L
	}

	internal class Builder(context: AndroidContext) {
		private var mLruCacheSize = DEFAULT_LRU_MEMORY_CACHE_SIZE
		private var mDiskLruCacheSize = DEFAULT_LRU_DISK_CACHE_SIZE
	}
}
