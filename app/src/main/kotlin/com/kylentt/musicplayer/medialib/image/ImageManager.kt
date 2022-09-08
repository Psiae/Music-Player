package com.kylentt.musicplayer.medialib.image

import android.graphics.Bitmap
import com.kylentt.musicplayer.medialib.android.internal.AndroidContext
import com.kylentt.musicplayer.medialib.cache.internal.CacheManager
import com.kylentt.musicplayer.medialib.cache.lru.DefaultLruCache
import com.kylentt.musicplayer.medialib.cache.lru.LruCache

class ImageManager internal constructor(androidContext: AndroidContext) {

	val sharedBitmapLru: LruCache<String, Bitmap> = object : DefaultLruCache<String, Bitmap>(40000000) {
		override fun sizeOf(key: String, value: Bitmap): Long = value.allocationByteCount.toLong()
	}
}
