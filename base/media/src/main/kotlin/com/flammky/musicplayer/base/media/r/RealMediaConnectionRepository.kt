package com.flammky.musicplayer.base.media.r

import android.content.Context
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.kotlin.common.lazy.LazyConstructor
import com.flammky.kotlin.common.lazy.LazyConstructor.Companion.valueOrNull
import com.flammky.musicplayer.base.media.r.RealMediaConnectionRepository.MapObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

class RealMediaConnectionRepository(
	private val context: Context,
	private val dispatchers: AndroidCoroutineDispatchers
) : MediaConnectionRepository {

	// should we use Mutex or Single Parallelism on Dispatchers.IO ?

	private val metadataMutex = Mutex()
	private val metadataObservers = mutableMapOf<String, MutableList<MapObserver<String, MediaMetadata>>>()
	private val metadataMap = mutableMapOf<String, MediaMetadata>()

	private val artLock = Any()
	private val artMutex = Mutex()
	private val artworkObservers = mutableMapOf<String, MutableList<MapObserver<String, Any>>>()
	private val artworkMap = mutableMapOf<String, Any?>()


	private val ioScope = CoroutineScope(dispatchers.io + SupervisorJob())

	private val mainScope = CoroutineScope(dispatchers.main + SupervisorJob())

	override suspend fun getMetadata(id: String): MediaMetadata? {
		return metadataMutex.withLock { metadataMap[id] }
	}

	override fun getMetadataBlocking(id: String): MediaMetadata? {
		return metadataMutex.withLockBlocking { metadataMap[id] }
	}

	override suspend fun observeMetadata(id: String): Flow<MediaMetadata?> = callbackFlow {
		val observer = MapObserver<String, MediaMetadata> { key, value ->
			check(key == id)
			send(value)
		}
		addMetadataObserver(id, observer)
		send(getMetadata(id))
		awaitClose {
			ioScope.launch { removeMetadataObserver(id, observer) }
		}
	}

	private suspend fun addMetadataObserver(id: String, observer: MapObserver<String, MediaMetadata>) {
		metadataMutex.withLock {
			metadataObservers.getOrPut(id) { mutableListOf() }.add(observer)
		}
	}

	private suspend fun removeMetadataObserver(id: String, observer: MapObserver<String, MediaMetadata>) {
		metadataMutex.withLock {
			metadataObservers[id]?.remove(observer)
		}
	}

	override suspend fun provideMetadata(id: String, metadata: MediaMetadata) {
		metadataMutex.withLock {
			metadataMap[id] = metadata
			dispatchMetadataChange(id, metadata)
		}
	}

	override suspend fun silentProvideMetadata(id: String, metadata: MediaMetadata) {
		metadataMutex.withLock {
			metadataMap[id] = metadata
		}
	}


	override suspend fun evictMetadata(id: String) {
		metadataMutex.withLock {
			if (metadataMap.remove(id) != null) dispatchMetadataChange(id, null)
		}
	}

	override suspend fun silentEvictMetadata(id: String) {
		metadataMutex.withLock {
			metadataMap.remove(id)
		}
	}

	override suspend fun getArtwork(id: String): Any? {
		return artMutex.withLock { artworkMap[id] }
	}

	override suspend fun observeArtwork(id: String): Flow<Any?> = callbackFlow {
		val observer = MapObserver<String, Any> { key, value ->
			check(key == id)
			send(value)
		}
		send(getArtwork(id))
		addArtworkObserver(id, observer)
		awaitClose {
			Timber.d("observeArtwork closed for $id, $observer")
			ioScope.launch { removeArtworkObserver(id, observer) }
		}
	}

	private suspend fun addArtworkObserver(id: String, observer: MapObserver<String, Any>) {
		artMutex.withLock {
			artworkObservers.getOrPut(id) { mutableListOf() }.add(observer)
		}
	}
	private suspend fun removeArtworkObserver(id: String, observer: MapObserver<String, Any>) {
		artMutex.withLock {
			artworkObservers[id]?.remove(observer)?.let {
				Timber.d("removed Artwork Observer $id, $observer" +
					"\nremaining: (${artworkObservers[id]?.size ?: 0}) ${artworkObservers[id]?.joinToString()}")
			}
		}
	}

	override suspend fun provideArtwork(id: String, artwork: Any?) {
		artMutex.withLock {
			artworkMap[id] = artwork
			dispatchArtworkChange(id, artwork)
		}
	}

	override suspend fun evictArtwork(id: String) {
		artMutex.withLock {
			if (artworkMap.remove(id) != null) dispatchArtworkChange(id, null)
		}
	}

	override suspend fun silentProvideArtwork(id: String, artwork: Any?) {
		artMutex.withLock {
			artworkMap[id] = artwork
		}
	}

	override suspend fun silentEvictArtwork(id: String) {
		artMutex.withLock {
			artworkMap.remove(id)
		}
	}

	private suspend fun dispatchMetadataChange(id: String, metadata: MediaMetadata?) {
		metadataObservers[id]?.forEach { observer -> observer.onChanged(id, metadata) }
	}

	private suspend fun dispatchArtworkChange(id: String, artwork: Any?) {
		artworkObservers[id]?.forEach { observer -> observer.onChanged(id, artwork) }
	}

	private fun <R> Mutex.withLockBlocking(block: () -> R): R {
		return if (tryLock()) {
			try {
				block()
			} finally {
				unlock()
			}
		} else {
			runBlocking {
				withLock(action = block)
			}
		}
	}

	private fun interface MapObserver<K: Any, V> {
		suspend fun onChanged(key: K, value: V?)
	}

	companion object {
		private val SINGLETON = LazyConstructor<RealMediaConnectionRepository>()
		fun provide(ctx: Context, dispatchers: AndroidCoroutineDispatchers) = SINGLETON.construct {
			RealMediaConnectionRepository(ctx, dispatchers)
		}

		fun get() = SINGLETON.valueOrNull() ?: error("RealMediaConnectionRepository was not provided")
	}
}
