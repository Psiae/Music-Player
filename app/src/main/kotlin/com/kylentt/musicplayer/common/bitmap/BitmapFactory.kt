package com.kylentt.musicplayer.common.bitmap

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import com.kylentt.musicplayer.core.sdk.VersionHelper

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

		data class SizeTarget(val width: Int, val height: Int) : SubSampleCalculationType() {
			override fun shouldSubSample(options: Options, inSampleSize: Int): Boolean {
				return options.outHeight / (inSampleSize * 2) >= height
					&& options.outWidth / (inSampleSize * 2) >= width
			}
		}

		data class MaxAlloc(val maxByte: Int) : SubSampleCalculationType() {
			override fun shouldSubSample(options: Options, inSampleSize: Int): Boolean {
				val bytePixelSize = getBitmapPixelSize(options)
				val pixels = (options.outHeight * options.outWidth) / (inSampleSize * inSampleSize)
				val currentAlloc = pixels * bytePixelSize
				return currentAlloc >= maxByte
			}
		}

		data class SizeTargetForceWithinAlloc(
			val width: Int,
			val height: Int,
			val maxByte: Int
		) : SubSampleCalculationType() {
			private val sizeTarget = SizeTarget(width, height)
			private val maxAlloc = MaxAlloc(maxByte)

			override fun shouldSubSample(options: Options, inSampleSize: Int): Boolean {
				return sizeTarget.shouldSubSample(options, inSampleSize)
					|| maxAlloc.shouldSubSample(options, inSampleSize)
			}
		}

		abstract fun shouldSubSample(options: BitmapFactory.Options, inSampleSize: Int): Boolean
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

		while (type.shouldSubSample(options, inSampleSize)) {
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
