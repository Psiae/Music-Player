package com.flammky.android.medialib.temp.image.internal

import android.graphics.Bitmap
import com.flammky.android.medialib.temp.internal.AndroidContext
import com.flammky.android.medialib.temp.cache.lru.DefaultLruCache
import com.flammky.android.medialib.temp.cache.lru.LruCache
import com.flammky.android.medialib.temp.cache.lru.SizeResolver
import com.flammky.android.medialib.temp.image.ImageRepository

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
