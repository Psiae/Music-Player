package com.kylentt.musicplayer.common.android.bitmap.bitmapfactory

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.kylentt.musicplayer.core.sdk.VersionHelper

object OptionsHelper {
	fun getBitmapPixelSize(options: BitmapFactory.Options) = OptionsInfo.getBitmapPixelSize(options)
}

private object OptionsInfo {

	fun getBitmapPixelSize(options: BitmapFactory.Options): Int {
		return when {
			options.inPreferredConfig == Bitmap.Config.ALPHA_8 -> 1
			options.inPreferredConfig == Bitmap.Config.RGB_565 -> 2
			options.inPreferredConfig == Bitmap.Config.ARGB_4444 -> 2
			options.inPreferredConfig == Bitmap.Config.ARGB_8888 -> 4
			VersionHelper.hasOreo() && options.inPreferredConfig == Bitmap.Config.RGBA_F16 -> 8
			else -> throw NotImplementedError()
		}
	}
}
