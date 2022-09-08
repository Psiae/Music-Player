package com.kylentt.musicplayer.medialib.image.internal

import android.graphics.Bitmap
import com.kylentt.musicplayer.medialib.android.internal.AndroidContext
import com.kylentt.musicplayer.medialib.cache.lru.DefaultLruCache
import com.kylentt.musicplayer.medialib.cache.lru.LruCache
import com.kylentt.musicplayer.medialib.cache.lru.SizeResolver
import com.kylentt.musicplayer.medialib.image.ImageRepository

internal class RealImageRepository(androidContext: AndroidContext) : ImageRepository {

	override val sharedBitmapLru: LruCache<String, Bitmap> = sharedBitmapLru()

	internal companion object {

		private fun sharedBitmapLru(): DefaultLruCache<String, Bitmap> =
			DefaultLruCache(30000000, LruBitmapSizeResolver)

		private val LruBitmapSizeResolver: SizeResolver<String, Bitmap> = { _, bitmap ->
			bitmap.allocationByteCount.toLong()
		}
	}
}
