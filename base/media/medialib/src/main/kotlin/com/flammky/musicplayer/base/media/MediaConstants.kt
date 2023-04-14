package com.flammky.musicplayer.base.media

import android.graphics.Bitmap

object MediaConstants {
	val ARTWORK_UNSET = Any()
	val NO_ARTWORK = Any()
	val NO_ARTWORK_BITMAP by lazy { Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8) }

	fun isNoArtwork(obj: Any) = obj === NO_ARTWORK || obj === NO_ARTWORK_BITMAP
}
