package com.kylentt.musicplayer.common.android.bitmap.bitmapfactory

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import androidx.core.graphics.scale
import com.kylentt.musicplayer.common.android.bitmap.config.BitmapConfigInfo

object BitmapSampler {

	fun fillOptions(
		source: kotlin.ByteArray,
		options: BitmapFactory.Options = BitmapFactory.Options()
	): BitmapFactory.Options {
		return options
			.apply {
				inJustDecodeBounds = true
				BitmapFactory.decodeByteArray(source, 0, source.size, this)
				inJustDecodeBounds = false
			}
	}

	private sealed class CalculationTarget {

		data class SizeTarget(val width: Int, val height: Int) : CalculationTarget() {
			override fun shouldSubSample(options: Options, inSampleSize: Int): Boolean {
				return options.outHeight / (inSampleSize * 2) >= height
					&& options.outWidth / (inSampleSize * 2) >= width
			}
		}

		data class MaxAlloc(val maxByte: Int) : CalculationTarget() {
			override fun shouldSubSample(options: Options, inSampleSize: Int): Boolean {
				val pixels = (options.outHeight * options.outWidth) / (inSampleSize * inSampleSize)
				val currentAlloc = pixels * BitmapConfigInfo.getPixelSize(options.inPreferredConfig)
				return currentAlloc >= maxByte
			}
		}

		data class SizeTargetForceAlloc(
			val width: Int,
			val height: Int,
			val maxByte: Int
		): CalculationTarget() {
			val sizeTarget = SizeTarget(width, height)
			val maxAlloc = MaxAlloc(maxByte)
			override fun shouldSubSample(options: Options, inSampleSize: Int): Boolean {
				return sizeTarget.shouldSubSample(options, inSampleSize)
					|| maxAlloc.shouldSubSample(options, inSampleSize)
			}
		}

		abstract fun shouldSubSample(options: BitmapFactory.Options, inSampleSize: Int): Boolean
	}

	object ByteArray {

		fun toSampledBitmap(
			array: kotlin.ByteArray,
			offset: Int,
			buffer: Int,
			targetWidth: Int,
			targetHeight: Int
		): Bitmap? {
			val target = CalculationTarget.SizeTarget(targetWidth, targetHeight)
			return decodeToSampledBitmap(array, offset, buffer, target)
		}

		fun toSampledBitmap(
			array: kotlin.ByteArray,
			offset: Int,
			buffer: Int,
			maxByteAlloc: Int
		): Bitmap? {
			val target = CalculationTarget.MaxAlloc(maxByteAlloc)
			return decodeToSampledBitmap(array, offset, buffer, target)
		}

		fun toSampledBitmap(
			array: kotlin.ByteArray,
			offset: Int,
			buffer: Int,
			targetWidth: Int,
			targetHeight: Int,
			maxByteAlloc: Int
		): Bitmap? {
			val target = CalculationTarget.SizeTargetForceAlloc(targetWidth, targetHeight, maxByteAlloc)
			return decodeToSampledBitmap(array, offset, buffer, target)
		}

	}

	private fun decodeToSampledBitmap(
		source: kotlin.ByteArray,
		offset: Int,
		buffer: Int,
		target: CalculationTarget
	): Bitmap? {
		return BitmapFactory.Options()
			.run {
				fillOptions(source, this)
				inSampleSize = calculateInSampleSize(this, target)
				BitmapFactory.decodeByteArray(source, offset, buffer, this)
			}
	}

	private fun calculateInSampleSize(
		options: BitmapFactory.Options,
		calculationTarget: CalculationTarget
	): Int {
		var inSampleSize = 1

		while (calculationTarget.shouldSubSample(options, inSampleSize)) {
			inSampleSize *= 2
		}

		return inSampleSize
	}
}

