package com.kylentt.musicplayer.ui.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import com.kylentt.musicplayer.core.sdk.VersionHelper

object BitmapFactoryHelper {

	fun decodeByteArrayToSampledBitmap(
		array: ByteArray,
		offset: Int,
		buffer: Int,
		reqWidth: Int,
		reqHeight: Int,
		sampleCalculationType: SubSampleCalculationType
	): Bitmap? {
		return BitmapFactory.Options()
			.run {

				inJustDecodeBounds = true
				decodeByteArray(array, offset, buffer, this)

				// Calculate inSampleSize
				inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight, sampleCalculationType)

				// Decode bitmap with inSampleSize set
				inJustDecodeBounds = false

				decodeByteArray(array, offset, buffer, this)
			}
	}

	fun decodeByteArray(
		array: ByteArray,
		offset: Int,
		buffer: Int,
		option: BitmapFactory.Options
	): Bitmap? {
		return BitmapFactory.decodeByteArray(array, offset, buffer, option)
	}

	sealed class SubSampleCalculationType {
		object IF_WH_IS_LARGER : SubSampleCalculationType()
		object IF_ANY_WH_IS_LARGER : SubSampleCalculationType()
		data class MaxAlloc(val maxByte: Int) : SubSampleCalculationType()
	}

	// Copied from docs
	private fun calculateInSampleSize(
		options: BitmapFactory.Options,
		reqWidth: Int,
		reqHeight: Int,
		type: SubSampleCalculationType
	): Int {
		// Raw height and width of image
		val (height: Int, width: Int) = options.run { outHeight to outWidth }
		var inSampleSize = 1

		if (height > reqHeight || width > reqWidth) {

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.

			val condition = {
				val toBeSampleSize = inSampleSize * 2

				when(type) {
					SubSampleCalculationType.IF_WH_IS_LARGER -> {
						height / toBeSampleSize >= reqHeight && width / toBeSampleSize >= reqWidth
					}
					SubSampleCalculationType.IF_ANY_WH_IS_LARGER -> {
						height / toBeSampleSize >= reqHeight || width / toBeSampleSize >= reqWidth
					}
					is SubSampleCalculationType.MaxAlloc -> {
						val bytePixel = getBitmapPixelSize(options)
						val alloc = (height * width) * bytePixel
						val afterSample = alloc / toBeSampleSize
						afterSample >= type.maxByte
					}
				}
			}
				while (condition()) {
					inSampleSize *= 2
				}
			}
		return inSampleSize
	}

	private fun getBitmapPixelSize(options: Options): Int {
		return when {
			options.inPreferredConfig == Bitmap.Config.ALPHA_8 -> 1
			options.inPreferredConfig == Bitmap.Config.RGB_565 -> 2
			options.inPreferredConfig == Bitmap.Config.ARGB_4444 -> 2
			options.inPreferredConfig == Bitmap.Config.ARGB_8888 -> 4
			VersionHelper.hasOreo() && options.inPreferredConfig == Bitmap.Config.RGBA_F16 -> 8
			else -> throw IllegalArgumentException()
		}
	}
}
