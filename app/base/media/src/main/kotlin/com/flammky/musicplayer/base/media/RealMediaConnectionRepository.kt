package com.flammky.musicplayer.base.media

import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.kotlin.common.sync.sync
import com.flammky.musicplayer.base.media.RealMediaConnectionRepository.MapObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.Executors

class RealMediaConnectionRepository(
	private val dispatchers: AndroidCoroutineDispatchers
) : MediaConnectionRepository {

	private val mutex = Mutex()
	private val eventDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
	private val coroutineScope = CoroutineScope(eventDispatcher + SupervisorJob())

	private val metadataObservers = mutableMapOf<String, MutableList<MapObserver<String, MediaMetadata>>>()
	private val metadataMap = mutableMapOf<String, MediaMetadata>()

	private val artworkObservers = mutableMapOf<String, MutableList<MapObserver<String, Any>>>()
	private val artworkMap = mutableMapOf<String, Any?>()

	override fun getMetadata(id: String): MediaMetadata? {
		return metadataMap.sync { get(id) }
	}

	override fun observeMetadata(id: String): Flow<MediaMetadata?> {
		return callbackFlow {
			val observer = MapObserver<String, MediaMetadata> { key, value ->
				check(key == id)
				trySend(value)
			}
			withContext(dispatchers.io) {
				addMetadataObserver(id, observer)
				send(getMetadata(id))
			}
			awaitClose {
				removeMetadataObserver(id, observer)
			}
		}
	}

	private fun addMetadataObserver(id: String, observer: MapObserver<String, MediaMetadata>) {
		metadataObservers.sync { getOrPut(id) { mutableListOf() }.add(observer) }
	}
	private fun removeMetadataObserver(id: String, observer: MapObserver<String, MediaMetadata>) {
		metadataObservers.sync { get(id)?.remove(observer) }
	}

	override fun provideMetadata(id: String, metadata: MediaMetadata) {
		metadataMap.sync {
			put(id, metadata)
			dispatchMetadataChange(id, metadata)
		}
	}

	override fun evictMetadata(id: String) {
		metadataMap.sync {
			if (containsKey(id)) {
				val get = get(id)
				remove(id)
				if (get != null) dispatchMetadataChange(id, null)
			}
		}
	}

	override fun getArtwork(id: String): Any? {
		return artworkMap.sync { get(id) }
	}

	override fun observeArtwork(id: String): Flow<Any?> {
		return callbackFlow {
			val observer = MapObserver<String, Any> { key, value ->
				check(key == id)
				trySend(value)
			}
			withContext(dispatchers.io) {
				addArtworkObserver(id, observer)
				send(getArtwork(id))
			}
			awaitClose {
				removeArtworkObserver(id, observer)
			}
		}
	}

	private fun addArtworkObserver(id: String, observer: MapObserver<String, Any>) {
		artworkObservers.sync { getOrPut(id) { mutableListOf() }.add(observer) }
	}
	private fun removeArtworkObserver(id: String, observer: MapObserver<String, Any>) {
		artworkObservers.sync { get(id)?.remove(observer) }
	}

	override fun provideArtwork(id: String, artwork: Any?) {
		artworkMap.sync {
			put(id, artwork)
			dispatchArtworkChange(id, artwork)
		}
	}

	override fun evictArtwork(id: String) {
		artworkMap.sync {
			if (containsKey(id)) {
				val get = get(id)
				remove(id)
				if (get != null) dispatchArtworkChange(id, null)
			}
		}
	}



	private fun dispatchMetadataChange(id: String, metadata: MediaMetadata?) {
		val observers = metadataObservers.sync { get(id) } ?: return
		coroutineScope.launch(eventDispatcher) {
			mutex.withLock { observers.forEach { observer -> observer.onChanged(id, metadata) } }
		}
	}

	private fun dispatchArtworkChange(id: String, artwork: Any?) {
		val observers = artworkObservers.sync { get(id) } ?: return
		coroutineScope.launch(eventDispatcher) {
			mutex.withLock { observers.forEach { observer -> observer.onChanged(id, artwork) } }
		}
	}

	private fun interface MapObserver<K: Any, V> {
		fun onChanged(key: K, value: V?)
	}
}
