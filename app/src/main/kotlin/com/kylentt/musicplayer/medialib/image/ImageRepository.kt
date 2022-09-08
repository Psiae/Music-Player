package com.kylentt.musicplayer.medialib.image

import android.graphics.Bitmap
import com.kylentt.musicplayer.medialib.cache.lru.LruCache

interface ImageRepository  {
	val sharedBitmapLru: LruCache<String, Bitmap>
}
