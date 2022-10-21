package com.flammky.musicplayer.base.media.mediaconnection

import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.musicplayer.base.media.mediaconnection.RealMediaConnectionRepository.MapObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.Executors

class RealMediaConnectionRepository(
	private val dispatchers: AndroidCoroutineDispatchers
) : MediaConnectionRepository {

	private val mutex = Mutex()
	private val singleThreadWorker = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
	private val coroutineScope = CoroutineScope(singleThreadWorker + SupervisorJob())

	private val metadataObservers = mutableMapOf<String, MutableList<MapObserver<String, MediaMetadata>>>()
	private val metadataMap = mutableMapOf<String, MediaMetadata>()

	private val artworkObservers = mutableMapOf<String, MutableList<MapObserver<String, Any>>>()
	private val artworkMap = mutableMapOf<String, Any?>()

	override suspend fun getMetadata(id: String): MediaMetadata? {
		return withContext(singleThreadWorker) { metadataMap[id] }
	}

	override fun observeMetadata(id: String): Flow<MediaMetadata?> = callbackFlow {
		val observer = MapObserver<String, MediaMetadata> { key, value ->
			check(key == id)
			trySend(value)
		}
		withContext(singleThreadWorker) {
			addMetadataObserver(id, observer)
			send(getMetadata(id))
		}
		awaitClose {
			removeMetadataObserver(id, observer)
		}
	}.flowOn(dispatchers.io)

	private fun addMetadataObserver(id: String, observer: MapObserver<String, MediaMetadata>) {
		coroutineScope.launch(singleThreadWorker) {
			metadataObservers.getOrPut(id) { mutableListOf() }.add(observer)
		}
	}

	private fun removeMetadataObserver(id: String, observer: MapObserver<String, MediaMetadata>) {
		coroutineScope.launch(singleThreadWorker) {
			metadataObservers[id]?.remove(observer)
		}
	}

	override fun provideMetadata(id: String, metadata: MediaMetadata) {
		coroutineScope.launch(singleThreadWorker) {
			metadataMap.run {
				put(id, metadata)
				dispatchMetadataChange(id, metadata)
			}
		}
	}

	override fun silentProvideMetadata(id: String, metadata: MediaMetadata) {
		coroutineScope.launch(singleThreadWorker) {
			metadataMap[id] = metadata
		}
	}


	override fun evictMetadata(id: String) {
		coroutineScope.launch(singleThreadWorker) {
			metadataMap.run {
				if (containsKey(id)) {
					val get = get(id)
					remove(id)
					if (get != null) dispatchMetadataChange(id, null)
				}
			}
		}
	}

	override fun silentEvictMetadata(id: String) {
		coroutineScope.launch(singleThreadWorker) {
			metadataMap.remove(id)
		}
	}

	override suspend fun getArtwork(id: String): Any? {
		return withContext(singleThreadWorker) { artworkMap[id] }
	}

	override fun observeArtwork(id: String): Flow<Any?> = callbackFlow {
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
	}.flowOn(dispatchers.io)

	private fun addArtworkObserver(id: String, observer: MapObserver<String, Any>) {
		coroutineScope.launch(singleThreadWorker) {
			artworkObservers.getOrPut(id) { mutableListOf() }.add(observer)
		}
	}
	private fun removeArtworkObserver(id: String, observer: MapObserver<String, Any>) {
		coroutineScope.launch(singleThreadWorker) {
			artworkObservers[id]?.remove(observer)
		}
	}

	override fun provideArtwork(id: String, artwork: Any?) {
		coroutineScope.launch(singleThreadWorker) {
			artworkMap[id] = artwork
			dispatchArtworkChange(id, artwork)
		}
	}

	override fun evictArtwork(id: String) {
		coroutineScope.launch(singleThreadWorker) {
			artworkMap.run {
				if (containsKey(id)) {
					val get = get(id)
					remove(id)
					if (get != null) dispatchArtworkChange(id, null)
				}
			}
		}
	}

	override fun silentProvideArtwork(id: String, artwork: Any?) {
		coroutineScope.launch {
			artworkMap[id] = artwork
		}
	}

	override fun silentEvictArtwork(id: String) {
		coroutineScope.launch(singleThreadWorker) {
			artworkMap.remove(id)
		}
	}



	private fun dispatchMetadataChange(id: String, metadata: MediaMetadata?) {
		coroutineScope.launch(singleThreadWorker) {
			val observers = metadataObservers[id] ?: return@launch
			observers.forEach { observer -> observer.onChanged(id, metadata) }
		}
	}

	private fun dispatchArtworkChange(id: String, artwork: Any?) {
		coroutineScope.launch(singleThreadWorker) {
			val observers = artworkObservers[id] ?: return@launch
			observers.forEach { observer -> observer.onChanged(id, artwork) }
		}
	}



	private fun interface MapObserver<K: Any, V> {
		fun onChanged(key: K, value: V?)
	}
}
