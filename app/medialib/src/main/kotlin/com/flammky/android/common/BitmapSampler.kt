package com.flammky.android.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import timber.log.Timber

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

	fun fillOptions(
		source: java.io.File,
		options: BitmapFactory.Options = BitmapFactory.Options()
	): BitmapFactory.Options {
		return options
			.apply {
				inJustDecodeBounds = true
				BitmapFactory.decodeFile(source.absolutePath, options)
				inJustDecodeBounds = false
			}
	}

	private sealed class CalculationTarget {

		object NONE : CalculationTarget() {
			override fun shouldSubSample(options: Options, inSampleSize: Int): Boolean = false
		}

		data class SizeTarget(val width: Int, val height: Int) : CalculationTarget() {
			override fun shouldSubSample(options: Options, inSampleSize: Int): Boolean {
				val nextLowerH = options.outHeight / (inSampleSize * 2) <= height
				val nextLowerW = options.outWidth / (inSampleSize * 2) <= width
				return !nextLowerH && !nextLowerW
			}
		}

		data class MaxAlloc(val maxByte: Int) : CalculationTarget() {
			override fun shouldSubSample(options: Options, inSampleSize: Int): Boolean {
				val currentPixels = (options.outHeight * options.outWidth) / (inSampleSize * inSampleSize)
				val currentAlloc = currentPixels * BitmapConfigInfo.getPixelSize(options.inPreferredConfig)
				return currentAlloc > maxByte
			}
		}

		data class SizeTargetForceAlloc(
			val width: Int,
			val height: Int,
			val maxByte: Int
		) : CalculationTarget() {
			val sizeTarget = SizeTarget(width, height)
			val maxAlloc = MaxAlloc(maxByte)
			override fun shouldSubSample(options: Options, inSampleSize: Int): Boolean {
				return sizeTarget.shouldSubSample(options, inSampleSize)
					|| maxAlloc.shouldSubSample(options, inSampleSize)
			}
		}

		abstract fun shouldSubSample(options: BitmapFactory.Options, inSampleSize: Int): Boolean
	}

	object File {
		fun toSampledBitmap(
			file: java.io.File
		): Bitmap? = decodeToSampledBitmap(file, CalculationTarget.NONE)

		private fun decodeToSampledBitmap(
			source: java.io.File,
			target: CalculationTarget
		): Bitmap? {
			return BitmapFactory.Options()
				.run {
					fillOptions(source, this)
					inSampleSize = calculateInSampleSize(this, target)
					BitmapFactory.decodeFile(source.absolutePath, this)
				}
		}
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
			val bm = decodeToSampledBitmap(array, offset, buffer, target)

			val alloc = bm?.allocationByteCount ?: 0

			Timber.d("toSampledBitmap from arr: ${array.size},to $target alloc: $alloc")

			return bm
		}

		fun toSampledBitmap(
			array: kotlin.ByteArray,
			offset: Int,
			buffer: Int,
			maxByteAlloc: Int
		): Bitmap? {
			val target = CalculationTarget.MaxAlloc(maxByteAlloc)
			val bm = decodeToSampledBitmap(array, offset, buffer, target)

			val alloc = bm?.allocationByteCount ?: 0

			Timber.d("toSampledBitmap from arr: ${array.size}, to max $maxByteAlloc alloc: $alloc")

			check(alloc <= maxByteAlloc)

			return bm
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

