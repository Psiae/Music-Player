package com.flammky.android.medialib.temp.image

import androidx.annotation.IntRange
import androidx.annotation.Px

interface ImageProvider {

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


			fun setMinimumSize(@Px @IntRange(from = 0, to = 2147483647) size: Int): Builder<R> {
				return setMinimumSize(size, size)
			}

			fun setMinimumSize(
				@Px @IntRange(from = 0, to = 2147483647) width: Int,
				@Px @IntRange(from = 0, to = 2147483647) height: Int,
			): Builder<R> {
				setMinimumWidth(width)
				setMinimumHeight(height)
				return this
			}

			fun setMinimumWidth(@Px @IntRange(from = 0, to = 2147483647) width: Int): Builder<R> {
				require(width in 0..2147483647) {
					"Invalid Width, $width is out of bounds"
				}
				minimumWidth = width
				return this
			}

			fun setMinimumHeight(@Px @IntRange(from = 0, to = 2147483647) height: Int): Builder<R> {
				require(height in 0..2147483647) {
					"Invalid Height, $height is out of bounds"
				}
				minimumHeight = height
				return this
			}

			fun setDiskCacheAllowed(allowed: Boolean): Builder<R> {
				diskCacheAllowed = allowed
				return this
			}

			fun setMemoryCacheAllowed(allowed: Boolean): Builder<R> {
				memoryCacheAllowed = allowed
				return this
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
	}

	interface RequestResult<R> {
		fun get(): R?
		fun isSuccessful(): Boolean
	}
}
