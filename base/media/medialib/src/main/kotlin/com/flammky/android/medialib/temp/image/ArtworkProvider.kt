package com.flammky.android.medialib.temp.image

import android.net.Uri
import androidx.annotation.IntRange
import androidx.annotation.Px

// rewrite ?
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
		val uri: Uri?,
		@Px val minimumWidth: Int,
		@Px val minimumHeight: Int,
		val diskCacheAllowed: Boolean,
		val memoryCacheAllowed: Boolean,
		val storeDiskCacheAllowed: Boolean,
		val storeMemoryCacheAllowed: Boolean,
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
			var diskCacheAllowed: Boolean = true
				private set

			/**
			 * Whether Memory Cache is allowed
			 */
			var memoryCacheAllowed: Boolean = true
				private set

			/**
			 * Whether Storing to Disk Cache is allowed
			 */
			var storeDiskCacheAllowed: Boolean = true
				private set

			/**
			 * Whether Storing to Memory Cache is allowed
			 */
			var storeMemoryCacheAllowed: Boolean = true
				private set

			var uri: Uri? = null
				private set

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

			fun setStoreDiskCacheAllowed(allowed: Boolean): Builder<R> = apply {
				storeDiskCacheAllowed = allowed
			}

			fun setStoreMemoryCacheAllowed(allowed: Boolean): Builder<R> = apply {
				storeMemoryCacheAllowed = allowed
			}

			fun setUri(uri: Uri?) = apply {
				this.uri = uri
			}

			fun build(): Request<R> = Request(
				id = id,
				cls = cls,
				uri = uri,
				minimumWidth = minimumWidth,
				minimumHeight = minimumHeight,
				diskCacheAllowed = diskCacheAllowed,
				memoryCacheAllowed = memoryCacheAllowed,
				storeDiskCacheAllowed = storeDiskCacheAllowed,
				storeMemoryCacheAllowed = storeMemoryCacheAllowed
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
