package com.flammky.android.medialib.temp.image

import androidx.annotation.IntRange
import androidx.annotation.Px

interface ArtworkProvider {

	fun <R> request(request: Request<R>): ListenableResult<R>

	interface ListenableResult<R> {

		fun isDone(): Boolean

		/**
		 * Await the Result
		 */
		suspend fun await(): RequestResult<R>

		/*fun asFuture(): ListenableFuture<RequestResult>*/

		/**
		 * Callback
		 */
		fun onResult(block: (RequestResult<R>) -> Unit)
	}

	class Request <R> private constructor(
		val id: String,
		val cls: Class<R>,
		@Px val minimumWidth: Int,
		@Px val minimumHeight: Int,
		val diskCacheAllowed: Boolean,
		val memoryCacheAllowed: Boolean,
	) {

		class Builder <R> (val id: String, val cls: Class<R>) {

			/**
			 * The minimum width of the Image, in pixels
			 */
			@Px
			var minimumWidth: Int = 0
				private set

			/**
			 * The minimum height of the Image, in pixels
			 */
			@Px
			var minimumHeight: Int = 0
				private set

			/**
			 * Whether Disk Cache is allowed
			 */
			private var diskCacheAllowed: Boolean = true

			/**
			 * Whether Memory Cache is allowed
			 */
			private var memoryCacheAllowed: Boolean = true


			fun setMinimumSize(@Px @IntRange(from = 0, to = MAX_WIDTH) size: Int): Builder<R> =
				apply {
					setMinimumSize(size, size)
				}

			fun setMinimumSize(
				@Px @IntRange(from = 0, to = MAX_WIDTH) width: Int,
				@Px @IntRange(from = 0, to = MAX_HEIGHT) height: Int,
			): Builder<R> = apply {
				setMinimumWidth(width)
				setMinimumHeight(height)
			}

			fun setMinimumWidth(@Px @IntRange(from = 0, to = MAX_WIDTH) width: Int): Builder<R> =
				apply {
					require(width in 0..MAX_WIDTH) {
						"Invalid Width, $width is out of bounds"
					}
					minimumWidth = width
				}

			fun setMinimumHeight(@Px @IntRange(from = 0, to = MAX_HEIGHT) height: Int): Builder<R> =
				apply {
					require(height in 0..MAX_HEIGHT) {
						"Invalid Height, $height is out of bounds"
					}
					minimumHeight = height
				}

			fun setDiskCacheAllowed(allowed: Boolean): Builder<R> = apply {
				diskCacheAllowed = allowed
			}

			fun setMemoryCacheAllowed(allowed: Boolean): Builder<R> = apply {
				memoryCacheAllowed = allowed
			}

			fun build(): Request<R> = Request(
				id = id,
				cls = cls,
				minimumWidth = minimumWidth,
				minimumHeight = minimumHeight,
				diskCacheAllowed = diskCacheAllowed,
				memoryCacheAllowed = memoryCacheAllowed,
			)
		}

		companion object {
			const val MAX_WIDTH = Int.MAX_VALUE.toLong()
			const val MAX_HEIGHT = Int.MAX_VALUE.toLong()
		}
	}

	interface RequestResult<R> {
		fun get(): R?
		fun isSuccessful(): Boolean
	}
}
