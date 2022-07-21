package com.kylentt.musicplayer.common.bitmap

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import android.view.View
import com.kylentt.musicplayer.core.sdk.VersionHelper
import timber.log.Timber

object BitmapFactoryHelper {

	fun decodeByteArrayToSampledBitmap(
		array: ByteArray,
		offset: Int,
		buffer: Int,
		sampleCalculationType: SubSampleCalculationType
	): Bitmap? {
		return BitmapFactory.Options()
			.run {

				inJustDecodeBounds = true
				decodeByteArray(array, offset, buffer, this)

				// Calculate inSampleSize
				inSampleSize = calculateInSampleSize(this, sampleCalculationType)

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
		data class SizeTarget(val width: Int, val height: Int) : SubSampleCalculationType()
		data class MaxAlloc(val maxByte: Int) : SubSampleCalculationType()
	}

	// Copied from docs
	private fun calculateInSampleSize(
		options: BitmapFactory.Options,
		type: SubSampleCalculationType
	): Int {
		// Raw height and width of image
		val (height: Int, width: Int) = options.run { outHeight to outWidth }
		// Raw SampleSize
		var inSampleSize = 1

		val shouldPower = {
			when(type) {
				is SubSampleCalculationType.SizeTarget -> {
					height / (inSampleSize * 2) >= type.height && width / (inSampleSize * 2) >= type.width
				}
				is SubSampleCalculationType.MaxAlloc -> {
					val bytePixelSize = getBitmapPixelSize(options)
					val currentAlloc = (((height * width) / (inSampleSize * inSampleSize)) * bytePixelSize)
					currentAlloc >= type.maxByte
				}
			}
		}

		while (shouldPower()) {
			inSampleSize *= 2
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
