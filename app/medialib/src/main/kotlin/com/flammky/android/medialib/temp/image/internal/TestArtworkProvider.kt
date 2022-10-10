package com.flammky.android.medialib.temp.image.internal

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import com.flammky.android.app.AppDelegate
import com.flammky.android.common.BitmapSampler
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.temp.cache.lru.LruCache
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.android.medialib.temp.provider.mediastore.api28.MediaStore28
import com.flammky.common.media.audio.AudioFile
import com.flammky.kotlin.common.sync.sync
import kotlinx.coroutines.*

class TestArtworkProvider(
	private val context: Context,
	private val lru: LruCache<String, Bitmap>
) : ArtworkProvider {
	private val dispatcher = AndroidCoroutineDispatchers.DEFAULT
	private val scope = CoroutineScope(dispatcher.io + SupervisorJob())
	private val cacheManager = AppDelegate.cacheManager

	suspend fun removeCacheForId(id: String, mem: Boolean, disk: Boolean) {
		if (mem) lru.remove(id)
		if (disk) cacheManager.removeImageFromCache(id, "TestArtworkProvider")
	}

	override fun <R> request(request: ArtworkProvider.Request<R>): ArtworkProvider.ListenableResult<R> {
		val listenable = ListenableResult<R>(request.id)
		doWork(request, listenable)
		return listenable
	}

	private fun <R> doWork(request: ArtworkProvider.Request<R>, listenable: ListenableResult<R>) {
		require(request.cls == Bitmap::class.java) {
			"Unsupported"
		}
		scope.launch {
			if (request.memoryCacheAllowed) {
				lru.get(request.id)?.let {
					listenable.setResult(it as? R)
					return@launch
				}
			}

			if (request.diskCacheAllowed) {
				cacheManager.retrieveImageCacheFile(request.id, "TestArtworkProvider")?.let { file ->
					BitmapFactory.decodeFile(file.absolutePath)?.let {
						listenable.setResult(it as? R)
						return@launch
					} ?: run { file.delete() }
				}
			}

			val resolvedUri = when {
				request.id.startsWith("MediaStore") -> {
					ContentUris.withAppendedId(MediaStore28.Audio.EXTERNAL_CONTENT_URI, request.id.takeLastWhile { it.isDigit() }.toLong())
				}
				else -> null
			}

			val embed = resolvedUri?.let { uri ->
				AudioFile.Builder(context, uri).build().let { af ->
					af.file?.delete()
					val data = af.imageData
					if (data != null && data.isNotEmpty()) {
						BitmapSampler.ByteArray.toSampledBitmap(data, 0, data.size, 2000000)
							?.let { bitmap ->
								bitmap.also {
									if (request.storeMemoryCacheAllowed) {
										lru.put(request.id, it)
									}
									if (request.storeDiskCacheAllowed) {
										cacheManager.insertImageToCache(it, request.id, "TestArtworkProvider")
									}
								}
							}
					} else {
						null
					}
				}
			}
			listenable.setResult(embed as R?)
		}
	}

	private class ListenableResult<R>(val id: String) : ArtworkProvider.ListenableResult<R> {
		private object UNSET
		private val awaiters = mutableListOf<Runnable>()
		private val exceptions = mutableListOf<Exception>()

		private var result: Any? = UNSET

		private val r = object : ArtworkProvider.RequestResult<R> {
			override fun isSuccessful(): Boolean = exceptions.isEmpty()
			override fun get(): R? {
				check(result !== UNSET)
				return result as? R
			}
		}

		fun setResult(result: R?) {
			sync {
				require(this.result === UNSET) {}
				this.result = result
				awaiters.forEach { it.run() }
			}
		}

		override fun isDone(): Boolean = result !== UNSET

		@OptIn(ExperimentalCoroutinesApi::class)
		override suspend fun await(): ArtworkProvider.RequestResult<R> {
			return if (isDone()) {
				r
			} else {
				suspendCancellableCoroutine { cont ->
					sync {
						if (isDone()) {
							cont.resume(r) {}
						} else {
							awaiters.add { cont.resume(r) {} }
						}
					}
				}
			}
		}

		override fun onResult(block: (ArtworkProvider.RequestResult<R>) -> Unit) {
			sync {
				if (isDone()) {
					block(r)
				} else {
					Looper.myLooper()
						?.let { looper ->
							awaiters.add { Handler(looper).post { block(r) } }
						}
						?: awaiters.add { block(r) }
				}
			}
		}
	}
}
