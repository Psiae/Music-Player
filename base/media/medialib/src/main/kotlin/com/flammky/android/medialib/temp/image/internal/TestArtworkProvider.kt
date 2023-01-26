package com.flammky.android.medialib.temp.image.internal

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.flammky.android.common.BitmapSampler
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.android.medialib.temp.provider.mediastore.api28.MediaStore28
import com.flammky.common.media.audio.AudioFile
import com.flammky.musicplayer.base.media.MediaConstants
import com.flammky.musicplayer.base.media.r.MediaMetadataCacheRepository
import com.flammky.musicplayer.core.common.sync
import kotlinx.coroutines.*
import timber.log.Timber

// rewrite
class TestArtworkProvider(
	private val context: Context,
	private val repo: MediaMetadataCacheRepository
) : ArtworkProvider {
	private val dispatcher = AndroidCoroutineDispatchers.DEFAULT
	private val ioScope = CoroutineScope(dispatcher.io.limitedParallelism(24) + SupervisorJob())
	private val rawJobMap = mutableMapOf<Uri, Deferred<Bitmap?>>()

	suspend fun removeCacheForId(id: String, mem: Boolean, disk: Boolean) {
		if (mem) repo.evictArtwork(id + "_raw")
		if (disk) {}
	}

	override fun <R> request(request: ArtworkProvider.Request<R>): ArtworkProvider.ListenableResult<R> {
		val listenable = ListenableResult<R>(request.id)
		doWork(request, listenable)
		return listenable
	}

	private fun <R> doWork(request: ArtworkProvider.Request<R>, listenable: ListenableResult<R>) {
		// other formats are to do
		if (request.cls != Bitmap::class.java) {
			val ex = IllegalArgumentException("Unsupported Class")
			listenable.addException(ex)
			listenable.setResult(null)
		}
		ioScope.launch {
			if (request.memoryCacheAllowed) {
				repo.getArtwork(
					request.id + "_raw"
				)?.let {
					listenable.setResult(it as? R)
					// maybe check restore cache?
					return@launch
				}
			}
			val resolvedUri = when {
				request.id.startsWith("MediaStore") || request.id.startsWith("MEDIASTORE") -> {
					ContentUris.withAppendedId(MediaStore28.Audio.EXTERNAL_CONTENT_URI, request.id.takeLastWhile { it.isDigit() }.toLong())
				}
				request.uri?.scheme == ContentResolver.SCHEME_CONTENT || request.uri?.scheme == ContentResolver.SCHEME_FILE -> request.uri
				else -> null
			}

			Timber.d("TestArtworkProvider, resolvedUri: $resolvedUri")

			val embed = resolvedUri?.let { uri ->
				rawJobMap.sync {
					getOrPut(uri) {
						async {
							AudioFile.Builder(context, uri).build().let { af ->
								af.file?.delete()
								val data = af.imageData
								Timber.d("AF($resolvedUri) data: ${data?.size}")
								if (data != null && data.isNotEmpty()) {
									BitmapSampler.ByteArray.toSampledBitmap(data, 0, data.size, 500, 500)
										?.let { bitmap ->
											bitmap.also {
												if (request.storeMemoryCacheAllowed) {
													repo.provideArtwork(request.id + "_raw", it)
												}
											}
										}
								} else if (af.ex == null) {
									if (request.storeMemoryCacheAllowed) {
										repo.provideArtwork(request.id + "_raw", MediaConstants.NO_ARTWORK)
									}
									MediaConstants.NO_ARTWORK_BITMAP
								} else {
									null
								}
							}
						}
					}
				}.await().also { rawJobMap.sync { remove(uri) } }
			}
			Timber.d("TestArtworkProvider, result: $embed")
			listenable.setResult(embed as? R)
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

		fun addException(exception: Exception) {
			exceptions.sync { add(exception) }
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
