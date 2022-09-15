package com.flammky.android.medialib.temp.image.internal

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import com.flammky.android.medialib.temp.cache.lru.LruCache
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.android.common.kotlin.coroutines.AndroidCoroutineDispatchers
import com.flammky.common.kotlin.generic.sync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

class TestArtworkProvider(private val lru: LruCache<String, Bitmap>) : ArtworkProvider {

	private val ioDispatcher = AndroidCoroutineDispatchers.DEFAULT.io
	private val scope = CoroutineScope(ioDispatcher)

	override fun <R> request(request: ArtworkProvider.Request<R>): ArtworkProvider.ListenableResult<R> {
		val listenable = ListenableResult<R>(request.id)
		doWork(request, listenable)
		return listenable
	}

	private fun <R> doWork(request: ArtworkProvider.Request<R>, listenable: ListenableResult<R>) {
		scope.launch {
			if (request.memoryCacheAllowed) {
				val fromLru = lru.get(request.id)

				if (fromLru != null
					&& fromLru.height >= request.minimumHeight
					&& fromLru.width >= request.minimumWidth
				) {
					// should process accordingly
					return@launch listenable.setResult(fromLru as R)
				}
			}
			listenable.setResult(null)
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
				return if (result !== UNSET && result != null) result as R else null
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
			if (isDone()) return r
			return suspendCancellableCoroutine { cont ->
				sync {
					if (isDone()) {
						cont.resume(r) {}
					} else {
						awaiters.add { cont.resume(r) {} }
					}
				}
			}
		}

		override fun onResult(block: (ArtworkProvider.RequestResult<R>) -> Unit) {
			sync {
				if (isDone()) return block(r)

				val looper = Looper.myLooper()

				if (looper != null) {
					val handler = Handler(looper)
					awaiters.add { handler.post { block(r) } }
				} else {
					awaiters.add { block(r) }
				}
			}
		}
	}
}
