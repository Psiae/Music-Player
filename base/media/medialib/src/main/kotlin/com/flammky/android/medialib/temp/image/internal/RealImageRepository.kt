package com.flammky.android.medialib.temp.image.internal

import android.graphics.Bitmap
import com.flammky.android.medialib.temp.cache.lru.DefaultLruCache
import com.flammky.android.medialib.temp.cache.lru.LruCache
import com.flammky.android.medialib.temp.cache.lru.SizeResolver
import com.flammky.android.medialib.temp.image.ImageRepository
import com.flammky.android.medialib.temp.internal.AndroidContext

internal class RealImageRepository(androidContext: AndroidContext) : ImageRepository {

	override val sharedBitmapLru: LruCache<ImageRepository.ImageCacheKey, Bitmap> = sharedBitmapLru()

	internal companion object {

		private fun sharedBitmapLru(): DefaultLruCache<ImageRepository.ImageCacheKey, Bitmap> =
			DefaultLruCache(150000000, LruBitmapSizeResolver)

		private val LruBitmapSizeResolver: SizeResolver<Any, Bitmap> = { _, bitmap ->
			bitmap.allocationByteCount.toLong()
		}
	}
}
